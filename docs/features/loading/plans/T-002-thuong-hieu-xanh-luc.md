---
ticket: T-002
title: Hệ thống thiết kế theo thương hiệu mới (xanh lục, Inter, logo)
status: done
version: 2
risk_score:
risk_level: high
blocking_points: [core]
run_order:
reviewer: Nhật
cross_reviewer: Nhật (vì là bản mẫu)
approved_at: 2026-10-17
---

# PLAN T-002: HỆ THỐNG THIẾT KẾ THEO THƯƠNG HIỆU MỚI (XANH LỤC, INTER, LOGO)

## 1. Tóm tắt

**Phiên bản 2 (2026-10-17)**, sau khi RUN-20261016-02 dừng ở bước 5 (mục 10). Thay đổi so với phiên bản 1:

- Không đổi phần code. Lý do dừng nằm ngoài plan: ba test của `scripts/figma-export.py` đọc `tokens.json` thật và mong giá trị của hệ thống thiết kế cũ. Đã sửa trên nhánh chính bằng PR riêng (L-020): test dùng `scripts/tests/fixtures/tokens.json` cố định; đã thử với `tokens.json` của nhánh T-002, qua.
- Vì sao plan chưa đủ tốt: plan đổi `docs/design-system/tokens.json` nhưng không tìm mọi nơi đọc file đó, nên không thấy test của script phụ thuộc vào nó.
- Ghi rõ cỡ dấu logo (mục 3.1): khung 56 x 52 dp của SVG gồm cả nửa nét vẽ ở mỗi phía, phần nét nhìn thấy là 52 x 48 như README thiết kế. Code phiên bản 1 đã đúng.
- Cách tiếp tục: tiếp tục trên nhánh cũ `claude/T-002-thuong-hieu-xanh-luc` (commit 9d23df1, 798245f), chưa có PR.

Đổi `:core:designsystem` theo thiết kế Figma của màn hình loading: bảng màu xanh lục sáng và tối, phông Inter, `PrimaryButton` mới có biểu tượng, hai thành phần mới `BrandLogo` và `ProgressBar`, khẩu hiệu mặc định. Kiểm chứng bằng `DesignTokensTest` (giá trị khớp bảng ở mục 3), test giao diện của thành phần mới, và ảnh chuẩn screenshot ghi lại cho catalog, màn hình mẫu, màn hình loading. Bố cục màn hình loading không đổi ở plan này (T-003).

## 2. Hiểu yêu cầu

Ticket chuẩn bị nền cho các kịch bản sau trong `features/loading/spec.md` (T-003 làm phần màn hình):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Màn hình loading có logo, tên app, khẩu hiệu, thanh tiến trình, đúng thiết kế | Hệ thống thiết kế phải có màu, phông, logo, thanh tiến trình như thiết kế, để màn hình dùng token, không viết cứng giá trị |
| KB-2 | Khi lỗi, thanh tiến trình chuyển màu báo lỗi, có nút "Thử lại" | Cần vai trò `error`, `errorContainer` theo thiết kế; `PrimaryButton` có biểu tượng |

Thiết kế tham chiếu: `features/loading/design/README.md` (mục 3: bảng màu, ánh xạ phần tử; mục "Tài nguyên trong `handoff/`"), số liệu trong `design/context/`, ảnh trong `design/screens/`.

ADR liên quan: ADR-0008 (code là gốc của hệ thống thiết kế, đổi hệ thống thiết kế là rủi ro cao), ADR-0020 (catalog, `tokens.json`, khung chuẩn).

## 3. Giải pháp

### 3.1. Cách làm

Cách tiếp tục: tiếp tục trên nhánh cũ `claude/T-002-thuong-hieu-xanh-luc`, chưa có PR (auto-run.md, mục Tiếp tục task đã dừng). Phiên cập nhật nhánh theo nhánh chính (đã có sửa L-020), chạy lại từ bước 5; không viết lại test của bước 3 vì không test nào đổi điều kiểm tra. Code trên nhánh đã làm đủ mục 3.1 dưới đây.

**Màu (`Color.kt`).** Thay bảng màu thương hiệu bằng bảng dưới, dùng cho `lightColorScheme(...)` và `darkColorScheme(...)`. Cột "Nguồn": "thiết kế" là mã lấy từ `design/context/`, "AI" là giá trị AI đề xuất cho vai trò thiết kế không dùng, cùng tông xanh lục (mục 8, câu 1).

| Vai trò | Sáng | Tối | Nguồn |
| --- | --- | --- | --- |
| `primary` | #147D68 | #84DCC1 | thiết kế |
| `onPrimary` | #FFFFFF | #101F1D | thiết kế |
| `primaryContainer` | #EBF3EE | #192E27 | thiết kế |
| `onPrimaryContainer` | #0B4A3D | #A6F0D8 | AI |
| `inversePrimary` | #84DCC1 | #147D68 | AI |
| `secondary` | #4C635B | #B3CCC2 | AI |
| `onSecondary` | #FFFFFF | #1E352E | AI |
| `secondaryContainer` | #CEE9DE | #344C44 | AI |
| `onSecondaryContainer` | #082019 | #CEE9DE | AI |
| `tertiary` | #3F6374 | #A7CCE0 | AI |
| `onTertiary` | #FFFFFF | #0A3445 | AI |
| `error` | #BA5148 | #F49A90 | thiết kế |
| `onError` | #FFFFFF | #5C1A14 | AI |
| `errorContainer` | #F0DEDA | #48332F | thiết kế |
| `onErrorContainer` | #5C1A14 | #F0DEDA | AI |
| `background`, `surface` | #F7FAF8 | #101F1D | thiết kế |
| `onBackground`, `onSurface` | #17352F | #EFF8F4 | thiết kế |
| `surfaceVariant` | #DFEAE4 | #293D36 | thiết kế |
| `onSurfaceVariant` | #6D8079 | #96ADA5 | thiết kế |
| `outline` | #8A9C95 | #6F857D | AI |
| `outlineVariant` | #C3D1CA | #3F534C | AI |
| `surfaceTint` | bằng `primary` | bằng `primary` | AI |
| `inverseSurface` | #2B3D38 | #EFF8F4 | AI |
| `inverseOnSurface` | #EEF5F1 | #2B3D38 | AI |
| `surfaceContainerLowest` | #FFFFFF | #0B1816 | AI |
| `surfaceContainerLow` | #F1F6F3 | #14241F | AI |
| `surfaceContainer` | #EBF1EE | #182925 | AI |
| `surfaceContainerHigh` | #E5ECE8 | #1F302B | AI |
| `surfaceContainerHighest` | #DFEAE4 | #293D36 | AI |
| `surfaceBright` | #F7FAF8 | #2B3D38 | AI |
| `surfaceDim` | #D6E0DA | #101F1D | AI |

Giữ chú thích "Brand palette. Each app cloned from the template replaces these values (ADR-0008)". Không vai trò nào còn giá trị mặc định tím của Material 3.

**Phông Inter.** Tải bản phát hành chính thức, kiểm tra mã băm, lấy ba file, không thêm thư viện:

```sh
curl -fL -o /tmp/Inter-4.1.zip https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip
echo "9883fdd4a49d4fb66bd8177ba6625ef9a64aa45899767dde3d36aa425756b11e  /tmp/Inter-4.1.zip" | shasum -a 256 -c
unzip -j /tmp/Inter-4.1.zip extras/ttf/Inter-Regular.ttf extras/ttf/Inter-Medium.ttf extras/ttf/Inter-SemiBold.ttf LICENSE.txt -d /tmp/inter
```

Mã băm của từng file sau khi giải nén: `Inter-Regular.ttf` 40d692fce188e4471e2b3cba937be967878f631ad3ebbbdcd587687c7ebe0c82, `Inter-Medium.ttf` 97ad806f526e41546d46365bb3a393145f75b7b1568913db74549ad8b8dba872, `Inter-SemiBold.ttf` 78a843fade9d4612a5567302fb595b56976eb5fcebf4fea5a5912d638bafcde3, `LICENSE.txt` 262481e844521b326f5ecd053e59b98c8b2da78c8ee1bdbb6e8174305e54935a. Chép thành `res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf` (tên tài nguyên Android chỉ chữ thường và gạch dưới) và `third_party/inter/LICENSE.txt` (giấy phép SIL OFL 1.1 yêu cầu đi kèm). Tải không được thì đó là lỗi hạ tầng, xử lý theo auto-run.md (chờ và thử lại); mã băm sai thì dừng (mục 7).

**Chữ (`Type.kt`, mới).** `internal val InterFamily = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold))`. `internal val AppTypography`: lấy `Typography()` mặc định, mọi kiểu chữ `copy(fontFamily = InterFamily)`; riêng `titleMedium` thêm `fontWeight = FontWeight.SemiBold` (tiêu đề lỗi trên thiết kế đậm 600). Cỡ chữ và chiều cao dòng giữ thang Material 3. `AppTheme` dùng `AppTypography` thay cho `Typography()`.

**`PrimaryButton`.** Thêm tham số `icon: Painter? = null` đặt sau `enabled`, giữ nguyên các tham số cũ để chỗ đang gọi không phải sửa. Nút cao tối thiểu 52 dp (vẫn trên 48), `shape = RoundedCornerShape(16.dp)`, chữ `MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)`. Có `icon` thì vẽ `Icon(painter = icon, contentDescription = null, modifier = Modifier.size(18.dp))`, cách chữ `Spacing.sm`; màu theo `contentColor` của nút (`onPrimary`).

**`BrandLogo` (mới, `component/BrandLogo.kt`).** `fun BrandLogo(modifier: Modifier = Modifier)`: vòng tròn 132 dp màu `primaryContainer`, giữa là ô 88 dp bo góc 26 dp màu `primary`, giữa ô là `Icon(painterResource(R.drawable.brand_mark), contentDescription = null, tint = onPrimary)` cỡ 56 x 52 dp (khung của SVG, nét dày 4; khung gồm nửa nét ở mỗi phía, nên phần nét nhìn thấy là 52 x 48 như README thiết kế). Không có mô tả cho trình đọc màn hình vì tên app đứng ngay dưới. Kích thước là hằng số `private` có tên trong file, như `PrimaryButton` đang làm với 48 dp.

**`brand_mark.xml` (mới).** Vector drawable từ `docs/features/loading/design/handoff/brand_mark.svg`: `android:width="56dp" android:height="52dp" android:viewportWidth="56" android:viewportHeight="52"`, một `path` với `android:pathData` chép nguyên thuộc tính `d`, `android:strokeColor="#FFFFFFFF"`, `android:strokeWidth="4"`, `android:strokeLineCap="round"`, không `fillColor`.

**`ProgressBar` (mới, `component/ProgressBar.kt`).** `fun ProgressBar(progress: () -> Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, trackColor: Color = MaterialTheme.colorScheme.surfaceVariant)`: gọi `LinearProgressIndicator` với `modifier.fillMaxWidth().height(8.dp)`, `strokeCap = StrokeCap.Round`, `gapSize = 0.dp`, `drawStopIndicator = {}`. Thanh liền một khối như thiết kế, không khe hở, không chấm cuối.

**Khẩu hiệu.** `core/designsystem/src/main/res/values/strings.xml` (mới) có `<string name="brand_tagline">Mọi việc vào đúng nhịp.</string>`, dùng cùng `BrandLogo`; mỗi app thay bằng khẩu hiệu của mình.

**Catalog.** `ColorCatalog` thêm dòng `errorContainer` (trên `onErrorContainer`). Thêm `BrandCatalog` (trang mới, chữ trong catalog không vào `strings.xml` như các trang khác): `BrandLogo`, khẩu hiệu `brand_tagline` kiểu `bodyMedium`, `ProgressBar` ở 0,68 màu mặc định, `ProgressBar` ở 0,68 màu `error` trên `errorContainer`, `PrimaryButton(text = "PrimaryButton", icon = painterResource(R.drawable.brand_mark))`. `DesignSystemCatalogScreenshotTest` thêm hai test `brand_light`, `brand_dark` ghi `DesignSystem_brand_light.png`, `DesignSystem_brand_dark.png`.

**`tokens.json`.** `DesignTokensTest` đọc `AppTypography` thay cho `Typography()`, thêm `errorContainer`, `onErrorContainer` vào danh sách vai trò màu (sau `onError`), và thêm dòng `"fontFamily": "Inter",` ngay sau dấu `{` mở đầu. Sinh lại file bằng `./gradlew :core:designsystem:testDebugUnitTest -PdesignTokens.update=true`.

**Hướng dẫn tạo app mới.** `docs/ops/new-app.md` thêm một bước "Thương hiệu": thay bảng màu trong `Color.kt`, `brand_mark.xml`, `brand_tagline`, và phông trong `res/font` nếu app dùng phông khác; sau đó sinh lại `tokens.json` và ghi lại ảnh catalog.

Thứ tự làm: test (mục 4) trước, chạy để thấy thất bại đúng lý do; rồi phông, màu, chữ, thành phần, catalog; sinh `tokens.json`; ghi lại ảnh chuẩn screenshot của đúng các ảnh ở mục 3.2; `scripts/check.sh`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Color.kt` | Bảng màu xanh lục sáng và tối, đủ vai trò ở mục 3.1 |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Type.kt` | `InterFamily`, `AppTypography` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Theme.kt` | Dùng `AppTypography` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PrimaryButton.kt` | Cao 52, bo góc 16, chữ đậm 600, tham số `icon` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/BrandLogo.kt` | Thành phần `BrandLogo` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/ProgressBar.kt` | Thành phần `ProgressBar` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | Dòng `errorContainer`, trang `BrandCatalog` |
| Tạo | `core/designsystem/src/main/res/drawable/brand_mark.xml` | Dấu của logo |
| Tạo | `core/designsystem/src/main/res/font/inter_regular.ttf` | Inter 4.1 Regular |
| Tạo | `core/designsystem/src/main/res/font/inter_medium.ttf` | Inter 4.1 Medium |
| Tạo | `core/designsystem/src/main/res/font/inter_semibold.ttf` | Inter 4.1 SemiBold |
| Tạo | `core/designsystem/third_party/inter/LICENSE.txt` | Giấy phép SIL OFL 1.1 của Inter |
| Tạo | `core/designsystem/src/main/res/values/strings.xml` | `brand_tagline` |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignTokensTest.kt` | `AppTypography`, `errorContainer`, `onErrorContainer`, `fontFamily` |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt` | Test `brand_light`, `brand_dark` |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/ComponentsTest.kt` | Test giao diện của `PrimaryButton`, `BrandLogo`, `ProgressBar` (mục 4) |
| Sửa | `docs/design-system/tokens.json` | Sinh lại bằng `DesignTokensTest` |
| Ghi lại | `core/designsystem/src/test/screenshots/DesignSystem_colors_light.png`, `DesignSystem_colors_dark.png`, `DesignSystem_typography_light.png`, `DesignSystem_spacing_components_light.png` | Màu, phông, nút mới |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_brand_light.png`, `DesignSystem_brand_dark.png` | Trang `BrandCatalog` |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`, `SampleScreen_disabled_light.png` | Màu, phông, nút mới; bố cục không đổi |
| Ghi lại | `feature/loading/src/test/screenshots/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png` | Màu, phông, nút mới; bố cục không đổi (T-003 đổi) |
| Sửa | `docs/ops/new-app.md` | Bước "Thương hiệu" |

Không file nào khác. Đặc biệt không sửa `build.gradle.kts` của module, `gradle/libs.versions.toml`, code của `:feature:sample` và `:feature:loading`.

**Nơi khác đọc các file trên** (docs/CLAUDE.md, mục Khi viết plan; L-020), không cần sửa:

- `docs/design-system/tokens.json`: `core/designsystem/build.gradle.kts` chỉ truyền đường dẫn file cho `DesignTokensTest`; `scripts/figma-export.py` đọc file khi người lấy thiết kế, và test của nó trong `scripts/tests/run.sh` dùng `scripts/tests/fixtures/tokens.json` cố định.
- `docs/ops/new-app.md`: `scripts/new-app.sh` chỉ xóa file này khỏi app mới, không đọc nội dung.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Màu, chữ đúng mục 3.1 | Unit test | `DesignTokensTest.tokensFile_matchesTheDesignSystem`: viết trước bằng cách sửa tay `tokens.json` theo bảng màu mục 3.1, thêm `errorContainer`, `onErrorContainer`, `"fontFamily": "Inter"`, `titleMedium` đậm 600; test thất bại cho tới khi code đổi theo. Sau đó sinh lại bằng `-PdesignTokens.update=true` và so: file sinh ra phải trùng file sửa tay |
| Nút có biểu tượng | Compose UI test (Robolectric) | `ComponentsTest.primaryButton_withIcon_showsIconAndText_andClicks`: có `icon` thì có nút mang chữ, bấm gọi `onClick` một lần; `primaryButton_height_isAtLeast52dp`: `assertHeightIsAtLeast(52.dp)` |
| Thanh tiến trình | Compose UI test | `ComponentsTest.progressBar_reportsProgress`: `ProgressBar(progress = { 0.68f })` có `ProgressBarRangeInfo(0.68f, 0f..1f)`; `progressBar_height_is8dp`: `assertHeightIsEqualTo(8.dp)` |
| Logo | Compose UI test | `ComponentsTest.brandLogo_is132dpSquare`: `assertWidthIsEqualTo(132.dp)`, `assertHeightIsEqualTo(132.dp)` |
| Phông Inter vẽ được trong test | Screenshot test | `DesignSystem_typography_light.png` ghi lại: người duyệt thấy phông Inter (chữ "a" hai tầng, "y" thẳng), có dấu tiếng Việt khi xem `DesignSystem_brand_light.png` |
| Giao diện catalog, màn hình mẫu, màn hình loading | Screenshot test | Ghi lại đúng 12 ảnh ở mục 3.2 bằng `./gradlew recordRoborazziDebug`, rồi `scripts/check.sh` xanh; người duyệt xem ảnh trên PR |
| Gần màu thiết kế | Kiểm tra thủ công | Mở `DesignSystem_brand_light.png` cạnh `docs/features/loading/design/screens/LoadingScreen_progress_light.png`: logo, thanh tiến trình, nút cùng màu |

**Ngoại lệ viết test trước:** ảnh chuẩn screenshot không viết trước được (ảnh là kết quả); phông, vector drawable và chuỗi là tài nguyên. Các phần này kiểm chứng bằng `DesignTokensTest` (viết trước) và ảnh được người duyệt xem.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core dùng chung `:core:designsystem`). Rủi ro cao, cần duyệt chéo. Điểm dưới đây để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Sửa một module core nhưng đổi giao diện của mọi màn hình ở hai module feature (ghi lại ảnh) |
| B. Độ rõ của yêu cầu | 1 | Thiết kế rõ các màu đang dùng; các vai trò khác do AI đề xuất, cần người duyệt đồng ý (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Giá trị kiểm bằng test; độ "đẹp" và độ giống thiết kế phải xem ảnh |
| D. Ảnh hưởng chéo | 3 | Mọi màn hình đang có đổi giao diện; T-003 dựa vào thành phần mới |
| E. Độ mới của kỹ thuật | 1 | Phông trong `res/font` và `FontFamily` là mẫu phổ biến nhưng chưa có trong dự án |
| **Tổng** | 9 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AppTheme`, `PrimaryButton` (dùng ở `SampleScreen`, `LoadingScreen`), `tokens.json`, ảnh catalog.
- Task khác trong sprint có thể xung đột: T-003 dùng `BrandLogo`, `ProgressBar`, `PrimaryButton(icon)` và ghi lại cùng ba ảnh loading.
- Đề xuất thứ tự chạy: T-002 chạy trước, T-003 sau (T-003 phụ thuộc T-002).

## 7. Điều kiện dừng riêng

1. Mã băm của file zip Inter hoặc của file giải nén không khớp mục 3.1.
2. Robolectric không vẽ được phông trong `res/font` (ảnh screenshot vẫn ra phông Roboto): dừng, không thay bằng cách khác.
3. `LinearProgressIndicator` của phiên bản Compose đang dùng không có `gapSize` hoặc `drawStopIndicator`: dừng, không tự vẽ thanh bằng `Canvas`.
4. Ảnh chuẩn screenshot đổi ngoài 12 ảnh ở mục 3.2, hoặc bố cục của màn hình mẫu đổi ngoài màu, phông và nút.

## 8. Câu hỏi cho người duyệt

1. **Các vai trò màu thiết kế không dùng** (cột "AI" ở mục 3.1): AI đề xuất cùng tông xanh lục để không còn màu tím mặc định. Đề xuất: duyệt bảng này; sau này chỉnh trên Figma thì làm plan mới.
- Trả lời: Duyệt bản này.
2. **Độ tương phản của `onSurfaceVariant` #6D8079 trên nền #F7FAF8** khoảng 4,0:1, hơi dưới mức 4,5:1 cho chữ nhỏ theo WCAG AA (khẩu hiệu, dòng hướng dẫn, chân trang dùng màu này). Đề xuất: giữ đúng thiết kế ở lần này, ghi thành góp ý cho thiết kế; hoặc đậm lên #5F716B (khoảng 4,9:1) nếu anh muốn đạt AA ngay.
- Trả lời: Dùng đúng thiết kế.
3. **Phông Inter làm APK nặng thêm khoảng 1,2 MB** (ba file). Đề xuất: chấp nhận; app nào muốn nhẹ thì đổi sang phông tải về sau.
- Trả lời: Chấp nhật.
4. **Ảnh catalog và ảnh `tokens.json` đổi** thì hệ thống thiết kế trên Figma cần dựng lại theo file mới (design-workflow.md, mục 1). Đề xuất: sau khi T-002 merge, anh tạo biến màu và kiểu chữ trên Figma theo `tokens.json` mới để các thiết kế sau dùng đúng token.
- Trả lời: OK.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:**

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (vì là bản mẫu)
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261016-02, phiên bản plan 1

- **Kết quả:** dừng ở bước 5 (kiểm tra cục bộ), điều kiện dừng 1 và 5 (risk-scoring.md, mục 4).
- **Nhánh, PR:** `claude/T-002-thuong-hieu-xanh-luc`, commit 9d23df1 (test) và 798245f (code). Không mở PR.
- **Đã làm:** đủ mục 3.1: bảng màu xanh lục sáng và tối, phông Inter 4.1 (mã băm zip và bốn file đều khớp), `AppTypography`, `PrimaryButton` mới có `icon`, `BrandLogo`, `ProgressBar`, `brand_mark.xml`, `brand_tagline`, dòng `errorContainer` và trang `BrandCatalog` trong catalog, bước "Thương hiệu" trong `docs/ops/new-app.md`. `tokens.json` sinh lại trùng file sửa tay. Ghi lại đúng 12 ảnh ở mục 3.2, không ảnh nào khác đổi. Phông Inter vẽ được trong Robolectric (chữ "a" hai tầng, "y" thẳng, dấu tiếng Việt đủ); bố cục màn hình mẫu không đổi.
- **Vì sao dừng:** `scripts/check.sh` đỏ ở phần test của script, 3 test thất bại: `figma-export: text style matched to a token`, `color matched to light roles`, `unknown values flagged`. Các test này trong `scripts/tests/run.sh` (dòng 120 đến 122) chạy `scripts/figma-export.py` trên `scripts/tests/fixtures/figma-nodes.json` với `docs/design-system/tokens.json` thật, và mong giá trị của hệ thống thiết kế cũ (`titleMedium` đậm 500, nền `#FEF7FF`). Sửa test này là sửa file ngoài danh sách file của plan, trong `scripts/`, và là sửa test để qua. Phần còn lại của cổng chất lượng chạy riêng thì qua: quy tắc phụ thuộc module, ktlint, 94 test JVM 0 lỗi, 12 ảnh screenshot khớp, Android Lint, build staging.
- **Test viết trước:** 6 test (`DesignTokensTest` với `tokens.json` sửa tay, 5 test trong `ComponentsTest`), đều thất bại đúng lý do trước khi viết code. Hai lần sửa chi tiết test, không đổi điều test kiểm tra:
  - Bước 3: `primaryButton_height_isAtLeast52dp` qua ngay khi chưa có code, vì Robolectric ở chế độ đồ họa cũ đo chữ cao 36 dp. Thêm `@GraphicsMode(GraphicsMode.Mode.NATIVE)` cho `ComponentsTest` (như test screenshot), sau đó test thất bại đúng lý do (48 dp).
  - Bước 4: `progressBar_height_is8dp` đo được 28 dp vì `LinearProgressIndicator` tự nới vùng ngữ nghĩa thêm 10 dp mỗi phía cho trình đọc màn hình; thanh vẽ ra vẫn cao 8 dp. Test đo qua một `Box` bọc ngoài mang `testTag`.
  - `primaryButton_withIcon_showsIconAndText_andClicks`: biểu tượng không có nút ngữ nghĩa, nên test kiểm tra có biểu tượng bằng vị trí chữ (chữ lùi vào 26 dp = 18 biểu tượng + 8 khoảng cách) so với nút không biểu tượng.
- **Rà soát độc lập:** chưa làm (dừng trước bước 6).
- **Kết quả CI:** chưa chạy (không mở PR).
- **Lệch so với plan:** Không có trong code. Ghi chú: plan đặt dấu logo 56 x 52 dp (khung của SVG), README thiết kế ghi "dấu 52 x 48"; code theo plan.
- **Việc còn lại:** cập nhật 3 test của `figma-export` để không phụ thuộc giá trị của hệ thống thiết kế, rồi chạy lại T-002 từ bước 5 trên nhánh cũ.
- **Đề xuất hướng xử lý:** cách gọn nhất là một PR riêng (người làm, hoặc plan rủi ro cao duyệt chéo vì chạm `scripts/`) cho `figma-export.py` đọc đường dẫn `tokens.json` từ biến môi trường, và `scripts/tests/run.sh` dùng một `tokens.json` cố định trong `scripts/tests/fixtures/`. Sau khi merge, plan T-002 phiên bản 2 ghi "Tiếp tục trên nhánh cũ `claude/T-002-thuong-hieu-xanh-luc`", không đổi phần code. Bài học L-020.
- **Cần người xem:** 12 ảnh chuẩn trên nhánh (`core/designsystem/src/test/screenshots/`, `feature/sample/src/test/screenshots/`, `feature/loading/src/test/screenshots/`), nhất là `DesignSystem_brand_light.png` cạnh ảnh thiết kế `LoadingScreen_progress_light.png`.

### RUN-20261017-01, phiên bản plan 2

- **Kết quả:** đã merge.
- **Nhánh, PR:** `claude/T-002-thuong-hieu-xanh-luc`, PR #39, merge thành commit 2c55a03 trên nhánh chính.
- **Đã làm:** cập nhật nhánh cũ theo nhánh chính (commit b8ecce4, đã có sửa L-020), chạy lại từ bước 5 như mục 3.1. Không đổi code. `scripts/plan-check.py` qua. Mã băm của ba file phông và `LICENSE.txt` vẫn khớp mục 3.1. `scripts/check.sh` qua lần đầu: 94 test JVM 0 lỗi, 12 ảnh screenshot khớp, không file nào đổi khi chạy. 30 file thay đổi so với nhánh chính đúng bằng danh sách ở mục 3.2.
- **Test viết trước:** 6 test, commit 9d23df1 từ RUN-20261016-02, đã thất bại đúng lý do ở phiên đó. Phiên này không viết lại test.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, 6 ghi chú. Nên sửa: `tertiaryContainer` và `onTertiaryContainer` vẫn giữ giá trị mặc định hồng tím của Material 3, vì bảng ở mục 3.1 không có hai vai trò này, trong khi mục 3.1 ghi "Không vai trò nào còn giá trị mặc định tím". Thêm màu cần người duyệt bảng, nên để người quyết. Ghi chú chính: mục 9 chưa điền quyết định duyệt cho phiên bản 2 (phần đầu plan ghi `approved`, ngày 2026-10-17); `DesignTokensTest` viết cứng `"Inter"`; chỗ nối giữa phần đã chạy và rãnh của thanh tiến trình có vết lõm nhỏ so với thiết kế.
- **Kết quả CI:** một vòng, xanh: `changes`, `build-and-test` (gồm build, lint, unit test, screenshot test), `ui-test` trên emulator.
- **Lệch so với plan:** Không có.
- **Việc còn lại:** người quyết màu cho `tertiaryContainer`, `onTertiaryContainer` (plan mới nếu cần). Sau khi merge, dựng lại biến màu và kiểu chữ trên Figma theo `tokens.json` mới (mục 8, câu 4).
- **Cần người xem:** 12 ảnh chuẩn trong PR #39, nhất là `DesignSystem_brand_light.png` cạnh ảnh thiết kế `LoadingScreen_progress_light.png`.
