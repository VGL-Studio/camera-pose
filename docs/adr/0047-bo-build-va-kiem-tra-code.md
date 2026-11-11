---
id: ADR-0047
title: Dự án mẫu build bằng Gradle 9.3.1 và AGP 9.1.0 với Kotlin dựng sẵn, và kiểm code bằng một cổng chất lượng `scripts/check.sh` mà AI và CI chạy như nhau
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-05
deciders: [Nhật]
supersedes: ADR-0011, ADR-0040  # ADR hợp nhất (ADR-0044, mục 4)
superseded_by:
---

# ADR-0047: DỰ ÁN MẪU BUILD BẰNG GRADLE 9.3.1 VÀ AGP 9.1.0 VỚI KOTLIN DỰNG SẴN, KIỂM CODE BẰNG MỘT CỔNG CHẤT LƯỢNG MÀ AI VÀ CI CHẠY NHƯ NHAU

## Bối cảnh

Quyết định về bộ công cụ build nằm ở hai ADR còn hiệu lực. ADR-0011 chốt bộ công cụ build, cổng chất lượng `scripts/check.sh`, định dạng code, screenshot test và phản hồi từ CI. ADR-0040 thay riêng phần phiên bản của ADR-0011, khi dự án mẫu lên AGP 9 cùng bộ đã chạy thật ở app Photune. ADR-0044, mục 4, yêu cầu hợp nhất: một ADR chép lại mọi quy tắc còn đúng, ADR cũ chuyển `superseded`.

ADR-0040 còn một ngoại lệ tạm: được đặt cờ `android.newDsl=false` trong lúc Roborazzi 1.43 cần DSL cũ, tới khi Roborazzi được nâng. Dự án mẫu đã nâng Roborazzi lên 1.77.0 cùng lúc lên AGP 9, nên cờ này chưa từng được đặt; ngoại lệ không còn chỗ dùng và không được chép lại. Ngoài ngoại lệ đó, ADR này không đổi quyết định nào; nó gộp hai ADR lại.

Những điều của AGP 9 và Gradle 9 mà dự án chạm tới, là lý do của mục 2 và 3:

- Kotlin được dựng sẵn trong AGP. Plugin `org.jetbrains.kotlin.android` không còn được áp dụng, và AGP tự đăng ký `KotlinAndroidProjectExtension`.
- `CommonExtension` không còn tham số kiểu, và không còn các hàm khối `defaultConfig { }`, `compileOptions { }`, `testOptions { }`, `packaging { }`. Chỉ còn thuộc tính.
- DSL mới là mặc định. Plugin còn tìm kiểu DSL cũ (`TestedExtension`) thì không áp dụng được. Cờ `android.newDsl=false` giữ DSL cũ, nhưng AGP báo cờ này sắp bỏ và sẽ gỡ nó ở AGP 10.
- Gradle 9 cho task test đỏ khi có nguồn test mà không tìm thấy test nào. Lớp do Hilt sinh cho source set unit test có thể được tính là nguồn test, nên module không có `src/test` cũng bị đỏ.
- AGP 9.1.0 chỉ được Google kiểm tới compileSdk 36.1, nên mỗi lần build với compileSdk 37 in một cảnh báo.

## Quyết định

### 1. Phiên bản

- **1.1** Gradle 9.3.1, AGP 9.1.0, Kotlin 2.2.10, KSP 2.3.12. Mã build biên dịch cho Java 17; Java chạy Gradle từ 17 tới 25, theo bảng tương thích của Gradle 9.3.1.
- **1.2** minSdk 26, compileSdk 37, targetSdk 36. Nâng targetSdk là một plan riêng.
- **1.3** Giữ cảnh báo "tested up to compile SDK version 36.1" của AGP 9.1.0, không đặt `android.suppressUnsupportedCompileSdk`. Lên AGP mới hơn là một ticket riêng.
- **1.4** `compileSdk = "<số>"` nằm trên một dòng của `gradle/libs.versions.toml`.

### 2. Cấu hình Kotlin và Android trong `build-logic`

- **2.1** Không áp dụng plugin `org.jetbrains.kotlin.android`. Tùy chọn của trình biên dịch Kotlin (`jvmTarget`) đặt qua `KotlinAndroidProjectExtension` mà AGP đăng ký.
- **2.2** Cấu hình chung của module Android đặt qua thuộc tính của `CommonExtension` (`defaultConfig.apply { }`, `compileOptions.apply { }`, ...).
- **2.3** Dùng DSL mới của AGP 9. Không đặt `android.newDsl=false`. Plugin Gradle nào cần DSL cũ thì nâng plugin đó.

### 3. Task test và giới hạn Java

- **3.1** Module không có thư mục `src/test` được chạy task test mà không có test nào (`failOnNoDiscoveredTests = false`, chỉ cho module đó). Module có `src/test` mà không chạy được test nào thì vẫn đỏ.
- **3.2** Mỗi lần nâng Gradle, thêm giới hạn Java của bản đó vào bảng của `scripts/java-check.sh`, kèm test. Bản mới hơn bảng thì không có giới hạn trên.

### 4. Cổng chất lượng

- **4.1** `scripts/check.sh` là cổng chất lượng trên JVM, gồm quy tắc phụ thuộc module, ktlint, unit test, test giao diện Robolectric, screenshot test Roborazzi, Android Lint và build bản staging. AI chạy script này trước khi đẩy code; CI chạy đúng script này trong `pr-check.yml` và `main-guard.yml`, để AI và CI kiểm như nhau.
- **4.2** `scripts/check.sh` gọi Gradle đúng một lần cho toàn bộ kiểm tra, nên unit test chỉ chạy một lần; task Roborazzi dùng lại lần chạy đó.
- **4.3** Các script của cổng chất lượng có test riêng trong `scripts/tests/`, chạy bằng bản giả lập `gh api`. `scripts/check.sh` chạy các test này trước Gradle.
- **4.4** Chưa dùng detekt: bản ổn định của nó được build với Kotlin cũ hơn, dễ xung đột. Xem xét lại khi có bản tương thích.

### 5. Định dạng code và ngôn ngữ

- **5.1** ktlint theo kiểu Android Studio, cho phép dấu phẩy cuối danh sách, độ dài dòng tối đa 120 ký tự (`.editorconfig`). Chạy `./gradlew ktlintFormat` để tự sửa; không tắt quy tắc.
- **5.2** Chú thích trong script và workflow viết bằng tiếng Anh, như code. Thông báo in ra cho nhóm viết bằng tiếng Việt.

### 6. Screenshot test

- **6.1** Ảnh chuẩn lưu ở `src/test/screenshots/` của từng module.
- **6.2** Mỗi lệnh `captureRoboImage` ghi rõ đường dẫn ảnh; `scripts/check.sh` báo lỗi nếu thiếu, vì đó là cách duy nhất để biết test cần những ảnh nào.
- **6.3** Script xét riêng từng ảnh: ảnh đã có thì so sánh; ảnh còn thiếu thì CI báo lỗi cho từng file, còn chạy cục bộ thì chỉ ghi các ảnh thiếu và giữ nguyên ảnh chuẩn cũ.
- **6.4** CI không bao giờ tự ghi ảnh chuẩn, để không có trường hợp CI luôn xanh mà không so sánh gì. Ảnh chuẩn mới hay thay đổi phải được người xem trước khi merge, và chỉ được cập nhật theo plan.

### 7. CI

- **7.1** Mọi workflow đặt `defaults.run.shell: bash`. Thiếu nó thì GitHub Actions chạy bash không có `pipefail`, và lệnh `scripts/check.sh | tee build.log` luôn báo thành công kể cả khi build hỏng.
- **7.2** Khi job đỏ, CI bình luận vào PR các dòng lỗi và đoạn cuối log.
- **7.3** CI luôn ghi chú thích tóm tắt số test, do `scripts/test-summary.sh` tạo cho cả test JVM và test emulator: đếm cả `failures` và `errors`; test emulator mà không có test nào chạy thì tính là đỏ. Cả hai đọc được qua API GitHub, không cần tải log.

### Mục mới ← ADR cũ

| Mục mới | Nguồn |
| --- | --- |
| 1.1 tới 1.3 | ADR-0040, Phiên bản (thay phần Phiên bản của ADR-0011) |
| 1.4 | ADR-0040, Việc AI phải tuân thủ |
| 2.1 tới 2.3 | ADR-0040, Cấu hình Kotlin và Android (bỏ ngoại lệ tạm về `android.newDsl=false`) |
| 3.1 | ADR-0040, Module không có test |
| 3.2 | ADR-0040, Giới hạn Java theo Gradle |
| 4.1 | ADR-0011, Một script cho mọi nơi |
| 4.2 | ADR-0011, Một lần gọi Gradle |
| 4.3 | ADR-0011, Kiểm thử script |
| 4.4 | ADR-0011, Chưa dùng detekt |
| 5.1 | ADR-0011, Định dạng code và Việc AI phải tuân thủ |
| 5.2 | ADR-0011, Chú thích trong script và workflow |
| 6.1 tới 6.4 | ADR-0011, Screenshot test và Việc AI phải tuân thủ |
| 7.1 | ADR-0011, Bắt buộc `pipefail` trong CI |
| 7.2, 7.3 | ADR-0011, Phản hồi từ CI |

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Hợp nhất ADR-0011 và ADR-0040, bỏ ngoại lệ đã hết (đã chọn, về cách hợp nhất) | Quy tắc bộ build đọc từ một ADR; `foundation.md` không còn vế nhắc ngoại lệ không còn | Thêm một ADR cần duyệt |
| Để ADR-0040 trong danh sách chưa dọn | Không tốn công lúc này | Vế quy tắc nhắc một ngoại lệ không còn; quy tắc vẫn nằm ở hai ADR |
| AGP 9.1.0, Gradle 9.3.1, DSL mới (đã chọn, về phiên bản) | Đúng bộ đã chạy thật ở Photune; không dùng cờ sắp bỏ | AGP 9.1.0 chưa được kiểm với compileSdk 37 (có cảnh báo) |
| AGP 9.5 hay mới hơn | Có thể đã kiểm với compileSdk 37 | Chưa app nào của nhóm chạy |
| Ở lại AGP 8.13, Gradle 8.14.3 | Không đổi gì | App mới bắt đầu từ bộ cũ; Photune không về được khung của mẫu |

## Hệ quả

- **Tích cực:**
  - AI và CI chạy cùng một kiểm tra; lỗi định dạng tự sửa được; cổng chất lượng không bị lỗi âm thầm.
  - App mới bắt đầu từ bộ công cụ hiện hành, cùng bộ với Photune.
  - Quy tắc bộ build đọc từ một ADR.
- **Phải chấp nhận:**
  - Mỗi lần build in cảnh báo về compileSdk 37.
  - Bản production chưa được dựng trên bộ này. Lần phát hành đầu tiên của một app trên bộ này là lần kiểm đầu tiên.
  - Cổng chất lượng chưa có phân tích tĩnh sâu (detekt).
  - Việc nâng phiên bản công cụ phải làm chủ động, mỗi lần là một plan riêng.
  - Module mới chưa có `src/test` sẽ chạy task test mà không báo gì. Mẫu feature (`:feature:sample`) có test, nên trường hợp này chỉ xảy ra khi feature bỏ qua bước 10 của `CLAUDE.md`.
  - App nhận qua `scripts/template-sync.sh` nhận cả bộ build. App có module hay thư viện riêng phải tự dựng lại.
- **Việc AI phải tuân thủ:** chạy `scripts/check.sh` trước khi đẩy code; chạy `./gradlew ktlintFormat` khi ktlint báo lỗi, không tắt quy tắc; không cập nhật ảnh chuẩn screenshot ngoài plan; không áp dụng plugin `org.jetbrains.kotlin.android`; trong `build-logic`, cấu hình module Android qua thuộc tính của `CommonExtension` của AGP 9; không đặt `android.newDsl=false`, plugin cần DSL cũ thì nâng plugin đó; giữ `compileSdk = "<số>"` trên một dòng của `gradle/libs.versions.toml`; nâng Gradle thì thêm giới hạn Java của bản đó vào `scripts/java-check.sh` kèm test.
