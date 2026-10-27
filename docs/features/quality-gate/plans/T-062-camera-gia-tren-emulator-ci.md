---
ticket: T-062
title: Emulator của `device-session` có camera sau giả, để kịch bản camera chạy được trên CI
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.5M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (L-081)
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-062: EMULATOR CỦA `DEVICE-SESSION` CÓ CAMERA SAU GIẢ, ĐỂ KỊCH BẢN CAMERA CHẠY ĐƯỢC TRÊN CI

## 1. Tóm tắt

Bước "Kịch bản chuẩn trên emulator" của `.github/workflows/device-session.yml` đổi `-camera-back none` thành `-camera-back emulated`. Emulator API 34 khi đó có một camera sau giả (cảnh giả do emulator vẽ), app mở được camera, nên hai tệp `T-020-camera-mo-gap.txt` và `T-058-chup-tu-chi-tiet.txt` chạy thật thay vì `bỏ qua (môi trường)` ở bước `need id=camera_grid_button`. Kiểm chứng: lần `device-session` chạy thử trên nhánh tạm với đúng thay đổi này (mục 4.1), và lần `device-session` cuối phiên sau khi merge.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là mục Đề xuất cho buổi họp tiếp theo của báo cáo RUN-20261026-03 (đề xuất 3), câu hỏi 2 của plan T-058, Tuấn chọn làm ngày 2026-10-26.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Kịch bản camera chạy trên CI | Khi `device-session` chạy trên nhánh chính thì `T-058-chup-tu-chi-tiet.txt` và `T-020-camera-mo-gap.txt` có kết quả `đạt` (hay `lỗi` có bước lỗi), không còn `bỏ qua (môi trường)` ở bước `need id=camera_grid_button` | Camera giả đủ cho hai tệp: cả hai chỉ cần camera mở được và chụp được, không cần nhận dạng dáng người (T-058 dừng ở `review_screen`; T-020 không chụp) |
| Log của phiên sạch hơn | Khi `device-session` chạy thì `app-log.txt` không còn dòng `VGL.Camera` "Camera could not open" khi mở camera sau | 125 dòng lỗi ở RUN-20261026-03 đều do không có camera; còn lại lỗi khi monkey đổi sang camera trước (mục 4.1) |
| Không làm hỏng phần khác | Khi `device-session` chạy thì kịch bản chuẩn, monkey và các tệp khác (T-022, T-024, T-056) giữ kết quả như trước; không thêm crash hay ANR | Monkey có thể vào màn camera nhiều hơn khi camera mở được; crash ở đó là lỗi thật của app |

Thiết kế tham chiếu: Không có thiết kế riêng (CI, docs/ops/design-workflow.md).

ADR liên quan: ADR-0010 (cổng chất lượng), ADR-0012 (cấu hình staging trong `device-session.yml`).

## 3. Giải pháp

### 3.1. Cách làm

**`.github/workflows/device-session.yml`**, bước "Kịch bản chuẩn trên emulator" (dòng 105): `emulator-options: -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back emulated`. Không đổi gì khác: `api-level: 34`, `target: google_apis`, `arch: x86_64`, điều kiện `if: github.ref == 'refs/heads/main'`, `timeout-minutes: 50`. Thêm một dòng chú thích tiếng Anh trên dòng đó: `# An emulated back camera (a generated scene, no person) lets the camera scenarios run (T-062).` Chú thích ở dòng 86 ("Same emulator setup as the ui-test job of pr-check.yml.") không còn đúng, sửa thành `# Same emulator setup as the ui-test job of pr-check.yml, except the emulated back camera (T-062).`

`-camera-back emulated` là camera giả của emulator: ảnh xem trước là một cảnh do emulator tự vẽ, không có người, nên bộ nhận dạng dáng không ra điểm khớp. Không dùng `virtualscene` (cảnh 3D, cần dựng bằng GPU; emulator CI chạy `swiftshader_indirect`) và `webcam0` (không có webcam trên runner).

**`feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`**: dòng 2 bỏ đoạn "; cần camera sau, emulator CI bỏ qua"; dòng 9 `need id=camera_grid_button 15` thành `wait id=camera_grid_button 15`.

**`feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt`**: dòng 9 `need id=camera_grid_button 15` thành `wait id=camera_grid_button 15`.

Lý do đổi `need` thành `wait`: sau T-062 cả máy dev lẫn emulator CI đều có camera, nên camera không mở được là lỗi của app (như FB-20261026-01), phải ra `lỗi`, không được giấu thành `bỏ qua (môi trường)`. `need id=camera_poses` ở dòng 10 của T-058 giữ nguyên.

**`docs/ops/test-sessions.md`**, mục 4, sau đoạn đầu (dòng 71): một câu: "Emulator có camera sau giả (`-camera-back emulated`, T-062): màn camera mở và chụp được, ảnh xem trước là cảnh giả không có người, nên tiêu chí cần nhận dạng dáng (điểm khớp, lời nhắc) vẫn kiểm trên máy thật." Bảng lệnh mục 5, dòng `need`: "(camera)" thành "(camera trên máy không có camera, mạng)".

**Đề xuất về dự án mẫu:** `device-session.yml` và `pr-check.yml` đến từ dự án mẫu (`-camera-back none` có từ commit khởi tạo `7762576`). Mô tả PR của task ghi đề xuất này theo `docs/ops/from-app.md`; người mở issue ở dự án mẫu.

Nơi khác đọc `device-session.yml` (`grep -rn "device-session" scripts/ .github/ docs/CLAUDE.md CLAUDE.md`): `scripts/wait-run.sh` chờ theo tên file; phiên khởi động workflow bằng `gh api .../dispatches` (`docs/auto-run.md`, mục 4) với input `reason`; `scripts/device-session.sh` không đọc tham số của emulator. Plan không đổi tên workflow, job, input hay artifact.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `.github/workflows/device-session.yml` | `-camera-back emulated`, một dòng chú thích mới, sửa chú thích dòng 86 (mục 3.1) |
| Sửa | `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` | Dòng `criterion:` bỏ "cần camera sau, emulator CI bỏ qua"; dòng 9 `need` thành `wait` |
| Sửa | `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt` | Dòng 9 `need` thành `wait` |
| Sửa | `docs/ops/test-sessions.md` | Mục 4: một câu về camera giả; mục 5, dòng `need` |
| Sửa | `docs/features/quality-gate/tickets/T-062-camera-gia-tren-emulator-ci.md` | Trạng thái theo phiên |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm. Quyền `CAMERA` đã có; `scenarios --yes` cấp sẵn quyền runtime. Workflow giữ `permissions: contents: read` và các bí mật staging như cũ
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

- **Điểm chưa chắc 1:** monkey (1500 sự kiện) vào màn camera có camera mở được, có thể làm app crash hay ANR mà trước đây không lộ ra (lỗi thật của app, nhưng chặn phát staging của phiên).
  - **Điều kiện chuyển:** `device-session` cuối phiên đỏ vì crash hay ANR trong màn camera.
  - **Cách làm thay:** không có trong phiên: đây là lỗi thật của app, phiên ghi phát hiện và đề xuất ticket sửa theo `docs/auto-run.md` mục 4 (không phát staging khi có crash). Không quay lại `-camera-back none` để làm xanh.
- **Điểm chưa chắc 2:** emulator có camera giả có thể treo giữa chừng (mục 4.1, lần 2 lượt 1: nguyên nhân chưa rõ).
  - **Điều kiện chuyển:** sau khi merge, `device-session` đỏ mà không có `summary.md` ở 2 phiên liên tiếp.
  - **Cách làm thay:** phiên không dừng: `docs/auto-run.md` mục 4 đã cho chạy lại một lần và vẫn phát staging. Phiên thứ hai gặp thì ghi vào báo cáo và đề xuất một ticket điều tra, hoặc plan phiên bản 2 trả về `-camera-back none` do người duyệt. Phiên không tự sửa `.github/`.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Kịch bản camera chạy trên CI | Kiểm tra thủ công (emulator: `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`, `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt`) | `device-session` cuối phiên: bảng "Kịch bản theo plan" có hai tệp với kết quả khác `bỏ qua (môi trường)`; đã chạy thử trước khi duyệt (mục 4.1) |
| Log sạch hơn | Kiểm tra thủ công (emulator: `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`) | `device-session` cuối phiên: `app-log.txt` không có "Camera could not open" ngoài khoảng giữa `scenario=monkey` và dòng `VGL.Session` kế tiếp; trong monkey thì chỉ có `No available camera can be found` (nút đổi sang camera trước, mục 4.1) |
| Không làm hỏng phần khác | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`, `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt`) | `device-session` cuối phiên: ba tệp vẫn `đạt`, crash 0, ANR 0 |
| Tệp kịch bản đúng cú pháp | Script có sẵn | `scripts/device-scenario.py check feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` và `... T-020-camera-mo-gap.txt` (đã chạy trên nhánh tạm, mã 0) |
| Chỉ đổi đúng phần của plan | Đọc lại | `git diff origin/main -- .github/` chỉ có dòng `emulator-options`, dòng chú thích mới và chú thích dòng 86 |

**Ngoại lệ viết test trước:** toàn bộ phần đổi là một tham số của workflow GitHub Actions, một dòng `criterion:` và tài liệu; không có test tự động trên máy, và tệp kịch bản không "thất bại trước" được. Kiểm bằng lần chạy thật trước khi duyệt (mục 4.1) và `device-session` cuối phiên.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-26, nhánh tạm `trial/T-062` (nhánh chính `f464d7f` cộng một commit tạm không merge): `-camera-back emulated` như mục 3.1, và điều kiện `if:` của job cho thêm `refs/heads/trial/T-062` để chạy được ngoài nhánh chính (chỉ ở nhánh tạm, không có trong plan). Lệnh: `gh workflow run device-session.yml --ref trial/T-062 -f reason=...`. Lần 3, lần 4 dùng nhánh tạm tạo lại từ nhánh chính `f464d7f` với đúng phần đổi của mục 3.1 (workflow, chú thích, `wait` thay `need` ở hai tệp kịch bản). Nhánh tạm xóa sau mỗi đợt chạy.
- **Lệnh:** `scripts/check.sh` không áp dụng: plan không đổi code, test hay tài nguyên nào mà `check.sh` kiểm; workflow và tệp kịch bản chỉ chạy ở `device-session`, nên lần chạy thử là chính workflow đó trên CI, đúng lệnh phiên dùng cuối phiên.
- **Kết quả:**
  - Lần 1 (lần chạy 40350378524, 15:32 đến 15:56 UTC): không kết luận được. Cả 5 tệp `lỗi` ở bước 3 vì app đứng ở màn loading: `W VGL.Assets: Could not download poses/catalog/v3/catalog-c2e0ce0d.json (reason=NOT_FOUND, status=404)` 35 lần. Khóa Remote Config `poses_catalog_path` của staging trỏ tới file chưa có trên R2; Tuấn sửa lúc khoảng 16:00 UTC. Không do camera.
  - Lần 2 (lần chạy 40355405834, lượt 1, 16:03 đến 16:37 UTC): đỏ, **nguyên nhân chưa rõ**. Sự kiện: emulator khởi động xong 16:17:19, cài app, `device-session.sh start` lúc 16:17:43; sau đó log của bước chỉ còn dòng `ERROR | Failed to find ColorBuffer` của emulator, dòng cuối 16:27:46, rồi im lặng; bước "Kịch bản chuẩn trên emulator" kết thúc `failure` lúc 16:36:57, ở phút 33 của job (dưới giới hạn 50 phút). Không có chú thích "lost communication" nào; bước `Complete job` vẫn chạy lúc 16:36:57 và dọn các tiến trình `python3`, `timeout`, `adb` còn sót, nghĩa là máy chạy còn sống và kịch bản còn đang chạy. Hai bước `Tóm tắt` và `Lưu phiên` (`if: always()`) bị bỏ qua, không có artifact; điều này chưa từng xảy ra ở hơn 10 lần chạy trước với `-camera-back none`. Vì vậy không loại trừ được emulator treo do camera giả. Chạy lại lượt 2 bằng `gh run rerun`, rồi chạy thêm lần 3, lần 4 để đo (bên dưới).
  - Lần 2, lượt 2 (16:38 đến 17:02 UTC), xanh, phiên `TS-20261026-03`: crash 0, ANR 0, monkey 1500 sự kiện; khởi động nguội 3488, 3605, 2204 ms; không còn dòng `NOT_FOUND` nào. Kịch bản theo plan: cả 5 tệp `đạt`, tổng 479 giây (ngân sách 1300): `T-056-man-lon-xoay-ngang.txt` 79 giây, `T-020-camera-mo-gap.txt` 75 giây, `T-058-chup-tu-chi-tiet.txt` 64 giây (bước 18 `wait id=review_screen 20 ok`, log `VGL.Camera: Photo captured`), `T-022-chi-tiet-sang-camera.txt` 139 giây, `T-024-thu-vien-mo-gap.txt` 122 giây. `camera-from-detail.png` cho thấy cảnh giả của emulator trong khung xem trước, nét viền và thẻ bước vẽ đè lên. `Camera ready` 11 lần.
  - Lần 3 (lần chạy 40366043473, 17:10 đến 17:36 UTC) và lần 4 (lần chạy 40366063060, 17:36 đến 18:02 UTC), trên nhánh tạm có đủ phần đổi của mục 3.1 (kể cả `wait id=camera_grid_button` ở hai tệp): cả hai xanh ngay lượt đầu, có `summary.md` và artifact, không treo; job 26 phút. Crash 0, ANR 0, monkey 0, `E VGL.*` 0, `NOT_FOUND` 0, "Camera could not open" 0 (monkey không bấm vào nút đổi camera). Kịch bản, lần 3 rồi lần 4, theo giây: T-056 `đạt` 94, 88; T-020 `đạt` 79, 80; T-058 `đạt` 58, 64 (mỗi lần một `Photo captured`); T-022 `đạt` 133, 146; T-024 `đạt` 142, 132. `Camera ready` 20 và 17 lần.
  - Tổng hợp: 4 lượt chạy có camera giả mà app tới được màn camera (lần 2 lượt 1 và 2, lần 3, lần 4): 1 lượt dừng giữa chừng không rõ nguyên nhân, 3 lượt xanh với cả 5 tệp `đạt`. Chưa đủ để kết luận lượt dừng do camera giả hay do máy chạy; phương án dự phòng 2 ở mục 3.4 theo dõi sau khi merge.
  - "Camera could not open": 10 sự kiện (mỗi sự kiện là một dòng `E VGL.Camera: Camera could not open` kèm khoảng 20 dòng stack trace; RUN-20261026-03 có 5 sự kiện, 125 dòng `E VGL.*`, kèm "Device reporting less cameras than anticipated" ở mọi lần mở camera). Cả 10 sự kiện trong lúc monkey (16:54:05 đến 16:54:41), ngay sau `Camera ready: back` lúc 16:54:02: `IllegalArgumentException: No available camera can be found. Cams:1 ... Lens:1`. Monkey bấm nút đổi camera; emulator chỉ có camera sau, không có camera trước. Không có lần nào trong kịch bản. Tiêu chí "Log sạch hơn" ở mục 4 vì vậy sửa thành: không có "Camera could not open" ngoài khoảng `scenario=monkey` (mục 8, câu 3).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI): `.github/workflows/device-session.yml`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 0 | Một tham số của workflow, một dòng của tệp kịch bản, hai câu tài liệu |
| B. Độ rõ của yêu cầu | 0 | Hai tệp hết `bỏ qua (môi trường)`; Tuấn đã chọn |
| C. Khả năng kiểm chứng tự động | 1 | Chỉ kiểm được bằng lần chạy `device-session` thật, không có test trên máy; đã chạy thử (mục 4.1) |
| D. Ảnh hưởng chéo | 2 | Mọi phiên sau chạy kịch bản và monkey với camera mở được; crash thật trong màn camera chặn phát staging; emulator có thể treo (mục 4.1) |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dùng camera giả của emulator trong dự án; đã chạy thử trên CI |
| **Tổng** | 5 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `.github/workflows/device-session.yml` (chạy cuối mọi phiên); `docs/ops/test-sessions.md`.
- Task khác trong sprint có thể xung đột: không có plan nào khác đang sửa các file này.
- Đề xuất thứ tự chạy: chạy được ở bất kỳ vị trí nào; tốt nhất trước các task camera, để `device-session` cuối phiên kiểm luôn kịch bản camera của chúng.

## 7. Điều kiện dừng riêng

- Cần đổi gì khác trong `.github/` (API của emulator, `target`, `timeout-minutes`, điều kiện `if`): dừng, viết plan phiên bản 2.

## 8. Câu hỏi cho người duyệt

1. `pr-check.yml` (job `ui-test`) cũng chạy `-camera-back none`. Có đổi luôn không? Đề xuất: không, giữ ngoài plan này. Test `app/src/androidTest` (ví dụ `AppLaunchTest`, `PhotoSaveDeviceTest`) viết theo emulator không có camera; đổi có thể làm chúng đổi kết quả, và `ui-test` chạy trên mọi PR. Nếu cần, làm ticket riêng.
2. Đề xuất về dự án mẫu: chỉ ghi vào mô tả PR, người mở issue ở `VGL-Studio/app-project-template` sau khi `device-session` cuối phiên xanh. Đề xuất: đồng ý.
3. Emulator chỉ có camera sau; monkey bấm đổi camera thì "Camera could not open" (10 lần ở lần chạy thử). Có thêm `-camera-front emulated` không? Đề xuất: chưa, giữ đúng phần đã chạy thử; ghi phát hiện nhỏ cho app: máy chỉ có một camera mà nút đổi camera vẫn bật, bấm vào thì báo lỗi camera. Nếu muốn, làm ticket camera riêng (ẩn hay tắt nút khi chỉ có một camera), hoặc plan phiên bản 2 thêm camera trước và chạy thử lại.
4. Ngoài phạm vi, sự cố hôm nay: `poses_catalog_path` của staging trỏ tới catalog v3 chưa có trên R2, app đứng ở màn loading, nhưng `preflight.yml` vẫn xanh vì chỉ kiểm đường dẫn ghi cứng trong `scripts/required-assets.txt` (v1). Đề xuất: một ticket riêng để `preflight.yml` đọc giá trị thật của `poses_catalog_path` trên Remote Config staging (và production khi có) rồi kiểm file đó trả `200`. Ticket đó cần cho `preflight.yml` quyền đọc Remote Config của staging (một bí mật mới, tài khoản dịch vụ chỉ đọc), nên cũng rủi ro cao; đề xuất cả về dự án mẫu. Không làm trong T-062.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-26, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: không đổi `pr-check.yml`. Câu 2: đề xuất về dự án mẫu sau khi merge. Câu 3: chưa thêm camera trước; nút đổi camera khi máy chỉ có một camera làm ở ticket riêng. Câu 4: `preflight.yml` kiểm catalog theo `poses_catalog_path` thật làm ở ticket riêng.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261027-01, phiên bản plan 1

- **Kết quả:** dừng ở bước 7 vì lệnh đẩy bị từ chối (`docs/auto-run.md`, mục "Khi không đẩy được"). Plan và ticket giữ `approved`: không phải lỗi của plan.
- **Nhánh, PR:** `claude/T-062-camera-gia`, commit `69db625`, chưa có trên GitHub (chỉ có phần trỏ vào nhánh chính `af437fb`, đẩy ở bước 1). Không có PR (GitHub trả `No commits between main and claude/T-062-camera-gia`). Bundle `/root/rescue/claude__T-062-camera-gia.bundle` trong máy cloud của phiên.
- **Đã làm:** đúng mục 3.1: `device-session.yml` (`-camera-back emulated`, chú thích mới, chú thích dòng 86), dòng `criterion:` và `need` thành `wait` ở `T-058-chup-tu-chi-tiet.txt`, `need` thành `wait` ở `T-020-camera-mo-gap.txt`, câu ở `docs/ops/test-sessions.md` mục 4 và dòng `need` ở bảng lệnh mục 5.
- **Test viết trước:** ngoại lệ đã duyệt (mục 4). `scripts/device-scenario.py check` cho hai tệp: mã 0. `scripts/check.sh`: QUA.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 3 ghi chú (câu mới ở `test-sessions.md` mục 4 nằm giữa đoạn đầu và hai gạch kết quả, đúng chỗ plan nêu; `pr-check.yml` giữ `-camera-back none` theo câu hỏi 1; ticket đi qua nhánh báo cáo). `git diff origin/main -- .github/` chỉ có ba dòng plan nêu.
- **Kết quả CI:** không chạy (chưa đẩy được).
- **Lệch so với plan:** Không có.
- **Lệnh bị từ chối:** `scripts/save-branch.sh claude/T-062-camera-gia` (05:27 UTC); GitHub trả `! [remote rejected] claude/T-062-camera-gia -> claude/T-062-camera-gia (refusing to allow an OAuth App to create or update workflow `.github/workflows/device-session.yml` without `workflow` scope)`. Token mà phiên `claude --cloud` dùng để đẩy không có quyền `workflow`, nên mọi commit sửa `.github/workflows/` bị từ chối. Phiên không đẩy lại dưới dạng khác (không qua API, không đổi tên nhánh).
- **Việc còn lại:** người lấy lại nhánh từ bundle và đẩy bằng tài khoản có quyền `workflow` trước khi máy cloud bị thu hồi (lệnh ở báo cáo phiên, mục Nhánh chưa đẩy), hoặc chạy lại task với cùng phiên bản plan trong một phiên đẩy được file workflow (routine, hay `claude --cloud` sau khi cấp quyền `workflow` cho GitHub App). Phần đổi chỉ 4 file, làm lại nhanh theo mục 3.1.
- **Cần người xem:** quyền `workflow` của token GitHub mà phiên `claude --cloud` dùng (bài học mới trong báo cáo phiên).
