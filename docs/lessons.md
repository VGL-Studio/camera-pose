# SỔ BÀI HỌC

Sổ này ghi những điều nhóm và AI học được khi làm việc, và đã làm gì với chúng. Đây là đầu vào của buổi retro sau mỗi phiên tự chạy (auto-run.md, mục 6).

## Cách ghi

- Mỗi bài học một mục, mã `L-NNN` tăng dần. Chỉ thêm mục mới hoặc cập nhật dòng **Trạng thái**, **Áp dụng**, **Kiểm chứng** của mục cũ; không sửa phần mô tả.
- Trạng thái:
  - `proposed`: AI hoặc người vừa ghi, chưa ai quyết định.
  - `accepted`: retro đồng ý làm, chưa làm.
  - `applied`: đã làm, ghi PR ở dòng **Áp dụng**.
  - `verified`: phiên sau cho thấy cải tiến có hiệu quả, ghi bằng chứng ở dòng **Kiểm chứng**.
  - `rejected`: retro quyết định không làm, ghi lý do.
- Loại: `quy trình` (auto-run, docs/CLAUDE.md, mẫu tài liệu), `môi trường` (cloud, CI, script), `quy tắc code` (CLAUDE.md), `plan` (cách viết plan, test), `rủi ro` (thang điểm).
- Phạm vi: `chung` (đúng với mọi app, làm trong repo dự án mẫu trước), `app` (chỉ app này).
- Chỉ người chuyển trạng thái sang `accepted`, `rejected` hay `verified`. AI chỉ ghi `proposed`, và ghi `applied` khi PR cải tiến đã merge.

Bài học `chung` ghi ở app thì được đưa về dự án mẫu qua issue `from-app` (`docs/ops/from-app.md`). Ở dự án mẫu, bài học từ app ghi nguồn là app và số issue. Các app đã tạo trước đó đọc sổ của dự án mẫu để chép những cải tiến cần thiết về (chưa có cách đồng bộ tự động).

## Bài học

<!-- Các bài học ghi dưới dòng này. -->

Bài học trước ngày 2026-10-21 nằm trong sổ bài học của repo dự án mẫu (commit 9475da7).

### L-001: Kỹ thuật đo lần đầu dùng phải chạy thử trước khi duyệt plan

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, T-007.
- **Chuyện gì:** plan T-007 dựa vào `captureToImage` trong Robolectric `NATIVE` để đo màu điểm ảnh; kỹ thuật này hết thời gian chờ, task dừng ở bước 3.
- **Bằng chứng:** `ComposeTimeoutException` tại `WindowCapture.forceRedraw`; nhật ký plan T-007.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: kỹ thuật hay thư viện lần đầu dùng trong dự án phải được chạy thử bằng một test tạm không commit trước khi duyệt, kết quả ghi vào mục 4 (mở rộng L-029).
- **Áp dụng:** app: PR #39 (mẫu plan mục 4.1, `plan-check.py` quy tắc 7). dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-041). Trước đó: đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#77.
- **Kiểm chứng:**

### L-002: Bí mật Firebase staging là file giả, cuối phiên không phát được

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01.
- **Chuyện gì:** `STAGING_GOOGLE_SERVICES_JSON` vẫn là file giả; `device-session.yml` và `staging.yml` lỗi ở bước cấu hình, sau khi cả phiên đã chạy.
- **Bằng chứng:** `device-session` 39589532354 và 39589890609, `staging` 39589924059: `processStagingReleaseGoogleServices: No matching client found for package name '...guide.staging'`.
- **Đề xuất sửa:** `docs/auto-run.md`, mục 2: kiểm tra đầu phiên có một điều kiện "bí mật Firebase staging có app staging", để lỗi lộ ra trước khi phiên bắt đầu.
- **Áp dụng:** plan T-030, PR #50: `scripts/preflight.sh` kiểm bí mật Firebase staging mỗi sáng (`preflight.yml`) và ngay trước khi `start-run.yml` kích hoạt routine; lỗi thì issue `setup-broken` chặn bắt đầu phiên.
- **Kiểm chứng:**

### L-003: Quy tắc đếm `captureRoboImage(` của `check.sh` chặn ảnh tạm để đo điểm ảnh

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-02, T-007.
- **Chuyện gì:** plan T-007 phiên bản 2 đo màu điểm ảnh từ ảnh `captureRoboImage` ghi vào `build/`; code và test qua, nhưng `scripts/check.sh` đòi mọi lần gọi `captureRoboImage(` ghi `"src/test/screenshots/<Tên>.png"`, nên cổng chất lượng không qua và task dừng ở bước 5.
- **Bằng chứng:** `scripts/check.sh` dòng 45 đến 55; log `::error:::core:designsystem: mọi captureRoboImage phải ghi rõ đường dẫn`; nhật ký plan T-007.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: kỹ thuật chụp hay đo mới phải được chạy thử cùng `scripts/check.sh`, không chỉ chạy test; tìm script đọc file test (`grep -n captureRoboImage scripts/`) như L-020. Hoặc đổi `check.sh` cho phép ảnh tạm ghi vào `build/` (qua dự án mẫu).
- **Áp dụng:** app: PR #39 (chạy thử bằng `check.sh`, L-001, L-010), PR #48 (`plan-check.py` quy tắc 9 dò điều `check.sh` từ chối).
- **Kiểm chứng:**

### L-004: Thuộc tính trợ năng của Compose không lan từ node cha xuống node con

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-03, T-011.
- **Chuyện gì:** plan T-011 ẩn màn hình phía sau hộp thoại khỏi TalkBack bằng `Modifier.semantics { hideFromAccessibility() }` trên khung bọc nội dung. Thuộc tính chỉ nằm trên node của khung; nút bên trong vẫn hiện với trình đọc màn hình. Test viết trước (nút phải mang `HideFromAccessibility`) bắt được, task dừng ở bước 4.
- **Bằng chứng:** `NetworkDialogHostTest.shown_blocksContentClicks_andHidesItFromAccessibility` trên nhánh `claude/T-011-core-network-hop-thoai` (commit 555eb12); bytecode Compose UI 1.9.1: `SemanticsOwnerKt.isHidden` chỉ đọc cấu hình của chính node, `AndroidComposeViewAccessibilityDelegateCompat` gọi `setVisibleToUser(!isHidden)` cho từng node; nhật ký plan T-011.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: thay đổi về trợ năng (ẩn, gộp, thứ tự đọc) phải được chạy thử bằng test tạm trước khi duyệt, đo trên cây `AccessibilityNodeInfo` hay node đích, không suy ra từ node cha (mở rộng L-001).
- **Áp dụng:** app: PR #48 (`docs/CLAUDE.md`, Khi viết plan: thay đổi trợ năng phải chạy thử, đo trên node đích).
- **Kiểm chứng:**

### L-005: Môi trường `claude --cloud` chặn máy chủ R2 staging, plan không kiểm được file bằng `curl`

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01, T-010.
- **Chuyện gì:** plan T-010 (mục 4, dòng cuối; mục 7, điều 3) yêu cầu `curl` 9 địa chỉ trên `https://camera-pose.vgl.io.vn/` trước bước 3. Proxy của môi trường cloud trả 403 cho máy chủ này (chính sách mạng), nên `curl` ra mã `000`. Phiên dựa vào ghi chú của danh sách chạy (dev đã kiểm, `200`) và `AppLaunchTest` trên CI (đã qua).
- **Bằng chứng:** `curl: (56) CONNECT tunnel failed, response 403` lúc 02:08 UTC; rà soát độc lập T-010 cũng không ra mạng được; CI lần chạy 39710999529 `ui-test` xanh.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: điều kiện về file trên R2 kiểm bằng một workflow trên GitHub (ví dụ thêm bước vào `device-session.yml` hay một workflow chỉ đọc) hoặc ghi rõ "dev kiểm trước phiên", không bằng `curl` từ phiên; hoặc thêm máy chủ R2 staging vào danh sách mạng cho phép của môi trường cloud (`docs/ops/` hướng dẫn dựng môi trường).
- **Áp dụng:** app: PR #48 (`docs/CLAUDE.md`: điều kiện file R2 không kiểm bằng `curl` từ phiên; từ dự án mẫu PR #84).
- **Kiểm chứng:**

### L-006: `captureToImage()` hết giờ trên Robolectric; đo điểm ảnh bằng cách vẽ view gốc ra Bitmap

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01, T-008 (lần thứ hai, sau T-007).
- **Chuyện gì:** plan T-008 ghi test màu nền dùng `onNodeWithTag(...).captureToImage()`. Trên Robolectric (`GraphicsMode.NATIVE`) lệnh này ném `ComposeTimeoutException` sau 2 giây, như T-007 đã gặp. Phiên đổi sang cách của `ComponentsTest` (vẽ `decorView` ra `Bitmap`), điều test kiểm tra không đổi.
- **Bằng chứng:** `background_isBackgroundRole_light`: `ComposeTimeoutException: Condition still not satisfied after 2000 ms` ở bước 3; nhật ký plan T-008, mục Lệch so với plan.
- **Đề xuất sửa:** `CLAUDE.md`, mục Viết một feature mới, bước 10: test đo màu điểm ảnh trên JVM vẽ view gốc ra `Bitmap` (hàm `pixelAt` của `ComponentsTest`), không dùng `captureToImage()`. Có thể đưa `pixelAt` vào một tiện ích test dùng chung.
- **Áp dụng:** app: PR #48 (`CLAUDE.md` bước 10: cách đo màu điểm ảnh bằng `decorView`; từ dự án mẫu PR #83).
- **Kiểm chứng:**

### L-007: Hàm CameraX trả `ListenableFuture` không gọi được vì Guava không có trên classpath biên dịch

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** app. **Nguồn:** RUN-20261022-01, T-013.
- **Chuyện gì:** `CameraControl.setZoomRatio` trả `com.google.common.util.concurrent.ListenableFuture`; Kotlin báo "Cannot access class 'ListenableFuture'". Plan cấm thêm thư viện (mục 7, điều 4), nên phiên gọi hàm qua reflection. Rà soát độc lập chỉ ra R8 có thể xóa hàm ở bản minify và làm app dừng khi vào camera; phiên tra hàm lười và thêm luật keep trong `consumer-rules.pro`, kiểm bằng `mapping.txt` của `stagingRelease`.
- **Bằng chứng:** lỗi biên dịch `CameraXController.kt:114`; rà soát độc lập T-013 (điểm chặn 1); PR #26.
- **Đề xuất sửa:** một plan rủi ro cao riêng thêm `androidx.concurrent:concurrent-futures-ktx` (hoặc `com.google.guava:listenablefuture`) vào `gradle/libs.versions.toml` cho `:feature:camera`, rồi bỏ reflection. Plan dùng CameraX về sau (T-015 chỉnh zoom, lấy nét) kiểm trước những hàm trả `ListenableFuture`.
- **Áp dụng:** plan T-031, PR #52: Guava `31.1-android` lên classpath biên dịch của `:feature:camera` (runtime không đổi, manifest gộp không thêm quyền), `CameraZoom.applyZoom` gọi thẳng `setZoomRatio`, bỏ reflection và luật keep.
- **Kiểm chứng:**

### L-008: Phiên mở bằng `claude --cloud` không có `resume-run.yml` tiếp tục, và `token-usage.py` cần thư mục gốc của checkout chính

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01.
- **Chuyện gì:** (1) Phiên này mở bằng `claude --cloud`, không qua routine. `resume-run.yml` kích hoạt lại routine khi phiên dừng vì hết hạn mức, nên không nối lại được phiên này; tiến trình của phiên còn bị khởi động lại một lần lúc 04:10 UTC giữa lúc chạy `check.sh` (lệnh bị dừng, mã 137), phiên tự chạy lại. (2) Nhánh báo cáo dùng một worktree riêng (`../report`) để không đổi nhánh dưới bản build của task; `scripts/token-usage.py` chạy trong worktree đó không thấy transcript (lọc theo thư mục gốc), phải đặt `TOKEN_USAGE_ROOT` về checkout chính.
- **Bằng chứng:** "Không đo được: không có transcript nào của repo này" khi chạy trong `../report`; chạy lại với `TOKEN_USAGE_ROOT=/home/user/camera-pose` ra bảng đầy đủ.
- **Đề xuất sửa:** `docs/auto-run.md`, mục 1 và mục 4: ghi cách tiếp tục khi phiên mở bằng `claude --cloud` (người mở lại phiên với cùng danh sách chạy, phiên đọc nhánh báo cáo như phiên tiếp tục); khuyến nghị worktree riêng cho nhánh báo cáo và ghi cách chạy `token-usage.py` từ đó. `scripts/token-usage.py` có thể tìm transcript theo tên repo thay vì đường dẫn tuyệt đối.
- **Áp dụng:** app: PR #39 (`auto-run.md` mục 1, ưu tiên routine). dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-043). Trước đó: đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-009: Hộp hỏi quyền camera còn mở làm hỏng lần đo khởi động nguội của `device-session`

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** app. **Nguồn:** RUN-20261022-01, T-013.
- **Chuyện gì:** từ T-013, màn chính ở staging là camera và app hỏi quyền camera. Kịch bản chuẩn của `device-session` dừng app bằng force-stop giữa các lần mở nguội; hộp hỏi quyền của lần 2 (`GrantPermissionsActivity`) vẫn ở trong task, nên lần mở thứ 3 đưa hộp đó lên thay vì mở `MainActivity`: `cold_start run=3 total_ms=0`, không có log nào của app. Cùng nguyên nhân làm `AppLaunchTest` đỏ ở vòng 2 của CI T-013.
- **Bằng chứng:** `scenarios.txt` và logcat của TS-20261022-01 (`onActivityRestartAttempt: ... GrantPermissionsActivity` lúc 04:58:46), lần chạy 39724404079.
- **Đề xuất sửa:** `scripts/device-session.sh` (kịch bản chuẩn) cấp quyền camera trước các lần mở nguội (`pm grant <gói> android.permission.CAMERA`) hoặc thêm một kịch bản riêng cho luồng hỏi quyền; ghi vào `docs/ops/test-sessions.md`.
- **Áp dụng:** app: PR #48 (`device-session.sh` cấp quyền runtime sau mỗi lần xóa dữ liệu, `--ask-permissions`; từ dự án mẫu PR #85).
- **Kiểm chứng:**
### L-010: Chạy thử kỹ thuật mới bằng đúng lệnh của phiên, ghi vào mục riêng của plan

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, -02, -03, RUN-20261022-01; T-007, T-008, T-011.
- **Chuyện gì:** ba lần dừng cùng một gốc (L-001, L-003, L-004), và lần thứ tư ở T-008 (L-006): kỹ thuật lần đầu dùng chưa được chạy thử, hoặc chạy thử bằng lệnh khác lệnh mà phiên dùng. Plan T-007 phiên bản 2 được kiểm chứng bằng `./gradlew :core:designsystem:testDebugUnitTest`, nhưng phiên dừng ở `scripts/check.sh`.
- **Bằng chứng:** nhật ký plan T-007 (phiên bản 1, 2), T-011 (phiên bản 1); phiên bản 3 của T-007 và 2 của T-011 được chạy thử bằng `scripts/check.sh` trên nhánh của task trước khi duyệt và không dừng nữa (T-007 merge ở PR #19, T-011 ở PR #23).
- **Đề xuất sửa:** `docs/templates/plan.md` thêm mục "Chạy thử" (test tạm, lệnh đã chạy, kết quả) bắt buộc khi tiêu chí E từ 2 trở lên; lệnh phải là `scripts/check.sh` trên nhánh có code liên quan. `scripts/plan-check.py` báo lỗi khi E từ 2 trở lên mà thiếu mục này. Ghi rõ: Robolectric không mô phỏng việc cửa sổ chồng nhau, phần trợ năng và chạm qua cửa sổ phải có bước kiểm tra thủ công; chạy thử đo cả hình ảnh thật, không chỉ cây ngữ nghĩa (báo cáo RUN-20261022-01, Kiểm chứng bài học cũ).
- **Áp dụng:** app: PR #39 (mẫu plan mục 4.1, `plan-check.py` quy tắc 7). dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-041). Trước đó: đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#77, VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-011: Tài nguyên R2 bắt buộc của app chưa có, PR có code đầu tiên đỏ ở emulator

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** PR #6.
- **Chuyện gì:** ba file `test/v1/*.txt` mà màn loading của dự án mẫu tải chưa được đưa lên R2 staging khi tạo app; mọi PR trước chỉ sửa tài liệu nên `ui-test` bị bỏ qua, lỗi chỉ lộ ra ở PR có code đầu tiên.
- **Bằng chứng:** lần chạy 39566140163, logcat `HTTP 404 for https://camera-pose.vgl.io.vn/test/v1/...`, `AppLaunchTest` hết 30 giây chờ.
- **Đề xuất sửa:** thêm `scripts/setup-check.sh`: `curl -I` mọi file R2 mà app bắt buộc có (danh sách trong một file cấu hình), chạy ở bước cuối của `docs/ops/setup-new-app.md`, trong `env-canary.yml` và ở kiểm tra đầu phiên. Kiểm bằng workflow trên GitHub, vì môi trường `claude --cloud` chặn máy chủ R2 (L-005).
- **Áp dụng:** plan T-030, PR #50: mỗi file trong `scripts/required-assets.txt` phải trả `200`, kiểm bằng `preflight.yml` trên GitHub; plan thêm file R2 bắt buộc thì thêm dòng (`docs/CLAUDE.md`, Khi viết plan).
- **Kiểm chứng:**

### L-012: Kiểm tra bí mật staging ở đầu phiên, không để tới cuối

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, -02, -03 (nối tiếp L-002), RUN-20261022-01.
- **Chuyện gì:** ba phiên liền chỉ biết bí mật `STAGING_GOOGLE_SERVICES_JSON` là file giả ở bước cuối (`device-session`, `staging`), sau khi đã tốn cả phiên.
- **Bằng chứng:** báo cáo RUN-20261021-01, -02, -03, mục Cần người kiểm tra.
- **Đề xuất sửa:** `start-run.yml` hoặc bước kiểm tra đầu phiên chạy một kiểm tra nhỏ, không in giá trị: bí mật có `project_id` khác project giả và có app `...staging`; sai thì không bắt đầu phiên, mở issue `env-broken`. Kiểm luôn nhóm người test trong `STAGING_TESTER_GROUPS` (mặc định `team`) đã có trong App Distribution: RUN-20261022-01 tải bản build lên được nhưng bước phát đỏ (`HTTP Error: 404`) vì chưa tạo nhóm.
- **Áp dụng:** plan T-030, PR #50: `preflight.sh` kiểm `project_id`, app `.staging` và nhóm người test trước phiên; issue `setup-broken`. Lệnh liệt kê nhóm người test chưa chạy thử với tài khoản thật (plan mục 3.4).
- **Kiểm chứng:**

### L-013: Quy tắc của `check.sh` phải hiện ra lúc viết plan

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-02, T-007 (nối tiếp L-003).
- **Chuyện gì:** quy tắc "mọi `captureRoboImage` ghi vào `src/test/screenshots/`" chỉ nằm trong `check.sh` và một câu của `CLAUDE.md`; người viết plan và phiên chính đều bỏ sót.
- **Bằng chứng:** `check.sh` dòng 45 đến 55; nhật ký plan T-007 phiên bản 2.
- **Đề xuất sửa:** `plan-check.py` dò trong plan các mẫu mà `check.sh` từ chối (ví dụ `captureRoboImage(` với đường dẫn ngoài `src/test/screenshots/`); `docs/auto-run.md` có bảng các quy tắc của `check.sh`.
- **Áp dụng:** app: PR #48 (`plan-check.py` quy tắc 9, bảng điều `check.sh` từ chối ở `auto-run.md` bước 5; từ dự án mẫu PR #84).
- **Kiểm chứng:**

### L-014: Plan đẩy việc cho nhau, hoặc cùng sửa một file mà không phụ thuộc nhau

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, plan T-009, T-010, T-011, T-013.
- **Chuyện gì:** plan T-009 (câu hỏi 1) giao việc thêm lý do lỗi tải cho T-011, plan T-011 (câu hỏi 5) giao lại cho T-009; plan T-010 và T-013 cùng sửa `AppNavHost.kt`, `MainActivity.kt`, `app/build.gradle.kts`, `AppLaunchTest.kt` mà ticket không phụ thuộc nhau. Chỉ bắt được khi phiên chính đối chiếu bằng tay.
- **Bằng chứng:** lịch sử PR #7 và #11; ticket T-011, T-013 thêm phụ thuộc sau đối chiếu.
- **Đề xuất sửa:** `plan-check.py` cảnh báo khi hai plan `approved` cùng sửa một file mà không có quan hệ `depends_on`; mẫu plan thêm câu "việc giao cho ticket khác phải được plan của ticket đó ghi nhận".
- **Áp dụng:** app: PR #39 (`run-check.py` cảnh báo hai plan cùng sửa một file), PR #48 (mẫu plan: việc giao ticket khác phải được plan đó ghi nhận).
- **Kiểm chứng:**

### L-015: Danh sách chạy chỉ một chuỗi phụ thuộc thì một task dừng làm cả phiên trống

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261021-02.
- **Chuyện gì:** danh sách chỉ có T-007 và T-011 (phụ thuộc T-007); T-007 dừng, phiên không merge được gì mà vẫn tốn phần cố định.
- **Bằng chứng:** báo cáo RUN-20261021-02.
- **Đề xuất sửa:** khi lập danh sách chạy, nếu chỉ có một chuỗi thì viết thêm plan độc lập trước khi chạy; một lệnh kiểm danh sách chạy cảnh báo trường hợp này.
- **Áp dụng:** app: PR #39 (`run-check.py` cảnh báo một chuỗi phụ thuộc). dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-044). Trước đó: đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-016: `docs/STATUS.md` xung đột ở mọi PR

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** PR #11, #14, #16.
- **Chuyện gì:** mỗi PR tài liệu sinh lại `docs/STATUS.md`; PR sau luôn xung đột với PR trước, phải gộp và sinh lại bằng tay.
- **Bằng chứng:** PR #11, #14, #16 đều báo `DIRTY` chỉ vì `docs/STATUS.md`.
- **Đề xuất sửa:** `.gitattributes` khai báo `docs/STATUS.md merge=ours` cùng một bước sinh lại trang trên nhánh chính sau khi merge; hoặc `status.py --check` chỉ chạy trên nhánh chính.
- **Áp dụng:** plan T-030, mục 8 câu 4: giữ như hiện nay, thêm hướng dẫn. PR #50: `docs/CLAUDE.md`, mục Trang tiến độ: nhánh chậm hay xung đột ở `docs/STATUS.md` thì gộp nhánh chính, chạy lại `scripts/status.py`, commit trang vừa sinh. Sau đó plan T-034 (cách (a), L-047): trang ra khỏi git, `status-page.yml` sinh trang trên nhánh `status`.
- **Kiểm chứng:**

### L-017: `merge-if-green.sh` cần tự cập nhật nhánh và chịu được job kẹt

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** PR #6, #11, #14.
- **Chuyện gì:** script từ chối mọi PR có nhánh cũ hơn nhánh chính, người phải cập nhật nhánh, chờ CI, gọi lại; một lần GitHub ghi job `changes` là `in_progress` dù lần chạy đã `completed success`.
- **Bằng chứng:** lần chạy 39613776353 (PR #11); PR #6 bị từ chối vì chậm 2 commit.
- **Đề xuất sửa:** thêm `merge-if-green.sh --update`: cập nhật nhánh, chờ CI của commit mới, rồi merge; coi lần chạy `completed success` là xanh khi trạng thái job còn lệch.
- **Áp dụng:** plan T-029: `merge-if-green.sh --update` cho GitHub cập nhật nhánh rồi trả mã 2; job kẹt được xét theo check suite; `auto-run.md` bước 9. Lệnh chờ CI vẫn tách riêng (`wait-run.sh`), vì một lệnh của phiên không chạy đủ lâu để chờ CI.
- **Kiểm chứng:**

### L-018: PR bị merge trước khi nhận commit cuối

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** PR #7, #8.
- **Chuyện gì:** commit duyệt plan được đẩy lên nhánh của PR #7 sau khi người đã merge PR đó; plan và ticket trên nhánh chính vẫn `draft`, phải mở PR #8 để đưa lại.
- **Bằng chứng:** lịch sử nhánh `claude/plans-foundation` (commit 4a11c4b).
- **Đề xuất sửa:** `merge-if-green.sh` in mã commit sẽ merge và cảnh báo nếu nhánh có commit trong vài phút gần đây; quy ước: thay đổi sau khi PR đã gửi người duyệt thì làm trên nhánh mới.
- **Áp dụng:** app: PR #48 (`auto-run.md` bước 9: đối chiếu mã commit, thay đổi sau khi gửi duyệt làm trên nhánh mới). Phần script: plan T-029, `merge-if-green.sh` cảnh báo khi commit cuối đẩy chưa tới 3 phút.
- **Kiểm chứng:**

### L-019: Trạng thái ticket và plan đổi tay, hay lệch nhau

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, 2026-10-22.
- **Chuyện gì:** ticket được chuyển `approved` khi plan còn `draft`; plan được duyệt mà ticket còn `planned`; plan thiếu `approved_at` (plan-check bắt được).
- **Bằng chứng:** lịch sử ticket T-008, T-010, T-013 và plan T-007 phiên bản 2.
- **Đề xuất sửa:** `scripts/status.py --sync`: plan `approved` hay `stopped` thì ticket chuyển theo; `plan-check.py` cảnh báo khi hai trạng thái lệch nhau.
- **Áp dụng:** app: PR #48 (`status.py --sync`, `plan-check.py` quy tắc 10; từ dự án mẫu PR #84).
- **Kiểm chứng:**

### L-020: Routine không dùng cloud session credits; mở phiên bằng `claude --cloud` thì dùng được

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, 2026-10-22; RUN-20261021-03.
- **Chuyện gì:** khuyến mãi cloud session credits (Max $250, hết hạn 2026-11-04 23:59 PT) không áp dụng cho "Projects and Routines". Ba phiên chạy qua routine tính vào hạn mức tuần (đã tới 98%); phiên mở bằng `claude --cloud` trừ vào credit.
- **Bằng chứng:** https://support.claude.com/en/articles/17152539-cloud-sessions-bonus-credit-promotion; số dư $248, $247 sau phiên thử, $237 sau RUN-20261021-03 (khoảng 24M token, khoảng $10).
- **Đề xuất sửa:** `docs/ops/staging-and-runs.md` ghi cách mở phiên bằng `claude --cloud` khi còn credit: prompt, các kiểm tra phải tự làm (thay `start-run.yml`); cách tiếp tục phiên khi bị ngắt nằm ở L-008. RUN-20261022-01 chạy bằng cách này, 4 trên 4 task merge. `docs/metrics/token-usage.csv` thêm cột tiền credit đã dùng.
- **Áp dụng:** app: PR #48 (`staging-and-runs.md` mục 2.7, `token-usage.py credit`; từ dự án mẫu PR #85).
- **Kiểm chứng:**

### L-021: Số ước tính token tạm quá cao

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, -02, -03, RUN-20261022-01.
- **Chuyện gì:** số tạm 40M cho task cỡ L; thực tế mỗi task 2M đến 15M (T-013 cỡ L khoảng 24M lúc gần xong), cả phiên 4M đến 97M.
- **Bằng chứng:** `docs/metrics/token-usage.csv`.
- **Đề xuất sửa:** đổi số tạm của `scripts/token-usage.py estimate` (ví dụ S 5M, M 15M, L 30M) cho tới khi đủ 5 task mỗi cỡ.
- **Áp dụng:** app: PR #48 (`token-usage.py` ước tính khởi đầu S 5M, M 15M, L 30M; từ dự án mẫu PR #85).
- **Kiểm chứng:**

### L-022: Java của máy dev quá mới làm Gradle không chạy

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21.
- **Chuyện gì:** máy chỉ có Java 26; Gradle 8.14.3 dừng ngay với lỗi chỉ ghi "26".
- **Bằng chứng:** log `scripts/check.sh` lần đầu trên máy dev; chạy lại với JDK 21 của Android Studio thì qua.
- **Đề xuất sửa:** `scripts/check.sh` kiểm phiên bản Java trước khi gọi Gradle và gợi ý `JAVA_HOME` trỏ tới JDK của Android Studio; thêm vào `docs/ops/setup-new-app.md`.
- **Áp dụng:** plan T-030, PR #50: `scripts/java-check.sh` gọi từ `check.sh` trước Gradle, chấp nhận Java 17 tới 24, gợi ý `JAVA_HOME`. Hướng dẫn ở `docs/ops/setup-new-app.md`.
- **Kiểm chứng:**

### L-023: Lỗi thiết kế lọt vào gói bàn giao

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, 2026-10-22.
- **Chuyện gì:** độ mờ của lớp tô mất khi gắn biến trên Figma (56 chỗ, lộ ra khi agent viết plan T-013 so README với ảnh); tên frame màn hình gập trong bản đề xuất khác ADR-0020.
- **Bằng chứng:** PR #11 (xuất lại 18 ảnh camera); PR #3, sửa mục 7 của bản đề xuất.
- **Đề xuất sửa:** `scripts/figma-export.py` ghi cả độ mờ của lớp tô vào `context/`; script so danh sách frame trong bản đề xuất với frame trên Figma; bước quét chỉ đọc (design-workflow.md, mục 8.4) kiểm instance có giá trị tô khác component gốc.
- **Áp dụng:** app: PR #48 (`figma-export.py` ghi độ mờ, đánh dấu instance ghi đè; `design-workflow.md`; từ dự án mẫu PR #85).
- **Kiểm chứng:**

### L-024: Cải tiến đã làm ở app, cần đưa về dự án mẫu

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** PR #10, #12, #16, #19.
- **Chuyện gì:** bốn thay đổi có ích cho mọi app đã merge ở app: `plan-check.py` cho plan khai báo ảnh thiết kế do ticket khác vẽ (PR #16); `.gitignore` chặn `*.b64` và khóa tài khoản dịch vụ (PR #10); `RemoteSettings.string()` (T-009, PR #12); đo màu điểm ảnh bằng cách vẽ `decorView` ra `Bitmap` thay `captureToImage` (T-007, PR #19; L-006).
- **Bằng chứng:** các PR đã nêu.
- **Đề xuất sửa:** mở issue `from-app` ở dự án mẫu cho từng thay đổi; ghi cách đo điểm ảnh vào `CLAUDE.md`, mục test giao diện.
- **Áp dụng:** app: PR #48 (`CLAUDE.md` bước 10: cách đo màu điểm ảnh). Ba cải tiến còn lại đã có ở app; dự án mẫu: PR #83, issue #72, #73, #74.
- **Kiểm chứng:**

### L-025: Tài liệu của app còn chỗ cũ sau khi quyết định đổi

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** app. **Nguồn:** phiên có người theo dõi 2026-10-22.
- **Chuyện gì:** `docs/runs/RUN-20261021-04.md` (`draft`, do RUN-20261021-03 lập) trùng với `RUN-20261022-01`; bản đề xuất mục 11 còn khuôn đường dẫn ảnh `<poseId>/<sha8>/` mà `AssetPath` từ chối (plan T-009 đã đổi sang mã băm trong tên file).
- **Bằng chứng:** `docs/runs/`; plan T-009, câu hỏi 2.
- **Đề xuất sửa:** ghi chú "không dùng, thay bằng RUN-20261022-01" trong `RUN-20261021-04`; sửa mục 11 của bản đề xuất theo plan T-009.
- **Áp dụng:** app: PR #48 (`RUN-20261021-04` ghi "Không dùng"; ghi chú đường dẫn ảnh ở bản đề xuất mục 11).
- **Kiểm chứng:**

### L-026: Mã phiên thử trên máy thật trùng mã phiên emulator của phiên tự chạy

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** TS-20261022-02.
- **Chuyện gì:** `scripts/device-session.sh` cấp mã `TS-20261022-01` cho phiên trên máy thật, trùng phiên emulator của RUN-20261022-01. Script chỉ tránh mã có thư mục trên máy hoặc có báo cáo trong `docs/test-sessions/`; phiên emulator chỉ được ghi trong báo cáo phiên tự chạy.
- **Bằng chứng:** `scripts/device-session.sh` hàm `next_session_dir`; báo cáo RUN-20261022-01, mục Kiểm thử trên máy ảo; báo cáo phải đổi sang `TS-20261022-02`.
- **Đề xuất sửa:** `next_session_dir` tìm thêm mã trong `docs/runs/*.md`; hoặc phiên emulator dùng tiền tố riêng.
- **Áp dụng:** app: PR #48 (`device-session.sh` tránh mã đã có trong `docs/runs/*.md`; từ dự án mẫu PR #85).
- **Kiểm chứng:**

### L-027: Lỗi DNS của tên miền file tĩnh chỉ lộ ra trên máy thật

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** TS-20261022-02.
- **Chuyện gì:** vùng `io.vn` không qua bước kiểm DNSSEC ở Cloudflare và Google DNS, nên `camera-pose.vgl.io.vn` không phân giải được; máy thật không tải được catalog, trong khi phiên emulator trên GitHub vài giờ trước vẫn qua. Log không cho biết lý do vì Android bỏ stack trace của `UnknownHostException`.
- **Bằng chứng:** TS-20261022-02, P-1; FB-20261022-02, FB-20261022-04.
- **Đề xuất sửa:** `env-canary.yml` kiểm phân giải tên miền file tĩnh qua 1.1.1.1 và 8.8.8.8 (không chỉ DNS của runner); `:core:assets` ghi tên lớp lỗi trong câu log; `docs/ops/setup-new-app.md` khuyên chọn tên miền thuộc vùng có DNSSEC ổn định.
- **Áp dụng:** app: PR #48 (`setup-new-app.md`: kiểm DNS công cộng, chọn vùng DNSSEC ổn định). Plan T-030, PR #50: `preflight.sh` kiểm qua 1.1.1.1 và 8.8.8.8 mỗi sáng. Plan T-032, PR #51: `:core:assets` ghi loại lỗi và lý do qua `AppLogger` (`VGL.Assets`). Dự án mẫu: phần log qua `AppLogger` là issue #105, PR #108.
- **Kiểm chứng:**

### L-028: Phiên kiểm `main-broken` trước khi `main-guard` của lần merge vừa rồi chạy xong

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, T-026, T-012.
- **Chuyện gì:** T-026 merge lúc 16:50 UTC; phiên kiểm `main-broken` ngay sau đó (0 issue) rồi bắt đầu T-012. `main-guard` của commit merge e9bf4bb chạy xong lúc 16:58 và mở issue #35. Phiên chỉ thấy issue khi kiểm lại lúc 17:35, sau khi T-012 đã làm xong bước 7 (khoảng 45 phút và 22M token cho một task không merge được).
- **Bằng chứng:** `main-guard` lần chạy 39817360705 (16:50 tới 16:58); issue #35 tạo 16:58:17; RUN-20261023-01, Dòng thời gian.
- **Đề xuất sửa:** `docs/auto-run.md` bước 9: sau khi merge, chờ `main-guard` của commit merge bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>` ở chế độ nền, trong lúc làm bước 10 và bước 1, 2 của task sau; kiểm lại `main-broken` trước khi đẩy commit đầu tiên của task sau. Hoặc `merge-if-green.sh` in nhắc lệnh này.
- **Áp dụng:** app: PR #39 (`auto-run.md` bước 9, chờ `main-guard`). dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-042).
- **Kiểm chứng:**

### L-029: Screenshot test của trạng thái có vòng chờ đỏ trên `main-guard` dù xanh ở PR và cục bộ

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** app. **Nguồn:** RUN-20261023-01, issue #35.
- **Chuyện gì:** `CameraScreenScreenshotTest > starting_dark` (trạng thái `Starting`, có vòng chờ chạy mãi) thất bại trên `main-guard` ở e9bf4bb với `AssertionFailedWithCauseError` tại dòng `captureRoboImage`. Cùng code xanh ở CI của PR #34 (lần chạy 39815764346), ở `build-and-test` của PR #36 (dựng trên e9bf4bb, lần chạy 39821691045) và ở 3 lần `scripts/check.sh` cục bộ. T-026 không đổi giao diện camera. Nhiều khả năng là test chập chờn vì hiệu ứng vô hạn; chưa xác nhận.
- **Bằng chứng:** log `main-guard` lần chạy 39817360705, dòng "CameraScreenScreenshotTest > starting_dark FAILED"; nhánh `revert/e9bf4bb`.
- **Đề xuất sửa:** screenshot test của trạng thái có hiệu ứng vô hạn dừng đồng hồ của Compose (`mainClock.autoAdvance = false`) hoặc dùng bản tĩnh của vòng chờ; `main-guard` chạy lại một lần test thất bại trước khi mở `main-broken` nếu lỗi là hết giờ chờ của Compose.
- **Áp dụng:** app: RUN-20261023-02, T-014 (`CameraScreenStartingScreenshotTest`) và T-016 (`ReviewScreenScreenshotTest.saving_dark`) chụp vòng chờ với đồng hồ Compose dừng; PR #39 (`CLAUDE.md`, bước 10: quy tắc chung); plan T-028, PR #40 (chạy lại test hỏng trên `main-guard`). dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-047).
- **Kiểm chứng:** RUN-20261023-02: nguyên nhân xác nhận bằng chạy thử ngày 2026-10-22 (vòng chờ giữ màn hình không rảnh, Robolectric vẽ tới khi máy test hết bộ nhớ, ảnh đổi theo cỡ bộ nhớ). Sau khi dừng đồng hồ, `starting_dark` chạy dưới 1 giây; `main-guard` xanh cả 5 lần merge của phiên (lần chạy 39872832021 tới 39893107034).

### L-030: Điều hướng sau một hàm `suspend` đọc DataStore chạy ngoài luồng chính dưới Compose test

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, T-012.
- **Chuyện gì:** T-012 cho `chooseMainScreen()` đọc DataStore. `AppLaunchTest` trên emulator báo `IllegalStateException: Method setCurrentState must be called on the main thread` ở `navController.navigate` trong `scope.launch` của `AppNavHost`: coroutine được tiếp tục trên `LimitedDispatcher$Worker`. `AppNavHost` nằm trong ngoại lệ viết test trước của plan (không có test JVM), nên lỗi chỉ lộ ra ở `ui-test`, vòng CI dài nhất.
- **Bằng chứng:** lần chạy 39821691045, job `ui-test`, bước "Chi tiết test trên emulator".
- **Đề xuất sửa:** `CLAUDE.md` mục Viết một feature mới: điều hướng chạy trên `Dispatchers.Main.immediate` khi đứng sau một lời gọi `suspend`; plan đổi hàm `suspend` mà `AppNavHost` gọi phải có test JVM (Robolectric) dựng `AppNavHost` với bản giả đổi luồng, thay vì ngoại lệ.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-051).
- **Kiểm chứng:**

### L-031: Giao bước 3 đến 5 của task lớn cho một phiên AI viết code riêng

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, T-012.
- **Chuyện gì:** với T-012 (47 file), phiên chính giao bước 3 đến 5 cho một phiên con có đủ quy tắc (danh sách file, thứ tự commit, đẩy sau mỗi commit, điều kiện dừng, báo cáo theo mẫu nhật ký). Phiên con xong trong 22 phút, `check.sh` qua, không file nào ngoài plan; phiên chính chỉ kiểm danh sách file, giao rà soát độc lập cho một phiên khác và giữ ngữ cảnh gọn cho các task sau. Cũng trong phiên: phiên chính lỡ sửa file khi `check.sh` đang chạy nền trong cùng thư mục (T-026) và phải chạy lại.
- **Bằng chứng:** commit dabc2a3 tới 15f10b2 (16:58 tới 17:12); token T-012 21,9M so với ước tính 20M.
- **Đề xuất sửa:** `docs/auto-run.md` mục 3: cho phép và mô tả cách giao bước 3 đến 5 cho phiên con (nội dung giao việc tối thiểu); nhắc không sửa file trong thư mục đang chạy `check.sh` nền, chỉ chạy rà soát độc lập song song.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-052).
- **Kiểm chứng:** RUN-20261025-01 (T-048): phiên con làm bước 3 đến 5 (32 test, code, `check.sh`) trong khoảng 22 phút; phiên chính chỉ đọc báo cáo, ngữ cảnh nhỏ.

### L-032: Nút chữ trong thành phần có nền khác màu tự đặt màu chữ, đè màu thiết kế

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261023-02, T-016.
- **Chuyện gì:** thông báo lưu lỗi của màn xem lại dùng `Snackbar` có nút `TextButton` "Thử lại". `TextButton` tự đặt màu chữ bằng `ButtonDefaults.textButtonColors()` (`primary`), đè `actionContentColor` mà `Snackbar` truyền xuống, nên chữ ra #6E9BFF thay vì `inversePrimary` #2F6BFF của thiết kế, độ tương phản giảm từ khoảng 3,9:1 xuống 2,4:1. Ảnh chuẩn ghi lại màu sai; không test nào đo màu chữ của nút; chỉ phiên rà soát độc lập thấy khi đo điểm ảnh.
- **Bằng chứng:** RUN-20261023-02, rà soát độc lập T-016 (điểm chặn); commit e2030bd; test `saveError_retryUsesInversePrimary`.
- **Đề xuất sửa:** mẫu plan và `CLAUDE.md` mục test giao diện: thành phần có nút chữ đặt trên nền khác màu (`inverseSurface`, thẻ màu, hộp thoại) phải có test đo màu chữ của nút bằng cách vẽ view gốc ra `Bitmap`; khi dựng, truyền `colors` cho `TextButton` thay vì dựa vào màu mà thành phần cha truyền xuống.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-053).
- **Kiểm chứng:**

### L-033: Các plan viết song song cùng ghi "Tạo" cho một file mà task chạy trước trong phiên tạo ra

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-02, T-014, T-025.
- **Chuyện gì:** plan T-014 và T-025 cùng ghi "Tạo" `core/designsystem/src/main/res/drawable/ic_chevron_right.xml`, một bản kiểu Outlined, một bản kiểu Rounded. T-014 chạy trước và tạo file; `scripts/plan-check.py` chỉ cảnh báo lúc bắt đầu T-025. Phiên con dùng lại bản có sẵn để không đổi giao diện camera, và ghi lệch plan. Tương tự, chú thích "until T-025 wires it" do T-021 viết ở `PosesNavigation.kt` trở nên lỗi thời mà file đó không thuộc plan T-025.
- **Bằng chứng:** kết quả `plan-check.py` lúc 02:26 UTC; nhật ký plan T-025, mục Lệch so với plan.
- **Đề xuất sửa:** `docs/CLAUDE.md` mục Khi lập danh sách chạy: chạy `plan-check.py` cho các plan theo thứ tự chạy dự kiến và tìm file "Tạo" trùng giữa các plan; mỗi file dùng chung chỉ một plan tạo, plan sau ghi "dùng lại" và ghi rõ kiểu. Plan để lại chú thích "task sau nối" thì liệt kê file đó cho task sau.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-054).
- **Kiểm chứng:**

### L-034: Phiên con viết code trong thư mục chính làm phiên chính không làm gì song song được

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261023-02.
- **Chuyện gì:** bốn phiên con (T-014, T-021, T-016, T-025) làm việc ngay trong thư mục chính của repo. Trong lúc chờ, hook dừng phiên của môi trường cloud ba lần báo "có thay đổi chưa commit" (đó là phần làm dở của phiên con, phiên chính không được commit chen vào). Phiên chính cũng không thể chạy `check.sh`, chuẩn bị nhánh task sau hay sửa file song song trong thư mục đó (L-031).
- **Bằng chứng:** phản hồi của hook trong phiên, lúc phiên con T-014, T-021, T-025 đang làm.
- **Đề xuất sửa:** `docs/auto-run.md` mục 3 (cùng chỗ với L-031): phiên chính tạo worktree riêng cho nhánh task (`git worktree add ../task-T-NNN claude/T-NNN-...`) và giao việc cho phiên con trong worktree đó; thư mục chính giữ sạch cho việc của phiên chính. Bộ nhớ đệm Gradle ở `~/.gradle` dùng chung nên không tốn thêm thời gian tải.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-052).
- **Kiểm chứng:** RUN-20261025-01: phiên con làm trong `../task-T-048`; phiên chính chờ `check.sh` đầu phiên và ghi nhánh báo cáo song song, không xung đột.

### L-035: Thêm một bước đối chứng để test viết trước không qua ngay trên khung rỗng

- **Trạng thái:** verified. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-02, T-014, T-021, T-016, T-025.
- **Chuyện gì:** ở cả bốn task lớn, một số test viết theo mục 4 của plan qua ngay trên khung rỗng, vì khung trả đúng giá trị mặc định mà test mong đợi (ví dụ "chưa lưu gì thì trả mặc định", "tệp hỏng thì trả `null`"). Phiên con tự viết chặt lại trước commit test bằng một bước đối chứng: ghi rồi đọc lại, hoặc đặt dữ liệu đúng cạnh dữ liệu sai. Sau đó các test này thất bại đúng lý do. Số test được viết chặt như vậy: T-014 2, T-016 8, T-025 9; ở T-021, 2 test được viết chặt ngay từ đầu.
- **Bằng chứng:** nhật ký plan T-014, T-021, T-016, T-025 (RUN-20261023-02), mục Test viết trước.
- **Đề xuất sửa:** `docs/auto-run.md` bước 3: nêu "thêm bước đối chứng" làm cách mặc định khi một test không phải test chặn qua ngay; mẫu plan nhắc người viết plan rằng test kiểm giá trị mặc định thường qua trên khung và nên có sẵn bước đối chứng.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-055).
- **Kiểm chứng:** RUN-20261026-01: plan T-052 và T-053 ghi sẵn đối chứng (`a Task line in the main session's transcript`, `rule 11 only warns for an approved plan`, hai đối chứng `fresh`); đối chứng qua ở bước 3, test chính thất bại đúng lý do. Có hiệu quả.

### L-036: Chạy lại `main-guard` trên đúng commit bị đổ lỗi, xanh, nhưng issue `main-broken` không tự đóng

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** issue #35, sau RUN-20261023-01.
- **Chuyện gì:** dev chạy lại `main-guard` (lần chạy 39817360705, lần 2) trên e9bf4bb, commit bị đổ lỗi; lần chạy xanh. Bước đóng issue chỉ đóng khi `compare/<commit bị đổ lỗi>...<commit xanh>` trả `ahead`; cùng một commit thì trả `identical`, nên issue #35 vẫn mở và dev phải tự đóng trước khi phiên sau chạy được.
- **Bằng chứng:** `.github/workflows/main-guard.yml`, bước "Đóng issue main-broken khi nhánh chính xanh trở lại"; lần chạy 39817360705, lần 1 đỏ 16:58, lần 2 xanh 22:53; issue #35 vẫn mở sau đó.
- **Đề xuất sửa:** `main-guard.yml` đóng issue khi `status` là `ahead` hoặc `identical`. ADR-0010, mục 2, câu cuối thêm "hoặc chính commit đó được chạy lại và xanh".
- **Áp dụng:** app: plan T-028, PR #40 (ADR-1002); làm ở PR #47. đề xuất về dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-046).
- **Kiểm chứng:**

### L-037: Một test chập chờn trên `main-guard` kết thúc cả phiên

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, issue #35.
- **Chuyện gì:** `CameraScreenScreenshotTest > starting_dark` thất bại một lần trên `main-guard` (L-029), xanh ở PR, ở cục bộ và khi chạy lại. Một lần đỏ đó mở `main-broken`, phiên kết thúc khi mới xong 2 trên 7 task; 4 task chưa bắt đầu và T-012 dở dang phải chờ người tới sáng.
- **Bằng chứng:** báo cáo RUN-20261023-01, mục Tóm tắt; lần chạy 39817360705.
- **Đề xuất sửa:** `main-guard.yml`: khi bước kiểm tra đỏ chỉ vì test thất bại, chạy lại riêng các test đó một lần trên cùng commit trước khi gọi `find-breaking-commit.sh`. Lần chạy lại xanh thì không mở `main-broken`, mà mở hoặc bình luận vào issue `flaky` ghi tên test, để sửa test trong một ticket thường. Lần chạy lại vẫn đỏ thì làm như hiện nay. ADR-0010, mục 2: thêm quy tắc này. Phiên tự chạy đọc issue `flaky` vào mục Cần người kiểm tra.
- **Áp dụng:** app: plan T-028, PR #40 (ADR-1002); làm ở PR #47. đề xuất về dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-047).
- **Kiểm chứng:**

### L-038: Phiên không tự đưa nhánh chính về trạng thái xanh được, kể cả bằng một bản revert đúng

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, issue #35.
- **Chuyện gì:** khi `main-broken` mở, `merge-if-green.sh` từ chối mọi PR, kể cả PR từ nhánh `revert/<commit>` mà `main-guard` đã dựng sẵn; chỉ người đóng issue mới mở lại được. Lý do của ADR-0010 là đúng: tên nhánh không phải bằng chứng. Hệ quả là một lần merge hỏng thật vào ban đêm làm mọi task còn lại chờ tới sáng.
- **Bằng chứng:** ADR-0010, mục 2; `scripts/merge-if-green.sh`, đoạn kiểm tra `main-broken`.
- **Đề xuất sửa:** cho `merge-if-green.sh` một ngoại lệ kiểm chứng bằng nội dung thay vì tên: khi `main-broken` mở, chỉ merge PR có đúng một commit, mà cây file của commit đó trùng khớp với kết quả `git revert <commit bị đổ lỗi>` do chính script dựng trên nhánh chính mới nhất, và CI của PR xanh. Phiên tự chạy được merge bản revert của commit do chính phiên merge, chuyển task đó sang `stopped`, rồi làm tiếp các task không phụ thuộc nó. Issue vẫn chỉ tự đóng bởi `main-guard`, AI vẫn không đóng hay sửa issue. Cần ADR thay một phần ADR-0010, plan rủi ro cao có duyệt chéo.
- **Áp dụng:** app: plan T-028, PR #40 (ADR-1002); làm ở PR #47. đề xuất về dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-048).
- **Kiểm chứng:**

### L-039: Plan ghi sẵn phương án dự phòng đã duyệt, để phiên chuyển sang thay vì dừng task

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, -02, -03 (T-007 hai lần, T-011), RUN-20261023-01.
- **Chuyện gì:** cả ba lần dừng vì plan đều cùng một dạng: kỹ thuật của plan không chạy (`captureToImage` hết giờ, quy tắc `captureRoboImage(` của `check.sh`, thuộc tính trợ năng không lan xuống node con), và cách thay đã rõ ngay khi gặp lỗi. Phiên phải dừng vì cách thay chưa được duyệt; mỗi lần mất một phiên và một vòng duyệt.
- **Bằng chứng:** nhật ký plan T-007 (phiên bản 1, 2), T-011 (phiên bản 1); L-001, L-003, L-004, L-006.
- **Đề xuất sửa:** mẫu plan thêm mục 3.4 Phương án dự phòng (điều kiện chuyển, cách làm thay, file ghi "(dự phòng)" trong mục 3.2) và mục 4.1 Chạy thử; `risk-scoring.md` mục 4: chuyển sang phương án dự phòng đã duyệt không phải dừng, chỉ một lần, không vượt các điều kiện dừng khác; `docs/CLAUDE.md` mục Khi viết plan; `plan-check.py` báo lỗi khi plan `draft` có tiêu chí E từ 2 mà thiếu chạy thử bằng `scripts/check.sh`.
- **Áp dụng:** app: PR #39. dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-040).
- **Kiểm chứng:**

### L-040: Mâu thuẫn và test yếu chỉ được tìm thấy khi phiên đã chạy, hoặc nhờ người tình cờ thấy

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** chuẩn bị RUN-20261023-01, RUN-20261023-02.
- **Chuyện gì:** khi chuẩn bị RUN-20261023-01, các lỗi sau chỉ lộ ra nhờ rà soát bằng tay, không có bước nào bắt buộc tìm:
  - plan T-016 phiên bản 1 cần quyền lưu tệp mà spec camera không có;
  - câu trả lời câu 5 của plan T-012 trái với quy tắc ghi `camera_permission_result` của T-026;
  - plan T-016 đặt dải tư thế trong `CameraViewModel`, trong khi T-014 tạo `PoseViewModel`.
  
  Trong phiên còn hai lỗi khác: T-012 để `AppNavHost` trong ngoại lệ viết test trước, nên lỗi luồng chỉ lộ ra ở `ui-test` (L-030); test `starting_dark` chụp một hiệu ứng chạy mãi (L-029). `run-check.py`, chạy thử trên RUN-20261023-02, còn thấy: spec camera và spec onboarding vẫn có dòng lịch sử "Chờ xác nhận lại"; năm plan có ngoại lệ viết test trước ở vỏ app; nhiều cặp plan cùng sửa `AppNavHost.kt` mà không phụ thuộc nhau.
- **Bằng chứng:** plan T-016 phiên bản 1 và 2, plan T-012 phiên bản 2 (mục 9), lần chạy 39821691045 (`ui-test` của PR #36), issue #35; kết quả `scripts/run-check.py docs/runs/RUN-20261023-02.md`.
- **Đề xuất sửa:** thêm `scripts/run-check.py`, kiểm cả danh sách chạy: spec đã chốt và trả lời hết, phụ thuộc đúng thứ tự, kịch bản có test, plan cùng sửa một file, ngoại lệ ở vỏ app. Thêm `docs/ops/pre-run-review.md`: một lần rà soát độc lập với ngữ cảnh mới, tìm mâu thuẫn, test chưa đủ tốt và điều kiện để phiên chạy được, kết quả ghi vào mục Kiểm tra trước khi duyệt của danh sách chạy. Dev chỉ duyệt danh sách khi không còn điểm chặn. Phiên chạy lại `run-check.py` ở đầu phiên.
- **Áp dụng:** app: PR #39. dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-044).
- **Kiểm chứng:**

### L-041: Quyền và thư viện cần thêm chỉ lộ ra khi viết code, phiên phải dừng hoặc đi vòng

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01 (T-013), chuẩn bị RUN-20261023-01 (T-016).
- **Chuyện gì:**
  - **T-013:** plan không biết `CameraControl.setZoomRatio` trả `ListenableFuture` mà Guava không có trên classpath biên dịch. Thêm thư viện là điều kiện dừng, nên phiên gọi hàm qua reflection, kèm rủi ro R8 xóa hàm ở bản minify (L-007).
  - **T-016 phiên bản 1:** plan cần `WRITE_EXTERNAL_STORAGE` cho Android 9 trở xuống, trong khi spec không có quyền này. Lỗi chỉ được thấy khi người rà soát bằng tay trước khi duyệt; nếu không, phiên sẽ dừng task ở điều kiện 2.
  - Chưa có bước nào bắt plan tra quyền theo từng phiên bản Android, thư viện trên classpath biên dịch, hay quyền mà thư viện kéo vào manifest gộp.
- **Bằng chứng:** L-007; plan T-016 phiên bản 1 và 2 (mục 1, mục 8 câu 1); spec `camera`, dòng lịch sử ngày 2026-10-22.
- **Đề xuất sửa:**
  - Mẫu plan mục 3.3 có hai dòng bắt buộc: Quyền truy cập (thêm, kèm phần spec cho phép; không thêm) và Thư viện và SDK.
  - `docs/CLAUDE.md` mục Khi viết plan: cách tra quyền theo phiên bản, thư viện trên classpath biên dịch, và quyền trong manifest gộp.
  - Mẫu spec mục 6 có Quyền của app và dịch vụ bên ngoài, để người phụ trách sản phẩm quyết từ lúc viết spec.
  - `plan-check.py` quy tắc 8: plan `draft` nhắc quyền mà manifest chưa có nhưng không quyết, thêm quyền mà không ghi spec hay thiếu `permission` và mức cao, hoặc dùng thư viện chưa có trong catalog mà không sửa `gradle/libs.versions.toml`, đều là lỗi.
  - `pre-run-review.md`: thiếu một trong các điểm trên là mức chặn.
- **Áp dụng:** app: PR #39. dự án mẫu: VGL-Studio/app-project-template#79, PR VGL-Studio/app-project-template#80 (L-045).
- **Kiểm chứng:**

### L-042: Bài học thêm ngoài phiên trùng số với bài học mà phiên thêm cùng lúc

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-22, RUN-20261023-02.
- **Chuyện gì:** PR #39 (mở trong lúc RUN-20261023-02 chạy) thêm bài học L-032 tới L-037; phiên thêm L-032 tới L-035 trong PR báo cáo. Sau khi phiên merge, PR #39 xung đột ở `docs/lessons.md` và phải đổi số 6 bài học cùng mọi chỗ nhắc tới chúng, ở PR #39, PR #40, dự án mẫu (PR #80, issue #79). Không công cụ nào báo trùng số nếu hai bài học nằm ở hai chỗ khác nhau trong sổ và git gộp được mà không xung đột.
- **Bằng chứng:** commit 02ebd3b (PR #39), 0348331 (PR #40); bình luận ở VGL-Studio/app-project-template#79.
- **Đề xuất sửa:** `docs/auto-run.md` mục 6: trước khi merge PR thêm bài học, gộp nhánh chính và đổi số cho liền sau sổ. `scripts/status.py` báo lỗi khi một số bài học dùng hai lần.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-056).
- **Kiểm chứng:**

### L-043: Trang tiến độ đếm 0 bài học vì sổ của app viết khác mẫu

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-23.
- **Chuyện gì:** `docs/STATUS.md` ghi "Bài học: 0 bài; chưa có" trong khi sổ có 41 bài. `scripts/status.py` chỉ nhận tiêu đề dạng `### L-001.` và dòng trạng thái chỉ có trạng thái; sổ của app viết `### L-001:` và `- **Trạng thái:** proposed. **Loại:** ...`. Vì vậy trang không cho thấy bài học nào chờ retro, và cả 41 bài vẫn `proposed`, chưa bài nào được nhận ở retro.
- **Bằng chứng:** `docs/STATUS.md` mục 5 trước commit của bài học này; `grep -c '^### L-[0-9]*:' docs/lessons.md` ra 41.
- **Đề xuất sửa:** `scripts/status.py` nhận cả hai dạng tiêu đề và đọc chữ trạng thái đầu tiên; có test cho dạng của app.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-057).
- **Kiểm chứng:**

### L-044: Plan đổi cổng chất lượng bỏ sót một kiểm tra mà trường hợp mới phải đi qua

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** T-028, phiên có người theo dõi 2026-10-23.
- **Chuyện gì:** plan T-028 cho `merge-if-green.sh` nhận bản revert đúng khi `main-broken` mở, "rồi đi tiếp tới các kiểm tra còn lại". Kiểm tra "phiên đang chạy" ngay sau đó từ chối mọi nhánh ngoài `claude/T-*` và nhánh báo cáo, nên trong phiên, bản revert vẫn bị từ chối và điểm 4 của ADR-1002 không làm được. Plan đã qua duyệt và duyệt chéo; lỗi chỉ thấy khi viết code.
- **Bằng chứng:** nhật ký plan T-028, mục Lệch so với plan; test `merge-if-green lets the exact revert through while a run is running` (đỏ trước khi sửa); PR #47.
- **Đề xuất sửa:** `docs/CLAUDE.md` mục Khi viết plan: plan đổi `merge-if-green.sh` hay `main-guard.yml` liệt kê mọi kiểm tra hiện có và nói trường hợp mới qua hay bị chặn ở từng kiểm tra, mỗi kiểm tra một test.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-058).
- **Kiểm chứng:**

### L-045: Plan đã duyệt nhưng ticket vẫn `ready`, mục 9 còn chữ mẫu

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** T-028, phiên có người theo dõi 2026-10-23.
- **Chuyện gì:** người duyệt chuyển plan T-028 sang `approved` và trả lời câu hỏi, nhưng ticket để `ready` và mục 9 còn chữ mẫu. `plan-check.py` chỉ báo mục 9; trạng thái ticket chỉ được `run-check.py` kiểm, mà task chạy trong phiên có người theo dõi không qua danh sách chạy. Phải thêm một lượt sửa trước khi merge.
- **Bằng chứng:** PR #47, commit e00cd7d.
- **Đề xuất sửa:** `plan-check.py` báo chú ý khi plan `approved` mà ticket còn `backlog`, `ready` hay `planned`; `docs/CLAUDE.md` nhắc người duyệt chuyển ticket cùng lúc.
- **Áp dụng:** app: PR #48. dự án mẫu: đã có (quy tắc 10 của `plan-check.py`, `status.py --sync`, L-050 của dự án mẫu), không đề xuất.
- **Kiểm chứng:**

### L-046: Hết hạn mức GitHub Actions làm mọi job không chạy, trông như CI đỏ

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-23, T-029 đến T-032.
- **Chuyện gì:** sau khi merge PR #49, main-guard đỏ; CI của PR #50 cũng đỏ. Job không chạy bước nào, chú thích của lần chạy ghi "The job was not started because recent account payments have failed or your spending limit needs to be increased". `wait-run.sh` chỉ báo `ĐỎ`, job `guard`, không nói lý do; phải đọc chú thích của check run mới biết. Phiên tự chạy gặp chuyện này sẽ thử sửa code hay chạy lại vô ích, rồi dừng. PR #51 thậm chí không có lần chạy nào, phải đóng rồi mở lại PR sau khi người nâng hạn mức.
- **Bằng chứng:** main-guard 39916343646 lần 1, pr-check 39916715279 lần 1 và 2 (0 bước); chạy lại sau khi nâng hạn mức thì xanh.
- **Đề xuất sửa:** `wait-run.sh` thấy job đỏ mà 0 bước thì đọc chú thích của check run và in `HẠ TẦNG: GitHub không cấp máy (<chú thích>)` với mã thoát riêng; `auto-run.md` ghi: gặp mã này thì không sửa code, ghi vào báo cáo, chờ người; `preflight.yml` không phát hiện được (chính nó cũng không chạy), nên `start-run.yml` không bắt đầu phiên khi lần chạy gần nhất của `preflight.yml` không xanh.
- **Áp dụng:** plan T-033: `wait-run.sh` báo `HẠ TẦNG` (mã 4) khi mọi job đỏ đều không chạy bước nào, kèm chú thích của GitHub; `auto-run.md`: kết thúc phiên mà giữ `running`, phiên tiếp tục chạy lại các lần chạy bị bỏ (`rerun-failed-jobs`). Dự án mẫu: issue #106, PR #107.
- **Kiểm chứng:**

### L-047: `merge-if-green.sh --update` thất bại ở mọi PR vì `docs/STATUS.md`

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-23, PR #51, #52.
- **Chuyện gì:** cả hai lần dùng `--update` đầu tiên, GitHub trả `422 merge conflict` chỉ vì `docs/STATUS.md`. Mỗi PR sửa trạng thái đều sinh lại trang, nên PR thứ hai trở đi luôn xung đột; `--update` (T-029) gần như không bao giờ dùng được, và mỗi PR tốn thêm một vòng gộp tay cùng một lần CI 15 phút. Cách (c) của L-016 (gộp tay theo hướng dẫn) giữ được an toàn nhưng không bớt vòng nào.
- **Bằng chứng:** `merge-if-green.sh 51 --update` và `52 --update`, cùng thông báo `gh: merge conflict between base and head (HTTP 422)`; xung đột duy nhất là `docs/STATUS.md`.
- **Đề xuất sửa:** cách (a) của L-016: PR thường không commit `docs/STATUS.md`; một workflow sinh lại trang trên nhánh chính sau mỗi lần merge, `status.py --check` chỉ chạy trên nhánh chính. Cần ADR, vì workflow đẩy commit lên nhánh chính ngoài `merge-if-green.sh`. Cách nhẹ hơn: `merge-if-green.sh --update` gặp 422 mà xung đột chỉ ở `docs/STATUS.md` thì tự gộp, sinh lại trang và đẩy lên nhánh của PR (không đụng nhánh chính), cần plan rủi ro cao.
- **Áp dụng:** plan T-034 (cách (a), không ADR vì nhánh chính không bị ghi): `docs/STATUS.md` không còn trong git (`.gitignore`); `status-page.yml` sinh trang sau mỗi lần đẩy lên nhánh chính có đổi `docs/` và đẩy đè lên nhánh `status`; `docs/CLAUDE.md` mục Trang tiến độ, `auto-run.md` PR báo cáo. Dự án mẫu: issue #104, PR #109 (thay cách hiện có của dự án mẫu, anh Nhật quyết định).
- **Kiểm chứng:**

### L-048: PR xung đột không có lần chạy CI nào, `wait-run.sh` chờ trọn 40 phút

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-23, PR #51, #56.
- **Chuyện gì:** GitHub không chạy workflow `pull_request` cho PR đang xung đột với nhánh chính. PR #56 xung đột ở `docs/STATUS.md` ngay khi mở (PR #55 vừa merge), không có lần chạy `pr-check` nào; `wait-run.sh --pr 56` chờ đủ 40 phút rồi báo "chưa thấy lần chạy nào". PR #51 cũng không có lần chạy vì cùng lý do; lúc đó tưởng là do hạn mức Actions (L-046) và đã đóng rồi mở lại PR. Một agent con của T-031 cũng gặp 40 phút chờ và tưởng là lỗi của `wait-run.sh`.
- **Bằng chứng:** `pulls/56` có `mergeable_state: dirty`, `actions/runs?head_sha=347472b` trả 0 lần chạy; PR #51 trước khi gộp nhánh chính.
- **Đề xuất sửa:** `wait-run.sh --pr` chưa thấy lần chạy mà PR `dirty` thì báo `XUNG ĐỘT` ngay với mã riêng; phiên gộp nhánh chính rồi đẩy. Gốc của phần lớn xung đột là `docs/STATUS.md` (L-047).
- **Áp dụng:** plan T-033: `wait-run.sh --pr` chưa thấy lần chạy mà PR `dirty` thì báo `XUNG ĐỘT` ngay (mã 5); `auto-run.md` bước 8: gộp nhánh chính rồi chờ lại. Gốc chính của xung đột (`docs/STATUS.md`) đã bỏ ở plan T-034. Dự án mẫu: issue #106, PR #107.
- **Kiểm chứng:**

### L-049: `java-check.sh` đọc nhầm dòng "Picked up JAVA_TOOL_OPTIONS", `check.sh` từ chối trên cloud

- **Trạng thái:** verified. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261023-03, kiểm tra đầu phiên.
- **Chuyện gì:** môi trường cloud đặt `JAVA_TOOL_OPTIONS` (proxy, truststore), nên `java -version` in dòng `Picked up JAVA_TOOL_OPTIONS: ...` trước dòng phiên bản. `scripts/java-check.sh` lấy dòng đầu (`head -n 1`), không đọc được phiên bản và dừng `scripts/check.sh`. Java thật là 21 (nằm trong khoảng 17 đến 24). Phiên không chạy task nào.
- **Bằng chứng:** `start-check.log`: `LỖI: không đọc được phiên bản Java từ 'Picked up JAVA_TOOL_OPTIONS: ...'`; `java -version` in `openjdk version "21.0.12.1"` ở dòng thứ hai.
- **Đề xuất sửa:** `scripts/java-check.sh` lấy dòng khớp `version "` thay vì dòng đầu (`grep -m1 ' version "'`), kèm test với đầu ra có dòng `Picked up`. Sửa file này là đổi cổng chất lượng: plan rủi ro cao, duyệt chéo. Phiên không tự sửa script và không bỏ `JAVA_TOOL_OPTIONS` (cấu hình tải nằm ở setup script).
- **Áp dụng:** T-035, PR #69 (`scripts/java-check.sh` đọc dòng có phiên bản; test với dòng `Picked up`). Dự án mẫu: không đề xuất, `java-check.sh` của mẫu đọc `java.specification.version` nên không gặp lỗi này.
- **Kiểm chứng:** RUN-20261023-04: kiểm tra đầu phiên trên cloud qua, `scripts/check.sh` nền qua (9 phút 10 giây, 748 test JVM); Tuấn xác nhận ngày 2026-10-24.

### L-050: `AppLaunchTest` đỏ vì tải file R2 hết giờ chờ trên runner; quy tắc "chạy lại một lần" biến nó thành lỗi thật

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04, T-022, PR #71.
- **Chuyện gì:** `ui-test` đỏ hai lần liên tiếp (lần chạy 39997301164, lần 1 và lần chạy lại) ở `AppLaunchTest.appStarts_passesOnboarding_andReachesCamera`, không liên quan code của task. Tải file R2 (ảnh viền, ảnh nhỏ của bộ khởi đầu) hết giờ chờ 15 giây trên runner; màn tải dừng ở lỗi và chờ người bấm "Thử lại"; test chờ 60 giây rồi hết giờ. Chỉ một commit mới (một sửa nhỏ hợp lệ theo rà soát) cho lần chạy xanh (40000627087).
- **Bằng chứng:** log `VGL.Loading: Task STARTER_SET failed`, `VGL.Assets: Could not download ... SocketTimeoutException`; lần 2 cùng dấu hiệu.
- **Đề xuất sửa:** `AppLaunchTest` bấm "Thử lại" khi màn tải lỗi, hoặc chờ lâu hơn; `auto-run.md` bước 8 ghi cách xử lý khi `ui-test` đỏ vì tải R2 hết giờ chờ (không tính vào 3 lần sửa, không tự đẩy commit rỗng).
- **Áp dụng:** T-039, PR #104: dòng PB-01 của `docs/ops/failure-playbook.md` (`ui-test` đỏ vì tải R2 hết giờ chờ: chạy lại job đỏ, không tính vào 3 lần sửa). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#129.
- **Kiểm chứng:**

### L-051: nhánh task chỉ nằm trên máy cloud khi lệnh đẩy bị từ chối

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04, T-017.
- **Chuyện gì:** phiên con của T-017 làm xong bước 3 đến 5 (4 commit, `check.sh` qua) nhưng `git push` bị bộ phân loại tự động từ chối ("dangerous", không giải thích); sáu lệnh đẩy tương tự của các phiên con khác trước và sau đó qua. Phiên chính không đẩy thay. Công việc chỉ nằm trên máy cloud tạm thời.
- **Bằng chứng:** `git branch -r` không có `claude/T-017-camera-diem-khop`; báo của phiên con.
- **Đề xuất sửa:** `auto-run.md` bước 3: đẩy nhánh task rỗng ngay lúc tạo (như phiên chính đã làm với T-022), để từ chối nếu có xảy ra sớm, trước khi tốn công viết code.
- **Áp dụng:** T-037, PR #105: `scripts/save-branch.sh` (đẩy sau mỗi commit, thử lại lỗi mạng, giữ nhánh thành bundle khi không đẩy được, mã 3); `auto-run.md` bước 3, 7, mục "Khi không đẩy được". Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#129.
- **Kiểm chứng:**

### L-052: rà soát độc lập và phiên viết code kết luận khác nhau về một điểm chặn

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04, T-015.
- **Chuyện gì:** rà soát độc lập nêu điểm chặn (vùng chạm thanh trượt dưới 48 dp) bằng đọc code; phiên viết code viết test chạm thật ở cả hai phía và không tái hiện được. Phiên chính tin test chạm thật và ghi cho người kiểm tay. Quy trình không có chỗ ghi "đã kiểm, không tái hiện".
- **Bằng chứng:** nhật ký plan T-015; `AdjustPanelTest.adjustSlider_touchHeight48`.
- **Đề xuất sửa:** mẫu rà soát độc lập (`auto-run.md` bước 6) cho phép điểm chặn có trạng thái "không tái hiện" kèm bằng chứng và mục kiểm thủ công trong báo cáo.
- **Áp dụng:** Nhật nhận ngày 2026-10-28. Thuộc quy trình chung (`docs/auto-run.md` bước 6), đề xuất về dự án mẫu: VGL-Studio/app-project-template#250. Dự án mẫu nhận: ADR-0035 (VGL-Studio/app-project-template#264), app nhận qua đợt đồng bộ 4 (PR #224).
- **Kiểm chứng:**

### L-053: thư viện mới qua `check.sh` và CI nhưng bản release (R8) hỏng

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04, T-019.
- **Chuyện gì:** T-019 thêm `play-review-ktx`. `check.sh`, `pr-check` và `main-guard` không build bản release, nên `:app:minifyStagingReleaseWithR8` hỏng (`Missing class com.google.android.gms.common.annotation.NoNullnessRewrite`) chỉ lộ ở `device-session` cuối phiên, sau khi PR đã merge. Mục 4.1 của plan T-019 ghi đã thử R8 qua. Bản staging không phát được cho tới khi sửa.
- **Bằng chứng:** `device-session` lần chạy 40031369521; PR nháp #78 (thêm `-dontwarn`, `:app:assembleStagingRelease` qua trên cloud).
- **Đề xuất sửa:** thêm `:app:assembleStagingRelease` (hoặc `minifyStagingReleaseWithR8`) vào cổng chất lượng khi PR thêm module hay thư viện; mục 4.1 của plan yêu cầu ghi đúng lệnh release đã chạy.
- **Áp dụng:** T-038, PR #92: `pr-check` build `:app:assembleStagingRelease` khi PR đổi thư viện, file build hay luật R8; PR thử #93 đỏ đúng lỗi của T-019. Dự án mẫu: VGL-Studio/app-project-template#131.
- **Kiểm chứng:**

### L-054: GitHub merge xong nhưng trả lỗi, PR vẫn mở và không có sự kiện đẩy

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-23, PR #67.
- **Chuyện gì:** `merge-if-green.sh 67` nhận lỗi `unexpected end of JSON input` từ API merge và in `TỪ CHỐI: GitHub từ chối merge`, nhưng commit c49a682 đã nằm trên nhánh chính. PR vẫn `OPEN`, không workflow `push` nào chạy (`main-guard`, `status-page`). Gọi lại script sẽ tạo commit trùng; người phải merge tay trên web (commit rỗng c83777a) để PR đóng, và các workflow `push` chạy trễ. Phiên tự chạy gặp chuyện này sẽ tưởng merge bị từ chối và dừng task, hoặc thử lại.
- **Bằng chứng:** đầu ra của `merge-if-green.sh 67`; `git log origin/main` có c49a682 khi PR #67 còn `OPEN`; không có lần chạy `main-guard` cho c49a682.
- **Đề xuất sửa:** `merge-if-green.sh` gặp lỗi từ API merge thì kiểm tra nhánh chính đã chứa commit đầu của PR chưa (`git merge-base --is-ancestor`, hoặc commit trên nhánh chính có số PR); có rồi thì in `ĐÃ MERGE (GitHub báo lỗi)` với mã riêng, ghi rằng workflow `push` có thể không chạy, và chạy `main-guard` bằng `workflow_dispatch`. `auto-run.md` bước 8 ghi cách xử lý mã này. Sửa script này cần plan rủi ro cao, duyệt chéo (L-044).
- **Áp dụng:** T-040, PR #107: `merge-if-green.sh` gặp lỗi từ API merge thì tìm commit của PR trên nhánh chính, in `ĐÃ MERGE (GitHub báo lỗi)`; dòng PB-04 của bảng tra lỗi. Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#129.
- **Kiểm chứng:**

### L-055: `start-run.yml` không kiểm `main-guard` xanh trên commit đầu nhánh chính

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-23, RUN-20261023-03.
- **Chuyện gì:** sau sự cố của L-054, commit đầu nhánh chính (c83777a, có code `scripts/run-check.py` của c49a682) chưa có lần chạy `main-guard` nào. `start-run.yml` vẫn khởi động phiên: nó chỉ xem issue `main-broken`, `setup-broken` và chạy `scripts/preflight.sh`, không xem `main-guard` đã chạy và xanh trên chính commit đó chưa. Phiên dựa vào kiểm tra đầu phiên của chính nó (`check.sh` nền), tốn khoảng 15 phút cloud trước khi biết nhánh chính có xanh không.
- **Bằng chứng:** lúc kích hoạt RUN-20261023-03 chưa có lần chạy `main-guard` cho c49a682 hay c83777a; `start-run.yml` xanh; các bước kiểm tra của `.github/workflows/start-run.yml`.
- **Đề xuất sửa:** `start-run.yml` thêm bước: commit đầu nhánh chính có đổi file ngoài `docs/` mà chưa có lần chạy `main-guard` xanh cho chính commit đó thì từ chối, in hướng dẫn chạy `main-guard` bằng `workflow_dispatch` rồi kích hoạt lại. Sửa `.github/` cần plan rủi ro cao, duyệt chéo.
- **Áp dụng:** T-041, PR #98: `scripts/main-guard-status.sh` trong `start-run.yml` và kiểm tra đầu phiên; `main-guard.yml` chạy được bằng tay (`workflow_dispatch`, chỉ trên `main`). Dự án mẫu: VGL-Studio/app-project-template#130.
- **Kiểm chứng:**

### L-056: kiểm tra trước phiên không chạy `java-check.sh` trong môi trường cloud

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261023-03 (L-049).
- **Chuyện gì:** `preflight.yml` và `start-run.yml` chạy trên runner GitHub, không có `JAVA_TOOL_OPTIONS` của môi trường cloud, nên lỗi của L-049 không lộ ở đó. Lỗi chỉ lộ khi phiên cloud đã khởi động và chạy `check.sh`, nên cả phiên kết thúc mà không task nào chạy. Mọi khác biệt giữa runner và môi trường cloud (biến môi trường, proxy, phiên bản công cụ) đều có thể làm hỏng phiên theo cách này.
- **Bằng chứng:** RUN-20261023-03: `start-run` xanh, phiên dừng ở `java-check.sh`; RUN-20261023-04 qua sau khi sửa T-035.
- **Đề xuất sửa:** `scripts/cloud-env-check.sh` gọi `scripts/java-check.sh` (và các kiểm tra rẻ khác mà `check.sh` chạy trước khi build), để lỗi môi trường lộ trong vài giây đầu phiên và báo rõ `env-broken`, thay vì sau `check.sh`; test của script dùng `fake_java` có dòng `Picked up`.
- **Áp dụng:** T-036, PR #103: `scripts/cloud-env-check.sh` gọi `java-check.sh` và các kiểm tra rẻ của `check.sh` trong vài giây đầu phiên. Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#130.
- **Kiểm chứng:**

### L-057: phiên không có bảng tra lỗi đã biết, nên lỗi lặp lại vẫn làm phiên dừng hay tốn vòng sửa

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** retro 2026-10-24 sau RUN-20261023-04 (L-046, L-048, L-050, L-054).
- **Chuyện gì:** mỗi lỗi hạ tầng đã gặp (hết hạn mức Actions, PR xung đột không có CI, R2 hết giờ chờ trong `ui-test`, API merge báo lỗi mà đã merge) được sửa riêng bằng một mã thoát hay một đoạn trong `auto-run.md`. Không có một chỗ duy nhất để phiên tra "thông báo này nghĩa là gì, làm gì tiếp". Lỗi mới cùng kiểu (R2 hết giờ chờ, L-050) vẫn bị tính là lỗi thật và tốn vòng CI.
- **Bằng chứng:** RUN-20261023-04, T-022: `ui-test` đỏ 2 lần vì R2; `scripts/failure-kind.sh` chỉ phân loại thô cho `main-guard`.
- **Đề xuất sửa:** `docs/ops/failure-playbook.md`: bảng mẫu thông báo lỗi (regex) và việc phải làm (chạy lại không tính vào 3 lần sửa, chờ, coi như đã merge, dừng task, dừng phiên); script phân loại đọc bảng này và in việc phải làm; mỗi bài học môi trường mới thêm một dòng.
- **Áp dụng:** T-039, PR #104: `docs/ops/failure-playbook.md` (PB-01 đến PB-10) và script phân loại đọc bảng. Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#129.
- **Kiểm chứng:**

### L-058: điều kiện dừng 1 dừng task khi task khác trong cùng danh sách đổi chỗ gọi

- **Trạng thái:** applied. **Loại:** rủi ro. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04, T-017, T-020.
- **Chuyện gì:** T-020 merge trước T-017 và tạo `CameraUnfolded.kt`, gọi thẳng `ShutterButton` và `PoseOverlay`. T-017 thêm tham số cho hai hàm đó, nên phải sửa `CameraUnfolded.kt` dù file không có trong mục 3.2 (điều kiện dừng 1). Phần sửa nhỏ, nối đúng ý của cả hai plan; người quyết định "sửa ngay, không cần plan". Phiên đông task sẽ còn gặp trường hợp này.
- **Bằng chứng:** PR #81, commit `2e28f8c`, `2d04149`; plan T-020 mục 3 ghi ý định giữ `MatchHint` ở máy gập.
- **Đề xuất sửa:** ADR của app nới điều kiện dừng 1: được sửa file ngoài mục 3.2 khi file đó do task khác của cùng danh sách tạo hay đổi sau khi plan được duyệt, phần sửa chỉ nối chỗ gọi theo ý đã ghi trong một trong hai plan, có test viết trước, ghi vào nhật ký và rà soát độc lập xét riêng phần này. Không áp dụng cho file của mục 1 thang rủi ro (`.github/`, `scripts/check.sh`, cấu hình build, manifest).
- **Áp dụng:** ADR-1003 (accepted 2026-10-24); T-043, PR #91 (`docs/risk-scoring.md` mục 4, `docs/auto-run.md` bước 4, 5, 6, 10). Dự án mẫu: VGL-Studio/app-project-template#132.
- **Kiểm chứng:**

### L-059: điểm "nên sửa" của rà soát độc lập bị để lại cho người, có điểm là lỗi thật

- **Trạng thái:** verified. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04 (T-018, T-019, T-020, T-022, T-023), PR #81.
- **Chuyện gì:** báo cáo RUN-20261023-04 liệt kê 13 điểm nên sửa chưa sửa. Một điểm là lỗi người dùng gặp được: T-018 hủy composition khi chờ quảng cáo làm `onFinished` không được gọi, màn xem lại đứng yên. Trên PR #81, hai điểm nên sửa (TalkBack, không đoạn nào so được) sửa trong 30 phút với test viết trước.
- **Bằng chứng:** báo cáo RUN-20261023-04, mục Cần người kiểm tra; PR #81.
- **Đề xuất sửa:** `auto-run.md` bước 6: phiên sửa mọi điểm nên sửa nằm trong file của mục 3.2, có test viết trước thất bại đúng lý do, không đổi điều test của plan kiểm; chỉ để người quyết điểm mâu thuẫn với plan, spec hay test của plan. Cần sửa cùng ADR của L-058.
- **Áp dụng:** ADR-1003 (accepted 2026-10-24); T-043, PR #91 (`docs/risk-scoring.md` mục 4, `docs/auto-run.md` bước 4, 5, 6, 10). Dự án mẫu: VGL-Studio/app-project-template#132.
- **Kiểm chứng:** RUN-20261026-01: T-050 có một điểm nên sửa (nhãn độ đậm dịch thành "độ trong suốt" ở `hi`, `id`, `th`, ngược nghĩa trên con trượt). Phiên sửa theo ADR-1003: test viết trước thất bại đúng lý do, sửa chữ, rà soát lần 2 cho phần sửa (0 chặn). T-053 có một điểm nên sửa mâu thuẫn với chữ của plan: không sửa, ghi vào báo cáo kèm phương án (L-079). Có hiệu quả.

### L-060: `run-check.py` không thấy hai plan phụ thuộc file của nhau

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** rà soát trước RUN-20261023-03 (điểm chặn T-017, T-020).
- **Chuyện gì:** `run-check.py` báo hai plan cùng sửa một file, nhưng không báo khi plan này tạo file mà plan kia giả định đã có (hoặc chưa có), nên thứ tự chạy sai chỉ được rà soát độc lập thấy. Khi một task dừng hay merge lệch thứ tự, không script nào báo trước file nào sẽ phải sửa thêm.
- **Bằng chứng:** RUN-20261023-03, bảng rà soát: "T-017 phải sửa `CameraUnfolded.kt` không có trong mục 3.2".
- **Đề xuất sửa:** `run-check.py` đọc mục 3.2 của mọi plan: plan sau nhắc tên file mà plan trước tạo thì phải xếp sau, và in "nếu <plan trước> chưa merge thì <plan sau> ..."; với hai plan cùng sửa một hàm có tên trong mục 3.1, in cảnh báo kèm tên hàm.
- **Áp dụng:** T-042, PR #106: `run-check.py` báo đề xuất thứ tự bị ngược và plan nhắc file mà plan khác tạo. Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#132.
- **Kiểm chứng:**

### L-061: hàng chờ plan cạn sau mỗi phiên, phiên kế tiếp phải chờ người và AI viết plan

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-23, 2026-10-24.
- **Chuyện gì:** sau RUN-20261023-02, phải một phiên có người theo dõi để viết 8 plan, trả lời câu hỏi, rà soát độc lập, soạn phiên bản 2, rồi mới lập được RUN-20261023-03. Thời gian từ lúc phiên xong tới lúc phiên kế tiếp chạy chủ yếu là chờ plan.
- **Bằng chứng:** RUN-20261023-02 kết thúc 03:35 UTC; RUN-20261023-03 bắt đầu 16:53 UTC.
- **Đề xuất sửa:** cuối mỗi phiên (sau báo cáo, khi còn hạn mức), phiên soạn plan `draft` cho các ticket `ready` kế tiếp, chạy thử mục 4.1, chạy `plan-check.py`, và gom mọi câu hỏi cần người vào một mục của báo cáo. Người chỉ trả lời và duyệt.
- **Áp dụng:** ticket T-045, plan phiên bản 2, PR #120, làm trong RUN-20261024-02 (`scripts/plan-queue.py`, `auto-run.md` mục 4 bước 5). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#133.
- **Kiểm chứng:**

### L-062: mục "kiểm thủ công" dồn lại sau mỗi phiên, phần lớn chạy được trên emulator

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04, mục Cần người kiểm tra.
- **Chuyện gì:** báo cáo RUN-20261023-04 có 7 nhóm kiểm thủ công. Nhiều mục không cần máy thật: xoay màn hình khi quảng cáo đang chờ, banner lỗi rồi làm mới, luồng "chi tiết tư thế, Chụp với tư thế này", gập mở (emulator gập). Chúng chờ người, nên lỗi như của L-059 chỉ lộ khi có người thử.
- **Bằng chứng:** báo cáo RUN-20261023-04; `device-session.yml` chỉ chạy một kịch bản chung.
- **Đề xuất sửa:** plan ghi mỗi tiêu chí kiểm thủ công là "máy thật" hay "emulator"; phần emulator thành kịch bản của `device-session.yml` (`scenarios.txt`), phiên chạy cuối phiên và đọc `summary.md`; chỉ phần máy thật (FPS, camera thật, Play) còn cho người.
- **Áp dụng:** ticket T-046, plan phiên bản 2, PR #121, làm trong RUN-20261024-02 (`scripts/device-scenario.py`, kịch bản `deviceScenarios`, quy tắc 11 của `plan-check.py`). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#133.
- **Kiểm chứng:**

### L-063: không có vòng nối từ số liệu của bản staging về thứ tự tính năng

- **Trạng thái:** accepted. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** retro 2026-10-24.
- **Chuyện gì:** phiên làm đúng danh sách, nhưng danh sách lập từ spec ban đầu. Phiên thử, phản hồi và số liệu phân tích của bản staging chưa được dùng để quyết tính năng nào làm tiếp, bỏ hay sửa, nên app có thể đủ tính năng nhưng chưa phải những tính năng hợp lý nhất.
- **Bằng chứng:** `docs/test-sessions/`, `features/*/feedback/` chưa dẫn tới đề xuất xếp lại backlog nào.
- **Đề xuất sửa:** sau mỗi bản staging, AI viết một đề xuất xếp lại backlog (thêm, bỏ, đổi thứ tự, sửa spec), mỗi dòng có bằng chứng từ phiên thử, phản hồi hay số liệu; người phụ trách sản phẩm chốt. Mẫu tài liệu mới trong `docs/templates/`.
- **Áp dụng:** Nhật nhận ngày 2026-10-28. Làm thử lần đầu: đề xuất xếp lại backlog sau bản `0.1.0-staging.15`, lưu ở `docs/backlog-reviews/`. Mẫu tài liệu chung trong `docs/templates/` đề xuất về dự án mẫu sau khi thấy được việc.
- **Kiểm chứng:**

### L-064: không có danh sách sẵn sàng phát hành, nên không biết còn bao xa tới bản production

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-04 (mã quảng cáo thật cho `poses_banner`, `poses_detail_banner`, `review_interstitial`).
- **Chuyện gì:** việc cần cho bản production nằm rải rác trong báo cáo các phiên: mã quảng cáo thật còn thiếu, kiểm thủ công chưa làm, lỗi đang mở, ảnh chuẩn chưa xem. Không ai thấy một danh sách, và `scripts/release.sh` chỉ báo thiếu khi đã chạy.
- **Bằng chứng:** báo cáo RUN-20261023-04, mục Đề xuất về dự án mẫu.
- **Đề xuất sửa:** `scripts/status.py` sinh thêm mục "Sẵn sàng phát hành" trên trang tiến độ, từ các nguồn đã có (vị trí quảng cáo trong `AdPlacement` so với mã đã cấu hình ở staging, mục kiểm thủ công chưa đánh dấu trong báo cáo, issue mở, ticket chưa `on-staging`). AI không đọc cấu hình production.
- **Áp dụng:** ticket T-047, plan phiên bản 2, PR #122, làm trong RUN-20261024-02 (`scripts/status.py`, mục 6 của trang tiến độ). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#133.
- **Kiểm chứng:**

### L-065: file mới nhắc tới file chung làm mọi plan đã duyệt lỗi `plan-check`, phiên sẽ bỏ qua cả danh sách

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-24, lập RUN-20261024-01.
- **Chuyện gì:** T-041 thêm `scripts/main-guard-status.sh`; chú thích của nó nhắc `scripts/tests/run.sh`, `docs/auto-run.md`. Quy tắc 2 của `plan-check.py` (L-020) báo lỗi cho cả 5 plan đã duyệt của danh sách, vì plan sửa các file đó mà không nêu file đọc mới. Không ai được sửa plan đã duyệt, và phiên chạy lại `plan-check` ngay trước task rồi bỏ qua plan có lỗi: cả phiên sẽ không chạy task nào. Mỗi file mới nhắc tới một file chung đều có thể làm vậy.
- **Bằng chứng:** `scripts/run-check.py docs/runs/RUN-20261024-01.md` sau PR #98: 10 lỗi "nhắc tới ... mà plan sửa" cho T-036, T-037, T-039, T-040, T-042.
- **Đề xuất sửa:** quy tắc 2 báo lỗi khi plan còn `draft`, chỉ chú ý khi plan đã `approved`, như quy tắc 7; file đọc thật sự bị ảnh hưởng sẽ lộ ra ở test của task, và phiên xử lý theo ADR-1003 hay điều kiện dừng.
- **Áp dụng:** `scripts/plan-check.py` quy tắc 2, test trong `scripts/tests/run.sh` (PR này). Dự án mẫu: VGL-Studio/app-project-template#132.
- **Kiểm chứng:**

### L-066: `wait-run.sh --pr` gọi ngay sau khi đẩy đọc lần chạy cũ bị hủy và báo đỏ

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-24, PR #98, #100.
- **Chuyện gì:** đẩy commit mới lên nhánh của PR thì GitHub hủy lần chạy `pr-check` của commit trước (`cancelled`) rồi mới tạo lần chạy mới. `wait-run.sh --pr` đọc commit đầu của PR một lần lúc bắt đầu; gọi trong lúc lần chạy của commit trước bị hủy thì thấy `cancelled` và báo `ĐỎ (cancelled)`, mã 1. Theo số đo của PR #100, lần chạy của commit mới (`eba5f98`) được tạo trước (05:31:35Z), lần chạy của commit cũ (`e35ae86`) bị hủy sau đó (05:32:16Z). Ba lần trong một buổi; phiên tự chạy gặp mã 1 sẽ đọc log, không thấy lỗi, và có thể tính nhầm vào 3 lần sửa hay chạy lại vô ích.
- **Bằng chứng:** PR #100, lần chạy 40066418977 `cancelled` (commit `e35ae86`) khi commit mới đã đẩy; lần chạy kế 40066593456 xanh. PR #98: cùng dấu hiệu.
- **Đề xuất sửa:** `wait-run.sh --pr` chỉ nhận lần chạy có `head_sha` bằng commit đầu hiện tại của PR (đọc `pulls/<số>` mỗi vòng), và coi `cancelled` của commit cũ là "chưa có lần chạy" thay vì đỏ; test với `gh` giả. Trong lúc chờ sửa: `auto-run.md` bước 8, kết quả `cancelled` thì gọi lại `wait-run.sh`, không tính là lỗi.
- **Áp dụng:** ticket T-044, plan phiên bản 2, PR #119, làm trong RUN-20261024-02 (`--pr` đọc lại commit đầu mỗi vòng, `--sha`). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#129.
- **Kiểm chứng:** RUN-20261024-02: từ PR #120, cả 3 lần chờ (PR #120, #121, #122) bằng `--sha` nhận đúng lần chạy của commit đầu, không gặp `cancelled`; trường hợp bị hủy mới có bằng chứng từ test giả.

### L-067: vòng chờ bằng `pgrep -f` khớp chính lệnh đang chạy nó, và `pkill -f` giết cả shell của phiên

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261024-01 (T-039).
- **Chuyện gì:** vòng `while pgrep -f "wait-run.sh --workflow" ...; do sleep; done` không bao giờ thoát vì `pgrep -f` thấy dòng lệnh của chính vòng đó; chạy `pkill -f "pgrep -f"` để dọn thì giết luôn shell (thoát 144). Mất khoảng 25 phút chờ vô ích.
- **Bằng chứng:** RUN-20261024-01, T-039, giờ 07:25 đến 07:35 UTC.
- **Đề xuất sửa:** `auto-run.md` bước 9 nói rõ: chờ `main-guard` chỉ bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ>` chạy ở chế độ nền, đọc kết quả từ tệp đầu ra của nó; không dùng `pgrep` hay `pkill` theo chuỗi lệnh.
- **Áp dụng:** `docs/auto-run.md` bước 9: chờ `main-guard` chỉ bằng `wait-run.sh` ở chế độ nền, không dùng `pgrep -f`, `pkill -f` (PR này). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#129.
- **Kiểm chứng:** RUN-20261024-02: 4 lần chờ `main-guard` đều bằng `wait-run.sh --workflow` ở chế độ nền, không dùng `pgrep` hay `pkill`, không treo.

### L-068: `scripts/tests/run.sh` đặt `origin` toàn cục thành URL giả, test dùng bare repo thật phải tắt biến đó

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261024-01 (T-037).
- **Chuyện gì:** đầu `run.sh` xuất `GIT_CONFIG_COUNT=1` với `remote.origin.url=https://github.com/o/r.git` cho mọi lệnh git. Khối test đẩy lên một bare repo cục bộ vì thế đẩy tới `github.com/o/r` (403). Khối `save-branch` phải `unset` ba biến này rồi đặt lại ở cuối.
- **Bằng chứng:** RUN-20261024-01, lần chạy đầu của khối `save-branch`: `access denied by the git proxy: o/r is not in this session's authorized repository set`.
- **Đề xuất sửa:** thêm chú thích đầu `scripts/tests/run.sh` nêu điều này, hoặc một hàm `with_real_git_config` dùng chung.
- **Áp dụng:** chú thích đầu `scripts/tests/run.sh` (PR này). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#129.
- **Kiểm chứng:**

### L-069: script dùng mảng rỗng với `set -u` dừng trên bash 3.2 của macOS, `check.sh` đỏ trên máy dev

- **Trạng thái:** accepted. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-24, sau RUN-20261024-01 (T-036, PR #103).
- **Chuyện gì:** `scripts/cloud-env-check.sh` in danh sách lỗi bằng `"${setup_problems[@]}"`. Bash 3.2 (bash mặc định của macOS) coi mảng rỗng là biến chưa đặt khi có `set -u`, nên script dừng ở dòng đó trước khi in thông báo. 5 test của `scripts/tests/run.sh` đỏ trên máy Mac, nên `scripts/check.sh` đỏ trên mọi máy dev từ PR #103. CI và môi trường cloud dùng bash 5 nên không thấy.
- **Bằng chứng:** `scripts/cloud-env-check.sh: line 54: setup_problems[@]: unbound variable`; 5 test `cloud-env-check.sh names ...` đỏ trên nhánh chính 5ed9c51 với `GNU bash, version 3.2.57`.
- **Đề xuất sửa:** dùng `${a[@]+"${a[@]}"}` cho mảng có thể rỗng (đã sửa trong PR này); thêm một quy tắc vào `scripts/tests/run.sh` hay `plan-check.py` dò `"${...[@]}"` trong script có `set -u`, hoặc chạy test của script bằng bash 3.2 trong CI (job macOS hay ảnh `bash:3.2`).
- **Áp dụng:** sửa `scripts/cloud-env-check.sh` (PR #109); phần dò tự động (chạy test của script bằng bash 3.2, hay quy tắc dò `"${...[@]}"` trong script có `set -u`) chưa có ticket. Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#130.
- **Kiểm chứng:**

### L-070: PR chỉ sửa một script của cổng chất lượng mà không sửa `scripts/tests/run.sh` thì không chạy test của script

- **Trạng thái:** accepted. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-24, viết plan T-046.
- **Chuyện gì:** bộ lọc `code` của `pr-check.yml` chỉ gồm `scripts/check.sh`, `scripts/test-summary.sh`, `scripts/tests/**`, `scripts/find-breaking-commit.sh` trong `scripts/`. Một PR chỉ sửa `scripts/merge-if-green.sh`, `wait-run.sh`, `run-check.py`, `plan-check.py`, `device-session.sh` hay script khác mà không đụng `scripts/tests/` thì `build-and-test` bị bỏ qua, nên `scripts/tests/run.sh` (gọi từ `check.sh`) không chạy và PR vẫn xanh. Các PR trước qua được chỉ vì luôn thêm test vào `run.sh`.
- **Bằng chứng:** `.github/workflows/pr-check.yml`, khối `filters`; các PR #99, #103 đến #107 đều sửa kèm `scripts/tests/run.sh`.
- **Đề xuất sửa:** thêm `'scripts/**'` vào bộ lọc `code` (hay ít nhất `scripts/*.sh`, `scripts/*.py`). Sửa `.github/` cần plan rủi ro cao, duyệt chéo. Trong lúc chờ: plan sửa script ghi `scripts/tests/run.sh` ở mục 3.2 (đã là thói quen, nhưng chưa thành quy tắc).
- **Áp dụng:** chưa làm: cần plan rủi ro cao sửa `.github/workflows/pr-check.yml` (thêm `scripts/**` vào bộ lọc `code`), chưa có ticket. Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#138.
- **Kiểm chứng:**

### L-071: plan dùng `$TMPDIR` cho bản chép script nhưng môi trường cloud không đặt biến đó

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261024-02 (T-044).
- **Chuyện gì:** mục 6 của plan T-044 viết `git show origin/main:scripts/wait-run.sh > "$TMPDIR/wait-run.sh"`. Trong phiên cloud `TMPDIR` rỗng, nên lệnh ghi thẳng vào `/wait-run.sh` ở gốc ổ đĩa. Cùng lúc, lệnh chờ bằng `sleep` đứng đầu bị công cụ chặn, phải viết lại thành vòng `for ... sleep` nằm trong lệnh.
- **Bằng chứng:** RUN-20261024-02, giờ 15:12 UTC: `echo "TMPDIR=[$TMPDIR]"` in `TMPDIR=[]`; file `/t044-tests.txt` nằm ở gốc.
- **Đề xuất sửa:** plan và `auto-run.md` ghi `${TMPDIR:-/tmp}` (hoặc thư mục scratchpad của phiên) thay cho `$TMPDIR`; ghi rõ vòng chờ phải là `for i in ...; do ...; sleep N; done` hoặc chạy nền, không bắt đầu lệnh bằng `sleep`.
- **Áp dụng:** `scripts/plan-check.py` quy tắc 12 (plan `draft` ghi `$TMPDIR` trần thì lỗi, plan `approved` thì chú ý), `docs/auto-run.md` bước 9, `docs/CLAUDE.md` mục Khi viết plan (PR này). Tuấn nhận ngày 2026-10-24. Dự án mẫu: VGL-Studio/app-project-template#150.
- **Kiểm chứng:** RUN-20261025-01: tệp tạm của phiên ở thư mục scratchpad, vòng chờ viết `for ... sleep`; code mới của T-048 dùng `$work`.

### L-072: test của script dùng `sed -i` kiểu GNU, bộ test dừng giữa chừng trên macOS

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-24, sau RUN-20261024-02 (T-047, PR #122).
- **Chuyện gì:** test `status` mà T-047 thêm vào `scripts/tests/run.sh` gọi `sed -i 's/.../'`. `sed` của macOS (BSD) đọc chữ ngay sau `-i` là đuôi tệp sao lưu, nên báo `invalid command code` và bộ test dừng ở test thứ 395, không in dòng tổng kết: `check.sh` đỏ trên máy dev. CI chạy Linux với `sed` GNU nên xanh; phiên cloud cũng là Linux nên không thấy. Cùng loại với L-069 (bash 3.2): lỗi chỉ lộ ra trên máy dev, sau khi đã merge.
- **Bằng chứng:** chạy `scripts/tests/run.sh` trên macOS ngày 2026-10-24: `sed: 1: "/var/folders/...": invalid command code f`; PR #124 sửa thành `sed -i.bak ... && rm`.
- **Đề xuất sửa:** trong script và test chỉ dùng dạng chạy được trên cả hai (`sed -i.bak ... && rm ...bak`, hay ghi ra tệp mới rồi `mv`); thêm vào phần dò tự động của L-069 một quy tắc tìm `sed -i` không có đuôi trong `scripts/`.
- **Áp dụng:** PR #124 sửa thành `sed -i.bak ... && rm`; dự án mẫu đã nhận (VGL-Studio/app-project-template#150, đã đóng). Phần dò tự động trong quy tắc của L-069 không làm (Nhật quyết ngày 2026-10-28).
- **Kiểm chứng:** 2026-10-28: `grep -rnE "sed -i( |'|\")" scripts/` không còn dòng nào thiếu đuôi; `scripts/check.sh` qua trên macOS ở RUN-20261028-01.

### L-073: `token-usage.py` gán token của phiên con vào dòng Chung, số theo task thiếu phần lớn

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261025-01 (T-048).
- **Chuyện gì:** T-048 giao bước 3 đến 5 cho một phiên con làm trong worktree `../task-T-048` (L-031, L-034), và rà soát độc lập cho một phiên con khác. Transcript của cả hai phiên con ghi `cwd` là thư mục chính và `gitBranch` là nhánh của thư mục chính (nhánh mặc định của phiên), vì lệnh của chúng chỉ `cd` vào worktree trong từng lệnh shell. `scripts/token-usage.py` gán theo nhánh, nên T-048 hiện 589k (0,1 lần ước tính 9M) còn phần thật khoảng 12,4M nằm ở dòng Chung. Chạy lại khi worktree còn trong `git worktree list` cũng không đổi.
- **Bằng chứng:** RUN-20261025-01, mục Token: `subagents/agent-*.jsonl` có 81 và 49 dòng, tất cả `('/home/user/camera-pose', 'claude/run-20261025-01-esag49')`; đếm tay 7,96M và 3,81M.
- **Đề xuất sửa:** `scripts/token-usage.py` gán transcript phiên con theo worktree hay nhánh xuất hiện trong lệnh của nó (`cd /home/user/task-T-NNN`, `git -C ...`), hoặc theo một bảng phiên con và task mà phiên chính ghi; `docs/ops/token-usage.md` ghi giới hạn này. Trong lúc chờ, báo cáo phiên đếm tay phần phiên con.
- **Áp dụng:** T-053 (PR này): `scripts/token-usage.py` chép phần `BRIEF_TASK`, `message_text` của dự án mẫu, gán mọi request của transcript trong `subagents/` theo dòng `Task: T-NNN` ở tin nhắn đầu; 4 test mới trong `scripts/tests/run.sh`; `docs/auto-run.md` (Giao bước 3 đến 5, bước 6, mục 4 bước 5) và `docs/ops/token-usage.md` ghi dòng `Task: T-NNN` trong lời giao việc cho phiên con. Tuấn nhận ngày 2026-10-25. Dự án mẫu: đã có (L-071 của mẫu); VGL-Studio/app-project-template#163.
- **Kiểm chứng:** RUN-20261026-01 (sau T-053): lần đầu bảng token có phần của phiên con (rà soát độc lập) trong dòng của task; dòng Chung chỉ còn kiểm tra đầu phiên (2.2M). Có hiệu quả; hai chỗ còn lệch là L-079 và L-080.

### L-074: `auto-run.md` bảo soạn phản hồi kịch bản lỗi mức `nen-sua`, mẫu phản hồi không có mức đó

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261025-01.
- **Chuyện gì:** `docs/auto-run.md` mục 4, bước 1 bảo mỗi dòng `lỗi` của "Kịch bản theo plan" có một phản hồi `FB-...` "mức `nen-sua`". Mẫu `docs/templates/feedback.md` chỉ có `chan`, `nang`, `nhe`, `gop-y`. Phiên phải chọn một bên; FB-20261025-01 ghi `nen-sua` theo auto-run.md, kèm chú thích.
- **Bằng chứng:** `docs/auto-run.md` dòng 289; `docs/templates/feedback.md`, dòng `severity`; `docs/features/quality-gate/feedback/FB-20261025-01-cham-sau-device-unfold.md`.
- **Đề xuất sửa:** thêm `nen-sua` vào danh sách mức của mẫu phản hồi (kèm định nghĩa), hoặc đổi auto-run.md sang một mức có sẵn; kiểm `scripts/status.py` đọc mức của phản hồi thế nào.
- **Áp dụng:** `docs/auto-run.md` mục 4, bước 1 đổi sang mức `nhe` có sẵn của mẫu phản hồi; FB-20261025-01 ghi `nhe`; mẫu `docs/templates/feedback.md` và `scripts/status.py` không đổi (PR này). Tuấn nhận ngày 2026-10-25. Dự án mẫu: VGL-Studio/app-project-template#163.
- **Kiểm chứng:**

### L-075: plan có test chạy trên chuỗi giả mà người viết suy kết quả bằng tay, kỳ vọng sai qua 4 vòng rà soát

- **Trạng thái:** verified. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-24, viết plan T-048 (PR #126).
- **Chuyện gì:** plan T-048 có nhiều test của `scripts/device-scenario.py` chạy trên `scripts/tests/fake-adb` với chuỗi dump giả. Người viết plan đọc code rồi suy ra test nào thất bại và vì sao, nên mỗi vòng rà soát độc lập lại tìm được điểm chặn mới trong phần kỳ vọng: 4 vòng liền có điểm chặn (2, 1, 2, 2).
- **Bằng chứng:** rà soát plan T-048 vòng 1 đến 4: `ui-seq.n` của `fake-adb` không vượt số tệp của chuỗi dump nên test giả định lần dump thứ n sai; một test qua ngay trên code cũ mà không ghi là đối chứng; công thức ngân sách giờ thiếu phần dự phòng bằng chứng; tự từ chối hộp thoại đồng ý không dump lại. Chỉ hết điểm chặn sau khi phiên viết plan chạy thật mọi test mới trên code hiện tại trong một khối tạm không commit và ghi kết quả thật vào mục 4.1. RUN-20261025-01 sau đó có đúng 29 trên 32 test thất bại đúng lý do và 3 đối chứng như plan, CI xanh ngay lần đầu.
- **Đề xuất sửa:** test của script chạy trên chuỗi giả hay tệp mẫu (adb giả, dump giả, repo giả) được chạy thật trên code hiện tại ngay lúc viết plan; mục 4.1 ghi kết quả thật của từng test (lý do thất bại, hay "đối chứng, qua"); không suy kết quả bằng cách đọc code.
- **Áp dụng:** `docs/CLAUDE.md` mục Khi viết plan, `docs/ops/pre-run-review.md` mục 2.2 (PR này). Tuấn nhận ngày 2026-10-25. Dự án mẫu: VGL-Studio/app-project-template#163 (dự án mẫu không nhận, L-098 của mẫu). Đồng bộ đợt 1 (PR #179): `pre-run-review.md` lấy bản mẫu không còn mục 2.2; app giữ quy tắc ở `docs/CLAUDE.md`, mục Khi viết plan và mục Khi lập danh sách chạy. Chỉ chạy các test mới của script lúc viết plan, không trái ADR-0029 (ADR này chỉ bỏ việc chạy toàn bộ test lúc soạn plan).
- **Kiểm chứng:** RUN-20261025-02, T-049 (plan viết theo bài học này, mục 4.1 ghi kết quả thật của 11 test với `adb` giả). Ở bước 3, cả 11 test cho đúng giá trị mục 4.1 ghi: 7 thất bại với cùng chuỗi kết quả, 4 đối chứng qua. Bước 4 viết đúng mục 3.1 thì qua hết; không có vòng sửa test nào, rà soát độc lập 0 chặn, CI xanh ở lần chạy đầu. Có hiệu quả. RUN-20261026-01: cả 4 plan ghi kết quả thật ở mục 4.1. Ở bước 3, mọi test cho đúng kết quả đã ghi: T-053 3 thất bại, 1 đối chứng; T-052 6 thất bại (cùng chuỗi kết quả), 3 đối chứng; T-050 2 thất bại, 4 đối chứng; T-051 8 thất bại Gradle và 3 thất bại script như mục 4.1. Không có vòng sửa test nào; 4 PR xanh ở lần CI đầu. Có hiệu quả.

### L-076: phiên đẩy commit test của task sau trước khi có kết quả `main-guard` của lần merge trước

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261026-01 (T-052, T-050).
- **Chuyện gì:** `docs/auto-run.md` bước 3 bảo đẩy nhánh task ngay sau mỗi commit; bước 9 bảo trước khi đẩy commit đầu tiên của task sau phải có kết quả `main-guard` của lần merge trước. Phiên làm theo bước 3 và đẩy commit test của task sau khi `main-guard` còn chạy. Không gây hại vì `main-guard` đều xanh, nhưng nếu đỏ thì task sau đã bắt đầu trên nhánh chính hỏng.
- **Bằng chứng:** commit test `d185cce` của T-052 đẩy lúc 17:57, `main-guard` sau T-053 xanh lúc 17:58 (lần chạy 40218650987); commit test `846cb47` của T-050 đẩy lúc 18:22, `main-guard` sau T-052 xanh lúc 18:23 (lần chạy 40220611120). T-051 thì phiên chờ `main-guard` (19:00) rồi mới đẩy, test viết sẵn cục bộ từ 18:53.
- **Đề xuất sửa:** `docs/auto-run.md` bước 3: ghi ngoại lệ "commit đầu tiên của task sau chỉ đẩy khi đã có kết quả `main-guard` của lần merge trước (bước 9); trong lúc chờ, commit cục bộ". Có thể thêm vào `scripts/save-branch.sh` một kiểm tra: nhánh chính có `main-guard` đang chạy thì in cảnh báo.
- **Áp dụng:** `docs/auto-run.md` bước 3: ngoại lệ đẩy commit đầu của task sau chỉ khi có kết quả `main-guard` (PR này). Không thêm cảnh báo vào `scripts/save-branch.sh`: quy tắc ở bước 3 và bước 9 đủ, phiên T-051 đã làm đúng khi chờ. Tuấn nhận ngày 2026-10-25. Dự án mẫu: VGL-Studio/app-project-template#188.
- **Kiểm chứng:** RUN-20261026-02: ba lần (T-055, T-057, T-056) commit test của task sau được giữ cục bộ tới khi `main-guard` của lần merge trước xanh, rồi mới đẩy; không CI nào phải chạy lại vì commit mới của nhánh chính. Có hiệu quả.

### L-077: từ dịch dùng lại từ feature khác đổi nghĩa khi đi cùng con số

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261026-01 (T-050, rà soát độc lập).
- **Chuyện gì:** plan T-050 bảo dịch "dùng lại cách gọi đã có trong settings và onboarding". Phiên lấy từ "transparency" của onboarding ("Atur transparansi... garis pose") cho nhãn con trượt `camera_adjust_opacity` ở `hi`, `id`, `th`. Ở onboarding câu không có số nên đúng; trên con trượt có phần trăm thì "Transparansi 100%" đọc thành "trong suốt hoàn toàn", ngược với nét viền đậm nhất. Test chữ của plan (đủ bản dịch, khác tiếng Anh, giữ tham số) không bắt được lỗi nghĩa này; chỉ rà soát độc lập thấy.
- **Bằng chứng:** PR #145, rà soát độc lập lần 1 (điểm nên sửa); `OverlayGeometry.kt` (mặc định 1.0, khoảng 0.2 đến 1.0); test `CameraStringsTest.opacityLabel_isNotTransparency` (commit `f374916`) và phần sửa `b47f863`.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: plan dịch chữ liệt kê các chuỗi đi cùng con số hay phần trăm, ghi giá trị lớn nhất nghĩa là gì, và có một test chặn từ ngược nghĩa khi có rủi ro; lời giao rà soát độc lập của task dịch nhắc kiểm nghĩa của nhãn đi cùng số.
- **Áp dụng:** `docs/CLAUDE.md` mục Khi viết plan (chuỗi đi cùng con số, test chặn từ ngược nghĩa); `docs/auto-run.md` bước 6, nội dung rà soát của task dịch chữ (PR này). Tuấn nhận ngày 2026-10-25. Dự án mẫu: VGL-Studio/app-project-template#188.
- **Kiểm chứng:** RUN-20261026-02: T-055 thêm test chặn từ ngược nghĩa theo gốc từ; rà soát độc lập T-055, T-057 kiểm nghĩa từng nhãn và thấy hai điểm nhỏ (gốc tiếng Hindi chỉ bắt danh từ, tiếng Thái "ในอีกสักครู่"). Có hiệu quả.

### L-078: plan chép code từ dự án mẫu, nhưng phiên cloud chỉ đọc được repo của app

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261026-01 (T-053).
- **Chuyện gì:** plan T-053 bảo chép ba khối của `scripts/token-usage.py` từ dự án mẫu (commit `a76b3cd`) và có điều kiện dừng riêng khi bản của mẫu khác bản đã so. Quyền GitHub của phiên `claude --cloud` chỉ gồm repo `VGL-Studio/camera-pose`, nên phiên không đọc được repo mẫu; phiên viết code theo mô tả ở mục 3.1 của plan, không so được với bản mẫu.
- **Bằng chứng:** phạm vi GitHub của phiên RUN-20261026-01 chỉ có `vgl-studio/camera-pose`; nhật ký plan T-053, dòng Lệch so với plan. Code vẫn đúng nhờ mục 3.1 mô tả từng khối, và rà soát độc lập không thấy lệch.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: plan chép code từ dự án mẫu dán nguyên đoạn code (hay `diff`) vào plan, hoặc ghi rõ phiên phải có quyền đọc repo mẫu; `docs/ops/from-app.md` ghi giới hạn này.
- **Áp dụng:** `docs/CLAUDE.md` mục Khi viết plan (dán nguyên code của mẫu vào mục 3.1); `docs/ops/from-app.md` mục 1 (PR này). Tuấn nhận ngày 2026-10-25. Dự án mẫu: VGL-Studio/app-project-template#188.
- **Kiểm chứng:**

### L-079: dòng `Task: T-NNN` trong lời giao soạn plan làm số token soạn plan thành mẫu ước tính của task

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261026-01 (T-053, rà soát độc lập).
- **Chuyện gì:** T-053 thêm dòng `Task: T-NNN` vào lời giao cho phiên con ở cả bước soạn plan cuối phiên (`docs/auto-run.md` mục 4, bước 5), đúng như plan. Khi đó `scripts/token-usage.py report --csv` ghi một dòng `(RUN soạn plan, T-NNN, cỡ)` chỉ có token soạn plan, và `estimate` lấy dòng đó làm mẫu cho trung vị, kéo số gợi ý `token_estimate` xuống. Phần soạn plan của phiên chính chạy trên nhánh `claude/plan-T-NNN-...`, không khớp `TASK_BRANCH`, nên lại vào dòng Chung: cách gán không nhất quán. Rà soát độc lập ghi mức nên sửa; phiên không sửa vì mâu thuẫn với chữ của plan đã duyệt.
- **Bằng chứng:** PR #143, rà soát độc lập (điểm nên sửa); `scripts/token-usage.py`, hàm `estimate()` lấy mọi dòng có `task` và `size`.
- **Đề xuất sửa:** bỏ dòng `Task:` khỏi lời giao soạn plan ở `docs/auto-run.md` mục 4, bước 5, hoặc dùng dạng riêng `Plan: T-NNN` mà script tính vào dòng Chung (hay một cột riêng), có test.
- **Áp dụng:** `docs/auto-run.md` mục 4, bước 5: lời giao soạn plan không có dòng `Task:`, token soạn plan vào dòng Chung; `docs/ops/token-usage.md` (PR này). Không đổi `scripts/token-usage.py`. Tuấn nhận ngày 2026-10-25. Dự án mẫu: VGL-Studio/app-project-template#188.
- **Kiểm chứng:**

### L-080: sau task cuối, thư mục chính vẫn đứng trên nhánh task, token của việc cuối phiên tính vào task đó

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261026-01 (T-051).
- **Chuyện gì:** `scripts/token-usage.py` gán request của phiên chính theo `gitBranch` của thư mục chính. Sau khi T-051 merge, phiên làm việc cuối phiên (chờ `main-guard`, `device-session`, staging, viết báo cáo trong worktree `../report`) mà thư mục chính vẫn đứng trên `claude/T-051-dich-chu-thu-vien`, nên phần đó tính vào T-051 thay vì dòng Chung.
- **Bằng chứng:** RUN-20261026-01, mục Token: T-051 15.0M lúc 19:36, 27.0M lúc 20:07; phiên chuyển thư mục chính sang nhánh mặc định lúc 20:07. CSV giữ số lúc 19:36 cho T-051.
- **Đề xuất sửa:** `docs/auto-run.md` mục 4, bước 1: trước việc cuối phiên, chuyển thư mục chính sang nhánh báo cáo hay nhánh mặc định của phiên (`git checkout`), không để trên nhánh task đã merge. Có thể để `scripts/token-usage.py` coi request sau giờ merge của task là phần Chung.
- **Áp dụng:** `docs/auto-run.md` mục 4, bước 0: `git checkout --detach origin/main` trước việc cuối phiên; `docs/ops/token-usage.md` (PR này). Không đổi `scripts/token-usage.py`. Tuấn nhận ngày 2026-10-25. Dự án mẫu: VGL-Studio/app-project-template#188.
- **Kiểm chứng:** RUN-20261026-02: thư mục chính `git checkout --detach origin/main` ngay sau merge T-056 (05:43); T-056 trong bảng token chỉ có 9.0M, việc cuối phiên vào dòng Chung. Có hiệu quả.

### L-081: `token_estimate` nâng tay gần gấp đôi số gợi ý, task thật chỉ dùng 0,4 đến 0,7 lần

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261026-02.
- **Chuyện gì:** bốn plan của RUN-20261026-02 nâng `token_estimate` lên gần gấp đôi số của `scripts/token-usage.py estimate`, theo hai task cỡ M của RUN-20261026-01 (15.0M, 19.7M). Thực tế cả bốn task dùng 0,4 đến 0,7 lần ước tính; T-057 lệch 2,5 lần.
- **Bằng chứng:** RUN-20261026-02, mục Token: T-054 5.5M/10M, T-055 7.1M/10M (đếm đủ: phiên chính làm trên nhánh task, rà soát là phiên con có `Task:`), T-057 6.0M/15M, T-056 9.0M/18M (thiếu phần phiên chính, L-083).
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan, dòng `token_estimate`: lấy số của `scripts/token-usage.py estimate` (nay có mẫu đã gồm phiên con), chỉ chỉnh khi task khác thường và ghi lý do; không nâng theo một hai task của một phiên.
- **Áp dụng:** `docs/CLAUDE.md`, mục Khi viết plan, dòng `token_estimate`: không nâng số gợi ý theo một hai task của một phiên (PR này). Tuấn nhận ngày 2026-10-26. Dự án mẫu: VGL-Studio/app-project-template#206.
- **Kiểm chứng:**

### L-082: điểm nên sửa chỉ đòi thêm test cho code đã đúng, ADR-1003 không nói cách xử lý

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261026-02 (T-054, rà soát độc lập).
- **Chuyện gì:** rà soát độc lập T-054 có một điểm nên sửa: nhánh `if len(starts) < attempt` của `killed_by` không có test riêng. ADR-1003 bắt sửa điểm nên sửa khi có test viết trước thất bại đúng lý do; test cho code đã đúng không thể thất bại trước, nên phiên phải đoán. Phiên chọn không sửa và ghi vào báo cáo.
- **Bằng chứng:** nhật ký plan T-054 (RUN-20261026-02); PR #158, mục Rà soát độc lập.
- **Đề xuất sửa:** `docs/auto-run.md` bước 6, dòng Nên sửa: điểm chỉ thêm test cho hành vi đã đúng thì thêm khi test nằm trong file của mục 3.2 và thất bại khi tạm bỏ dòng code đó (đột biến, như đối chứng của L-035), ghi kết quả đột biến vào nhật ký; không thì ghi vào báo cáo.
- **Áp dụng:** `docs/auto-run.md` bước 6, dòng Nên sửa: điểm chỉ đòi thêm test cho hành vi đã đúng thì thêm khi test nằm trong mục 3.2 và thất bại khi tạm bỏ dòng code đó (đột biến), không thì ghi vào báo cáo (PR này). Tuấn nhận ngày 2026-10-26. Dự án mẫu: VGL-Studio/app-project-template#206.
- **Kiểm chứng:**

### L-083: phiên con giữ nhánh task trong worktree, request của phiên chính cho task đó tính vào dòng Chung

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261026-02 (T-057, T-056).
- **Chuyện gì:** khi giao bước 3 đến 5 cho phiên con trong worktree của nhánh task (auto-run.md, mục Giao bước 3 đến 5), thư mục chính không mở được cùng nhánh đó nên đứng ở `origin/main` tách rời. `scripts/token-usage.py` gán request của phiên chính theo nhánh của thư mục chính, nên phần phiên chính làm cho task (đọc plan, mở PR, chờ CI, merge, nhật ký) tính vào dòng Chung.
- **Bằng chứng:** RUN-20261026-02, mục Token: dòng T-056 có đúng 66 request, bằng 55 của phiên con bước 3 đến 5 cộng 11 của phiên rà soát; T-057 50 = 39 + 11; Chung 22.4M.
- **Đề xuất sửa:** `scripts/token-usage.py`: request của phiên chính trên HEAD tách rời tính vào task có nhánh được tạo gần nhất trước đó mà chưa merge (theo giờ tạo nhánh và giờ merge), hoặc phiên chính ghi task đang làm vào một tệp trạng thái mà script đọc; `docs/ops/token-usage.md` ghi giới hạn này. Trong lúc chờ, báo cáo phiên ghi rõ dòng nào thiếu phần phiên chính.
- **Áp dụng:** T-063 (RUN-20261027-01): lệnh `scripts/token-usage.py mark T-NNN` và `mark chung`; `report` đọc lại dấu mốc trong transcript của phiên chính và gán request của phiên chính không ở nhánh task nào cho task của dấu mốc gần nhất, từ `--since` trở đi; 11 test trong `scripts/tests/run.sh`; `docs/auto-run.md` (bước 1, mục Giao bước 3 đến 5, bước 10, mục 4 bước 0, mục Tiếp tục phiên bước 5) và `docs/ops/token-usage.md` (mục 1, mục 5). PR #186. Tuấn nhận ngày 2026-10-26. Dự án mẫu: VGL-Studio/app-project-template#206.
- **Kiểm chứng:** RUN-20261027-01 (ngay sau T-063): phiên chính chạy lệnh `mark` ở bước 1 của T-062, T-064 và `mark chung` sau mỗi task. T-064 giao bước 3 đến 5 cho phiên con: dòng T-064 của cả phiên có 49 request, riêng transcript của phiên con và phiên rà soát (có dòng `Task: T-064`) chỉ 34 request; 15 request của phiên chính (đẩy nhánh, PR, chờ CI, merge) nay vào task. Có hiệu quả.

### L-084: phiên con dừng giữa chừng vì lỗi API, phiên `claude --cloud` không tự tiếp tục

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261026-03 (T-059).
- **Chuyện gì:** phiên con làm bước 3 của T-059 dừng vì lỗi API `oauth_org_not_allowed` (HTTP 403: tổ chức tắt quyền dùng gói Claude cho Claude Code), tiến trình phiên chính khởi động lại. Phiên mở bằng `claude --cloud` nên không tự tiếp tục (L-008); người nhắn "Tiếp tục" sau khoảng 2 giờ 30 phút. Phần test phiên con đã viết còn nguyên trong worktree (chưa commit, chưa đẩy); phiên chính chạy test, thấy đúng mục 4 của plan, commit và làm tiếp. Tài liệu chỉ nói tới hết hạn mức, không nói phiên con chết giữa chừng thì làm gì.
- **Bằng chứng:** thông báo lỗi của phiên con (request id `req_011CgQhvfc2tZWzDZDAjMrn6`); commit cuối trước khi dừng 10:37 (nhánh báo cáo 0f3f6b8), tiếp tục 13:10; commit test T-059 94a69fe lúc 13:12; RUN-20261026-03, mục Tiếp tục.
- **Đề xuất sửa:** `docs/auto-run.md`, mục Giao bước 3 đến 5: phiên con kết thúc vì lỗi API (không có báo cáo) thì phiên chính đọc `git status`, `git log` của worktree, chạy test của phần đã viết, đối chiếu mục 4 của plan, rồi tự làm tiếp hoặc giao phiên con mới tiếp từ phần đã có; không tính là task dừng. Thêm dòng `oauth_org_not_allowed` vào bảng tra lỗi (hạ tầng). Phiên qua đêm ưu tiên routine (L-008).
- **Áp dụng:** `docs/auto-run.md`, mục Giao bước 3 đến 5: phiên con kết thúc mà không có báo cáo thì phiên chính tiếp từ phần đã có trong worktree, không tính là task dừng; bảng tra lỗi thêm PB-11 (`oauth_org_not_allowed`, loại `cho`), kèm `scripts/tests/playbook/PB-11.log` và test (PR này). Lỗi ở cấp tổ chức do Tuấn hỏi người quản trị. Tuấn nhận ngày 2026-10-26. Dự án mẫu: VGL-Studio/app-project-template#206.
- **Kiểm chứng:** RUN-20261027-02: T-068 thêm `run-watchdog.yml` (mỗi 30 phút); chạy tay lúc 19:29 UTC (lần chạy 40569540482) in "RUN-20261027-02: đang chạy, commit gần nhất 0 phút trước", không mở issue. Chưa có lần phiên đứng im thật để kiểm việc mở issue.

### L-085: thiết kế xuất lại mà không sinh lại `design/review/`, task khác thấy ảnh so sánh đổi ngoài mục 3.2

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261026-03 (T-060).
- **Chuyện gì:** `scripts/design-compare.sh camera` ở bước 5 của T-060 đổi `CameraScreen_countdown_dark_compare.png` và `summary.md` (6.0% thành 9.6%), không do T-060: PR thiết kế #167 xuất lại ảnh đếm ngược mà không sinh lại `design/review/`. Hai file nằm ngoài mục 3.2; tài liệu không nói làm gì, phiên phải đoán: không commit, ghi vào mô tả PR. T-061 sau đó sinh lại đúng theo plan.
- **Bằng chứng:** PR #171, mục Tóm tắt; nhật ký plan T-060, dòng Lệch so với plan.
- **Đề xuất sửa:** `docs/ops/design-workflow.md`: PR xuất lại ảnh thiết kế chạy `scripts/design-compare.sh <feature>` và commit `design/review/` cùng PR. `docs/auto-run.md` bước 5: ảnh so sánh đổi mà ảnh chuẩn của task không đổi và `design/review/` không có trong mục 3.2 thì không commit, ghi vào mô tả PR.
- **Áp dụng:** `docs/ops/design-workflow.md`, mục Xuất từ Figma: PR xuất lại ảnh chạy `scripts/design-compare.sh` và commit `design/review/` cùng PR; `docs/auto-run.md` bước 5: ảnh so sánh đổi không do task thì không commit, ghi vào mô tả PR (PR này). Tuấn nhận ngày 2026-10-26. Dự án mẫu: VGL-Studio/app-project-template#206.
- **Kiểm chứng:**

### L-086: token đẩy của phiên `claude --cloud` không có quyền `workflow`, task sửa `.github/workflows/` không đẩy được

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261027-01 (T-062, T-065).
- **Chuyện gì:** T-062 (plan rủi ro cao đã duyệt chéo, sửa `.github/workflows/device-session.yml`) làm xong, `check.sh` qua, rà soát không có điểm nào; lệnh đẩy commit đầu tiên bị GitHub từ chối vì token mà phiên `claude --cloud` dùng không có quyền `workflow`. Lệnh đẩy ở bước 1 qua được vì nhánh chưa có commit nào đổi workflow, nên lỗi chỉ lộ ra sau khi đã làm xong task. T-065 cũng sửa hai workflow nên phiên bỏ qua trước khi tạo nhánh. `run-check.py`, `plan-check.py` và rà soát trước khi duyệt không biết điều này; danh sách chạy ghi "T-062 và T-065 sửa `.github/` đúng như plan rủi ro cao đã duyệt chéo" và chọn mở phiên bằng `claude --cloud` (L-020).
- **Bằng chứng:** `scripts/save-branch.sh claude/T-062-camera-gia` lúc 05:27 UTC: `! [remote rejected] claude/T-062-camera-gia -> claude/T-062-camera-gia (refusing to allow an OAuth App to create or update workflow `.github/workflows/device-session.yml` without `workflow` scope)`; bundle `/root/rescue/claude__T-062-camera-gia.bundle` (RUN-20261027-01, mục Nhánh chưa đẩy).
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi lập danh sách chạy, và `docs/ops/pre-run-review.md`: plan có file `.github/workflows/` trong mục 3.2 không xếp vào phiên mở bằng `claude --cloud`, trừ khi đã thử đẩy một nhánh tạm có đổi workflow từ chính môi trường đó; `scripts/run-check.py` báo chú ý khi danh sách chạy ghi `claude --cloud` mà có plan như vậy. `docs/auto-run.md`, mục Khi không đẩy được: lỗi `without workflow scope` thì bỏ qua luôn các task sau có file `.github/workflows/` trong mục 3.2, không đợi tới lần đẩy thứ hai. Thêm một dòng vào bảng tra lỗi (loại `moi-truong`, không thử lại) kèm log thật. Người kiểm có cấp được quyền `workflow` cho GitHub App mà phiên cloud dùng không; nếu được, ghi vào `docs/ops/` cách cấp.
- **Áp dụng:** làm ở ticket T-066 (`open_with` của danh sách chạy, bảng khả năng `docs/ops/session-capabilities.md`, `scripts/run-check.py` quy tắc 8, câu hỏi bắt buộc của rà soát danh sách, `scripts/push-probe.sh` ở đầu phiên; PR #197); dòng bảng tra lỗi ở T-067. Tuấn nhận ngày 2026-10-27; quyền `workflow` của kết nối GitHub do Tuấn cấp lại; T-067: dòng PB-13 (loại `bo-qua-loai`) ở docs/ops/failure-playbook.md, docs/auto-run.md mục Khi không đẩy được bước 5, scripts/plan-check.py --touches, ADR-1005/L8.
- **Kiểm chứng:** RUN-20261027-02: `scripts/push-probe.sh RUN-20261027-02` lúc 18:37 UTC mã 0 (QUA, token sau `/web-setup`); ba task sửa `.github/workflows/` (T-068, T-062, T-065) đẩy được và merge. Có hiệu quả.

### L-087: test của script dựa vào cấu hình git toàn cục (và Java) của máy, qua trên cloud nhưng đỏ trên runner

- **Trạng thái:** applied. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261027-02 (T-068, T-069).
- **Chuyện gì:** khối test `save-branch patch` của T-068 qua trên máy viết plan và trên máy cloud, nhưng đỏ trên runner: `git -C "$sb_origin" commit-tree` trong kho trần giả cần tên người commit, runner không có cấu hình git toàn cục. Cùng khối, kho trần giả tạo bằng `git init --bare` không `-b main`, nên khi git không đặt `init.defaultBranch` (máy cloud, runner) bản sao mới không có nhánh `main` và `git log main..` lỗi. Ở T-069, test `env.sh gives a Java that java-check.sh accepts` qua ngay trên khung rỗng vì máy cloud có `JAVA_HOME` hợp lệ (máy dev của lần chạy thử không có Java).
- **Bằng chứng:** PR #198, `pr-check` lần chạy 40566657337: `fatal: empty ident name ... not allowed`; tái hiện bằng `GIT_CONFIG_GLOBAL=/dev/null bash scripts/tests/run.sh`; sửa ở commit `9e63b5f`, `37d8e5a`. Nhật ký plan T-069 (ADR-1005/L4).
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan (ý L-075), và `docs/auto-run.md` bước 5: chạy test của script mới cả với `GIT_CONFIG_GLOBAL=/dev/null`; khối test dựng kho git giả luôn dùng `-b main` và tên người commit riêng. Cân nhắc đặt `GIT_CONFIG_GLOBAL=/dev/null` cùng `GIT_AUTHOR_*`, `GIT_COMMITTER_*` ở đầu `scripts/tests/run.sh`, để máy nào chạy cũng như runner.
- **Áp dụng:** dự án mẫu đã nhận lớp lỗi này (đồng bộ dự án mẫu, đợt 2, PR #202): `scripts/tests/run.sh` đặt `GIT_CONFIG_GLOBAL=/dev/null` cùng `GIT_AUTHOR_*`, `GIT_COMMITTER_*` ở đầu, nên mọi máy chạy test của script như runner. Phần còn lại (biến môi trường khác như `JAVA_HOME`, test Gradle) và cách phiên tự sửa: ADR-0033/L11 của dự án mẫu (đồng bộ dự án mẫu, đợt 3), thay ADR-1006 của app (Nhật nhận ngày 2026-10-28, nay `superseded`).
- **Kiểm chứng:**

### L-088: `gh` của môi trường cloud không có cờ `--allow-escape-sequences` mà `docs/auto-run.md` bước 8 dùng để tải log job

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261027-02 (T-068).
- **Chuyện gì:** CI của T-068 đỏ; lệnh tải log theo bước 8 (`gh api --allow-escape-sequences "repos/{owner}/{repo}/actions/jobs/<id>/logs"`) trả "unknown flag: --allow-escape-sequences", tệp log chỉ có trợ giúp của `gh`, nên `failure-kind.sh --playbook` ra `khong-co` trên một tệp không phải log. Bỏ cờ thì tải được log (1339 dòng).
- **Bằng chứng:** job 122409697329, 19:15 UTC.
- **Đề xuất sửa:** `docs/auto-run.md` bước 8: bỏ cờ `--allow-escape-sequences` (log tải về vẫn đọc được), hay ghi "`gh` báo unknown flag thì bỏ cờ". `scripts/failure-kind.sh --playbook` có thể báo khi tệp không giống log của job.
- **Áp dụng:** dự án mẫu đã nhận (đồng bộ dự án mẫu, đợt 2, PR #202): `docs/auto-run.md` bước 8 không dùng `gh api .../jobs/<id>/logs` nữa, `scripts/failure-kind.sh --playbook` in `log-hong` khi tệp là thông báo lỗi hay trợ giúp của `gh` (bài học L-104 của dự án mẫu). Nhật nhận ngày 2026-10-28.
- **Kiểm chứng:**

### L-089: phiên mở `local` không có bước kiểm môi trường, `cloud-env-check.sh` không áp dụng ở máy dev

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261028-01.
- **Chuyện gì:** người yêu cầu chạy phiên trên máy dev (`local`) thay cho `claude --cloud`. `docs/auto-run.md` mục 2 chỉ có `scripts/cloud-env-check.sh` để kiểm môi trường; script này luôn báo lỗi ở máy dev vì đòi tệp phiên bản của `scripts/cloud-setup.sh` và init script mirror Maven, nên phiên phải tự quyết không coi đó là điều kiện dừng và dùng `scripts/check.sh` thay cho bước kiểm.
- **Bằng chứng:** `cloud-env-check.sh` ngày 2026-10-28: "môi trường dựng từ setup script phiên bản không có ... thiếu ~/.gradle/init.d/maven-central-mirror.gradle"; báo cáo RUN-20261028-01, mục Quyết định AI đã tự làm.
- **Đề xuất sửa:** `docs/auto-run.md` mục 2 và `docs/ops/session-capabilities.md`: phiên `local` chạy `scripts/local-setup.sh --check` (hay `scripts/java-check.sh`) thay cho `cloud-env-check.sh`.
- **Áp dụng:** Nhật nhận ngày 2026-10-28. app: `docs/auto-run.md` mục 2 và `docs/ops/session-capabilities.md` (dòng `local`), cùng PR với việc nhận bài này; máy dev cài JDK và SDK cố định bằng `scripts/local-setup.sh` trước (bản cũ nằm trong thư mục tạm của phiên Claude nên `--check` báo thiếu). dự án mẫu: VGL-Studio/app-project-template#258. Dự án mẫu nhận: L-116 của mẫu (VGL-Studio/app-project-template#262), app nhận qua đợt đồng bộ 4 (PR #224).
- **Kiểm chứng:** RUN-20261030-01: phiên `local` chạy `scripts/local-setup.sh --check` ở đầu phiên (qua trong vài giây), không chạy `cloud-env-check.sh`, rồi `source` tệp `env.sh`. Có hiệu quả.

### L-090: `ui-test` đỏ vì runner tải gói Android Emulator lỗi, bảng tra lỗi chưa có dòng cho lỗi này

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261028-02 (T-071, PR #216).
- **Chuyện gì:** sau `merge-if-green.sh --update`, job `ui-test` đỏ sau khoảng 2 phút: runner tải gói Android Emulator lỗi nên emulator không khởi động, không có test nào chạy. `scripts/failure-kind.sh --playbook` trả `khong-co`, nên theo `docs/auto-run.md` bước 8 phiên phải xử lý như lỗi thật (chạy lại một lần, rồi tối đa 3 lần sửa) dù không có gì để sửa trong code. Lần chạy lại ngay cũng lỗi; phiên tự xếp là lỗi hạ tầng và chạy lại theo lịch của bước 5, lần sau 2 phút thì xanh.
- **Bằng chứng:** lần chạy 40734943741 (17:21 và 17:35 UTC): "Warning: An error occurred while preparing SDK package Android Emulator: Error on ZipFile unknown archive.", "error: could not connect to TCP port 5554: Connection refused", "Không có test nào chạy trên emulator."; lần chạy lại 17:39 xanh, `ui-test` 9 phút.
- **Đề xuất sửa:** `docs/ops/failure-playbook.md` thêm một dòng loại `lich-thu-lai` cho "Error on ZipFile unknown archive" (hay "could not connect to TCP port 5554") đi cùng "Không có test nào chạy trên emulator": chạy lại job theo lịch 1, 2, 4, 8, 15 phút, không tính vào 3 lần sửa; kèm log thật trong `scripts/tests/playbook/`.
- **Áp dụng:** Nhật nhận ngày 2026-10-28, đề xuất về dự án mẫu: VGL-Studio/app-project-template#259. App nhận lại qua đợt đồng bộ sau, không sửa `failure-playbook.md` riêng. Dự án mẫu nhận: L-117 của mẫu, dòng PB-15 (VGL-Studio/app-project-template#263), app nhận qua đợt đồng bộ 4 (PR #224).
- **Kiểm chứng:**

### L-091: quy trình chưa có khái niệm plan nhiều lượt, phiên phải dựa vào quy tắc riêng của danh sách chạy

- **Trạng thái:** accepted. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261029-01 (T-074, ba lượt, PR #228, #229, #230).
- **Chuyện gì:** T-074 chia ba lượt cùng một plan và một ticket, mỗi lượt một PR. `docs/auto-run.md` bước 10 chuyển plan `done`, ticket `on-staging` ngay sau lượt 1, nên lượt 2 và 3 sẽ bị bỏ qua ở kiểm tra đầu task. Cột "Phụ thuộc: lượt N" không script nào đọc. `scripts/plan-check.py` báo lỗi trước lượt 2 chỉ vì lượt 1 đã xóa file đúng plan. `scripts/run-check.py` có 32 dòng chú ý (cùng sửa, cùng tạo file) vì cùng một plan nằm ở ba dòng. Phiên chạy được nhờ mục "Cách chạy ba lượt" (sáu quy tắc) do Nhật thêm vào danh sách chạy.
- **Bằng chứng:** danh sách chạy RUN-20261029-01, mục "Cách chạy ba lượt" và bảng rà soát trước khi chạy (dòng "Chặn"); nhật ký lượt 2 của plan T-074 (hai lỗi `plan-check.py` về `InterstitialFlow.kt`, `InterstitialFlowTest.kt`).
- **Đề xuất sửa:** dự án mẫu: plan có trường `passes:` và mục 3.1, 3.2 theo lượt; `docs/auto-run.md` bước 10 chỉ chuyển `done` ở lượt cuối; `plan-check.py` nhận tham số lượt và bỏ qua file mà lượt trước đã tạo hay xóa; `run-check.py` không báo cùng sửa file giữa các lượt của một plan và kiểm cột phụ thuộc theo lượt.
- **Áp dụng:** chưa làm ở app: cần plan sửa `scripts/plan-check.py`, `scripts/run-check.py`, `docs/auto-run.md`, `docs/templates/plan.md`, chưa có ticket. Nhật nhận ngày 2026-10-30. Dự án mẫu: VGL-Studio/app-project-template#328.
- **Kiểm chứng:**

### L-092: `build-and-test` của PR đổi thư viện chạy 35 tới 38 phút, sát giới hạn 45 phút của job

- **Trạng thái:** accepted. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261029-01 (T-074 lượt 1 và 2).
- **Chuyện gì:** PR đổi thư viện và file build chạy thêm bản release (R8) trong `build-and-test`. Lượt 1 bị hủy lần đầu vì chạm `timeout-minutes: 45` khi đang build R8; lần chạy lại xanh trong 35 phút. Lượt 2 xanh trong 38 phút. `scripts/failure-kind.sh --playbook` khớp nhầm PB-08 (lỗi R8) dù log không có lỗi R8, chỉ có dòng hủy vì hết giờ.
- **Bằng chứng:** lần chạy 40835766905 (PR #228, lần 1 `cancelled`, lần 2 `success`); PR #229 `build-and-test` 38 phút.
- **Đề xuất sửa:** dự án mẫu: tách bản release thành job riêng chạy song song với `build-and-test`, hoặc nâng giới hạn lên 60 phút cho job có bản release; bảng tra lỗi thêm dòng "The job running on runner ... has exceeded the maximum execution time" loại chạy lại một lần, đứng trước PB-08.
- **Áp dụng:** chưa làm ở app: cần plan rủi ro cao sửa `.github/workflows/pr-check.yml` và bảng tra lỗi, chưa có ticket. Nhật nhận ngày 2026-10-30. Dự án mẫu: VGL-Studio/app-project-template#329.
- **Kiểm chứng:**

### L-093: bước thử tay của plan dựa vào Firebase staging để tắt một cờ mà bản staging bật sẵn trong code

- **Trạng thái:** accepted. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261029-01 (T-074 lượt 3, PR #230).
- **Chuyện gì:** plan T-074 mục 4 ghi bước thử "tắt `feature_loading_enabled` trên Firebase staging: không có quảng cáo mở app". `StagingFeatureFlagsModule` bật cờ này trong code, và `DefaultFeatureFlags` không đọc giá trị từ xa cho cờ đã bật như vậy, nên bước này không làm được trên staging. Rà soát lượt 3 cũng thấy bước 11.3.3 chờ một dòng log mà hộp hỏi quyền không bao giờ sinh ra (hộp hỏi quyền không đưa app ra nền).
- **Bằng chứng:** `docs/ops/manual-test.md` mục 11.2 và mục 12 sau PR #230; rà soát độc lập của lượt 3.
- **Đề xuất sửa:** `docs/CLAUDE.md` mục Khi viết plan: mỗi bước kiểm tra thủ công cần tắt hay bật một khóa trên Firebase staging phải đối chiếu `StagingFeatureFlagsModule` và `StagingRemoteValuesModule` (khóa đặt sẵn trong code thì không đổi được từ Firebase); câu kiểm log phải dẫn dòng code ghi log đó và điều kiện để dòng đó chạy.
- **Áp dụng:** chưa làm ở app: cần sửa `docs/CLAUDE.md`, chưa có ticket. Nhật nhận ngày 2026-10-30. Dự án mẫu: VGL-Studio/app-project-template#330.
- **Kiểm chứng:**

### L-094: ở máy dev, tóm tắt "Test JVM" của `scripts/check.sh` đếm cả worktree cũ của agent con trong `.claude/worktrees/`

- **Trạng thái:** accepted. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261030-01 (T-076, T-077).
- **Chuyện gì:** phiên `local` chạy `scripts/check.sh` trong thư mục repo. Dòng tóm tắt in "Test JVM: 4635 test" kèm số của từng `./.claude/worktrees/agent-*/...`, vì các worktree cũ của agent con nằm trong thư mục repo và có kết quả test cũ. Cổng chất lượng vẫn đúng, nhưng con số báo cáo gần gấp đôi số thật và dễ đọc nhầm.
- **Bằng chứng:** `check.sh` của T-076 (02:44 UTC) và T-077 (03:36 UTC) trên máy dev: 4635 và 4652 test; cùng ngày `check.sh` trong worktree `../start-check` (ngoài repo) đếm 2373.
- **Đề xuất sửa:** dự án mẫu: `scripts/check.sh` bỏ qua `.claude/` (và mọi worktree lồng trong repo) khi gom kết quả test; hoặc `docs/ops/` ghi dọn `.claude/worktrees/` cũ trước phiên `local`.
- **Áp dụng:** chưa làm ở app: `scripts/check.sh` chỉ sửa qua plan rủi ro cao, chưa có ticket. Nhật nhận ngày 2026-10-30. Dự án mẫu: VGL-Studio/app-project-template#331.
- **Kiểm chứng:**

### L-095: issue `env-broken` của môi trường cloud chặn cả phiên `local`

- **Trạng thái:** accepted. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261030-02 (T-078).
- **Chuyện gì:** bảng đầu phiên của `docs/auto-run.md` mục 2 nói có issue `env-broken` đang mở thì không chạy task nào, không có ngoại lệ. Issue #242 nói proxy của môi trường cloud trả 403 cho kho Maven của Mintegral và Pangle; phiên mở `local` trên máy dev, không đi qua proxy đó. Phiên phải hỏi người mới chạy tiếp được. Dòng `local` của bảng đã bỏ `cloud-env-check.sh` vì lý do tương tự (L-116 của mẫu), nhưng dòng `env-broken` chưa theo.
- **Bằng chứng:** RUN-20261030-02, mục Kiểm tra đầu phiên: #242 mở lúc 07:28 UTC; Nhật quyết chạy tiếp; `scripts/check.sh` trên máy dev qua (09:37 UTC, 2394 test) và CI của PR #246 xanh.
- **Đề xuất sửa:** `docs/auto-run.md` mục 2, dòng `env-broken`: phiên `local` không dừng vì `env-broken`, vì `env-canary` kiểm môi trường cloud; `scripts/local-setup.sh --check` và `scripts/check.sh` trên máy là cổng của phiên `local`. Ghi issue vào mục Kiểm tra đầu phiên. Phiên `cloud` và `routine` giữ như cũ.
- **Áp dụng:** chưa làm ở app: `docs/auto-run.md` là tài liệu quy trình lấy từ dự án mẫu, chờ dự án mẫu sửa rồi nhận qua đợt đồng bộ. Nhật nhận ngày 2026-10-30. Dự án mẫu: VGL-Studio/app-project-template#348.
- **Kiểm chứng:**

### L-096: agent SSH mất khóa sau khi phiên Claude Code mở lại, lệnh đẩy báo `Permission denied (publickey)`

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** app. **Nguồn:** RUN-20261030-02 (T-078).
- **Chuyện gì:** phiên `local` bị ngắt rồi mở lại sau khoảng 6 giờ. Lần đẩy đầu sau đó bị GitHub từ chối: `ssh-add -l` báo "The agent has no identities", khóa GitHub ở `~/.ssh/github` không có dòng trong `~/.ssh/config`, nên ssh không tự dùng nó. `scripts/save-branch.sh` giữ bundle và trả lỗi. `ssh -i ~/.ssh/github -o IdentitiesOnly=yes` xác thực được, khóa không có mật khẩu.
- **Bằng chứng:** RUN-20261030-02, Dòng thời gian 15:41 UTC; bundle `~/rescue/claude__T-078-mo-lai-sau-khi-dong.bundle`; đẩy lại được bằng `GIT_SSH_COMMAND="ssh -i $HOME/.ssh/github -o IdentitiesOnly=yes"`.
- **Đề xuất sửa:** máy dev: thêm vào `~/.ssh/config` một khối `Host github.com` với `IdentityFile ~/.ssh/github` và `IdentitiesOnly yes` (người làm, ngoài repo). Hoặc `docs/ops/session-capabilities.md`, dòng `local`: ghi lệnh `ssh -T -o BatchMode=yes git@github.com` vào kiểm tra đầu phiên, để lỗi lộ ra trước khi viết code.
- **Áp dụng:** Nhật nhận ngày 2026-10-30 (PR #249). Cùng ngày, theo yêu cầu của Nhật, AI thêm vào `~/.ssh/config` của máy dev (ngoài repo, trước đó chưa có tệp này) khối `Host github.com` với `IdentityFile ~/.ssh/github` và `IdentitiesOnly yes`; `ssh -T -o BatchMode=yes git@github.com` xác thực được mà không cần agent hay `GIT_SSH_COMMAND`. Phạm vi app, không đề xuất về dự án mẫu.
- **Kiểm chứng:**

### L-097: lệnh kiểm ảnh chuẩn `git status --short -- '*/src/test/screenshots/'` không khớp file nào

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261031-01 (T-079).
- **Chuyện gì:** `CLAUDE.md` mục Lệnh và mục 3.1 của plan T-079 dùng `git status --short -- '*/src/test/screenshots/'` để thấy ảnh chuẩn nào đã đổi sau khi ghi lại. Pathspec này không khớp file nào trong thư mục ảnh, nên lệnh luôn in trống, kể cả khi có ảnh đổi. Phép kiểm "chỉ ảnh của mục 3.2 đổi" vì thế qua cả khi ảnh khác đổi.
- **Bằng chứng:** RUN-20261031-01: phiên con lượt 1 báo lệnh không in gì, phải dùng đường dẫn thư mục. Phiên chính thử lại: thêm một byte vào `feature/poses/src/test/screenshots/PosesScreen_ad_light.png` thì lệnh trên không in gì, còn `git status --short -- ':(glob)**/src/test/screenshots/**'` in ` M feature/poses/src/test/screenshots/PosesScreen_ad_light.png`.
- **Đề xuất sửa:** `CLAUDE.md` mục Lệnh, mẫu plan và mọi chỗ dẫn lệnh này: đổi thành `git status --short -- ':(glob)**/src/test/screenshots/**'`. Thêm một test của script kiểm lệnh in đúng file ảnh đổi.
- **Áp dụng:**
- **Kiểm chứng:**

### L-098: ghi lại ảnh chuẩn đổi cả ảnh ngoài plan vì nhiễu vẽ

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261031-01 (T-079).
- **Chuyện gì:** `./gradlew :feature:poses:recordRoborazziDebug :feature:poses:testDebugUnitTest --rerun` ghi lại mọi ảnh của module. Ngoài 6 ảnh `ad` mà plan cho phép, 11 ảnh không có quảng cáo đổi vì nhiễu vẽ (tối đa 1 mỗi kênh màu, vài chục tới khoảng 110 điểm ảnh), dù `verifyRoborazziDebug` vẫn qua với ảnh cũ. Không quy tắc nào nói phải làm gì; phiên con tự khôi phục 11 ảnh và không coi là điều kiện dừng.
- **Bằng chứng:** RUN-20261031-01, báo cáo phiên con T-079 lượt 1; PR #256 chỉ có 6 ảnh `ad`; `scripts/check.sh` và CI xanh.
- **Đề xuất sửa:** `CLAUDE.md` mục Lệnh, dòng ghi lại ảnh chuẩn: sau khi ghi, khôi phục mọi ảnh ngoài mục 3.2 (`git checkout -- <ảnh>`) khi `verifyRoborazziDebug` qua với ảnh cũ; ảnh ngoài mục 3.2 mà `verify` đỏ với ảnh cũ thì vẫn là điều kiện dừng.
- **Áp dụng:**
- **Kiểm chứng:**

### L-099: phiên `local` và một phiên Claude Code khác dùng chung thư mục chính

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261031-01 (T-079).
- **Chuyện gì:** phiên `local` làm việc của nhánh báo cáo trong thư mục chính của repo. Trong lúc phiên chờ `device-session`, một phiên Claude Code khác cũng làm việc trong thư mục đó: chuyển sang `main`, `pull`, tạo nhánh `claude/docs-T-081-ticket` và commit, rồi về `main`. Lần sửa báo cáo tiếp theo của phiên chạy trên tệp của `main` (thất bại vì không thấy đoạn cần thay), và commit kèm theo rơi vào `main` cục bộ thay vì nhánh báo cáo.
- **Bằng chứng:** RUN-20261031-01, `git reflog` lúc khoảng 14:58 UTC: `checkout: moving from claude/RUN-20261031-01-report to main`, `pull -q --ff-only`, commit `78bdb32` (T-081, không phải của phiên), rồi commit `d8ac7b4` của phiên trên `main`. Không có gì bị đẩy lên `main`. Phiên chuyển commit sang nhánh báo cáo trong worktree `../run-report` (`46cdde8`) và trả `main` cục bộ về `origin/main` bằng `git reset --keep`.
- **Đề xuất sửa:** `docs/auto-run.md` mục 2, phiên `local`: làm mọi việc của nhánh báo cáo trong một worktree riêng (`git worktree add ../run-report claude/RUN-YYYYMMDD-NN-report`), để thư mục chính không mang trạng thái của phiên. `docs/ops/session-capabilities.md`, dòng `local`: người không mở phiên khác trong thư mục repo khi danh sách chạy đang `running`, hay mở phiên đó trong worktree riêng.
- **Áp dụng:**
- **Kiểm chứng:**

### L-100: bảng thiết lập môi trường cloud thiếu tên miền kho Maven của mediation, env-canary đỏ hai ngày

- **Trạng thái:** applied. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** issue #242 (`env-broken`), sau T-074.
- **Chuyện gì:** T-074 (#228) lấy khung quảng cáo AdsKit, thêm vào `settings.gradle.kts` hai kho Maven ngoài của Mintegral (`dl-maven-android.mintegral.com`) và Pangle (`artifact.bytedance.com`). Proxy của môi trường cloud trả 403 cho hai tên miền này, nên `scripts/check.sh` trong env-canary đỏ ngay ở bước tải `:app:stagingDebugRuntimeClasspath`, và issue `env-broken` chặn `start-run.yml`. `docs/ops/ads-guide.md` mục 7 đã ghi môi trường phải cho phép hai tên miền, nhưng bảng thiết lập môi trường ở `docs/ops/staging-and-runs.md` mục 2.1, nơi người làm theo khi tạo môi trường, không có chúng. Dự án mẫu cũng thiếu như vậy.
- **Bằng chứng:** issue #242, env-canary đỏ ngày 2026-10-30 (07:28 UTC) và 2026-10-31 (06:58 UTC), commit `9d0e335`: `Could not GET 'https://dl-maven-android.mintegral.com/...' Received status code 403` và `Could not GET 'https://artifact.bytedance.com/...' Received status code 403`. CI trên máy của GitHub và `check.sh` trên máy dev vẫn xanh (L-095).
- **Đề xuất sửa:** `docs/ops/staging-and-runs.md` mục 2.1: thêm hai tên miền vào Allowed domains, nhắc thêm tên miền khi thêm kho Maven ngoài mới; mục 2.4: log Gradle có `Received status code 403` thì thêm tên miền vào môi trường, không sửa code hay file build. Người thêm hai tên miền vào môi trường trên claude.ai/code rồi chạy lại env-canary. Dự án mẫu có thể cho `scripts/cloud-env-check.sh` hay `scripts/env-canary.sh` thử tải từ từng kho `maven(...)` để báo rõ tên miền bị chặn, và thêm vào `docs/ops/failure-playbook.md` một dòng cho 403 của Gradle trong môi trường cloud.
- **Áp dụng:** tài liệu sửa ở PR #264 (merge ngày 2026-10-31). Nhật đóng #242 cùng ngày. Thêm hai tên miền vào môi trường cloud là việc của người, chưa kiểm. Dự án mẫu: VGL-Studio/app-project-template#363.
- **Kiểm chứng:**

### L-101: plan chạy trước xóa ảnh thiết kế mà plan chạy sau còn dẫn, plan-check chặn task sau giữa phiên

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261031-02, T-082 và T-083.
- **Chuyện gì:** T-082 xóa đúng theo plan hai ảnh thiết kế `OnboardingScreen_default_light`, `OnboardingScreen_default_dark` (frame của T-012). Mục 4 của plan T-083 (chạy ngay sau trong cùng danh sách) vẫn liệt kê hai ảnh này trong "ảnh thiết kế của ticket khác", ghi "T-012 (bỏ ở T-082)". `plan-check.py` chạy ngay trước T-083 báo lỗi ảnh không có trong `design/screens/`, nên phiên bỏ qua T-083 theo quy tắc. `run-check.py` và lần rà soát danh sách trước khi duyệt không thấy, vì chúng kiểm từng plan trên trạng thái repo lúc duyệt, khi hai ảnh còn đó.
- **Bằng chứng:** danh sách chạy RUN-20261031-02, mục 2 (13:46 UTC): `LỖI: ảnh thiết kế của ticket khác OnboardingScreen_default_dark không có trong docs/features/onboarding/design/screens/.` (và `_light`).
- **Đề xuất sửa:** `scripts/run-check.py` thêm quy tắc: plan nào trong danh sách Xóa một file ở mục 3.2 mà plan chạy sau dẫn tên file đó (nhất là ảnh thiết kế ở mục 4) thì báo lỗi, để sửa plan trước khi duyệt. Hoặc quy tắc ảnh thiết kế của `plan-check.py` bỏ qua tên mà plan tự ghi là bỏ ở ticket khác. `docs/CLAUDE.md`, mục Khi viết plan: không liệt kê ảnh mà plan khác trong cùng danh sách sẽ xóa.
- **Áp dụng:**
- **Kiểm chứng:**
