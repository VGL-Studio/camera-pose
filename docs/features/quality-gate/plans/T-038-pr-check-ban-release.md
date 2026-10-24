---
ticket: T-038
title: pr-check build bản release khi đổi thư viện hay module
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-038: PR-CHECK BUILD BẢN RELEASE KHI ĐỔI THƯ VIỆN HAY MODULE

## 1. Tóm tắt

Job `changes` của `pr-check.yml` có thêm bộ lọc `release`. Bộ lọc khớp khi PR đổi thư viện (`gradle/libs.versions.toml`), một `build.gradle.kts`, `settings.gradle.kts`, `build-logic/`, `gradle.properties`, hay luật R8 (`proguard-rules.pro`, `consumer-rules.pro`). Khi khớp, job `build-and-test` chạy thêm `./gradlew :app:assembleStagingRelease` sau `scripts/check.sh`. Lỗi R8 kiểu `Missing class` của T-019 (L-053) vì thế đỏ ngay trên PR, không đợi tới `device-session` cuối phiên. Bước build không cần bí mật: dùng file Firebase giả `app/src/staging/google-services.json` và ra APK chưa ký, như trên máy dev. `docs/CLAUDE.md` ghi plan thêm thư viện chạy đúng lệnh này ở mục 4.1. `docs/auto-run.md` bước 8 ghi lỗi ở bước này là lỗi thật của task.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-053 (`docs/lessons.md`), từ RUN-20261023-04, T-019, PR #78.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-053, bắt lỗi trên PR | Khi PR đổi thư viện, `build.gradle.kts` hay luật R8 mà bản release hỏng ở R8 thì `pr-check` đỏ ở job `build-and-test`, và `merge-if-green.sh` từ chối | `merge-if-green.sh` đã từ chối mọi kiểm tra đỏ (dòng 131), nên chỉ cần bước nằm trong job `build-and-test`, không đổi script |
| L-053, không tốn CI vô ích | Khi PR không đổi các file trên thì không build bản release | Phần lớn PR chỉ đổi code Kotlin và tài liệu; lỗi R8 thiếu lớp đến từ thư viện hay luật mới |
| L-053, plan | Plan thêm thư viện ghi ở mục 4.1 kết quả của `./gradlew :app:assembleStagingRelease` | Mục 4.1 của plan T-019 ghi đã thử R8 qua nhưng không đúng lệnh; `docs/CLAUDE.md` mục Khi viết plan nói rõ lệnh |

Thiết kế tham chiếu: Không có thiết kế riêng (CI, docs/ops/design-workflow.md).

ADR liên quan: ADR-0002, ADR-0010 (cổng chất lượng), ADR-0012 (file Firebase giả cho bản staging), ADR-0016 (khóa ký staging, chỉ trong `staging.yml`).

## 3. Giải pháp

### 3.1. Cách làm

**`.github/workflows/pr-check.yml`:**

1. Job `changes`: thêm `release: ${{ steps.filter.outputs.release }}` vào `outputs`, và bộ lọc:

   ```yaml
   release:
     - 'gradle/libs.versions.toml'
     - 'settings.gradle.kts'
     - 'build.gradle.kts'
     - '**/build.gradle.kts'
     - 'build-logic/**'
     - 'gradle.properties'
     - '**/proguard-rules.pro'
     - '**/consumer-rules.pro'
     - '.github/workflows/pr-check.yml'
   ```

   Dòng `build.gradle.kts` viết riêng cho chắc ở thư mục gốc. Dòng cuối để PR sửa chính workflow này chạy bước mới một lần.
2. Job `build-and-test`: sau bước "Cổng chất lượng (scripts/check.sh)", thêm bước "Build bản release (R8)". Điều kiện: `needs.changes.outputs.code == 'true' && needs.changes.outputs.release == 'true'`. Lệnh: `./gradlew --console=plain :app:assembleStagingRelease 2>&1 | tee release.log`. Không truyền bí mật, không có `stagingKeystoreFile`, nên Gradle ra APK chưa ký (`app/build.gradle.kts` dòng 21 đến 24) với file Firebase giả (dòng 5, 6).
3. Bước "Bình luận lỗi vào PR": đọc thêm `release.log` khi có. Câu đầu của bình luận ghi "build bản release (R8) thất bại" nếu bước này đỏ, và trích các dòng `ERROR: R8:` và `Missing class`.
4. `timeout-minutes: 45` giữ nguyên. Job hiện mất khoảng 14 phút (PR #81: 02:45:07 đến 02:59:12), cộng khoảng 4 đến 8 phút (mục 4.1).

**`docs/auto-run.md`, bước 8:** một gạch đầu dòng: bước "Build bản release (R8)" đỏ là lỗi thật của task. Sửa trong luật R8 của module trong mục 3.2 (`consumer-rules.pro`, `proguard-rules.pro`). File đó không có trong mục 3.2 thì dừng task theo điều kiện 1. Phần của T-038 chỉ là gạch đầu dòng này. T-039 sửa đoạn đầu bước 8 (tra bảng lỗi); hai đoạn không chồng nhau (L-014).

**`docs/CLAUDE.md`, mục Khi viết plan, gạch đầu dòng "Quyết quyền và thư viện":** thêm một ý con. Thêm thư viện hay module mới thì chạy `./gradlew :app:assembleStagingRelease` trên nhánh có code liên quan, ghi lệnh và kết quả vào mục 4.1. `pr-check` cũng build bản release khi đổi các file đó (T-038, L-053).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `.github/workflows/pr-check.yml` | Bộ lọc `release`, bước build bản release, bình luận lỗi (mục 3.1) |
| Sửa | `docs/auto-run.md` | Bước 8: một gạch đầu dòng |
| Sửa | `docs/CLAUDE.md` | Mục Khi viết plan: một ý con về lệnh release ở mục 4.1 |
| Sửa | `docs/lessons.md` | L-053: dòng Áp dụng ghi T-038 khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-038-pr-check-ban-release.md` | Trạng thái theo phiên |

Nơi khác đọc `pr-check.yml` (`grep -rn "pr-check" scripts/ .github/ docs/CLAUDE.md CLAUDE.md`):

- `scripts/merge-if-green.sh` kiểm tên job `changes`, `build-and-test`, `ui-test` (dòng 20 đến 23). Plan không đổi tên job và không thêm job: bước mới nằm trong `build-and-test`.
- `scripts/wait-run.sh` chờ cả workflow `pr-check.yml` theo tên file, không đọc bước nào.
- `scripts/check.sh` (dòng 4), `.github/workflows/device-session.yml` (dòng 86) và `.github/workflows/status-page.yml` (dòng 4) chỉ nhắc `pr-check` trong chú thích.
- `scripts/tests/run.sh` không đọc nội dung `pr-check.yml`.

Nơi khác đọc `docs/auto-run.md` và `docs/CLAUDE.md` chỉ dẫn tới bước hay mục, không đọc nội dung:

- `.github/workflows/device-session.yml`, `.github/workflows/start-run.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/pr-check.yml`;
- `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/resume-check.py`, `scripts/plan-check.py`, `scripts/status.py`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh`, `scripts/device-session.sh`, `scripts/required-assets.txt` (chú thích dẫn tới `docs/CLAUDE.md`).

Plan không đổi tên hay số của bước, mục nào. `scripts/status.py` đọc `docs/lessons.md`; plan chỉ thêm chữ vào dòng Áp dụng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm. Bước mới không dùng bí mật nào, workflow giữ `permissions: contents: read, pull-requests: write`
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Bước mới làm `build-and-test` vượt 45 phút trên runner, khi đo ở PR của chính task này: đổi bước sang một job riêng `release-build` (`needs: changes`, chạy song song). Khi đó `scripts/merge-if-green.sh` phải thêm `release-build` vào `SKIPPABLE_CHECKS` để chờ job này. File đó chưa có trong mục 3.2, nên phương án này cần plan phiên bản 2, không làm trong phiên (mục 7).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Bắt lỗi R8 trên PR | Kiểm trên CI, trước khi merge | Một PR nháp tạm, nhánh `claude/T-038-probe` từ nhánh của task. PR bỏ dòng `-dontwarn` của `core/review/consumer-rules.pro` (bản sửa của PR #78), nên đổi một file khớp bộ lọc. `build-and-test` phải đỏ ở bước "Build bản release (R8)", bình luận trích `Missing class com.google.android.gms.common.annotation.NoNullnessRewrite`. Đóng PR và xóa nhánh sau khi xem. Đã chứng minh trước trên máy (mục 4.1): cùng lệnh đỏ trên `9b8cab5`, xanh trên nhánh chính |
| Bước chạy khi đổi file của bộ lọc | Kiểm trên CI | PR của task đổi `pr-check.yml`, nên bước chạy và xanh; ghi thời gian bước vào nhật ký |
| Không build khi không cần | Kiểm trên CI | PR kế tiếp chỉ đổi code Kotlin hay tài liệu: bước "Build bản release (R8)" hiện là bỏ qua (skipped); ghi số lần chạy vào nhật ký |
| `merge-if-green.sh` từ chối khi bước đỏ | Test script có sẵn | `merge-if-green` từ chối mọi check run `failure` (dòng 131); bước đỏ làm job `build-and-test` `failure`. Không cần test mới |
| Không dùng bí mật | Đọc lại | `grep -n "secrets\." .github/workflows/pr-check.yml` không có kết quả sau khi sửa |

**Ngoại lệ viết test trước:** toàn bộ phần đổi là cấu hình workflow GitHub Actions, không có test tự động trên máy. Kiểm bằng lần chạy thật trên PR như bảng trên. Lỗi được chứng minh trước bằng lệnh tương đương trên máy (mục 4.1).

### 4.1. Chạy thử

Chạy ngày 2026-10-24 trên máy dev, macOS, JDK 21, `ANDROID_HOME` của scratchpad, không có `stagingKeystoreFile`, không có file Firebase thật trong `app/src/staging/release/`. Mỗi lệnh chạy trong một worktree mới, nên thư mục `build/` trống, chỉ có cache Gradle dùng chung.

| Commit | Lệnh | Kết quả | Thời gian |
| --- | --- | --- | --- |
| `b3459bf` (nhánh chính, sau PR #78 và T-017) | `./gradlew :app:assembleStagingRelease --console=plain` | `BUILD SUCCESSFUL`, 683 task (445 chạy, 238 từ cache), ra `app-staging-release-unsigned.apk` | 6 phút 1 giây |
| `9b8cab5` (sau T-019, trước PR #78) | như trên | `BUILD FAILED`: `ERROR: R8: Missing class com.google.android.gms.common.annotation.NoNullnessRewrite (referenced from: ... ReviewManagerKtxKt ...)`, đúng lỗi của `device-session` 40031369521 | 1 phút 29 giây (dừng ở R8) |

Ước lượng thời gian thêm trên CI:

- `device-session` 40031369521 (runner GitHub) build từ đầu tới lúc R8 đỏ hết khoảng 3 phút 40 giây cho cả job.
- `staging.yml` 40057781151 build bản release đầy đủ, ký và phát trong 11 phút.
- Trong `build-and-test`, cache Gradle đã ấm sau `check.sh`, nhưng biến thể release biên dịch Kotlin và chạy R8 riêng.

Ước lượng thêm 4 đến 8 phút cho job, chỉ trên PR khớp bộ lọc. Nhật ký ghi số đo thật ở PR của task.

Bí mật: không cần. Bản giả `app/src/staging/google-services.json` đủ cho plugin Google Services (ADR-0012), như hai lần chạy trên.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, cổng chất lượng): `.github/workflows/pr-check.yml`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một workflow, hai đoạn tài liệu |
| B. Độ rõ của yêu cầu | 0 | Lỗi tái hiện được trên commit cũ, lệnh đã biết |
| C. Khả năng kiểm chứng tự động | 2 | Không có test trên máy cho workflow; kiểm trên PR thật và PR nháp tạm |
| D. Ảnh hưởng chéo | 2 | Mọi PR đổi file build: CI dài thêm vài phút |
| E. Độ mới của kỹ thuật | 0 | `paths-filter` và lệnh Gradle đã dùng |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `.github/workflows/pr-check.yml`, `docs/auto-run.md`, `docs/CLAUDE.md`.
- Task khác có thể xung đột:
  - T-036, T-037, T-039, T-040 cùng sửa `docs/auto-run.md` ở đoạn khác. T-039 sửa đầu bước 8, T-040 sửa bước 9.
  - T-038 không sửa `scripts/tests/run.sh`.
  - T-039 có thể đọc log của `build-and-test`: tên bước mới "Build bản release (R8)" và dòng `ERROR: R8:` nên có một dòng trong bảng tra lỗi (loại "lỗi thật"). Ghi điều này vào plan T-039.
- Đề xuất thứ tự chạy: độc lập, làm sớm. Mọi task thêm thư viện sau đó (ví dụ quảng cáo, đánh giá) được bảo vệ.

## 7. Điều kiện dừng riêng

- `build-and-test` trên PR của task vượt 40 phút: dừng, đề xuất phương án dự phòng ở mục 3.4 bằng plan phiên bản 2.
- Bước build release trên runner đỏ vì lý do khác lỗi R8 của code (thiếu bộ nhớ, thiếu SDK): dừng, ghi log vào nhật ký.

## 8. Câu hỏi cho người duyệt

1. **Bước trong `build-and-test` hay job riêng.** Có hai cách:
   - bước trong job (mục 3.1): không đổi `merge-if-green.sh`, nhưng job dài thêm 4 đến 8 phút trên PR khớp bộ lọc;
   - job riêng chạy song song: không thêm thời gian chờ, nhưng phải sửa `merge-if-green.sh` để chờ job đó, và T-040 cũng sửa script này.

   Đề xuất của AI: bước trong job. PR đổi file build không nhiều: 3 trên 8 PR task của RUN-20261023-04 (#72, #77, #81) khớp bộ lọc, và giữ một file của cổng chất lượng ngoài thay đổi.
2. **Bộ lọc có cần `AndroidManifest.xml` và `res/xml/` không.** R8 còn giữ lớp theo tên trong manifest. Đề xuất của AI: chưa thêm. Lỗi đã gặp đến từ thư viện; thêm manifest làm bộ lọc khớp nhiều PR giao diện. Nếu một lỗi release lọt qua đường đó thì thêm sau, kèm bài học.
3. **Có cần build cả trên `main-guard`.** Đề xuất của AI: không. `main-guard` chạy sau merge, khi lỗi đã vào nhánh chính. `device-session` và `staging.yml` cuối phiên vẫn build bản release đầy đủ.
4. **Đưa về dự án mẫu.** Đề xuất của AI: có, issue `from-app` sau khi merge. Mọi app tạo từ mẫu đều có thể thêm thư viện cần luật R8.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Duyệt có sửa / Yêu cầu làm lại / Hoãn
- **Người duyệt:** ..., **duyệt chéo:** ...
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
