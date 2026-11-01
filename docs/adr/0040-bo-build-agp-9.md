---
id: ADR-0040
title: Dự án mẫu build bằng Gradle 9.3.1 và AGP 9.1.0 với Kotlin dựng sẵn, compileSdk 37, targetSdk 36
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-30
deciders: [Nhật]
supersedes: ADR-0011         # chỉ thay phần "Phiên bản" của ADR-0011; các quyết định còn lại của ADR-0011 vẫn còn hiệu lực
superseded_by:
---

# ADR-0040: DỰ ÁN MẪU BUILD BẰNG GRADLE 9.3.1 VÀ AGP 9.1.0 VỚI KOTLIN DỰNG SẴN, COMPILESDK 37, TARGETSDK 36

Nguồn: bài học L-127 (Photune, issue #276), ticket T-080, plan `docs/features/quy-trinh/plans/T-080-bo-build-agp-9.md`.

## Bối cảnh

ADR-0011 chốt Gradle 8.14.3, AGP 8.13, Kotlin 2.2.10, JDK 17, compileSdk và targetSdk 36. ADR đó hoãn AGP 9 "vì thay đổi lớn về cấu hình Kotlin", và để việc nâng cấp cho một plan rủi ro cao riêng.

Photune đã build, test và phát staging bằng AGP 9.1.0 (Kotlin dựng sẵn), Gradle 9.3.1, compileSdk 37 qua 2 phiên tự chạy và khoảng 15 PR (L-127). Nếu mẫu ở lại AGP 8, app mới bắt đầu từ bộ công cụ cũ, và Photune không về được khung nhiều module của mẫu mà không hạ AGP.

Bộ mới khác bộ cũ ở bốn chỗ mà dự án chạm tới; ba chỗ đầu của AGP 9, chỗ thứ tư của Gradle 9:

- Kotlin được dựng sẵn trong AGP. Plugin `org.jetbrains.kotlin.android` không còn được áp dụng, và AGP tự đăng ký `KotlinAndroidProjectExtension`.
- `CommonExtension` không còn tham số kiểu, và không còn các hàm khối `defaultConfig { }`, `compileOptions { }`, `testOptions { }`, `packaging { }`. Chỉ còn thuộc tính.
- DSL mới là mặc định. Plugin còn tìm kiểu DSL cũ (`TestedExtension`) thì không áp dụng được, ví dụ Roborazzi 1.43. Cờ `android.newDsl=false` giữ DSL cũ, nhưng AGP báo cờ này sắp bỏ và sẽ gỡ nó ở AGP 10.
- Gradle 9 cho task test đỏ khi có nguồn test mà không tìm thấy test nào. `:core:data` không có `src/test`, vậy mà task test của nó đỏ với thông báo "There are test sources present ... did not discover any tests". Giả thuyết: lớp do Hilt sinh cho source set unit test được tính là nguồn test.

AGP 9.1.0 chỉ được Google kiểm tới compileSdk 36.1, và mỗi lần build với compileSdk 37 in một cảnh báo. Lúc viết ADR này đã có AGP 9.5.0, nhưng chưa app nào của nhóm chạy bản đó.

## Quyết định

**Phiên bản** (thay phần "Phiên bản" của ADR-0011):

- Gradle 9.3.1, AGP 9.1.0, Kotlin 2.2.10, KSP 2.3.12. Mã build biên dịch cho Java 17; Java chạy Gradle từ 17 tới 25 (bảng tương thích của Gradle 9.3.1).
- minSdk 26, compileSdk 37, targetSdk 36. Nâng targetSdk là một plan riêng.
- Giữ cảnh báo "tested up to compile SDK version 36.1" của AGP 9.1.0, không đặt `android.suppressUnsupportedCompileSdk`. Lên AGP 9.5 hay mới hơn là một ticket sau.

**Cấu hình Kotlin và Android trong `build-logic`:**

- Không áp dụng plugin `org.jetbrains.kotlin.android`. Tùy chọn của trình biên dịch Kotlin (`jvmTarget`) đặt qua `KotlinAndroidProjectExtension` mà AGP đăng ký.
- Cấu hình chung của module Android đặt qua thuộc tính của `CommonExtension` (`defaultConfig.apply { }`, `compileOptions.apply { }`, ...).
- Dùng DSL mới của AGP 9. Không đặt `android.newDsl=false`. Plugin Gradle nào cần DSL cũ thì nâng plugin đó. Ngoại lệ duy nhất: phương án dự phòng của plan T-080 (Roborazzi 1.43 cần DSL cũ) đặt cờ này cho tới khi T-081 nâng Roborazzi và gỡ nó.

**Module không có test:** module không có thư mục `src/test` được chạy task test mà không có test nào (`failOnNoDiscoveredTests = false`, chỉ cho module đó). Module có `src/test` mà không chạy được test nào thì vẫn đỏ.

**Giới hạn Java theo Gradle:** mỗi lần nâng Gradle, thêm giới hạn Java của bản đó vào bảng của `scripts/java-check.sh`, kèm test. Bản mới hơn bảng thì không có giới hạn trên.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| AGP 9.1.0, Gradle 9.3.1, DSL mới (đã chọn) | Đúng bộ đã chạy thật ở Photune; không dùng cờ sắp bỏ | AGP 9.1.0 chưa được kiểm với compileSdk 37 (có cảnh báo); phải nâng Roborazzi cùng lúc |
| Như trên, nhưng giữ DSL cũ (`android.newDsl=false`) | Giữ Roborazzi 1.43 tới T-081 | Cờ sắp bỏ, gỡ ở AGP 10; mỗi file build của module có một cảnh báo |
| AGP 9.5.0 | Mới hơn, có thể đã kiểm với compileSdk 37 | Chưa app nào của nhóm chạy; Photune còn ở 9.1.0 |
| Ở lại AGP 8.13, Gradle 8.14.3 | Không đổi gì | App mới bắt đầu từ bộ cũ; Photune không về được khung của mẫu |

## Hệ quả

- **Tích cực:**
  - App mới bắt đầu từ bộ công cụ hiện hành, cùng bộ với Photune.
  - `build-logic` không còn dùng API đã bỏ của AGP 9 hay cờ sắp bỏ.
- **Phải chấp nhận:**
  - Mỗi lần build in cảnh báo về compileSdk 37.
  - Bản production chưa được dựng trên bộ mới. Lần phát hành đầu tiên của một app trên bộ mới là lần kiểm đầu tiên.
  - Module mới chưa có `src/test` (feature hay core) sẽ chạy task test mà không báo gì. Mẫu feature (`:feature:sample`) có test, nên trường hợp này chỉ xảy ra khi feature bỏ qua bước 10 của `CLAUDE.md`.
  - App nhận qua `scripts/template-sync.sh` nhận cả bộ build mới. App có module hay thư viện riêng phải tự dựng lại.
- **Việc AI phải tuân thủ:** không áp dụng plugin `org.jetbrains.kotlin.android`; trong `build-logic`, cấu hình module Android qua thuộc tính của `CommonExtension` của AGP 9; không đặt `android.newDsl=false` ngoài ngoại lệ của plan T-080, plugin cần DSL cũ thì nâng plugin đó; giữ `compileSdk = "<số>"` trên một dòng của `gradle/libs.versions.toml`; nâng Gradle thì thêm giới hạn Java của bản đó vào `scripts/java-check.sh` kèm test
