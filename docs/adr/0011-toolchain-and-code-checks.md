---
id: ADR-0011
title: Bộ công cụ build và kiểm tra code của dự án mẫu
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0011: BỘ CÔNG CỤ BUILD VÀ KIỂM TRA CODE CỦA DỰ ÁN MẪU

## Bối cảnh

Khi dựng khung dự án mẫu, cần chốt phiên bản công cụ build, cách kiểm tra định dạng code và cách chạy screenshot test, sao cho AI và CI chạy giống hệt nhau. Trong lúc dựng cũng phát hiện một lỗi có thể làm cổng chất lượng mất tác dụng.

## Quyết định

**Phiên bản:**

- Gradle 8.14.3, Android Gradle Plugin 8.13, Kotlin 2.2.10, JDK 17.
- minSdk 26, compileSdk và targetSdk 36.
- Chưa dùng Android Gradle Plugin 9 vì thay đổi lớn về cấu hình Kotlin. Nâng cấp là một plan rủi ro cao riêng.

**Một lần gọi Gradle:** `scripts/check.sh` gọi Gradle đúng một lần cho toàn bộ kiểm tra, nên unit test chỉ chạy một lần (task Roborazzi dùng lại lần chạy đó).

**Một script cho mọi nơi:** `scripts/check.sh` là cổng chất lượng trên JVM, gồm quy tắc phụ thuộc module, ktlint, unit test, test giao diện Robolectric, screenshot test Roborazzi, Android Lint và build bản staging. AI chạy script này ở bước 5, CI chạy đúng script này trong `pr-check.yml` và `main-guard.yml`.

**Định dạng code:** ktlint theo kiểu Android Studio, cho phép dấu phẩy cuối danh sách, độ dài dòng tối đa 120 ký tự (xem `.editorconfig`). Chạy `./gradlew ktlintFormat` để tự sửa.

**Screenshot test:** ảnh chuẩn lưu ở `src/test/screenshots/` của từng module. Mỗi lệnh `captureRoboImage` phải ghi rõ đường dẫn ảnh; script báo lỗi nếu thiếu, vì đó là cách duy nhất để biết test cần những ảnh nào. Script xét riêng từng ảnh, không phải từng module: ảnh đã có thì so sánh; ảnh còn thiếu thì CI báo lỗi cho từng file, còn chạy cục bộ (người hoặc AI) thì chỉ ghi các ảnh thiếu, ảnh chuẩn cũ được giữ nguyên để vẫn so sánh được. CI không bao giờ tự ghi ảnh chuẩn, để không có trường hợp CI luôn xanh mà không so sánh gì. Ảnh chuẩn mới hoặc thay đổi phải được người xem trước khi merge.

**Kiểm thử script:** các script của cổng chất lượng (`find-breaking-commit.sh`, `merge-if-green.sh`) có test riêng trong `scripts/tests/`, chạy bằng một bản giả lập `gh api`. `scripts/check.sh` chạy các test này trước Gradle.

**Chú thích trong script và workflow:** viết bằng tiếng Anh như code. Thông báo in ra cho nhóm viết bằng tiếng Việt.

**Bắt buộc `pipefail` trong CI:** mọi workflow đặt `defaults.run.shell: bash`. Nếu không, GitHub Actions chạy bash không có `pipefail`, và lệnh `scripts/check.sh | tee build.log` luôn báo thành công kể cả khi build hỏng. Lỗi này đã xảy ra thật khi dựng khung và được phát hiện nhờ kiểm tra log.

**Phản hồi từ CI:** khi job đỏ, CI bình luận vào PR các dòng lỗi và đoạn cuối log. CI luôn ghi chú thích tóm tắt số test, do `scripts/test-summary.sh` tạo cho cả test JVM và test emulator (đếm cả lỗi `failures` và `errors`; test emulator mà không có test nào chạy thì tính là đỏ). Cả hai đọc được qua API GitHub, không cần tải log.

**Chưa dùng detekt:** detekt bản ổn định hiện tại được build với Kotlin cũ hơn, dễ xung đột. Xem xét lại khi có bản tương thích.

## Hệ quả

- **Tích cực:** AI và CI chạy cùng một kiểm tra; lỗi định dạng tự sửa được; cổng chất lượng không còn bị lỗi âm thầm.
- **Phải chấp nhận:** cổng chất lượng chưa có phân tích tĩnh sâu (detekt); việc nâng phiên bản công cụ phải làm chủ động.
- **Việc AI phải tuân thủ:** chạy `scripts/check.sh` trước khi đẩy code; chạy `./gradlew ktlintFormat` khi ktlint báo lỗi, không tắt quy tắc; không cập nhật ảnh chuẩn screenshot ngoài plan.
