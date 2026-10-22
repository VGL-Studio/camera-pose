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

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, T-007.
- **Chuyện gì:** plan T-007 dựa vào `captureToImage` trong Robolectric `NATIVE` để đo màu điểm ảnh; kỹ thuật này hết thời gian chờ, task dừng ở bước 3.
- **Bằng chứng:** `ComposeTimeoutException` tại `WindowCapture.forceRedraw`; nhật ký plan T-007.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: kỹ thuật hay thư viện lần đầu dùng trong dự án phải được chạy thử bằng một test tạm không commit trước khi duyệt, kết quả ghi vào mục 4 (mở rộng L-029).
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#77.
- **Kiểm chứng:**

### L-002: Bí mật Firebase staging là file giả, cuối phiên không phát được

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01.
- **Chuyện gì:** `STAGING_GOOGLE_SERVICES_JSON` vẫn là file giả; `device-session.yml` và `staging.yml` lỗi ở bước cấu hình, sau khi cả phiên đã chạy.
- **Bằng chứng:** `device-session` 39589532354 và 39589890609, `staging` 39589924059: `processStagingReleaseGoogleServices: No matching client found for package name '...guide.staging'`.
- **Đề xuất sửa:** `docs/auto-run.md`, mục 2: kiểm tra đầu phiên có một điều kiện "bí mật Firebase staging có app staging", để lỗi lộ ra trước khi phiên bắt đầu.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-003: Quy tắc đếm `captureRoboImage(` của `check.sh` chặn ảnh tạm để đo điểm ảnh

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-02, T-007.
- **Chuyện gì:** plan T-007 phiên bản 2 đo màu điểm ảnh từ ảnh `captureRoboImage` ghi vào `build/`; code và test qua, nhưng `scripts/check.sh` đòi mọi lần gọi `captureRoboImage(` ghi `"src/test/screenshots/<Tên>.png"`, nên cổng chất lượng không qua và task dừng ở bước 5.
- **Bằng chứng:** `scripts/check.sh` dòng 45 đến 55; log `::error:::core:designsystem: mọi captureRoboImage phải ghi rõ đường dẫn`; nhật ký plan T-007.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: kỹ thuật chụp hay đo mới phải được chạy thử cùng `scripts/check.sh`, không chỉ chạy test; tìm script đọc file test (`grep -n captureRoboImage scripts/`) như L-020. Hoặc đổi `check.sh` cho phép ảnh tạm ghi vào `build/` (qua dự án mẫu).
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#77.
- **Kiểm chứng:**

### L-004: Thuộc tính trợ năng của Compose không lan từ node cha xuống node con

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-03, T-011.
- **Chuyện gì:** plan T-011 ẩn màn hình phía sau hộp thoại khỏi TalkBack bằng `Modifier.semantics { hideFromAccessibility() }` trên khung bọc nội dung. Thuộc tính chỉ nằm trên node của khung; nút bên trong vẫn hiện với trình đọc màn hình. Test viết trước (nút phải mang `HideFromAccessibility`) bắt được, task dừng ở bước 4.
- **Bằng chứng:** `NetworkDialogHostTest.shown_blocksContentClicks_andHidesItFromAccessibility` trên nhánh `claude/T-011-core-network-hop-thoai` (commit 555eb12); bytecode Compose UI 1.9.1: `SemanticsOwnerKt.isHidden` chỉ đọc cấu hình của chính node, `AndroidComposeViewAccessibilityDelegateCompat` gọi `setVisibleToUser(!isHidden)` cho từng node; nhật ký plan T-011.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: thay đổi về trợ năng (ẩn, gộp, thứ tự đọc) phải được chạy thử bằng test tạm trước khi duyệt, đo trên cây `AccessibilityNodeInfo` hay node đích, không suy ra từ node cha (mở rộng L-001).
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#72.
- **Kiểm chứng:**

### L-005: Môi trường `claude --cloud` chặn máy chủ R2 staging, plan không kiểm được file bằng `curl`

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01, T-010.
- **Chuyện gì:** plan T-010 (mục 4, dòng cuối; mục 7, điều 3) yêu cầu `curl` 9 địa chỉ trên `https://camera-pose.vgl.io.vn/` trước bước 3. Proxy của môi trường cloud trả 403 cho máy chủ này (chính sách mạng), nên `curl` ra mã `000`. Phiên dựa vào ghi chú của danh sách chạy (dev đã kiểm, `200`) và `AppLaunchTest` trên CI (đã qua).
- **Bằng chứng:** `curl: (56) CONNECT tunnel failed, response 403` lúc 02:08 UTC; rà soát độc lập T-010 cũng không ra mạng được; CI lần chạy 39710999529 `ui-test` xanh.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: điều kiện về file trên R2 kiểm bằng một workflow trên GitHub (ví dụ thêm bước vào `device-session.yml` hay một workflow chỉ đọc) hoặc ghi rõ "dev kiểm trước phiên", không bằng `curl` từ phiên; hoặc thêm máy chủ R2 staging vào danh sách mạng cho phép của môi trường cloud (`docs/ops/` hướng dẫn dựng môi trường).
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-006: `captureToImage()` hết giờ trên Robolectric; đo điểm ảnh bằng cách vẽ view gốc ra Bitmap

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01, T-008 (lần thứ hai, sau T-007).
- **Chuyện gì:** plan T-008 ghi test màu nền dùng `onNodeWithTag(...).captureToImage()`. Trên Robolectric (`GraphicsMode.NATIVE`) lệnh này ném `ComposeTimeoutException` sau 2 giây, như T-007 đã gặp. Phiên đổi sang cách của `ComponentsTest` (vẽ `decorView` ra `Bitmap`), điều test kiểm tra không đổi.
- **Bằng chứng:** `background_isBackgroundRole_light`: `ComposeTimeoutException: Condition still not satisfied after 2000 ms` ở bước 3; nhật ký plan T-008, mục Lệch so với plan.
- **Đề xuất sửa:** `CLAUDE.md`, mục Viết một feature mới, bước 10: test đo màu điểm ảnh trên JVM vẽ view gốc ra `Bitmap` (hàm `pixelAt` của `ComponentsTest`), không dùng `captureToImage()`. Có thể đưa `pixelAt` vào một tiện ích test dùng chung.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#77.
- **Kiểm chứng:**

### L-007: Hàm CameraX trả `ListenableFuture` không gọi được vì Guava không có trên classpath biên dịch

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** app. **Nguồn:** RUN-20261022-01, T-013.
- **Chuyện gì:** `CameraControl.setZoomRatio` trả `com.google.common.util.concurrent.ListenableFuture`; Kotlin báo "Cannot access class 'ListenableFuture'". Plan cấm thêm thư viện (mục 7, điều 4), nên phiên gọi hàm qua reflection. Rà soát độc lập chỉ ra R8 có thể xóa hàm ở bản minify và làm app dừng khi vào camera; phiên tra hàm lười và thêm luật keep trong `consumer-rules.pro`, kiểm bằng `mapping.txt` của `stagingRelease`.
- **Bằng chứng:** lỗi biên dịch `CameraXController.kt:114`; rà soát độc lập T-013 (điểm chặn 1); PR #26.
- **Đề xuất sửa:** một plan rủi ro cao riêng thêm `androidx.concurrent:concurrent-futures-ktx` (hoặc `com.google.guava:listenablefuture`) vào `gradle/libs.versions.toml` cho `:feature:camera`, rồi bỏ reflection. Plan dùng CameraX về sau (T-015 chỉnh zoom, lấy nét) kiểm trước những hàm trả `ListenableFuture`.
- **Áp dụng:**
- **Kiểm chứng:**

### L-008: Phiên mở bằng `claude --cloud` không có `resume-run.yml` tiếp tục, và `token-usage.py` cần thư mục gốc của checkout chính

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01.
- **Chuyện gì:** (1) Phiên này mở bằng `claude --cloud`, không qua routine. `resume-run.yml` kích hoạt lại routine khi phiên dừng vì hết hạn mức, nên không nối lại được phiên này; tiến trình của phiên còn bị khởi động lại một lần lúc 04:10 UTC giữa lúc chạy `check.sh` (lệnh bị dừng, mã 137), phiên tự chạy lại. (2) Nhánh báo cáo dùng một worktree riêng (`../report`) để không đổi nhánh dưới bản build của task; `scripts/token-usage.py` chạy trong worktree đó không thấy transcript (lọc theo thư mục gốc), phải đặt `TOKEN_USAGE_ROOT` về checkout chính.
- **Bằng chứng:** "Không đo được: không có transcript nào của repo này" khi chạy trong `../report`; chạy lại với `TOKEN_USAGE_ROOT=/home/user/camera-pose` ra bảng đầy đủ.
- **Đề xuất sửa:** `docs/auto-run.md`, mục 1 và mục 4: ghi cách tiếp tục khi phiên mở bằng `claude --cloud` (người mở lại phiên với cùng danh sách chạy, phiên đọc nhánh báo cáo như phiên tiếp tục); khuyến nghị worktree riêng cho nhánh báo cáo và ghi cách chạy `token-usage.py` từ đó. `scripts/token-usage.py` có thể tìm transcript theo tên repo thay vì đường dẫn tuyệt đối.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-009: Hộp hỏi quyền camera còn mở làm hỏng lần đo khởi động nguội của `device-session`

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** app. **Nguồn:** RUN-20261022-01, T-013.
- **Chuyện gì:** từ T-013, màn chính ở staging là camera và app hỏi quyền camera. Kịch bản chuẩn của `device-session` dừng app bằng force-stop giữa các lần mở nguội; hộp hỏi quyền của lần 2 (`GrantPermissionsActivity`) vẫn ở trong task, nên lần mở thứ 3 đưa hộp đó lên thay vì mở `MainActivity`: `cold_start run=3 total_ms=0`, không có log nào của app. Cùng nguyên nhân làm `AppLaunchTest` đỏ ở vòng 2 của CI T-013.
- **Bằng chứng:** `scenarios.txt` và logcat của TS-20261022-01 (`onActivityRestartAttempt: ... GrantPermissionsActivity` lúc 04:58:46), lần chạy 39724404079.
- **Đề xuất sửa:** `scripts/device-session.sh` (kịch bản chuẩn) cấp quyền camera trước các lần mở nguội (`pm grant <gói> android.permission.CAMERA`) hoặc thêm một kịch bản riêng cho luồng hỏi quyền; ghi vào `docs/ops/test-sessions.md`.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**
### L-010: Chạy thử kỹ thuật mới bằng đúng lệnh của phiên, ghi vào mục riêng của plan

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, -02, -03, RUN-20261022-01; T-007, T-008, T-011.
- **Chuyện gì:** ba lần dừng cùng một gốc (L-001, L-003, L-004), và lần thứ tư ở T-008 (L-006): kỹ thuật lần đầu dùng chưa được chạy thử, hoặc chạy thử bằng lệnh khác lệnh mà phiên dùng. Plan T-007 phiên bản 2 được kiểm chứng bằng `./gradlew :core:designsystem:testDebugUnitTest`, nhưng phiên dừng ở `scripts/check.sh`.
- **Bằng chứng:** nhật ký plan T-007 (phiên bản 1, 2), T-011 (phiên bản 1); phiên bản 3 của T-007 và 2 của T-011 được chạy thử bằng `scripts/check.sh` trên nhánh của task trước khi duyệt và không dừng nữa (T-007 merge ở PR #19, T-011 ở PR #23).
- **Đề xuất sửa:** `docs/templates/plan.md` thêm mục "Chạy thử" (test tạm, lệnh đã chạy, kết quả) bắt buộc khi tiêu chí E từ 2 trở lên; lệnh phải là `scripts/check.sh` trên nhánh có code liên quan. `scripts/plan-check.py` báo lỗi khi E từ 2 trở lên mà thiếu mục này. Ghi rõ: Robolectric không mô phỏng việc cửa sổ chồng nhau, phần trợ năng và chạm qua cửa sổ phải có bước kiểm tra thủ công; chạy thử đo cả hình ảnh thật, không chỉ cây ngữ nghĩa (báo cáo RUN-20261022-01, Kiểm chứng bài học cũ).
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#77, VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-011: Tài nguyên R2 bắt buộc của app chưa có, PR có code đầu tiên đỏ ở emulator

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** PR #6.
- **Chuyện gì:** ba file `test/v1/*.txt` mà màn loading của dự án mẫu tải chưa được đưa lên R2 staging khi tạo app; mọi PR trước chỉ sửa tài liệu nên `ui-test` bị bỏ qua, lỗi chỉ lộ ra ở PR có code đầu tiên.
- **Bằng chứng:** lần chạy 39566140163, logcat `HTTP 404 for https://camera-pose.vgl.io.vn/test/v1/...`, `AppLaunchTest` hết 30 giây chờ.
- **Đề xuất sửa:** thêm `scripts/setup-check.sh`: `curl -I` mọi file R2 mà app bắt buộc có (danh sách trong một file cấu hình), chạy ở bước cuối của `docs/ops/setup-new-app.md`, trong `env-canary.yml` và ở kiểm tra đầu phiên. Kiểm bằng workflow trên GitHub, vì môi trường `claude --cloud` chặn máy chủ R2 (L-005).
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-012: Kiểm tra bí mật staging ở đầu phiên, không để tới cuối

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, -02, -03 (nối tiếp L-002), RUN-20261022-01.
- **Chuyện gì:** ba phiên liền chỉ biết bí mật `STAGING_GOOGLE_SERVICES_JSON` là file giả ở bước cuối (`device-session`, `staging`), sau khi đã tốn cả phiên.
- **Bằng chứng:** báo cáo RUN-20261021-01, -02, -03, mục Cần người kiểm tra.
- **Đề xuất sửa:** `start-run.yml` hoặc bước kiểm tra đầu phiên chạy một kiểm tra nhỏ, không in giá trị: bí mật có `project_id` khác project giả và có app `...staging`; sai thì không bắt đầu phiên, mở issue `env-broken`. Kiểm luôn nhóm người test trong `STAGING_TESTER_GROUPS` (mặc định `team`) đã có trong App Distribution: RUN-20261022-01 tải bản build lên được nhưng bước phát đỏ (`HTTP Error: 404`) vì chưa tạo nhóm.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-013: Quy tắc của `check.sh` phải hiện ra lúc viết plan

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-02, T-007 (nối tiếp L-003).
- **Chuyện gì:** quy tắc "mọi `captureRoboImage` ghi vào `src/test/screenshots/`" chỉ nằm trong `check.sh` và một câu của `CLAUDE.md`; người viết plan và phiên chính đều bỏ sót.
- **Bằng chứng:** `check.sh` dòng 45 đến 55; nhật ký plan T-007 phiên bản 2.
- **Đề xuất sửa:** `plan-check.py` dò trong plan các mẫu mà `check.sh` từ chối (ví dụ `captureRoboImage(` với đường dẫn ngoài `src/test/screenshots/`); `docs/auto-run.md` có bảng các quy tắc của `check.sh`.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-014: Plan đẩy việc cho nhau, hoặc cùng sửa một file mà không phụ thuộc nhau

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, plan T-009, T-010, T-011, T-013.
- **Chuyện gì:** plan T-009 (câu hỏi 1) giao việc thêm lý do lỗi tải cho T-011, plan T-011 (câu hỏi 5) giao lại cho T-009; plan T-010 và T-013 cùng sửa `AppNavHost.kt`, `MainActivity.kt`, `app/build.gradle.kts`, `AppLaunchTest.kt` mà ticket không phụ thuộc nhau. Chỉ bắt được khi phiên chính đối chiếu bằng tay.
- **Bằng chứng:** lịch sử PR #7 và #11; ticket T-011, T-013 thêm phụ thuộc sau đối chiếu.
- **Đề xuất sửa:** `plan-check.py` cảnh báo khi hai plan `approved` cùng sửa một file mà không có quan hệ `depends_on`; mẫu plan thêm câu "việc giao cho ticket khác phải được plan của ticket đó ghi nhận".
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-015: Danh sách chạy chỉ một chuỗi phụ thuộc thì một task dừng làm cả phiên trống

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261021-02.
- **Chuyện gì:** danh sách chỉ có T-007 và T-011 (phụ thuộc T-007); T-007 dừng, phiên không merge được gì mà vẫn tốn phần cố định.
- **Bằng chứng:** báo cáo RUN-20261021-02.
- **Đề xuất sửa:** khi lập danh sách chạy, nếu chỉ có một chuỗi thì viết thêm plan độc lập trước khi chạy; một lệnh kiểm danh sách chạy cảnh báo trường hợp này.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-016: `docs/STATUS.md` xung đột ở mọi PR

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** PR #11, #14, #16.
- **Chuyện gì:** mỗi PR tài liệu sinh lại `docs/STATUS.md`; PR sau luôn xung đột với PR trước, phải gộp và sinh lại bằng tay.
- **Bằng chứng:** PR #11, #14, #16 đều báo `DIRTY` chỉ vì `docs/STATUS.md`.
- **Đề xuất sửa:** `.gitattributes` khai báo `docs/STATUS.md merge=ours` cùng một bước sinh lại trang trên nhánh chính sau khi merge; hoặc `status.py --check` chỉ chạy trên nhánh chính.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-017: `merge-if-green.sh` cần tự cập nhật nhánh và chịu được job kẹt

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** PR #6, #11, #14.
- **Chuyện gì:** script từ chối mọi PR có nhánh cũ hơn nhánh chính, người phải cập nhật nhánh, chờ CI, gọi lại; một lần GitHub ghi job `changes` là `in_progress` dù lần chạy đã `completed success`.
- **Bằng chứng:** lần chạy 39613776353 (PR #11); PR #6 bị từ chối vì chậm 2 commit.
- **Đề xuất sửa:** thêm `merge-if-green.sh --update`: cập nhật nhánh, chờ CI của commit mới, rồi merge; coi lần chạy `completed success` là xanh khi trạng thái job còn lệch.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-018: PR bị merge trước khi nhận commit cuối

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** PR #7, #8.
- **Chuyện gì:** commit duyệt plan được đẩy lên nhánh của PR #7 sau khi người đã merge PR đó; plan và ticket trên nhánh chính vẫn `draft`, phải mở PR #8 để đưa lại.
- **Bằng chứng:** lịch sử nhánh `claude/plans-foundation` (commit 4a11c4b).
- **Đề xuất sửa:** `merge-if-green.sh` in mã commit sẽ merge và cảnh báo nếu nhánh có commit trong vài phút gần đây; quy ước: thay đổi sau khi PR đã gửi người duyệt thì làm trên nhánh mới.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-019: Trạng thái ticket và plan đổi tay, hay lệch nhau

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, 2026-10-22.
- **Chuyện gì:** ticket được chuyển `approved` khi plan còn `draft`; plan được duyệt mà ticket còn `planned`; plan thiếu `approved_at` (plan-check bắt được).
- **Bằng chứng:** lịch sử ticket T-008, T-010, T-013 và plan T-007 phiên bản 2.
- **Đề xuất sửa:** `scripts/status.py --sync`: plan `approved` hay `stopped` thì ticket chuyển theo; `plan-check.py` cảnh báo khi hai trạng thái lệch nhau.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-020: Routine không dùng cloud session credits; mở phiên bằng `claude --cloud` thì dùng được

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, 2026-10-22; RUN-20261021-03.
- **Chuyện gì:** khuyến mãi cloud session credits (Max $250, hết hạn 2026-11-04 23:59 PT) không áp dụng cho "Projects and Routines". Ba phiên chạy qua routine tính vào hạn mức tuần (đã tới 98%); phiên mở bằng `claude --cloud` trừ vào credit.
- **Bằng chứng:** https://support.claude.com/en/articles/17152539-cloud-sessions-bonus-credit-promotion; số dư $248, $247 sau phiên thử, $237 sau RUN-20261021-03 (khoảng 24M token, khoảng $10).
- **Đề xuất sửa:** `docs/ops/staging-and-runs.md` ghi cách mở phiên bằng `claude --cloud` khi còn credit: prompt, các kiểm tra phải tự làm (thay `start-run.yml`); cách tiếp tục phiên khi bị ngắt nằm ở L-008. RUN-20261022-01 chạy bằng cách này, 4 trên 4 task merge. `docs/metrics/token-usage.csv` thêm cột tiền credit đã dùng.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-021: Số ước tính token tạm quá cao

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, -02, -03, RUN-20261022-01.
- **Chuyện gì:** số tạm 40M cho task cỡ L; thực tế mỗi task 2M đến 15M (T-013 cỡ L khoảng 24M lúc gần xong), cả phiên 4M đến 97M.
- **Bằng chứng:** `docs/metrics/token-usage.csv`.
- **Đề xuất sửa:** đổi số tạm của `scripts/token-usage.py estimate` (ví dụ S 5M, M 15M, L 30M) cho tới khi đủ 5 task mỗi cỡ.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-022: Java của máy dev quá mới làm Gradle không chạy

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21.
- **Chuyện gì:** máy chỉ có Java 26; Gradle 8.14.3 dừng ngay với lỗi chỉ ghi "26".
- **Bằng chứng:** log `scripts/check.sh` lần đầu trên máy dev; chạy lại với JDK 21 của Android Studio thì qua.
- **Đề xuất sửa:** `scripts/check.sh` kiểm phiên bản Java trước khi gọi Gradle và gợi ý `JAVA_HOME` trỏ tới JDK của Android Studio; thêm vào `docs/ops/setup-new-app.md`.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-023: Lỗi thiết kế lọt vào gói bàn giao

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** phiên có người theo dõi 2026-10-21, 2026-10-22.
- **Chuyện gì:** độ mờ của lớp tô mất khi gắn biến trên Figma (56 chỗ, lộ ra khi agent viết plan T-013 so README với ảnh); tên frame màn hình gập trong bản đề xuất khác ADR-0020.
- **Bằng chứng:** PR #11 (xuất lại 18 ảnh camera); PR #3, sửa mục 7 của bản đề xuất.
- **Đề xuất sửa:** `scripts/figma-export.py` ghi cả độ mờ của lớp tô vào `context/`; script so danh sách frame trong bản đề xuất với frame trên Figma; bước quét chỉ đọc (design-workflow.md, mục 8.4) kiểm instance có giá trị tô khác component gốc.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-024: Cải tiến đã làm ở app, cần đưa về dự án mẫu

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** PR #10, #12, #16, #19.
- **Chuyện gì:** bốn thay đổi có ích cho mọi app đã merge ở app: `plan-check.py` cho plan khai báo ảnh thiết kế do ticket khác vẽ (PR #16); `.gitignore` chặn `*.b64` và khóa tài khoản dịch vụ (PR #10); `RemoteSettings.string()` (T-009, PR #12); đo màu điểm ảnh bằng cách vẽ `decorView` ra `Bitmap` thay `captureToImage` (T-007, PR #19; L-006).
- **Bằng chứng:** các PR đã nêu.
- **Đề xuất sửa:** mở issue `from-app` ở dự án mẫu cho từng thay đổi; ghi cách đo điểm ảnh vào `CLAUDE.md`, mục test giao diện.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#72, VGL-Studio/app-project-template#73, VGL-Studio/app-project-template#74, VGL-Studio/app-project-template#75, VGL-Studio/app-project-template#76, VGL-Studio/app-project-template#77.
- **Kiểm chứng:**

### L-025: Tài liệu của app còn chỗ cũ sau khi quyết định đổi

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** app. **Nguồn:** phiên có người theo dõi 2026-10-22.
- **Chuyện gì:** `docs/runs/RUN-20261021-04.md` (`draft`, do RUN-20261021-03 lập) trùng với `RUN-20261022-01`; bản đề xuất mục 11 còn khuôn đường dẫn ảnh `<poseId>/<sha8>/` mà `AssetPath` từ chối (plan T-009 đã đổi sang mã băm trong tên file).
- **Bằng chứng:** `docs/runs/`; plan T-009, câu hỏi 2.
- **Đề xuất sửa:** ghi chú "không dùng, thay bằng RUN-20261022-01" trong `RUN-20261021-04`; sửa mục 11 của bản đề xuất theo plan T-009.
- **Áp dụng:**
- **Kiểm chứng:**

### L-026: Mã phiên thử trên máy thật trùng mã phiên emulator của phiên tự chạy

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** TS-20261022-02.
- **Chuyện gì:** `scripts/device-session.sh` cấp mã `TS-20261022-01` cho phiên trên máy thật, trùng phiên emulator của RUN-20261022-01. Script chỉ tránh mã có thư mục trên máy hoặc có báo cáo trong `docs/test-sessions/`; phiên emulator chỉ được ghi trong báo cáo phiên tự chạy.
- **Bằng chứng:** `scripts/device-session.sh` hàm `next_session_dir`; báo cáo RUN-20261022-01, mục Kiểm thử trên máy ảo; báo cáo phải đổi sang `TS-20261022-02`.
- **Đề xuất sửa:** `next_session_dir` tìm thêm mã trong `docs/runs/*.md`; hoặc phiên emulator dùng tiền tố riêng.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-027: Lỗi DNS của tên miền file tĩnh chỉ lộ ra trên máy thật

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** TS-20261022-02.
- **Chuyện gì:** vùng `io.vn` không qua bước kiểm DNSSEC ở Cloudflare và Google DNS, nên `camera-pose.vgl.io.vn` không phân giải được; máy thật không tải được catalog, trong khi phiên emulator trên GitHub vài giờ trước vẫn qua. Log không cho biết lý do vì Android bỏ stack trace của `UnknownHostException`.
- **Bằng chứng:** TS-20261022-02, P-1; FB-20261022-02, FB-20261022-04.
- **Đề xuất sửa:** `env-canary.yml` kiểm phân giải tên miền file tĩnh qua 1.1.1.1 và 8.8.8.8 (không chỉ DNS của runner); `:core:assets` ghi tên lớp lỗi trong câu log; `docs/ops/setup-new-app.md` khuyên chọn tên miền thuộc vùng có DNSSEC ổn định.
- **Áp dụng:** đề xuất về dự án mẫu (`docs/ops/from-app.md`): issue VGL-Studio/app-project-template#73, VGL-Studio/app-project-template#78.
- **Kiểm chứng:**

### L-028: Phiên kiểm `main-broken` trước khi `main-guard` của lần merge vừa rồi chạy xong

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, T-026, T-012.
- **Chuyện gì:** T-026 merge lúc 16:50 UTC; phiên kiểm `main-broken` ngay sau đó (0 issue) rồi bắt đầu T-012. `main-guard` của commit merge e9bf4bb chạy xong lúc 16:58 và mở issue #35. Phiên chỉ thấy issue khi kiểm lại lúc 17:35, sau khi T-012 đã làm xong bước 7 (khoảng 45 phút và 22M token cho một task không merge được).
- **Bằng chứng:** `main-guard` lần chạy 39817360705 (16:50 tới 16:58); issue #35 tạo 16:58:17; RUN-20261023-01, Dòng thời gian.
- **Đề xuất sửa:** `docs/auto-run.md` bước 9: sau khi merge, chờ `main-guard` của commit merge bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>` ở chế độ nền, trong lúc làm bước 10 và bước 1, 2 của task sau; kiểm lại `main-broken` trước khi đẩy commit đầu tiên của task sau. Hoặc `merge-if-green.sh` in nhắc lệnh này.
- **Áp dụng:**
- **Kiểm chứng:**

### L-029: Screenshot test của trạng thái có vòng chờ đỏ trên `main-guard` dù xanh ở PR và cục bộ

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** app. **Nguồn:** RUN-20261023-01, issue #35.
- **Chuyện gì:** `CameraScreenScreenshotTest > starting_dark` (trạng thái `Starting`, có vòng chờ chạy mãi) thất bại trên `main-guard` ở e9bf4bb với `AssertionFailedWithCauseError` tại dòng `captureRoboImage`. Cùng code xanh ở CI của PR #34 (lần chạy 39815764346), ở `build-and-test` của PR #36 (dựng trên e9bf4bb, lần chạy 39821691045) và ở 3 lần `scripts/check.sh` cục bộ. T-026 không đổi giao diện camera. Nhiều khả năng là test chập chờn vì hiệu ứng vô hạn; chưa xác nhận.
- **Bằng chứng:** log `main-guard` lần chạy 39817360705, dòng "CameraScreenScreenshotTest > starting_dark FAILED"; nhánh `revert/e9bf4bb`.
- **Đề xuất sửa:** screenshot test của trạng thái có hiệu ứng vô hạn dừng đồng hồ của Compose (`mainClock.autoAdvance = false`) hoặc dùng bản tĩnh của vòng chờ; `main-guard` chạy lại một lần test thất bại trước khi mở `main-broken` nếu lỗi là hết giờ chờ của Compose.
- **Áp dụng:**
- **Kiểm chứng:**

### L-030: Điều hướng sau một hàm `suspend` đọc DataStore chạy ngoài luồng chính dưới Compose test

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, T-012.
- **Chuyện gì:** T-012 cho `chooseMainScreen()` đọc DataStore. `AppLaunchTest` trên emulator báo `IllegalStateException: Method setCurrentState must be called on the main thread` ở `navController.navigate` trong `scope.launch` của `AppNavHost`: coroutine được tiếp tục trên `LimitedDispatcher$Worker`. `AppNavHost` nằm trong ngoại lệ viết test trước của plan (không có test JVM), nên lỗi chỉ lộ ra ở `ui-test`, vòng CI dài nhất.
- **Bằng chứng:** lần chạy 39821691045, job `ui-test`, bước "Chi tiết test trên emulator".
- **Đề xuất sửa:** `CLAUDE.md` mục Viết một feature mới: điều hướng chạy trên `Dispatchers.Main.immediate` khi đứng sau một lời gọi `suspend`; plan đổi hàm `suspend` mà `AppNavHost` gọi phải có test JVM (Robolectric) dựng `AppNavHost` với bản giả đổi luồng, thay vì ngoại lệ.
- **Áp dụng:**
- **Kiểm chứng:**

### L-031: Giao bước 3 đến 5 của task lớn cho một phiên AI viết code riêng

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261023-01, T-012.
- **Chuyện gì:** với T-012 (47 file), phiên chính giao bước 3 đến 5 cho một phiên con có đủ quy tắc (danh sách file, thứ tự commit, đẩy sau mỗi commit, điều kiện dừng, báo cáo theo mẫu nhật ký). Phiên con xong trong 22 phút, `check.sh` qua, không file nào ngoài plan; phiên chính chỉ kiểm danh sách file, giao rà soát độc lập cho một phiên khác và giữ ngữ cảnh gọn cho các task sau. Cũng trong phiên: phiên chính lỡ sửa file khi `check.sh` đang chạy nền trong cùng thư mục (T-026) và phải chạy lại.
- **Bằng chứng:** commit dabc2a3 tới 15f10b2 (16:58 tới 17:12); token T-012 21,9M so với ước tính 20M.
- **Đề xuất sửa:** `docs/auto-run.md` mục 3: cho phép và mô tả cách giao bước 3 đến 5 cho phiên con (nội dung giao việc tối thiểu); nhắc không sửa file trong thư mục đang chạy `check.sh` nền, chỉ chạy rà soát độc lập song song.
- **Áp dụng:**
- **Kiểm chứng:**
