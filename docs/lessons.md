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
- **Kiểm chứng:**

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
- **Kiểm chứng:**

### L-035: Thêm một bước đối chứng để test viết trước không qua ngay trên khung rỗng

- **Trạng thái:** applied. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-02, T-014, T-021, T-016, T-025.
- **Chuyện gì:** ở cả bốn task lớn, một số test viết theo mục 4 của plan qua ngay trên khung rỗng, vì khung trả đúng giá trị mặc định mà test mong đợi (ví dụ "chưa lưu gì thì trả mặc định", "tệp hỏng thì trả `null`"). Phiên con tự viết chặt lại trước commit test bằng một bước đối chứng: ghi rồi đọc lại, hoặc đặt dữ liệu đúng cạnh dữ liệu sai. Sau đó các test này thất bại đúng lý do. Số test được viết chặt như vậy: T-014 2, T-016 8, T-025 9; ở T-021, 2 test được viết chặt ngay từ đầu.
- **Bằng chứng:** nhật ký plan T-014, T-021, T-016, T-025 (RUN-20261023-02), mục Test viết trước.
- **Đề xuất sửa:** `docs/auto-run.md` bước 3: nêu "thêm bước đối chứng" làm cách mặc định khi một test không phải test chặn qua ngay; mẫu plan nhắc người viết plan rằng test kiểm giá trị mặc định thường qua trên khung và nên có sẵn bước đối chứng.
- **Áp dụng:** app: PR #48. dự án mẫu: VGL-Studio/app-project-template#86, PR VGL-Studio/app-project-template#87 (L-055).
- **Kiểm chứng:**

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

- **Trạng thái:** accepted (Tuấn, 2026-10-23). **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261023-03, kiểm tra đầu phiên.
- **Chuyện gì:** môi trường cloud đặt `JAVA_TOOL_OPTIONS` (proxy, truststore), nên `java -version` in dòng `Picked up JAVA_TOOL_OPTIONS: ...` trước dòng phiên bản. `scripts/java-check.sh` lấy dòng đầu (`head -n 1`), không đọc được phiên bản và dừng `scripts/check.sh`. Java thật là 21 (nằm trong khoảng 17 đến 24). Phiên không chạy task nào.
- **Bằng chứng:** `start-check.log`: `LỖI: không đọc được phiên bản Java từ 'Picked up JAVA_TOOL_OPTIONS: ...'`; `java -version` in `openjdk version "21.0.12.1"` ở dòng thứ hai.
- **Đề xuất sửa:** `scripts/java-check.sh` lấy dòng khớp `version "` thay vì dòng đầu (`grep -m1 ' version "'`), kèm test với đầu ra có dòng `Picked up`. Sửa file này là đổi cổng chất lượng: plan rủi ro cao, duyệt chéo. Phiên không tự sửa script và không bỏ `JAVA_TOOL_OPTIONS` (cấu hình tải nằm ở setup script).
- **Áp dụng:**
- **Kiểm chứng:**
