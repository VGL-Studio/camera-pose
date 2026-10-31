---
ticket: T-081
title: Biểu tượng app
status: done
version: 1
risk_score: 2
risk_level: low
token_estimate: 7.1M
blocking_points: []
run_order:
reviewer: Nhật
cross_reviewer:
approved_at: 2026-10-31
lane: quick
---

# PLAN T-081: BIỂU TƯỢNG APP

## 1. Tóm tắt

Thêm biểu tượng riêng cho app dưới dạng adaptive icon ở `:app`: lớp hình là ảnh PNG đã thu nhỏ vào vùng an toàn, xuất theo năm mật độ; lớp nền là dải màu cam vẽ bằng XML. Manifest khai báo `android:icon` và `android:roundIcon`. Test Robolectric kiểm manifest, kiểu biểu tượng, màu nền, cỡ và vùng an toàn của lớp hình.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/loading/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Khi cài app thì biểu tượng là máy ảnh vàng cam trước một ảnh tư thế, trên nền cam chuyển màu, theo ảnh trong `docs/brand/app-icon/`; không còn biểu tượng mặc định | Manifest trỏ `android:icon` và `android:roundIcon` tới biểu tượng mới; lớp hình lấy từ `docs/brand/app-icon/foreground-1024.png`; nền là dải màu từ `#FDBE0C` (góc trên trái) tới `#FD5E02` (góc dưới phải), hai màu AI đo ở góc ảnh nền Nhật đưa |
| KB-6 | Khi máy cắt biểu tượng theo hình bất kỳ thì máy ảnh và ảnh tư thế hiện trọn: mọi phần của hình nằm trong vòng tròn 66 dp ở giữa khung 108 dp | Mọi điểm ảnh có độ đục lớn hơn 0 của lớp hình nằm trong bán kính 33 dp tính từ tâm, ở cả năm mật độ |
| KB-6 | Khi biểu tượng nhỏ thì vẫn nhận ra máy ảnh và gương mặt | Không đo tự động được; người xem trên máy (mục 4) |

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md). Ảnh gốc ở `docs/brand/app-icon/`; ảnh xem trước (hình tròn, vuông bo góc, cỡ thật) Nhật đã xem trong phiên.

ADR liên quan: ADR-0021 (icon không giống app mẫu), ADR-0034 (đường làm nhanh).

## 3. Giải pháp

### 3.1. Cách làm

Dự án chưa có biểu tượng nào; đây là mẫu chuẩn của Android Studio (Image Asset Studio) viết tay.

1. **Lớp hình:** ảnh 1024 x 1024 Nhật duyệt trong phiên, đã xử lý: xóa điểm ảnh có độ đục từ 10 trở xuống (chấm mờ của bước xóa nền), đặt độ đục từ 245 trở lên thành 255 (thân hình trước đây 253), cắt sát hình rồi thu nhỏ để điểm xa tâm nhất nằm ở bán kính 32 dp của khung 108 dp, đặt giữa khung. Từ ảnh đó xuất `ic_launcher_foreground.png` theo năm mật độ: 108, 162, 216, 324, 432 px (mdpi tới xxxhdpi). PNG, không đổi sang WebP để giữ đúng điểm ảnh test kiểm.
2. **Lớp nền:** `drawable/ic_launcher_background.xml`, `<shape>` hình chữ nhật với `<gradient android:angle="315">` (từ góc trên trái xuống góc dưới phải), `startColor="#FDBE0C"`, `endColor="#FD5E02"`. Màu viết trong tài nguyên của biểu tượng, không thuộc `AppTheme`: biểu tượng do launcher vẽ, không qua Compose.
3. **Biểu tượng:** `mipmap-anydpi/ic_launcher.xml` và `mipmap-anydpi/ic_launcher_round.xml`, cùng nội dung `<adaptive-icon>` với `<background android:drawable="@drawable/ic_launcher_background"/>` và `<foreground android:drawable="@mipmap/ic_launcher_foreground"/>`. `minSdk` là 26 nên không cần hậu tố `-v26` (lint báo `ObsoleteSdkInt`) và không cần biểu tượng PNG kiểu cũ.
4. **Manifest:** thêm `android:icon="@mipmap/ic_launcher"` và `android:roundIcon="@mipmap/ic_launcher_round"` vào `<application>`. Không đổi gì khác. Màn hình chào của hệ thống trên Android 12 trở lên tự lấy biểu tượng này, không cần sửa theme.
5. **Ảnh gốc:** `docs/brand/app-icon/play-store-512.png` (biểu tượng Google Play, chính là `app_icon.png` của Nhật) và `docs/brand/app-icon/foreground-1024.png` (lớp hình đã xử lý) để làm lại các mật độ hay tải lên Play sau này.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `docs/features/loading/requests/2026-10-31-bieu-tuong-app.md` | Lời yêu cầu của Nhật trong phiên |
| Sửa | `docs/features/loading/spec.md` | Phạm vi, KB-6, lịch sử |
| Tạo | `docs/features/loading/tickets/T-081-bieu-tuong-app.md` | Ticket |
| Tạo | `docs/features/loading/plans/T-081-bieu-tuong-app.md` | Plan này |
| Tạo | `docs/brand/app-icon/play-store-512.png` | Biểu tượng Google Play, 512 x 512 |
| Tạo | `docs/brand/app-icon/foreground-1024.png` | Lớp hình đã xử lý, 1024 x 1024 |
| Tạo | `app/src/main/res/mipmap-mdpi/ic_launcher_foreground.png` | Lớp hình 108 px |
| Tạo | `app/src/main/res/mipmap-hdpi/ic_launcher_foreground.png` | Lớp hình 162 px |
| Tạo | `app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.png` | Lớp hình 216 px |
| Tạo | `app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.png` | Lớp hình 324 px |
| Tạo | `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png` | Lớp hình 432 px |
| Tạo | `app/src/main/res/drawable/ic_launcher_background.xml` | Lớp nền dải màu |
| Tạo | `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | Adaptive icon |
| Tạo | `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` | Adaptive icon cho launcher dùng biểu tượng tròn |
| Sửa | `app/src/main/AndroidManifest.xml` | `android:icon`, `android:roundIcon` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/LauncherIconTest.kt` | Test của mục 4 |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. Test dùng `javax.imageio` của JDK, Robolectric và `androidx.test` đã có trong test của `:app`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** Robolectric có nạp được `<adaptive-icon>` trong `mipmap-anydpi` thành `AdaptiveIconDrawable` không.
- **Điều kiện chuyển:** test `launcherIcon_isAdaptiveIcon` thất bại vì Robolectric trả kiểu drawable khác (không phải vì thiếu tài nguyên) sau 2 lần sửa.
- **Cách làm thay:** đọc thẳng hai file XML trong `src/main/res/mipmap-anydpi/` bằng `DocumentBuilderFactory` như `MergedManifestTest`, kiểm thẻ gốc `adaptive-icon`, `background` trỏ `@drawable/ic_launcher_background`, `foreground` trỏ `@mipmap/ic_launcher_foreground`; màu nền đọc từ `ic_launcher_background.xml`. Tiêu chí và các test khác giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin ngoài thư mục test. Manifest chỉ thêm hai thuộc tính biểu tượng; `MergedManifestTest` kiểm quyền, activity, provider, receiver, queries, không kiểm thuộc tính `icon`.

## 4. Kiểm thử

Test trong `LauncherIconTest` (Robolectric, `@Config(application = Application::class)` như `ScreenOrientationTest`, không khởi động Hilt hay Firebase). Thư mục làm việc của unit test là thư mục module, nên đường dẫn tài nguyên là `src/main/res/...`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-6, tiêu chí 1: manifest dùng biểu tượng mới | Unit test | `LauncherIconTest.mergedManifest_usesLauncherIcons`: đọc manifest gộp của stagingDebug (cùng đường dẫn với `MergedManifestTest`), `<application>` có `android:icon` là `@mipmap/ic_launcher` và `android:roundIcon` là `@mipmap/ic_launcher_round` |
| KB-6, tiêu chí 1: biểu tượng có hai lớp | Robolectric | `LauncherIconTest.launcherIcon_isAdaptiveIcon`: `context.getDrawable(R.mipmap.ic_launcher)` và `R.mipmap.ic_launcher_round` đều là `AdaptiveIconDrawable`, `background` là `GradientDrawable`, `foreground` là `BitmapDrawable` |
| KB-6, tiêu chí 1: nền cam chuyển màu | Robolectric | `LauncherIconTest.background_isOrangeGradientTopLeftToBottomRight`: `GradientDrawable.colors` là `[0xFFFDBE0C, 0xFFFD5E02]`, `orientation` là `TL_BR` |
| KB-6, tiêu chí 2: đủ năm mật độ, đúng cỡ | Unit test | `LauncherIconTest.foreground_hasLauncherSizeAtEveryDensity`: năm file `src/main/res/mipmap-<mật độ>/ic_launcher_foreground.png` có, cỡ 108, 162, 216, 324, 432 px vuông |
| KB-6, tiêu chí 2: hình nằm trong vùng an toàn | Unit test | `LauncherIconTest.foreground_staysInsideSafeZone`: ở mỗi mật độ, mọi điểm ảnh có alpha lớn hơn 0 cách tâm khung không quá 33 dp đổi ra px, cộng 1 px cho sai số khi thu nhỏ. Đối chứng trong cùng test: số điểm ảnh có alpha bằng 255 lớn hơn 25% diện tích vòng tròn an toàn, để ảnh rỗng không qua. Trước khi viết code: chạy test với lớp hình chưa thu nhỏ (ảnh AI gốc xuất 432 px) và ghi kết quả thất bại vào nhật ký |
| KB-6, tiêu chí 2: thân hình đục hẳn | Unit test | `LauncherIconTest.foreground_artworkIsOpaque`: ở bản xxxhdpi, điểm ảnh ở tâm khung có alpha 255 |
| KB-6, tiêu chí 1 và 3: nhìn thấy biểu tượng trên máy, nhận ra máy ảnh và gương mặt ở cỡ thường | Kiểm tra thủ công (máy thật: khác, hình dạng biểu tượng do launcher của từng hãng quyết, emulator chỉ có một launcher) | Dev cài bản staging, xem biểu tượng trên màn hình chính, danh sách app và màn hình chào khi mở app (Android 12 trở lên) |

**Ngoại lệ viết test trước:** các file ảnh và XML là tài nguyên, không có logic; test viết trước thất bại vì chưa có tài nguyên (lỗi biên dịch `R.mipmap.ic_launcher`, thiếu file PNG, manifest thiếu thuộc tính).

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Manifest chỉ thêm thuộc tính biểu tượng, không thêm quyền; không chạm `:core:*`, cấu hình build, quảng cáo, dữ liệu.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Vài file tài nguyên và manifest trong `:app`, không đổi lớp nào |
| B. Độ rõ của yêu cầu | 0 | Nhật duyệt ảnh xem trước trong phiên, KB-6 nêu tiêu chí đo được |
| C. Khả năng kiểm chứng tự động | 1 | Khai báo, cỡ và vùng an toàn có test; nhận ra hình ở cỡ nhỏ do người xem |
| D. Ảnh hưởng chéo | 0 | Không feature nào đọc biểu tượng app |
| E. Độ mới của kỹ thuật | 0 | Adaptive icon là mẫu chuẩn của Android; test đọc manifest gộp lặp lại `MergedManifestTest` |
| **Tổng** | 2 | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `app/src/main/AndroidManifest.xml` (chỉ thẻ `<application>`).
- Nơi khác đọc file plan sửa (L-020): `scripts/status.py` đọc mọi `docs/features/*/spec.md` lấy frontmatter (`status`, `confirmed`) và kịch bản; spec giữ đúng khuôn, chỉ thêm một kịch bản, nên trang tiến độ không cần sửa. `scripts/tests/run.sh` chỉ tự tạo `app/src/main/AndroidManifest.xml` giả trong repo tạm của test, không đọc file thật của app. `MergedManifestTest` đọc manifest gộp nhưng không kiểm thuộc tính biểu tượng (mục 3.5). Các file tạo mới đã có trên máy vì dev xem và duyệt ảnh trong phiên trước khi có plan (đường làm nhanh); task dùng đúng các file đó.
- Task khác trong sprint có thể xung đột: T-079, T-080 (quảng cáo) có thể sửa manifest của `:app` hay `:core:ads`; khác dòng, không xung đột nội dung.
- Đề xuất thứ tự chạy: độc lập.

## 7. Điều kiện dừng riêng

Không có.

## 8. Câu hỏi cho người duyệt

1. **Biểu tượng một màu (Android 13 trở lên, chế độ biểu tượng theo chủ đề màu)**: chưa có ảnh, nên app giữ màu cam khi người dùng bật chế độ này. Đề xuất của AI: làm ticket sau khi có ảnh (prompt 3 trong phiên), không chặn ticket này.
2. **Logo tạm trên màn hình loading** (`BrandLogo` của `:core:designsystem`, spec mục 4 ghi "logo tạm cho tới khi có icon app"): đề xuất của AI: ticket riêng đi đường thường, vì chạm module core (điểm chặn 7).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt. Duyệt trong phiên làm nhanh (ADR-0034).
- **Người duyệt:** Nhật (chủ ticket)
- **Ghi chú:** Lời đồng ý của Nhật, 2026-10-31: chọn "Xác nhận và duyệt" cho câu hỏi "xác nhận phần sửa spec loading (thêm KB-6) và đồng ý plan T-081 (biểu tượng một màu và logo loading để ticket sau)". Câu 1 và 2 của mục 8 theo đề xuất của AI.

## 10. Nhật ký thực hiện

### Làm nhanh 2026-10-31, phiên bản plan 1

- **Kết quả:** merge bằng PR này
- **Nhánh, PR:** `claude/quick-T-081-bieu-tuong-app`, PR #263. Lúc bắt đầu, issue #242 (`env-broken`) đang mở và nhánh báo cáo của RUN-20261030-02 còn ghi `running` dù PR báo cáo #247 đã merge với `finished`; Nhật chọn "Chuẩn bị sẵn, merge sau". Theo yêu cầu của Nhật, nhánh báo cáo được chép lại bản `finished` của nhánh chính. Nhật quyết merge khi #242 còn mở (lời của Nhật: "merge", sau khi AI nêu #242 chỉ chặn phiên tự chạy trên cloud và CI của PR đã xanh).
- **Đã làm:** adaptive icon ở `:app` (lớp hình PNG năm mật độ, lớp nền dải màu `#FDBE0C` tới `#FD5E02`), `android:icon` và `android:roundIcon` trong manifest, ảnh gốc ở `docs/brand/app-icon/`; KB-6 trong spec loading, Nhật xác nhận trong phiên.
- **Test viết trước:** 6 test trong `LauncherIconTest`. Trước khi viết code, test không biên dịch vì chưa có `R.mipmap.ic_launcher` và `R.drawable.ic_launcher_background`. Đối chứng với lớp hình AI chưa xử lý (thu thẳng xuống từng mật độ): `foreground_staysInsideSafeZone` thất bại ("mdpi: pixel (65, 10) alpha 1 is 45.0 px from the center, outside the safe zone of 34.0 px"), `foreground_artworkIsOpaque` thất bại (253 thay vì 255); 4 test còn lại qua. Với lớp hình đã xử lý: 6 test qua. `scripts/check.sh`: 2424 test, 0 lỗi, cổng chất lượng QUA.
- **Rà soát độc lập:** 1 điểm phải sửa, đã sửa: khi thêm KB-6 vào spec đã làm mất tiêu đề `## 4. Giao diện`, trả lại và ghi dòng lịch sử (không đổi nội dung đã xác nhận). 2 điểm nên sửa: `launcherIcon_isAdaptiveIcon` nay kiểm cả màu và hướng của lớp nền mà biểu tượng vẽ, đã sửa; `foreground_artworkIsOpaque` chỉ kiểm điểm ảnh ở tâm, không sửa vì đối chứng 25% điểm đục của test vùng an toàn đã bắt phần lớn ý này. Ghi chú: bản mdpi có điểm mờ (alpha 1 đến 4) cách tâm 33,98 dp, sát giới hạn 34 dp của test; làm lại ảnh thì nên thu nhỏ thêm một chút.
- **Kết quả CI:** CI của PR này. Trước khi merge: `scripts/run-check.py` cho danh sách nháp RUN-20261021-04 cho kết quả giống hệt nhánh chính, không có lỗi mới.
- **Lệch so với plan:** mục 3.3 ghi test đọc ảnh bằng `javax.imageio`; classpath unit test Android không có gói này, nên test đọc bằng `BitmapFactory` của Robolectric ở chế độ `NATIVE` (`inScaled = false`, `inPremultiplied = false`). Không thêm thư viện, tiêu chí giữ nguyên.
- **Việc còn lại:** biểu tượng một màu (lint báo `MonochromeLauncherIcon`, mục 8 câu 1); logo tạm trên màn hình loading (mục 8 câu 2); tải `docs/brand/app-icon/play-store-512.png` lên Google Play khi phát hành.
- **Cần người kiểm:**
  - [ ] Nhật cài bản staging trên máy thật: biểu tượng trên màn hình chính, danh sách app, màn hình chào (Android 12 trở lên); nhận ra máy ảnh và gương mặt ở cỡ thường (KB-6, tiêu chí 1 và 3).
