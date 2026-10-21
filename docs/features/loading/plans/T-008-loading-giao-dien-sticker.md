---
ticket: T-008
title: Màn loading theo thiết kế Sticker và chữ của app
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 7
risk_level: medium
token_estimate: 20.0M
blocking_points: []
run_order:
reviewer:
cross_reviewer:
approved_at:
---

# PLAN T-008: MÀN LOADING THEO THIẾT KẾ STICKER VÀ CHỮ CỦA APP

## 1. Tóm tắt

Đổi bố cục `LoadingScreen` theo thiết kế Sticker đã duyệt (logo bắt đầu ở 200 trên 891, vùng nội dung rộng 318, nền `background`, khoảng cách và cỡ biểu tượng của `context/`), thay chữ bằng chữ của Camera Pose (khẩu hiệu, dòng hướng dẫn, chân trang) và đưa toàn bộ chữ của màn hình sang 7 ngôn ngữ phát hành, tiếng Anh là ngôn ngữ gốc trong `values/`. Ba biểu tượng đổi sang Material Symbols Rounded như `sticker.md`. Kiểm chứng bằng test đo vị trí theo số liệu thiết kế, test đọc chữ ở từng ngôn ngữ, test màu nền, test cuộn tới nút "Thử lại" ở màn hình thấp cho cả 7 ngôn ngữ, 6 ảnh chuẩn screenshot và ảnh so sánh của `scripts/design-compare.sh loading`. Plan dựa trên `:core:designsystem` mà T-007 giao; T-007 chưa merge hay giao khác plan của nó thì dừng (mục 7).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/loading/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi mở app thì màn hình loading hiện đầu tiên, có logo, tên app, khẩu hiệu, nhãn "Đang tải dữ liệu", thanh tiến trình ngang, phần trăm, dòng chữ cho biết app đang chuẩn bị, và dòng chân trang, đúng thiết kế | Phần giao diện của tiêu chí: đủ các phần, đúng vị trí (lệch không quá 4 dp, `design/README.md` mục 7), đúng vai trò màu, đúng bảng chữ. Tên app là `app_name` của `:app` ("Camera Pose: Lines and Guides", spec câu hỏi 10), xuống hai dòng, không dịch. Khẩu hiệu "Tạo dáng theo nét viền, chụp đẹp ngay.", dòng hướng dẫn "Đang chuẩn bị bộ tư thế…", chân trang "Ảnh chỉ lưu trên máy của bạn" (spec mục 4, Chữ của Camera Pose). Việc "hiện đầu tiên" đã có từ T-001, không đổi |
| KB-1 | Các tiêu chí còn lại (thanh tăng theo phần việc, tối thiểu 2 giây, tự chuyển, `loading_completed`, quay lại, xoay máy) | Không đổi trong ticket này. Phần việc tải đổi ở T-010. Test hiện có (`LoadingViewModelTest`, `DefaultPreparationRepositoryTest`, test trên emulator của `:app`) phải qua mà không sửa |
| KB-2 | Khi một phần việc không xong được thì màn hình báo lỗi gồm tiêu đề "Không thể tải dữ liệu" và lời hướng dẫn; nhãn đổi thành "Tiến độ đã dừng", thanh tiến trình và phần trăm chuyển màu báo lỗi, giữ ở mức đã đạt; có nút "Thử lại" | Giao diện lỗi theo `LoadingScreen_failed_light` và `_dark`: khối lỗi cách thanh 24, nút cách lời hướng dẫn 24, biểu tượng cảnh báo 20. Logo không đổi chỗ khi khối lỗi hiện. Logic thử lại không đổi |
| Phạm vi ticket | Chữ mới đủ 7 ngôn ngữ phát hành (spec `onboarding`, câu hỏi 1) | Mọi chữ của màn hình (không chỉ chữ mới) có bản Anh, Hindi, Bồ Đào Nha Brazil, Tây Ban Nha Mỹ Latinh, Indonesia, Thái, Việt. Tiếng Anh nằm trong `values/` (ngôn ngữ gốc, máy đặt ngôn ngữ khác thì hiện tiếng Anh); tiếng Việt chuyển sang `values-vi/`. AI dịch, người biết tiếng duyệt trước khi phát hành (mục 8, câu 1) |
| Phạm vi ticket | Screenshot test 4 ảnh thiết kế và ảnh màn hình thấp | Bốn test cùng tên bốn ảnh trong `design/screens/`, chạy bằng tiếng Việt (chữ của thiết kế) và tên app thật; hai ảnh `_small` ở khung `w360dp-h640dp-xxhdpi` (L-028) |

Thiết kế tham chiếu: `features/loading/design/README.md` (bảng chữ mục 2, ánh xạ mục 3, màn hình thấp mục 6, tiêu chí duyệt mục 7); ảnh `design/screens/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`; số liệu `design/context/` cùng tên; `docs/design-system/sticker.md` (icon Material Symbols Rounded, mục 2).

ADR liên quan: ADR-0008 (chỉ dùng màu, chữ, khoảng cách, thành phần của `:core:designsystem`), ADR-0020 (tên screenshot test trùng tên ảnh thiết kế, `scripts/design-compare.sh`, `design/review/` vào PR). ADR-0018 và ADR-1001 không bị chạm (không đổi việc chờ).

## 3. Giải pháp

### 3.1. Cách làm

**Điều kiện đầu vào: T-007.** Plan dựa trên những gì plan T-007 (đã duyệt, đang chạy trong RUN-20261021-01) nói sẽ giao. Trước khi viết test, phiên tự chạy kiểm tra trên nhánh chính: có commit merge của T-007; `BrandLogo` là ô 112 dp và nhận `modifier`; `PrimaryButton` cao 56, biểu tượng 20, chữ ký `text`, `onClick`, `modifier`, `enabled`, `icon` (T-011 có thể thêm `loading` ở cuối, không ảnh hưởng); `ProgressBar(progress, modifier, color, trackColor, borderColor)` với mặc định `secondaryContainer`, `surfaceVariant`, `outline`; `colorScheme.background` là #FFFDF8 (sáng) và #121212 (tối); `typography.headlineLarge` là 32/40, đậm 800, Bricolage Grotesque. Một điều không đúng thì dừng (mục 7). Plan này không dùng `MaterialTheme.appColors`, `Radius`, `Stroke` hay `stickerShadow` trực tiếp: bóng, viền, bo góc đi kèm `BrandLogo`, `PrimaryButton`, `ProgressBar`.

Làm theo mẫu của plan T-003 (cùng màn hình): `LoadingScreen` không trạng thái, test giao diện và screenshot trên JVM, đối chiếu bằng `scripts/design-compare.sh`. `LoadingRoute`, `LoadingViewModel`, `LoadingUiState`, dữ liệu và điều hướng không đổi.

**Bố cục (`LoadingScreen.kt`).** Số liệu lấy từ `design/context/` (khung 411 x 891):

- `Surface(color = MaterialTheme.colorScheme.background)` thay màu mặc định `surface`: thiết kế có nền #FFFDF8 và #121212, tức `background`. Màu chữ mặc định thành `onBackground`.
- Khoảng trên giữ cách tính theo chiều cao màn hình, `CONTENT_TOP_FRACTION = 200f / 891f` (thay `0.25f`; thiết kế: logo ở 200 trên 891). Màn hình thấp hơn `TALL_SCREEN_MIN_HEIGHT` (800 dp) giữ `SHORT_SCREEN_TOP_FRACTION = 0.08f` như T-005.
- Vùng nội dung: `Modifier.padding(top = topSpace).padding(horizontal = Spacing.md).widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth()`, với `private val CONTENT_MAX_WIDTH = 318.dp` (thiết kế: rộng 318, căn giữa). Ở khung 411 nội dung rộng 318, lề 46,5 như thiết kế (code cũ rộng 315 với lề cố định 48). Ở khung 360 nội dung vẫn rộng 318 (lề 21), nên tên app vẫn xuống hai dòng như thiết kế thay vì có nguy cơ xuống ba dòng ở chiều rộng 264 (mục 8, câu 5).
- Trong cột nội dung, theo thứ tự và khoảng cách của thiết kế:
  1. `BrandLogo(modifier = Modifier.testTag(LoadingTestTags.LOGO))`;
  2. `Spacing.lg`, tên app (`headlineLarge`, `onBackground`, căn giữa, tag `APP_NAME`);
  3. `Spacing.sm`, khẩu hiệu `stringResource(R.string.loading_tagline)` (`bodyMedium`, `onSurfaceVariant`, căn giữa). Không dùng `brand_tagline` của `:core:designsystem` nữa (mục 8, câu 3);
  4. `Spacing.xl + Spacing.sm`, hàng nhãn và phần trăm (`Arrangement.SpaceBetween`). Đang tải: nhãn `onSurfaceVariant`, phần trăm `onSurface` (thay `primary`; README mục 3). Lỗi: cả hai `error`;
  5. `Spacing.md`, `ProgressBar` (tag `PROGRESS`). Đang tải: màu mặc định của T-007. Lỗi: `color = error`, `trackColor = errorContainer`, viền mặc định `outline`;
  6. Đang tải: `Spacing.lg`, dòng hướng dẫn `loading_guidance` (`bodySmall`, `onSurfaceVariant`, căn giữa). Lỗi: `Spacing.lg` (thay `Spacing.lg + Spacing.xs`), rồi khối lỗi.
- Khối lỗi (`Column`, `fillMaxWidth`, căn giữa ngang, tag `ERROR`), khoảng cách viết bằng `Spacer` thay `spacedBy`: hàng tiêu đề (`Icon(ic_loading_alert)` 20 dp màu `error`, `Spacing.sm`, `loading_error_title` `titleMedium` `onSurface`); `Spacing.sm`; `loading_error` (`bodyMedium`, `onSurfaceVariant`, căn giữa); `Spacing.lg` (thiết kế: 8 + khung trống 8 + 8 = 24); `PrimaryButton(text = loading_retry, icon = painterResource(ic_loading_retry), modifier = fillMaxWidth().testTag(RETRY))`.
- Chân trang giữ cách đặt của T-005 (cùng cột cuộn, `padding(top = Spacing.lg, bottom = Spacing.xl)`); biểu tượng khóa 14 dp (thay 12), cách chữ `Spacing.sm`, chữ `labelSmall`, cả hai `onSurfaceVariant`.
- Hằng số: `ALERT_ICON_SIZE = 20.dp`, `LOCK_ICON_SIZE = 14.dp`, `CONTENT_MAX_WIDTH = 318.dp`, chú thích ghi nguồn là `design/context/`. Không viết cứng mã màu hay cỡ chữ.
- `LoadingTestTags` thêm `LOGO = "loading_logo"`.
- Chú thích KDoc của `LoadingScreen` sửa theo bố cục mới (200 trên 891, rộng 318). Hai preview dùng `PREVIEW_APP_NAME = "Camera Pose: Lines and Guides"`.
- Bỏ `import ... core.designsystem.R as DesignSystemR` vì không còn dùng.

**Số đo dự kiến sau khi làm** (khung 411 x 891, tiếng Việt, tên app thật; dp từ đỉnh màn hình): logo 200 tới 312; tên app 336 tới 416; nhãn 484; thanh 520 tới 528; dòng hướng dẫn 552; khối lỗi 552; nút "Thử lại" 648 tới 704 (thiết kế 708 vì nút cao 60; plan T-007 chốt 56); chân trang 843 tới 859. Khung 360 x 640, tiếng Anh, lỗi: nút "Thử lại" kết thúc khoảng 555, chân trang bắt đầu ở 592.

**Biểu tượng.** Ba file drawable giữ tên (code gọi không đổi), thay nội dung bằng Material Symbols Rounded (`sticker.md`, mục 2; tên icon theo README mục 3: `warning`, `refresh`, `lock`), lấy từ `google/material-design-icons`, commit `49d4db35df873165d6bd6ba09b063c7dafbac2f4`, file `symbols/web/<tên>/materialsymbolsrounded/<tên>_24px.svg` (AI đã tải ngày 2026-10-21; SHA-256 `warning` b6907a9a2d0f1bd6b57191c958c221a2c35cf105e880d04f2a7455791b7a9ea8, `refresh` 9287665cb15b61441487cb2d579ba5630f9b249dbcaa5991e7028aa6f925edf8, `lock` 148a3da500ee74920d4f80984e8f73830b1f4e613bac095502d854b9c700315c). Đường vẽ đã chép dưới đây, phiên tự chạy không cần tải SVG. Mỗi file có dạng:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Material Symbols Rounded "<name>" (Apache 2.0, google/material-design-icons@49d4db3, see third_party/material-symbols/LICENSE.txt). Color comes from the tint. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960">
    <group android:translateY="960">
        <path
            android:fillColor="#FFFFFFFF"
            android:pathData="<path>" />
    </group>
</vector>
```

`translateY="960"` vì SVG gốc có `viewBox="0 -960 960 960"`. Kích thước thật đặt bằng `Modifier.size` (20, 14) và bởi `PrimaryButton` (20).

- `ic_loading_alert.xml`, `<name>` là `warning`, `<path>`: `M109-120q-11 0-20-5.5T75-140q-5-9-5.5-19.5T75-180l370-640q6-10 15.5-15t19.5-5q10 0 19.5 5t15.5 15l370 640q6 10 5.5 20.5T885-140q-5 9-14 14.5t-20 5.5H109Zm69-80h604L480-720 178-200Zm302-40q17 0 28.5-11.5T520-280q0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5 28.5T480-240Zm0-120q17 0 28.5-11.5T520-400v-120q0-17-11.5-28.5T480-560q-17 0-28.5 11.5T440-520v120q0 17 11.5 28.5T480-360Zm0-100Z`
- `ic_loading_retry.xml`, `<name>` là `refresh`, `<path>`: `M480-160q-134 0-227-93t-93-227q0-134 93-227t227-93q69 0 132 28.5T720-690v-70q0-17 11.5-28.5T760-800q17 0 28.5 11.5T800-760v200q0 17-11.5 28.5T760-520H560q-17 0-28.5-11.5T520-560q0-17 11.5-28.5T560-600h128q-32-56-87.5-88T480-720q-100 0-170 70t-70 170q0 100 70 170t170 70q68 0 124.5-34.5T692-367q8-14 22.5-19.5t29.5-.5q16 5 23 21t-1 30q-41 80-117 128t-169 48Z`
- `ic_loading_lock.xml`, `<name>` là `lock`, `<path>`: `M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240Zm0-80h480v-400H240v400Zm240-120q33 0 56.5-23.5T560-360q0-33-23.5-56.5T480-440q-33 0-56.5 23.5T400-360q0 33 23.5 56.5T480-280ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85v80ZM240-160v-400 400Z`

Giấy phép Apache 2.0 yêu cầu kèm bản giấy phép. Tải và kiểm tra:

```sh
C=49d4db35df873165d6bd6ba09b063c7dafbac2f4
mkdir -p feature/loading/third_party/material-symbols
curl -fL -o feature/loading/third_party/material-symbols/LICENSE.txt \
  "https://raw.githubusercontent.com/google/material-design-icons/$C/LICENSE"
echo "58d1e17ffe5109a7ae296caafcadfdbe6a7d176f0bc4ab01e12a689b0499d8bd  feature/loading/third_party/material-symbols/LICENSE.txt" | shasum -a 256 -c
```

Tải không được (429, 5xx, hết thời gian, mất kết nối) là lỗi hạ tầng (auto-run.md, bước 5). Mã băm sai thì dừng (mục 7). T-011 có thể đã thêm cùng giấy phép vào `core/designsystem/third_party/material-symbols/`; file trong `:feature:loading` vẫn tạo, vì biểu tượng nằm ở module này.

**Chữ và ngôn ngữ.** Đây là ticket đầu tiên của app có nhiều ngôn ngữ. Khóa trong `feature/loading/src/main/res/values*/strings.xml`:

- `loading_tagline` (mới), `loading_status`, `loading_guidance` (đổi chữ), `loading_status_failed`, `loading_error_title`, `loading_error`, `loading_retry`, `loading_footer` (đổi chữ): dịch đủ 7 ngôn ngữ theo bảng dưới.
- `loading_percent` (`%1$d%%`) đánh dấu `translatable="false"`, chỉ có trong `values/`: cả 7 ngôn ngữ viết "68%" giống nhau (mục 8, câu 6).
- `app_name` của `:app` giữ nguyên, không dịch (tên thương hiệu; spec câu hỏi 10). `:app` không có thư mục ngôn ngữ nào nên Android Lint không đòi bản dịch.
- Thư mục: `values/` (Anh, gốc), `values-vi/`, `values-hi/`, `values-pt-rBR/`, `values-b+es+419/`, `values-in/`, `values-th/`. Indonesia dùng `values-in`, không dùng `values-id`: Android Lint báo `LocaleFolder` ("The locale folder "id" should be called "in" instead") với `values-id`, và máy đặt `id` hay `id-ID` vẫn đọc được `values-in` (đã đo, mục 4).
- Chữ chép đúng từng ký tự từ bảng; dòng hướng dẫn dùng `…` (U+2026). Không chữ nào có dấu nháy đơn, `&`, `<`, `>`, nên không cần thoát ký tự.

| Khóa | `values/` (Anh) | `values-vi/` | `values-hi/` | `values-pt-rBR/` | `values-b+es+419/` | `values-in/` | `values-th/` |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `loading_tagline` | Follow the outline, get a great shot. | Tạo dáng theo nét viền, chụp đẹp ngay. | आउटलाइन से पोज़ करें, तुरंत बढ़िया फ़ोटो लें। | Siga o contorno e faça a foto perfeita. | Sigue el contorno y logra la foto perfecta. | Ikuti garis pose, langsung dapat foto keren. | โพสตามเส้นโครง ถ่ายสวยได้ทันที |
| `loading_status` | Loading data | Đang tải dữ liệu | डेटा लोड हो रहा है | Carregando dados | Cargando datos | Memuat data | กำลังโหลดข้อมูล |
| `loading_guidance` | Preparing your poses… | Đang chuẩn bị bộ tư thế… | आपके पोज़ तैयार हो रहे हैं… | Preparando suas poses… | Preparando tus poses… | Menyiapkan pose Anda… | กำลังเตรียมท่าโพสของคุณ… |
| `loading_status_failed` | Progress stopped | Tiến độ đã dừng | प्रगति रुक गई | Progresso interrompido | Progreso detenido | Proses terhenti | ความคืบหน้าหยุดลง |
| `loading_error_title` | Unable to load data | Không thể tải dữ liệu | डेटा लोड नहीं हो सका | Não foi possível carregar os dados | No se pudieron cargar los datos | Tidak dapat memuat data | โหลดข้อมูลไม่ได้ |
| `loading_error` | The connection was interrupted. Check your network and try again to continue. | Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục. | कनेक्शन टूट गया है। कृपया अपना नेटवर्क जाँचें और जारी रखने के लिए फिर से कोशिश करें। | A conexão foi interrompida. Verifique sua rede e tente novamente para continuar. | Se interrumpió la conexión. Revisa tu red y vuelve a intentarlo para continuar. | Koneksi terputus. Periksa jaringan Anda, lalu coba lagi untuk melanjutkan. | การเชื่อมต่อขัดข้อง โปรดตรวจสอบเครือข่ายแล้วลองอีกครั้งเพื่อดำเนินการต่อ |
| `loading_retry` | Try again | Thử lại | फिर से कोशिश करें | Tentar novamente | Reintentar | Coba lagi | ลองอีกครั้ง |
| `loading_footer` | Your photos stay on your device | Ảnh chỉ lưu trên máy của bạn | आपकी फ़ोटो सिर्फ़ आपके डिवाइस पर रहती हैं | Suas fotos ficam só no seu aparelho | Tus fotos solo se guardan en tu dispositivo | Foto Anda hanya tersimpan di perangkat ini | รูปภาพของคุณจะอยู่ในเครื่องนี้เท่านั้น |

Bản dịch thiếu một khóa ở một ngôn ngữ thì Android Lint báo lỗi `MissingTranslation` và `scripts/check.sh` (`:app:lintStagingDebug`) đỏ: AI đã thử trên bản sao của repo (mục 4). Lint chỉ đòi đủ bản dịch trong từng module, nên chuỗi của `:feature:sample`, `:core:ads`, `:core:designsystem`, `:app` không cần dịch trong ticket này (đã thử, không có lỗi).

Phông Bricolage Grotesque (T-007) không có chữ Devanagari và chữ Thái; Android tự dùng phông hệ thống cho hai ngôn ngữ này. Thiết kế chỉ có tiếng Việt nên đây không phải lệch thiết kế (mục 8, câu 1).

**Đối chiếu.** Sau khi `scripts/check.sh` qua: chạy `scripts/design-compare.sh loading`. Script xóa và sinh lại `docs/features/loading/design/review/` (4 ảnh `*_compare.png` và `summary.md`, cùng tên với kết quả cũ của thiết kế dự án mẫu). Commit `review/`, dán `review/summary.md` vào mô tả PR và vào `design/README.md` mục 8 (thay đoạn "Chưa có cho thiết kế này"). Rà soát độc lập xem bốn ảnh ghép theo README mục 7 và ghi nhận xét vào mục 8. Script chỉ cần `java` (JDK mà Gradle dùng; đặt `JAVA_HOME` như khi chạy Gradle). Không chạy được vì thiếu công cụ hay mạng là lỗi hạ tầng; script chạy được mà báo lỗi khác thì dừng (mục 7).

**Thứ tự commit (L-025).**

- Commit test (bước 3): `LoadingScreenTest.kt`, `LoadingStringsTest.kt`, `LoadingScreenScreenshotTest.kt` theo mục 4; trong `LoadingScreen.kt` chỉ thêm hằng `LoadingTestTags.LOGO` và gắn `Modifier.testTag(LoadingTestTags.LOGO)` vào `BrandLogo()` (móc cho test, không đổi giao diện); trong `values/strings.xml` chỉ thêm khóa `loading_tagline` với chữ tiếng Anh ở bảng trên, để test biên dịch được. Chạy và ghi lý do thất bại thật vào nhật ký.
- Commit code (bước 4): phần còn lại của `LoadingScreen.kt`, `values/strings.xml` sang tiếng Anh, 6 thư mục ngôn ngữ, ba drawable, giấy phép.
- Commit ảnh: ghi lại 6 ảnh chuẩn bằng `./gradlew :feature:loading:recordRoborazziDebug`, xem từng ảnh; rồi `scripts/check.sh`; rồi `scripts/design-compare.sh loading`, `review/` và README mục 8.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreen.kt` | Bố cục theo mục 3.1, nền `background`, khẩu hiệu `loading_tagline`, tag `LOGO`, cỡ biểu tượng, preview, bỏ `DesignSystemR` |
| Sửa | `feature/loading/src/main/res/values/strings.xml` | Chữ tiếng Anh, khóa `loading_tagline`, `loading_percent` không dịch |
| Tạo | `feature/loading/src/main/res/values-vi/strings.xml` | Tiếng Việt (chữ của thiết kế) |
| Tạo | `feature/loading/src/main/res/values-hi/strings.xml` | Tiếng Hindi |
| Tạo | `feature/loading/src/main/res/values-pt-rBR/strings.xml` | Tiếng Bồ Đào Nha Brazil |
| Tạo | `feature/loading/src/main/res/values-b+es+419/strings.xml` | Tiếng Tây Ban Nha Mỹ Latinh |
| Tạo | `feature/loading/src/main/res/values-in/strings.xml` | Tiếng Indonesia |
| Tạo | `feature/loading/src/main/res/values-th/strings.xml` | Tiếng Thái |
| Sửa | `feature/loading/src/main/res/drawable/ic_loading_alert.xml` | Material Symbols Rounded `warning` |
| Sửa | `feature/loading/src/main/res/drawable/ic_loading_retry.xml` | Material Symbols Rounded `refresh` |
| Sửa | `feature/loading/src/main/res/drawable/ic_loading_lock.xml` | Material Symbols Rounded `lock` |
| Tạo | `feature/loading/third_party/material-symbols/LICENSE.txt` | Giấy phép Apache 2.0 của Material Symbols |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` | Tên app thật, khẩu hiệu `loading_tagline`, test bố cục, màu nền, cuộn ở 7 ngôn ngữ (mục 4) |
| Tạo | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingStringsTest.kt` | Test chữ theo từng ngôn ngữ (mục 4) |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenScreenshotTest.kt` | Khung có ngôn ngữ `vi`, tên app thật, chú thích lớp |
| Ghi lại | `feature/loading/src/test/screenshots/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`, `LoadingScreen_progress_small.png`, `LoadingScreen_failed_small.png` | Bố cục, chữ, tên app, biểu tượng mới |
| Ghi lại | `docs/features/loading/design/review/LoadingScreen_progress_light_compare.png`, `LoadingScreen_progress_dark_compare.png`, `LoadingScreen_failed_light_compare.png`, `LoadingScreen_failed_dark_compare.png`, `summary.md` | Sinh bởi `scripts/design-compare.sh loading` |
| Sửa | `docs/features/loading/design/README.md` | Chỉ mục 8: kết quả đối chiếu và nhận xét rà soát |

Không file nào khác. Đặc biệt không sửa `:core:designsystem` (kể cả `brand_tagline`), `LoadingViewModel`, `LoadingUiState`, `data/`, `navigation/`, `di/`, `:app` (kể cả `app_name`), `build.gradle.kts`, `AndroidManifest.xml`.

**Nơi khác đọc các file trên** (L-020; `grep -rn` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`):

- Ảnh chuẩn `feature/loading/src/test/screenshots/*.png`: chỉ `LoadingScreenScreenshotTest.kt` (trong danh sách) ghi và so; `scripts/check.sh` và `.github/workflows/pr-check.yml` đọc theo mẫu đường dẫn chung, không theo tên, không cần sửa. `scripts/tests/run.sh` (dòng 172 tới 202) dùng ảnh của `:feature:sample` và file `LoadingScreen_failed_light` dựng trong thư mục tạm của test `figma-export`, không đọc ảnh hay `context/` thật của loading.
- `docs/features/loading/design/review/`: chỉ `scripts/design-compare.sh` ghi, qua `scripts/design-compare/DesignCompare.java` (ghi `summary.md` vào thư mục được truyền vào); hai file này không đổi. `scripts/device-session.sh` và `.github/workflows/device-session.yml` nhắc tới một `summary.md` khác, của thư mục phiên thử `test-sessions/`, không đọc `review/summary.md`. Không script hay test nào đọc `review/`.
- `strings.xml` của `:feature:loading`: chỉ code và test của module này đọc qua `R.string`. `scripts/tests/run.sh` dòng 261 đọc `app/src/main/res/values/strings.xml` của repo do `new-app.sh` tạo, và bỏ qua ở app này (không có `scripts/new-app.sh`).
- Ba drawable: chỉ `LoadingScreen.kt` dùng.
- `LoadingScreenTest.kt`, `LoadingScreenScreenshotTest.kt`: không script hay workflow nào đọc theo tên.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không chạm (`feature_loading_enabled` như cũ).
- Quảng cáo: Không chạm. Spec mục 5: màn loading không có quảng cáo.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

**Số đo trên code hiện tại** (L-029; đo ngày 2026-10-21 trên commit 9671124, trước T-007, bằng một test tạm không commit chạy trên bản sao của repo; `getUnclippedBoundsInRoot()`, tên app "Camera Pose: Lines and Guides", dp từ đỉnh màn hình):

| Khung, trạng thái | Tên app | Dòng hướng dẫn | Khối lỗi | Nút "Thử lại" | Chân trang |
| --- | --- | --- | --- | --- | --- |
| 411 x 891, đang tải | 378,67 tới 458,67, trái 48, phải 363 | 594,67 | | | 843 tới 859 |
| 411 x 891, lỗi | 378,67 tới 458,67 | | 598,67 | 686,67 tới 738,67 | 843 tới 859 |
| 360 x 640, lỗi | 207,33 tới 287,33, trái 48, phải 312 | | 427,33 | 515,33 tới 567,33 | 592 tới 608 |

Logo bắt đầu ở 222,75 (0,25 x 891) và cao 132. Sau T-007 logo cao 112, nên mọi phần dưới logo lên 20 dp: tên app 358,75, dòng hướng dẫn 574,75, khối lỗi 578,75, nút 666,75. Thiết kế: logo 200, tên app 336, dòng hướng dẫn và khối lỗi 552, nút 648, chân trang kết thúc ở 859. Tag `PROGRESS` đo được cao 28 (vùng ngữ nghĩa của `LinearProgressIndicator` nới ra, CLAUDE.md bước 10), nên test không đo thanh qua tag này.

**Đã thử ngôn ngữ và Lint** (cùng ngày, trên bản sao của repo, không commit): `createConfigurationContext` với `Locale.forLanguageTag` đọc đúng thư mục: `vi`, `vi-VN` ra `values-vi`; `hi`, `hi-IN` ra `values-hi`; `pt-BR` ra `values-pt-rBR`; `es-419`, `es-MX` ra `values-b+es+419`; `id`, `id-ID`, `in` ra `values-in`; `th`, `th-TH` ra `values-th`; `en`, `fr` ra `values/`. `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")` và `"b+es+419-w411dp-h891dp-xxhdpi"` đặt được ngôn ngữ trong Robolectric. `:app:lintStagingDebug` qua khi `:feature:loading` có `values-vi` đủ khóa; bỏ một khóa thì lỗi `MissingTranslation`; `values-id` cho cảnh báo `LocaleFolder`.

Quy ước chung của test dưới đây: lớp `LoadingScreenTest` giữ `@GraphicsMode(NATIVE)` và khung `w411dp-h891dp-xxhdpi` (không có ngôn ngữ, tức tiếng Anh). `APP_NAME` đổi từ "Todo App" thành "Camera Pose: Lines and Guides" (tên thật, nội dung nhiều nhất). So vị trí bằng `assertEquals(expected, actual.value, 4f)` (một bước lưới 4 dp, README mục 7), đo bằng `getUnclippedBoundsInRoot()`. Thêm hằng `DESIGN_FRAME_VI = "vi-w411dp-h891dp-xxhdpi"`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1: đủ các phần khi đang tải | Compose UI test (Robolectric) | `LoadingScreenTest.inProgress_showsIdentityProgressAndGuidance` sửa: khẩu hiệu tìm theo `string(R.string.loading_tagline)` thay `DesignSystemR.string.brand_tagline`, tên app thật; các khẳng định khác giữ. Qua ở bước 3 (khóa đã thêm), là test giữ hành vi |
| KB-1, tiêu chí 1: đúng vị trí thiết kế khi đang tải | Compose UI test, `@Config(qualifiers = DESIGN_FRAME_VI)` | `LoadingScreenTest.inProgress_layoutMatchesDesign`, `InProgress(0.68f)`: `LOGO` đỉnh 200, cao 112; `APP_NAME` đỉnh 336, cao 80 (hai dòng), trái 46,5, rộng 318; dòng `loading_guidance` đỉnh 552; dòng `loading_footer` đáy 859. Thất bại ở bước 3: logo đỉnh 222,75 |
| KB-2, tiêu chí 1: đúng vị trí thiết kế khi lỗi | Compose UI test, `@Config(qualifiers = DESIGN_FRAME_VI)` | `LoadingScreenTest.failed_layoutMatchesDesign`, `Failed(0.68f)`: `ERROR` đỉnh 552; `RETRY` đỉnh 648, cao 56; `loading_footer` đáy 859. Thất bại ở bước 3: khối lỗi 578,75 (sau T-007) |
| KB-2, tiêu chí 1: giao diện lỗi, bấm "Thử lại" | Compose UI test | `failed_showsErrorTitleGuidanceAndRetry_andRetryCallsOnRetry` giữ, chỉ đổi tên app. Phải vẫn qua |
| Logo không đổi chỗ khi lỗi | Compose UI test | `appName_staysInPlace_whenFailed` giữ, chỉ đổi tên app. Phải vẫn qua |
| Nút đủ lớn | Compose UI test | `retryButton_isAtLeast48dpHigh` giữ. Phải vẫn qua |
| Nền đúng vai trò `background` | Compose UI test | `LoadingScreenTest.background_isBackgroundRole_light` và `_dark`: trong `setContent` ghi lại `MaterialTheme.colorScheme.background` của `AppTheme(darkTheme = false)` (và `true`); điểm ảnh ở (4 dp, 4 dp) của `onNodeWithTag(SCREEN).captureToImage()` bằng màu đó (`toArgb()`). Thất bại ở bước 3: nền là `surface` (#FFFFFF, #1E1E1E sau T-007) |
| Màn hình thấp, tiếng Anh, không cần cuộn (L-028, FB-20261017-01) | Compose UI test, khung `w360dp-h640dp-xxhdpi` | `failed_onShortScreen_retryButtonFitsOnScreen` và `failed_onShortScreen_footerBelowError` giữ nguyên khẳng định, chỉ đổi tên app. Dự kiến nút kết thúc ở khoảng 555, chân trang bắt đầu ở 592. Phải vẫn qua; không qua thì dừng, không nới test (mục 7) |
| Màn hình thấp, cả 7 ngôn ngữ: tới được nút "Thử lại" | Compose UI test, 7 test, mỗi test `@Config(qualifiers = "<ngôn ngữ>-w360dp-h640dp-xxhdpi")` | `failed_onShortScreen_retryReachable_en` (không tiền tố ngôn ngữ), `_vi`, `_hi`, `_ptBR` (`pt-rBR`), `_es419` (`b+es+419`), `_in` (`in`), `_th`. Chung một hàm phụ trợ: `Failed(0.68f)`; `performScrollTo()` trên `RETRY`, `assertIsDisplayed()`; đáy `RETRY` không lớn hơn đáy `SCREEN`; cuộn tới dòng `loading_footer`, đỉnh chân trang không nhỏ hơn đáy `RETRY`; bấm `RETRY` gọi `onRetry` một lần. Qua ngay ở bước 3 vì chưa có bản dịch (mọi ngôn ngữ ra chữ gốc); đây là test chặn chữ dài của bản dịch làm hỏng màn hình thấp |
| Chữ đúng bảng chữ của thiết kế (tiếng Việt) | Unit test (Robolectric) | `LoadingStringsTest.vietnamese_matchesDesignTextTable`: ngữ cảnh `vi`, 8 khóa dịch đúng từng ký tự cột `values-vi/` (chữ của README mục 2 và spec mục 4). Thất bại ở bước 3: `loading_guidance` còn "Đang chuẩn bị không gian của bạn…", `loading_footer` còn chữ cũ, `loading_tagline` là chữ tiếng Anh |
| Tiếng Anh là ngôn ngữ gốc | Unit test | `LoadingStringsTest.english_isDefault`: ngữ cảnh `en` và `fr` (ngôn ngữ chưa có), 8 khóa đúng cột `values/`. Thất bại ở bước 3: `values/` còn tiếng Việt |
| Đủ 7 ngôn ngữ, đúng thư mục | Unit test | `LoadingStringsTest.everyReleaseLanguage_isTranslated`: với `hi`, `pt-BR`, `es-419`, `es-MX`, `id`, `th`, `vi`, mỗi khóa trong 8 khóa không rỗng và khác chữ của ngữ cảnh `en`. Thất bại ở bước 3: mọi ngôn ngữ ra chữ của `values/`. Thêm `LoadingStringsTest.percent_isSameInEveryLanguage`: `loading_percent` với 68 ra "68%" ở 7 ngôn ngữ (qua ngay, giữ hành vi) |
| Bản dịch đủ khóa | Android Lint | `:app:lintStagingDebug` trong `scripts/check.sh` báo `MissingTranslation` khi thiếu khóa (đã thử) |
| Đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, ảnh so sánh, rà soát độc lập | `LoadingScreenScreenshotTest`: lớp đổi sang `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")`, `APP_NAME = "Camera Pose: Lines and Guides"`, tiến độ 0,68. `progress_light`, `progress_dark`, `failed_light`, `failed_dark` ghi `src/test/screenshots/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`. `scripts/design-compare.sh loading`; rà soát theo README mục 7, nhất là phần trăm `onSurface` khi đang tải, màu `error` khi lỗi, viền thanh, biểu tượng mới |
| Màn hình thấp (L-028) | Screenshot test khung `vi-w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `SHORT_SCREEN = "vi-w360dp-h640dp-xxhdpi"`; `failed_small` ghi `LoadingScreen_failed_small.png`, `progress_small` ghi `LoadingScreen_progress_small.png`. Chú thích lớp: test khung chuẩn trùng tên ảnh thiết kế, test `_small` không có ảnh thiết kế nên không được so (ADR-0020) |
| Luồng của KB-1, KB-2 không đổi | Unit test, test trên emulator | `LoadingViewModelTest`, `DefaultPreparationRepositoryTest`, test trên emulator của `:app` qua, không sửa |
| Giao diện trên máy thật, ngôn ngữ của máy | Kiểm tra thủ công | Mở bản staging ở chế độ sáng và tối; đổi ngôn ngữ máy sang tiếng Việt, Hindi, Thái, mở lại app, xem chữ đúng ngôn ngữ và không bị cắt; tắt mạng, xóa dữ liệu app, mở lại để xem trạng thái lỗi |
| Chất lượng bản dịch | Người biết tiếng duyệt | Trước khi phát hành, theo bảng ở mục 3.1 (mục 8, câu 1) |

**Ngoại lệ viết test trước:** ảnh chuẩn screenshot, ảnh so sánh, ba drawable và file giấy phép không viết test trước được; người duyệt xem trên PR. Các test giữ hành vi (`inProgress_showsIdentityProgressAndGuidance` sau khi đổi khóa, các test hiện có chỉ đổi tên app, `percent_isSameInEveryLanguage`) và 7 test `failed_onShortScreen_retryReachable_*` qua ngay ở bước 3, lý do ở bảng trên; ghi vào nhật ký. Commit test có hai thay đổi nhỏ ngoài file test (tag `LOGO`, khóa `loading_tagline`) để test biên dịch và đo được (mục 3.1, Thứ tự commit).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Mọi file nằm trong `:feature:loading` và `docs/features/loading/`; không sửa module core, cấu hình build, manifest, quảng cáo hay sự kiện phân tích. Giấy phép Material Symbols chỉ là đường vẽ chép vào drawable, không phải SDK hay thư viện mới.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Nhiều phần trong một module: bố cục, 7 file chữ, 3 drawable, 3 file test, 6 ảnh chuẩn; không đổi giao diện công khai của lớp nào ngoài module |
| B. Độ rõ của yêu cầu | 1 | Spec xác nhận, thiết kế duyệt, có số liệu; AI tự quyết bản dịch, chữ tiếng Anh, chiều rộng ở màn 360, biểu tượng (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Vị trí, chữ từng ngôn ngữ, màu nền, cuộn, đủ bản dịch đều có test; chất lượng bản dịch, màu chữ và nét biểu tượng xem bằng mắt |
| D. Ảnh hưởng chéo | 1 | Dùng `BrandLogo`, `PrimaryButton`, `ProgressBar` của T-007 mà không sửa; T-010 sửa phần việc của cùng module nhưng không chạm file của plan này |
| E. Độ mới của kỹ thuật | 2 | Tài nguyên nhiều ngôn ngữ, ngôn ngữ trong khung Robolectric, `captureToImage` trong test feature chưa có trong dự án; đều là cách phổ biến, đã thử trước (mục 4) |
| **Tổng** | 7 | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không sửa file dùng chung. Dùng `BrandLogo`, `PrimaryButton`, `ProgressBar`, `AppTheme` của T-007. Màn loading thôi dùng `brand_tagline` của `:core:designsystem`; khóa này còn ở catalog với chữ của dự án mẫu (mục 8, câu 3).
- Ngôn ngữ: đây là ticket đầu tiên đặt tiếng Anh làm gốc. Ticket sau (T-012 màn chào và các màn khác) làm theo cùng cách: `values/` tiếng Anh, 6 thư mục `values-vi`, `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th`. Chọn ngôn ngữ trong app (`locales_config`, spec `settings`) không thuộc ticket này.
- Task khác có thể xung đột:
  - **T-007** (đang chạy): plan này dựa trên kết quả của T-007; T-007 ghi lại 6 ảnh loading theo hệ thống mới với bố cục cũ, plan này ghi lại lần nữa vì bố cục và chữ đổi.
  - **T-011** (cùng phiên RUN-20261021-01): có thể thêm tham số `loading` cho `PrimaryButton` và `ic_refresh.xml` trong `:core:designsystem`. Không ảnh hưởng: plan dùng drawable riêng của feature và chữ ký cũ của nút.
  - **T-010** (chưa có plan): sửa `LoadingViewModel`, phần việc tải, có thể thêm chữ cho lỗi tải. Nếu T-010 thêm chữ vào `strings.xml` của `:feature:loading` thì phải dịch đủ 7 ngôn ngữ (Lint báo nếu thiếu). Đề xuất T-008 chạy trước T-010.
  - **T-006** (đã duyệt ở dự án mẫu, chưa chạy ở app này, chưa có trong danh sách chạy nào): thêm test cuộn ở khung 320 x 480 vào `LoadingScreenTest.kt` và sửa chú thích `LoadingScreenScreenshotTest.kt`. Plan này đã gồm phần chú thích và test cuộn ở 360 x 640 cho 7 ngôn ngữ; số đo của T-006 không còn đúng sau bố cục mới (mục 8, câu 7).
- Đề xuất thứ tự chạy: sau khi T-007 merge (phiên sau RUN-20261021-01, hoặc cùng phiên nếu dev thêm vào sau T-007), trước T-010 và T-012.

## 7. Điều kiện dừng riêng

1. Trên nhánh chính chưa có T-007, hoặc thành phần T-007 giao khác điều mục 3.1 ghi (`BrandLogo` không phải 112 hay không nhận `modifier`, `PrimaryButton` không cao 56 hay đổi chữ ký cũ, `ProgressBar` không có tham số `color`, `trackColor` hay viền mặc định, `colorScheme.background` không phải #FFFDF8 và #121212, `headlineLarge` không phải 32/40). Dừng trước khi viết test, ghi điều khác vào nhật ký; không sửa `:core:designsystem`.
2. Mã băm của `LICENSE.txt` tải về không khớp mục 3.1.
3. Sau bước 4, một số đo của `inProgress_layoutMatchesDesign` hay `failed_layoutMatchesDesign` lệch quá 4 dp mà không sửa được bằng bố cục trong `LoadingScreen.kt` (ví dụ tên app xuống ba dòng ở khung 411 vì nét phông của T-007). Dừng, ghi số đo; không đổi số mong đợi của test.
4. `failed_onShortScreen_retryButtonFitsOnScreen`, `failed_onShortScreen_footerBelowError` hay một test `failed_onShortScreen_retryReachable_*` thất bại. Dừng, không nới khẳng định hay đổi khung test.
5. `:app:lintStagingDebug` báo lỗi mới ngoài `:feature:loading` (ví dụ `MissingTranslation` cho chuỗi của module khác), hoặc báo lỗi trong `:feature:loading` mà chỉ sửa được bằng `tools:ignore` hay tắt quy tắc.
6. Robolectric không đặt được ngôn ngữ theo khung (ảnh `vi` ra chữ tiếng Anh), hoặc chữ tiếng Việt trong ảnh hiện ô vuông.
7. Ảnh chuẩn đổi ngoài 6 ảnh "Ghi lại" của `:feature:loading` ở mục 3.2.
8. `scripts/design-compare.sh loading` chạy được nhưng báo thiếu ảnh chuẩn, thiếu `context/`, hoặc lỗi khác ngoài lỗi hạ tầng.
9. Rà soát độc lập thấy chữ trên ảnh khác bảng chữ, thiếu trạng thái, sai thứ tự hay sai vai trò màu so với thiết kế, mà sửa cần file ngoài danh sách.

## 8. Câu hỏi cho người duyệt

1. **Bản dịch do AI làm.** Spec `onboarding`, câu hỏi 1 chốt: AI dịch trong cùng ticket, người biết tiếng duyệt trước khi phát hành. Đề xuất: merge với bản dịch ở mục 3.1; báo cáo phiên liệt kê bảng cho người duyệt; anh sắp xếp người duyệt tiếng Hindi, Thái, Indonesia, Bồ Đào Nha, Tây Ban Nha trước bản production đầu tiên. Bản dịch sửa sau chỉ đổi file chữ, không đổi ảnh chuẩn (ảnh chụp tiếng Việt). Hindi và Thái hiện bằng phông hệ thống vì Bricolage Grotesque không có hai bộ chữ này; đề xuất chấp nhận.
2. **Chữ tiếng Anh.** Thiết kế chỉ có tiếng Việt, nên chữ tiếng Anh (ngôn ngữ gốc, hiện ở Ấn Độ, Philippines và mọi máy đặt ngôn ngữ khác) là AI viết: "Follow the outline, get a great shot.", "Loading data", "Preparing your poses…", "Progress stopped", "Unable to load data", "The connection was interrupted. Check your network and try again to continue.", "Try again", "Your photos stay on your device". Đề xuất: dùng như trên; anh sửa câu nào thì AI sửa plan trước khi duyệt.
3. **Khẩu hiệu đặt ở feature.** `brand_tagline` nằm trong `:core:designsystem` (sửa là điểm chặn, và phải dịch 7 ngôn ngữ trong core). Đề xuất: khẩu hiệu thành `loading_tagline` của `:feature:loading`; `brand_tagline` giữ chữ của dự án mẫu, chỉ catalog dùng. Một task core sau (ví dụ khi có icon app và logo thật) đổi hay bỏ `brand_tagline`.
4. **Biểu tượng Material Symbols Rounded.** Code đang dùng biểu tượng Lucide của thiết kế cũ (cảnh báo hình tròn); thiết kế mới dùng `warning` hình tam giác, `refresh`, `lock` của Material Symbols (`sticker.md` mục 2). Đề xuất: đổi trong ticket này, đường vẽ chép sẵn ở mục 3.1, kèm giấy phép Apache 2.0 trong `feature/loading/third_party/`.
5. **Chiều rộng nội dung.** Thiết kế ghi vùng nội dung rộng 318, căn giữa. Đề xuất: rộng tối đa 318, lề tối thiểu 16; ở khung 411 đúng thiết kế, ở màn 360 lề còn 21 thay vì 48, để tên app vẫn hai dòng và nút "Thử lại" lọt màn hình không cần cuộn. Nếu anh muốn giữ lề 46 ở mọi màn hình thì tên app có thể xuống ba dòng ở màn 360 và phải cuộn mới tới nút ở vài ngôn ngữ.
6. **Phần trăm không dịch.** "68%" giống nhau ở 7 ngôn ngữ phát hành. Đề xuất: `loading_percent` đánh dấu `translatable="false"`.
7. **T-006.** Plan T-006 của dự án mẫu (test cuộn ở 320 x 480) chưa chạy ở app này; số đo của nó không còn đúng sau bố cục mới. Đề xuất: đóng T-006 ở app này, vì 7 test cuộn ở mục 4 đã phủ đường cuộn; nếu anh muốn giữ khung 320 x 480 thì viết lại plan T-006 phiên bản 2 sau khi T-008 merge.
8. **Logo vẫn tạm.** `BrandLogo` của T-007 dùng `brand_mark.xml` cũ trong ô vàng; thiết kế có hình dáng người. Đề xuất: giữ như T-007 đã duyệt, thay khi có icon app; rà soát độc lập không tính điểm này là lệch thiết kế (README mục 6 cho phép).
9. **Ảnh màn hình thấp chỉ bằng tiếng Việt.** Đề xuất: không thêm ảnh chuẩn cho ngôn ngữ khác; 7 test cuộn ở mục 4 kiểm tra mọi ngôn ngữ ở khung 360 x 640, còn ảnh tiếng Việt khớp thiết kế. Thêm ảnh Hindi hay Thái thì ảnh phụ thuộc phông hệ thống của Robolectric, dễ đổi khi nâng phiên bản.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-YYYYMMDD-NN, phiên bản plan 1

- **Kết quả:** đã merge / dừng / CI thất bại
- **Nhánh, PR:** <...>
- **Đã làm:** <...>
- **Test viết trước:** <số test, xác nhận đã thất bại đúng lý do trước khi viết code>
- **Rà soát độc lập:** <số điểm phát hiện, đã xử lý những gì, điểm nào còn để người xem>
- **Kết quả CI:** <build, lint, unit test, UI test, screenshot test>
- **Lệch so với plan:** <chỗ nào và vì sao, hoặc "Không có">
- **Việc còn lại:** <...>
- **Cần người xem:** <điểm dev nên kiểm tra sáng nay>
