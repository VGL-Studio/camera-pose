---
ticket: T-007
title: Hệ thống thiết kế theo hướng Sticker
status: draft
version: 1
risk_score:
risk_level: high
token_estimate: 40.0M
blocking_points: [core]
run_order:
reviewer:
cross_reviewer:
approved_at:
---

# PLAN T-007: HỆ THỐNG THIẾT KẾ THEO HƯỚNG STICKER

## 1. Tóm tắt

Đổi `:core:designsystem` từ hệ thống của dự án mẫu (Inter, xanh lục) sang hướng Sticker trong `docs/design-system/sticker.md`: bảng màu sáng và tối, màu riêng của app qua `MaterialTheme.appColors`, phông Bricolage Grotesque, bo góc, độ dày viền và bóng đổ cứng dùng chung, `PrimaryButton`, `ProgressBar`, `BrandLogo` đổi kiểu, thêm `SecondaryButton`. Kiểm chứng bằng `DesignTokensTest` (so `tokens.json` sửa tay theo bảng ở mục 3.1), test giao diện đo kích thước và màu điểm ảnh của từng thành phần, và 19 ảnh chuẩn screenshot ghi lại hoặc tạo mới. Bố cục màn loading không đổi ở plan này (T-008).

## 2. Hiểu yêu cầu

Ticket không xử lý kịch bản nào của `features/loading/spec.md` (`scenarios: []`). Nó chuẩn bị nền cho mọi màn hình theo thiết kế Sticker. Tiêu chí lấy từ phạm vi của ticket:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Phạm vi 1 | Bảng màu sáng và tối theo `sticker.md`, mục 1, gồm màu riêng của app | Mọi vai trò Material 3 lấy giá trị của bảng; vai trò ghi "xem Figma" lấy từ `design/context/` khi có, còn lại AI đề xuất (mục 3.1, mục 8 câu 2). Màu riêng nằm trong `AppColors`, đọc qua `MaterialTheme.appColors` |
| Phạm vi 2 | Phông Bricolage Grotesque trong `res/font`, độ đậm theo mục 2 | Bốn file tĩnh 400, 500, 600, 800. Cỡ chữ, chiều cao dòng giữ như `tokens.json` hiện tại, giãn chữ 0 |
| Phạm vi 3 | Bo góc, độ dày viền, bóng `shadow/sticker` thành giá trị dùng chung | `Radius`, `Stroke`, `StickerShadow` và `Modifier.stickerShadow` trong `theme/Shape.kt` |
| Phạm vi 4 | Đổi kiểu `PrimaryButton`, `ProgressBar`, `BrandLogo`; thêm `SecondaryButton` | Theo `sticker.md`, mục 4 và bảng ánh xạ của `features/loading/design/README.md`, mục 3 |
| Phạm vi 5 | Sinh lại `tokens.json`, ghi lại ảnh catalog | Bằng `DesignTokensTest` và `./gradlew recordRoborazziDebug`. Xuất lại `context/` bằng `scripts/figma-export.py` là việc của phiên có người theo dõi, sau khi merge, không thuộc phiên tự chạy (mục 6) |

Thiết kế tham chiếu: `docs/design-system/sticker.md` (nguồn của mọi giá trị); `features/loading/design/README.md`, mục 3 (logo, thanh tiến trình, nút chính); số liệu trong `features/loading/design/context/LoadingScreen_failed_light.md` và `_dark.md` (nút, logo, thanh tiến trình), `features/poses/design/context/PosesScreen_ad_light.md`, `PoseDetailScreen_loading_light.md` và bản tối (màu vùng quảng cáo và khung xương).

ADR liên quan: ADR-0008 (code là gốc của hệ thống thiết kế; đổi là rủi ro cao), ADR-0020 (catalog, `tokens.json`, khung chuẩn 411 x 891).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu của plan T-002 (cùng module): giá trị đặt trong `theme/`, thành phần trong `component/`, catalog chụp bằng screenshot test, `tokens.json` sinh từ `DesignTokensTest`.

**Màu Material 3 (`Color.kt`).** Thay toàn bộ `LightColors`, `DarkColors`. Cột "Nguồn": "sticker" là `sticker.md`, mục 1; "context" là giá trị đọc được trong `design/context/` (mục 2); "AI" là giá trị AI đề xuất cho vai trò `sticker.md` ghi "xem Figma" mà repo không có số (mục 8, câu 2).

| Vai trò | Sáng | Tối | Nguồn |
| --- | --- | --- | --- |
| `primary`, `onPrimary` | #2F6BFF, #FFFFFF | #6E9BFF, #0A0F2A | sticker |
| `primaryContainer`, `onPrimaryContainer` | #DCE6FF, #0A2A7A | #1C3277, #DCE6FF | sticker |
| `secondary`, `onSecondary` | #111111, #FFFFFF | #FFFFFF, #111111 | sticker |
| `secondaryContainer`, `onSecondaryContainer` | #FFC83D, #111111 | #FFC83D, #111111 | sticker |
| `tertiary`, `onTertiary` | #FF5C8A, #111111 | #FF8AAA, #3A0A18 | sticker |
| `tertiaryContainer`, `onTertiaryContainer` | #FFD6E2, #4A0F22 | #6B1D35, #FFD6E2 | sticker |
| `error`, `onError` | #C8322B, #FFFFFF | #FF8A80, #3D0704 | sticker |
| `errorContainer`, `onErrorContainer` | #FFDAD5, #5C0F0A | #5C1A14, #FFDAD5 | sticker |
| `background`, `onBackground` | #FFFDF8, #111111 | #121212, #FFFFFF | sticker |
| `surface`, `onSurface` | #FFFFFF, #111111 | #1E1E1E, #FFFFFF | sticker |
| `surfaceVariant`, `onSurfaceVariant` | #F3F0E8, #5A5650 | #2A2A2A, #B5B0A8 | sticker |
| `outline` | #111111 | #FFFFFF | sticker |
| `outlineVariant` | #D9D4C8 | #4A4744 | sticker |
| `inverseSurface`, `inverseOnSurface`, `inversePrimary` | #1E1E1E, #FFFFFF, #9DB8FF | #F3F0E8, #111111, #2F6BFF | sticker |
| `surfaceContainerHigh` | #F0ECE3 | #262626 | Figma (biến `color`) |
| `surfaceContainerHighest` | #EAE6DC | #2E2E2E | Figma (biến `color`) |
| `surfaceContainerLowest` | #FFFFFF | #0C0C0C | Figma (biến `color`) |
| `surfaceContainerLow` | #FBF8F1 | #181818 | Figma (biến `color`) |
| `surfaceContainer` | #F6F3EB | #1E1E1E | Figma (biến `color`) |
| `surfaceBright` | #FFFFFF | #333333 | Figma (biến `color`) |
| `surfaceDim` | #E8E4DA | #121212 | Figma (biến `color`) |
| `surfaceTint` | #2F6BFF | #6E9BFF | Figma (biến `color`), bằng `primary` |

Chú thích đầu file đổi thành: bảng màu Sticker của Camera Pose, nguồn `docs/design-system/sticker.md`, mọi vai trò đều lấy từ biến `color` trên Figma (`sticker.md`, mục 1).

**Màu riêng của app (`AppColors.kt`, mới).** Một `@Immutable data class AppColors` với các trường dưới, hai bản `internal val LightAppColors`, `DarkAppColors`, và `val LocalAppColors = staticCompositionLocalOf { LightAppColors }`. Feature đọc qua thuộc tính mở rộng `val MaterialTheme.appColors: AppColors @Composable @ReadOnlyComposable get() = LocalAppColors.current`, cùng cách gọi với `MaterialTheme.colorScheme`. Lý do chọn cách này: Material 3 không có chỗ cho vai trò thêm; `CompositionLocal` đổi theo chế độ sáng, tối như `colorScheme`; một lớp có tên trường rõ ràng giúp feature không phải viết cứng mã màu (ADR-0008). `staticCompositionLocalOf` vì giá trị chỉ đổi khi đổi chế độ.

| Trường | Tên trong `tokens.json` | Sáng | Tối |
| --- | --- | --- | --- |
| `success`, `onSuccess` | `success`, `onSuccess` | #1F9D63, #FFFFFF | #3DDC97, #0B2E1E |
| `successContainer`, `onSuccessContainer` | `successContainer`, `onSuccessContainer` | #3DDC97, #111111 | như sáng |
| `shadowSticker` | `shadowSticker` | #FFC83D | như sáng |
| `categoryWomen`, `categoryMen`, `categoryCouple`, `categoryFriends`, `categoryFamily`, `categoryKids`, `categoryTravel`, `categoryCafe` | `category/women`, `category/men`, `category/couple`, `category/friends`, `category/family`, `category/kids`, `category/travel`, `category/cafe` | #FF5C8A, #FFC83D, #3DDC97, #4D8DFF, #5ED3F3, #FFA8D0, #FF8A3D, #B8E986 | như sáng |
| `onCategory` | `category/onCategory` | #111111 | như sáng |
| `cameraBackground`, `cameraSurface` | `camera/background`, `camera/surface` | #0A0A0B, #1E1E1E | như sáng |
| `onCamera`, `cameraOnSurfaceVariant` | `camera/onCamera`, `camera/onSurfaceVariant` | #FFFFFF, #B5B0A8 | như sáng |
| `cameraControl`, `cameraOnControl` | `camera/control`, `camera/onControl` | #FFFFFF, #111111 | như sáng |
| `cameraOverlayStroke`, `cameraScrim` | `camera/overlayStroke`, `camera/scrim` | #FFFFFF, #000000 | như sáng |

Độ mờ của `camera/scrim` (35%, 45%, 50%) là việc của màn camera, không phải giá trị của hệ thống. Chế độ tối luôn bật cho camera cũng là việc của T-013: màu camera ở đây giống nhau ở hai chế độ nên màn camera dùng được ngay.

**`Theme.kt`.** `AppTheme` bọc `MaterialTheme` trong `CompositionLocalProvider(LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors)`. Chữ ký `AppTheme(darkTheme, content)` không đổi, nên `MainActivity` và test của feature không phải sửa.

**Bo góc, viền, bóng (`Shape.kt`, mới).**

- `object Radius`: `image` 10, `button` 14, `thumb` 14, `card` 16, `chip` 18, `sheet` 24, `full` 999 (dp), và `val scale: List<Pair<String, Dp>>` theo thứ tự đó, như `Spacing.scale`.
- `object Stroke`: `thin` 1, `sticker` 2, `selected` 3 (dp), và `scale`.
- `object StickerShadow`: `offset` 4 dp (cả hai trục), không mờ.
- `fun Modifier.stickerShadow(shape: Shape, color: Color): Modifier = drawBehind { translate(offsetPx, offsetPx) { drawOutline(shape.createOutline(size, layoutDirection, this), color) } }`. Bóng vẽ ra ngoài khung nhưng không chiếm chỗ trong bố cục, nên vị trí các phần tử khác không đổi. Người gọi truyền `MaterialTheme.appColors.shadowSticker`.

**Phông Bricolage Grotesque.** Google Fonts chỉ có bản biến thiên (`ofl/bricolagegrotesque/BricolageGrotesque[opsz,wdth,wght].ttf`). Bản tĩnh nằm ở repo gốc mà Google Fonts ghi trong `METADATA.pb`, cùng commit 84745e5b96261ae5f8c6c856e262fe78d1d6efdd. Dùng bản tĩnh vì `res/font` với `Font(resId, weight)` cần mỗi độ đậm một file trên minSdk 26, và không cần thêm thư viện. Tải, kiểm tra mã băm (AI đã tải thử ngày 2026-10-21 để lấy mã băm):

```sh
C=84745e5b96261ae5f8c6c856e262fe78d1d6efdd
mkdir -p /tmp/bricolage
for w in Regular Medium SemiBold ExtraBold; do
  curl -fL -o /tmp/bricolage/BricolageGrotesque-$w.ttf \
    "https://raw.githubusercontent.com/ateliertriay/bricolage/$C/fonts/ttf/BricolageGrotesque-$w.ttf"
done
curl -fL -o /tmp/bricolage/OFL.txt "https://raw.githubusercontent.com/ateliertriay/bricolage/$C/OFL.txt"
cd /tmp/bricolage && shasum -a 256 -c <<'EOF'
dcfe24ee4e7aa40aa13a91837acca9b170befd4dbbbcf9e084a0db1c1676e06f  BricolageGrotesque-Regular.ttf
1dd2a3b41e0ce8eff2d9000ce8e79e8a5d9d2f0b22f4e27dc8c59e94894fe50a  BricolageGrotesque-Medium.ttf
25534ff95de6305903a3f74b237f07d9e32adefec13d0f0a0d99a4b820c6a8cf  BricolageGrotesque-SemiBold.ttf
20ca28c496aef07993031176c1a02da6959682abbc4cbf73776f83e63ef80c00  BricolageGrotesque-ExtraBold.ttf
4b5a7d8f37f5602621c8a8d7358a6a2e71317e6c231c661e15aef0275d3e07ba  OFL.txt
EOF
```

Chép thành `res/font/bricolage_grotesque_regular.ttf`, `bricolage_grotesque_medium.ttf`, `bricolage_grotesque_semibold.ttf`, `bricolage_grotesque_extrabold.ttf`, và `third_party/bricolage-grotesque/OFL.txt` (SIL OFL 1.1 yêu cầu giấy phép đi kèm). Bốn file khoảng 465 KB. Tải không được (429, 5xx, hết thời gian, mất kết nối) là lỗi hạ tầng, xử lý theo auto-run.md, bước 5. Mã băm sai thì dừng (mục 7). Xóa ba file Inter và `third_party/inter/LICENSE.txt`, vì không còn chỗ nào dùng (khoảng 1,2 MB, mục 8 câu 6).

**Chữ (`Type.kt`).** `internal const val FONT_FAMILY_NAME = "Bricolage Grotesque"` (thay chữ `"Inter"` viết cứng trong `DesignTokensTest`). `internal val BricolageFamily = FontFamily(Font(R.font.bricolage_grotesque_regular, FontWeight.Normal), Font(..._medium, FontWeight.Medium), Font(..._semibold, FontWeight.SemiBold), Font(..._extrabold, FontWeight.ExtraBold))`. `AppTypography`: mọi kiểu chữ `copy(fontFamily = BricolageFamily, letterSpacing = 0.sp)` như hiện tại, và độ đậm:

| Kiểu chữ | Độ đậm |
| --- | --- |
| `displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`, `headlineMedium`, `headlineSmall`, `titleLarge` | `FontWeight.ExtraBold` (800) |
| `titleMedium` | `FontWeight.SemiBold` (600) |
| `titleSmall`, `labelLarge`, `labelMedium`, `labelSmall` | `FontWeight.Medium` (500) |
| `bodyLarge`, `bodyMedium`, `bodySmall` | `FontWeight.Normal` (400) |

Cỡ chữ và chiều cao dòng giữ thang Material 3 (đúng như `tokens.json` hiện có).

**`PrimaryButton`.** Chữ ký không đổi (`text`, `onClick`, `modifier`, `enabled`, `icon`), nên `SampleScreen` và `LoadingScreen` không phải sửa. `Button` với:

- `shape = RoundedCornerShape(Radius.button)`, `modifier.stickerShadow(shape, appColors.shadowSticker).heightIn(min = 56.dp)` (bóng chỉ vẽ khi `enabled`).
- `colors`: nền `secondary`, chữ `onSecondary`; khi tắt, nền `onSurface` độ mờ 12%, chữ `onSurface` độ mờ 38% (mặc định của Material 3).
- `border = BorderStroke(Stroke.sticker, outline)`, khi tắt dùng `outlineVariant`.
- `elevation = null` (không bóng mềm của Material), `contentPadding = PaddingValues(horizontal = Spacing.lg)`.
- Chữ `MaterialTheme.typography.titleMedium` (16/24, đậm 600, như nút trong `context/`). Biểu tượng 20 dp (thay 18), cách chữ `Spacing.sm`.

Hằng số `private val MinHeight = 56.dp`, `IconSize = 20.dp` trong file. Chiều cao 56 theo `sticker.md` và README thiết kế; `context/` ghi khung 60 (mục 8, câu 1).

**`SecondaryButton` (mới, `component/SecondaryButton.kt`).** Cùng chữ ký, cùng hình với `PrimaryButton` (cao 56, bo `Radius.button`, viền `Stroke.sticker` màu `outline`, chữ `titleMedium`, biểu tượng 20), nhưng nền `surface`, chữ `onSurface`, không có bóng. Thiết kế chưa có màn nào dùng nút phụ, nên màu là đề xuất của AI (mục 8, câu 3). Mỗi nút một file cho dễ tìm; phần lặp lại (biểu tượng và chữ) là composable `internal fun ButtonContent(text, icon)` đặt trong `PrimaryButton.kt`.

**`ProgressBar`.** Chữ ký thêm `borderColor: Color = MaterialTheme.colorScheme.outline` ở cuối. Mặc định `color = secondaryContainer` (thay `primary`), `trackColor = surfaceVariant`. `LinearProgressIndicator` giữ `strokeCap`, `gapSize = 0.dp`, `drawStopIndicator = {}`, modifier thêm `.clip(CircleShape).border(Stroke.thin, borderColor, CircleShape)` sau `height(8.dp)`. Cao vẫn 8 dp, viền nằm trong thanh. Màn loading đang truyền `error`, `errorContainer` khi lỗi, vẫn đúng thiết kế.

**`BrandLogo` (logo tạm).** Ô vuông 112 dp, `RoundedCornerShape(Radius.sheet)`, nền `secondaryContainer`, viền `Stroke.sticker` màu `outline`, `stickerShadow` màu `shadowSticker`. Giữa ô là `brand_mark.xml` hiện có, tô `onSecondaryContainer`, cỡ 56 x 52 dp như cũ. Bỏ vòng tròn 132 dp. Không đổi `brand_mark.xml`: repo không có SVG của hình `pose-outline`, phiên tự chạy không mở Figma (ADR-0020), và README thiết kế cho phép logo tạm khác hình (mục 6). Thay bằng hình dáng người khi có icon app (mục 8, câu 4).

**Catalog (`DesignSystemCatalog.kt`).**

- `ColorCatalog` giữ các dòng hiện có (giá trị mới tự hiện).
- `AppColorCatalog` (mới): 15 ô màu xếp hai cột: `success`, `successContainer`, `shadowSticker`, tám màu nhóm (chữ `category/onCategory`), `camera/background`, `camera/surface`, `camera/control`, `camera/scrim` (chữ là vai trò "on" tương ứng).
- `ShapeCatalog` (mới): bảy mẫu bo góc (ô 64 x 40, viền `Stroke.sticker`, nhãn `image 10 dp`...), ba mẫu độ dày viền, một ô 112 dp có `stickerShadow`.
- `SpacingAndComponentCatalog` thêm `SecondaryButton` và `SecondaryButton` tắt sau hai `PrimaryButton`.
- `BrandCatalog` giữ nguyên nội dung (logo, khẩu hiệu, hai thanh tiến trình, nút có biểu tượng).

`DesignSystemCatalogScreenshotTest` thêm `appColors_light`, `appColors_dark`, `shapes_light`, `spacingAndComponents_dark` (nút chính màu trắng ở chế độ tối cần ảnh riêng).

**`tokens.json`.** Cấu trúc mới (khóa cũ giữ nguyên chỗ, thêm khóa ở cuối):

```json
{
  "fontFamily": "Bricolage Grotesque",
  "color": {
    "light": {<35 vai trò Material 3 như cũ>, "success": "#1F9D63", ..., "camera/scrim": "#000000"},
    "dark": {...}
  },
  "typography": {...},
  "spacingDp": {"xs": 4, "sm": 8, "md": 16, "lg": 24, "xl": 32},
  "radiusDp": {"image": 10, "button": 14, "thumb": 14, "card": 16, "chip": 18, "sheet": 24, "full": 999},
  "strokeDp": {"thin": 1, "sticker": 2, "selected": 3},
  "shadow": {"sticker": {"offsetXDp": 4, "offsetYDp": 4, "blurDp": 0, "color": "shadowSticker"}}
}
```

Màu riêng của app nằm ngay trong `color.light` và `color.dark`, sau 35 vai trò Material 3, theo thứ tự của bảng `AppColors` ở trên, tên như cột "Tên trong `tokens.json`" (trùng tên biến trên Figma). Nhờ vậy `scripts/figma-export.py` nhận ra các màu này theo giá trị khi xuất lại `context/`, thay vì ghi "ngoài hệ thống". Lớp `Tokens` của script chỉ đọc `color`, `typography`, `spacingDp` và đọc mỗi giá trị màu như một chuỗi, nên khóa mới không làm hỏng script.

Hàm phụ trợ trong `DesignTokensTest.kt` (L-025), tất cả vào commit test của bước 3: `tokensJson()` in thêm màu app, `radiusDp`, `strokeDp`, `shadow`, và dùng `FONT_FAMILY_NAME`; hàm mới `appColorRoles(colors: AppColors): List<Pair<String, Color>>` liệt kê 22 màu theo thứ tự trên. `colorRoles(scheme)` không đổi, nên `colorRoles_listEveryRoleOfColorScheme` vẫn chỉ so vai trò Material 3. Để commit test biên dịch được, bước 3 tạo khung rỗng (xem mục 4): `AppColors` đủ trường, `LightAppColors` và `DarkAppColors` mọi trường `Color.Transparent`, `LocalAppColors`, `MaterialTheme.appColors`; `Radius`, `Stroke`, `StickerShadow` mọi giá trị `0.dp`, `scale` đủ tên; `stickerShadow` trả về `this`; `FONT_FAMILY_NAME = "Inter"`; `BricolageFamily = FontFamily.Default`; `SecondaryButton` chỉ là `Box(modifier)`. Commit test cũng chứa `tokens.json` sửa tay theo đúng các bảng ở mục này. Bước 4 viết giá trị thật, rồi sinh lại `tokens.json` bằng `./gradlew :core:designsystem:testDebugUnitTest -PdesignTokens.update=true`: `git diff` của `tokens.json` so với commit test phải rỗng.

Thứ tự làm: test và khung rỗng (mục 4), chạy để thấy thất bại đúng lý do, commit; tải phông; màu, `AppColors`, `Shape.kt`, chữ, `Theme.kt`; thành phần; catalog; sinh `tokens.json`; ghi lại đúng 19 ảnh ở mục 3.2 bằng `./gradlew recordRoborazziDebug`; `scripts/check.sh`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Color.kt` | Bảng màu Sticker sáng và tối, đủ vai trò ở mục 3.1 |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/AppColors.kt` | `AppColors`, `LightAppColors`, `DarkAppColors`, `LocalAppColors`, `MaterialTheme.appColors` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Shape.kt` | `Radius`, `Stroke`, `StickerShadow`, `Modifier.stickerShadow` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Type.kt` | `FONT_FAMILY_NAME`, `BricolageFamily`, độ đậm mới của `AppTypography` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Theme.kt` | Cung cấp `LocalAppColors` theo chế độ |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PrimaryButton.kt` | Kiểu Sticker, `ButtonContent` dùng chung |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/SecondaryButton.kt` | Thành phần `SecondaryButton` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/ProgressBar.kt` | Màu mặc định `secondaryContainer`, viền `outline` 1, tham số `borderColor` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/BrandLogo.kt` | Ô vàng 112, bo 24, viền 2, bóng cứng |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | `AppColorCatalog`, `ShapeCatalog`, `SecondaryButton` trong trang thành phần |
| Tạo | `core/designsystem/src/main/res/font/bricolage_grotesque_regular.ttf` | Bricolage Grotesque 400 |
| Tạo | `core/designsystem/src/main/res/font/bricolage_grotesque_medium.ttf` | Bricolage Grotesque 500 |
| Tạo | `core/designsystem/src/main/res/font/bricolage_grotesque_semibold.ttf` | Bricolage Grotesque 600 |
| Tạo | `core/designsystem/src/main/res/font/bricolage_grotesque_extrabold.ttf` | Bricolage Grotesque 800 |
| Tạo | `core/designsystem/third_party/bricolage-grotesque/OFL.txt` | Giấy phép SIL OFL 1.1 |
| Xóa | `core/designsystem/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf` | Inter không còn dùng |
| Xóa | `core/designsystem/third_party/inter/LICENSE.txt` | Giấy phép của Inter |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignTokensTest.kt` | `FONT_FAMILY_NAME`, `appColorRoles`, khóa `radiusDp`, `strokeDp`, `shadow` |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/ComponentsTest.kt` | Test ở mục 4, hàm `pixelAt` |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/AppThemeTest.kt` | Test của `appColors` và phông |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt` | Bốn test ảnh mới |
| Sửa | `docs/design-system/tokens.json` | Sửa tay ở bước 3, sinh lại ở bước 4 |
| Ghi lại | `core/designsystem/src/test/screenshots/DesignSystem_colors_light.png`, `DesignSystem_colors_dark.png`, `DesignSystem_typography_light.png`, `DesignSystem_spacing_components_light.png`, `DesignSystem_brand_light.png`, `DesignSystem_brand_dark.png` | Màu, phông, thành phần mới |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_app_colors_light.png`, `DesignSystem_app_colors_dark.png`, `DesignSystem_shapes_light.png`, `DesignSystem_spacing_components_dark.png` | Trang catalog mới |
| Ghi lại | `feature/loading/src/test/screenshots/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`, `LoadingScreen_progress_small.png`, `LoadingScreen_failed_small.png` | Màu, phông, logo, nút, thanh tiến trình mới; code màn hình không đổi (T-008 đổi) |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`, `SampleScreen_disabled_light.png` | Màu, phông, nút mới; bố cục không đổi |

Không file nào khác. Đặc biệt không sửa `core/designsystem/build.gradle.kts`, `gradle/libs.versions.toml`, `brand_mark.xml`, `core/designsystem/src/main/res/values/strings.xml`, code và test giao diện của `:feature:loading`, `:feature:sample`, `:app`.

Đây là toàn bộ ảnh chuẩn screenshot của repo (`find . -path '*src/test/screenshots/*.png'`): 6 ảnh catalog, 6 ảnh loading, 3 ảnh mẫu. `:app` không có screenshot test.

**Nơi khác đọc các file trên** (docs/CLAUDE.md, mục Khi viết plan; L-020), không cần sửa:

- `docs/design-system/tokens.json`: `core/designsystem/build.gradle.kts` chỉ truyền đường dẫn cho `DesignTokensTest`. `scripts/figma-export.py` đọc file khi người xuất thiết kế; lớp `Tokens` chỉ đọc `color`, `typography`, `spacingDp`, nên khóa mới bị bỏ qua và màu mới được đối chiếu như chuỗi. Test của script trong `scripts/tests/run.sh` dùng `scripts/tests/fixtures/tokens.json` cố định (`FIGMA_EXPORT_TOKENS`), không đọc file thật.
- Ảnh chuẩn: chỉ `DesignSystemCatalogScreenshotTest.kt` (trong danh sách), `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenScreenshotTest.kt` và `feature/sample/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/sample/SampleScreenScreenshotTest.kt` ghi các ảnh này. Hai test feature không đổi, chỉ ảnh đổi.
- Phông Inter: chỉ `Type.kt` (trong danh sách) dùng `R.font.inter_*`.
- `BrandLogo`, `PrimaryButton`, `ProgressBar`: `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreen.kt` và `feature/sample/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/sample/SampleScreen.kt` gọi với chữ ký cũ, chữ ký giữ nguyên. `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` kiểm tra nút "Thử lại" cao ít nhất 48 và nằm trong màn hình thấp; logo thấp đi 20 dp, nút cao thêm 4 dp, nên tổng chiều cao giảm. Test này phải qua mà không sửa (mục 7).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm. `surfaceContainerHigh` chỉ là màu nền mà vùng quảng cáo sẽ dùng sau này.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

Mọi test dưới đây và khung rỗng ở mục 3.1 vào commit test của bước 3. Lý do thất bại ghi ở cột cuối là điều AI dự kiến trên code hiện tại; bước 3 chạy và ghi lý do thật vào nhật ký. `ComponentsTest` và `AppThemeTest` có `@GraphicsMode(GraphicsMode.Mode.NATIVE)` (như hiện tại), không đặt `@Config`. Hàm `pixelAt(tag, x: Dp, y: Dp): Color` trong `ComponentsTest` đổi dp sang px bằng `composeRule.density`, đọc `onNodeWithTag(tag).captureToImage().toPixelMap()[x, y]`. So màu bằng `assertEquals(expected.toArgb(), actual.toArgb())`. Điểm đo luôn nằm giữa vùng màu, xa mép và góc bo, để không dính khử răng cưa.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Phạm vi 1, 2, 3: giá trị màu, chữ, bo góc, viền, bóng | Unit test | `DesignTokensTest.tokensFile_matchesTheDesignSystem`: `tokens.json` sửa tay theo mọi bảng ở mục 3.1 (vai trò Material 3, 22 màu app, độ đậm, `radiusDp`, `strokeDp`, `shadow`, `"fontFamily": "Bricolage Grotesque"`). Thất bại vì code còn giá trị xanh lục, `"Inter"`, và khung rỗng in `#000000` (từ `Color.Transparent`) và `0`. Sau bước 4, file sinh lại phải trùng file sửa tay |
| Đủ vai trò Material 3 | Unit test | `DesignTokensTest.colorRoles_listEveryRoleOfColorScheme` giữ nguyên, phải vẫn qua |
| Màu app theo chế độ | Compose UI test | `AppThemeTest.appColors_followTheme`: trong `AppTheme(darkTheme = false)` đọc `MaterialTheme.appColors.success` là #1F9D63, `shadowSticker` là #FFC83D; trong `AppTheme(darkTheme = true)` `success` là #3DDC97. Thất bại vì khung rỗng trả `Color.Transparent` |
| Phông Bricolage Grotesque đủ bốn độ đậm | Unit test | `AppThemeTest.bricolageFamily_hasFourWeights`: `(BricolageFamily as? FontListFontFamily)?.fonts?.map { it.weight.weight }` bằng `[400, 500, 600, 800]`. Thất bại vì khung rỗng là `FontFamily.Default` (kết quả `null`) |
| Mọi kiểu chữ dùng phông mới | Unit test | `AppThemeTest.typography_everyStyleUsesBricolage`: 15 kiểu chữ của `AppTypography` có `fontFamily == BricolageFamily`. Thất bại vì đang là `InterFamily` |
| Nút chính cao 56 | Compose UI test | `ComponentsTest.primaryButton_height_is56dp` thay `primaryButton_height_isAtLeast52dp`: `assertHeightIsEqualTo(56.dp)`. Thất bại: đo được 52 |
| Nút chính có biểu tượng 20 | Compose UI test | `ComponentsTest.primaryButton_withIcon_showsIconAndText_andClicks`: đổi độ lệch chữ mong đợi từ 26 sang 28 (20 biểu tượng + 8 khoảng cách), còn lại giữ. Thất bại: 26 |
| Nút chính đen ở chế độ sáng, trắng ở chế độ tối | Compose UI test | `ComponentsTest.primaryButton_fill_isSecondary_light` và `_dark`: `pixelAt(tag, 10.dp, 28.dp)` (trong lề trái 24, giữa chiều cao) là #111111 và #FFFFFF. Thất bại: đang là #147D68 và #84DCC1 |
| Bóng cứng lệch 4, màu `shadowSticker` | Compose UI test | `ComponentsTest.primaryButton_drawsHardShadow`: `Box(Modifier.testTag(...).padding(8.dp)) { PrimaryButton(...) }`, điểm ở giữa chiều ngang, dưới đáy nút 2 dp là #FFC83D. Thất bại: không có bóng (điểm trong suốt) |
| Nút phụ cao 56, bấm được | Compose UI test | `ComponentsTest.secondaryButton_height_is56dp_andClicks`: `assertHeightIsEqualTo(56.dp)`, bấm chữ gọi `onClick` một lần. Thất bại: khung rỗng cao 0, không có chữ |
| Nút phụ nền `surface`, không bóng | Compose UI test | `ComponentsTest.secondaryButton_fill_isSurface_dark`: `pixelAt(tag, 10.dp, 28.dp)` là #1E1E1E; `secondaryButton_hasNoShadow`: điểm dưới đáy nút 2 dp (như test bóng ở trên) khác #FFC83D. Test đầu thất bại vì khung rỗng trong suốt. Test sau qua ngay ở bước 3 vì khung rỗng không vẽ gì; đây là test chặn bóng lọt vào nút phụ khi viết code, nên giữ, ghi vào nhật ký |
| Thanh tiến trình vàng | Compose UI test | `ComponentsTest.progressBar_defaultFill_isSecondaryContainer`: `Box(Modifier.width(200.dp).testTag(...)) { ProgressBar(progress = { 0.68f }) }`, `pixelAt(tag, 40.dp, 4.dp)` là #FFC83D. Thất bại: #147D68. `progressBar_reportsProgress` và `progressBar_height_is8dp` giữ nguyên, phải vẫn qua |
| Logo 112, nền vàng | Compose UI test | `ComponentsTest.brandLogo_is112dpSquare` thay `brandLogo_is132dpSquare`. Thất bại: 132. `brandLogo_fill_isSecondaryContainer`: `pixelAt(tag, 56.dp, 8.dp)` là #FFC83D. Thất bại: #EBF3EE (vòng `primaryContainer`) |
| Giao diện catalog | Screenshot test | Bốn test mới trong `DesignSystemCatalogScreenshotTest`: `appColors_light`, `appColors_dark`, `shapes_light`, `spacingAndComponents_dark`, ghi `src/test/screenshots/DesignSystem_app_colors_light.png`, `DesignSystem_app_colors_dark.png`, `DesignSystem_shapes_light.png`, `DesignSystem_spacing_components_dark.png`, khung `w411dp-h891dp-xxhdpi` của lớp |
| Ảnh chuẩn đổi đúng phạm vi | Screenshot test | `./gradlew recordRoborazziDebug` ghi lại đúng 15 ảnh "Ghi lại" và tạo 4 ảnh "Tạo" ở mục 3.2, không ảnh nào khác đổi; rồi `scripts/check.sh` xanh |
| Phông vẽ được, đủ dấu tiếng Việt | Screenshot test, người duyệt xem | `DesignSystem_typography_light.png`: tiêu đề đậm 800, nét Bricolage (chữ "g" một tầng, "a" hai tầng, mép vuông). `LoadingScreen_failed_light.png`: "Không thể tải dữ liệu", "Kết nối đang gián đoạn" đủ dấu, không ô vuông |
| Màn hình thấp (L-028) | Test giao diện có sẵn | `LoadingScreenTest` giữ nguyên và phải qua (nút "Thử lại" trong màn hình 360 x 640, chân trang dưới nút); ảnh `LoadingScreen_failed_small.png` ghi lại |
| Gần thiết kế | Kiểm tra thủ công | Mở `DesignSystem_brand_light.png` cạnh `docs/features/loading/design/screens/LoadingScreen_failed_light.png`: logo vàng viền đen có bóng, nút đen bóng vàng, thanh vàng viền đen. Hình trong logo khác (logo tạm, mục 3.1) |

**Ngoại lệ viết test trước:** bốn file phông và `OFL.txt` là tài nguyên tải về, không viết test trước được; `AppThemeTest` kiểm tra gián tiếp (bốn độ đậm). Ảnh chuẩn screenshot là kết quả, không viết trước được; người duyệt xem trên PR. Xóa file Inter không có test riêng; `scripts/check.sh` (build, Android Lint) báo nếu còn chỗ dùng. Test `secondaryButton_hasNoShadow` qua ngay ở bước 3 (lý do ở bảng trên).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core dùng chung `:core:designsystem`). Rủi ro cao, cần duyệt chéo. Nhóm hiện có một dev, nên áp dụng ngoại lệ ở risk-scoring.md, mục 3: người duyệt vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9. Điểm dưới đây để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Sửa một module core, đổi giao diện công khai (`appColors`, `Radius`, `Stroke`, `SecondaryButton`) và ảnh của mọi màn hình ở hai module feature |
| B. Độ rõ của yêu cầu | 1 | `sticker.md` có đủ giá trị chính; màu nút phụ, chênh 56 và 60 của nút (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Giá trị, kích thước và màu điểm ảnh có test; độ giống thiết kế và nét phông phải xem ảnh |
| D. Ảnh hưởng chéo | 3 | Mọi màn hình đổi giao diện; T-008, T-011, T-012, T-013, T-021, T-025 dựa vào hệ thống mới |
| E. Độ mới của kỹ thuật | 2 | `CompositionLocal` cho màu riêng, bóng tự vẽ bằng `drawBehind`, test đọc điểm ảnh bằng `captureToImage` đều chưa có trong dự án |
| **Tổng** | 10 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AppTheme`, `PrimaryButton` (dùng ở `SampleScreen`, `LoadingScreen`), `ProgressBar`, `BrandLogo` (dùng ở `LoadingScreen`), `tokens.json`, mọi ảnh catalog.
- Ảnh chuẩn của feature đổi: 6 ảnh của `:feature:loading`, 3 ảnh của `:feature:sample` (mục 3.2). Màn loading sau plan này có màu, phông mới nhưng bố cục cũ, và phần trăm vẫn màu `primary` (xanh dương) thay vì `onSurface` như thiết kế; T-008 sửa.
- Task phụ thuộc: T-008, T-011, T-012, T-013, T-021, T-025 ghi `depends_on: [T-007]`. Các task này dùng `appColors`, `Radius`, `Stroke`, `stickerShadow`, `SecondaryButton` và ghi lại ảnh loading (T-008).
- T-006 (đã duyệt) chỉ thêm test vào `LoadingScreenTest.kt` và chú thích `LoadingScreenScreenshotTest.kt`, không chạm file của plan này. Chạy trước hay sau đều được; chạy sau thì test của T-006 đo trên kích thước mới.
- Khẩu hiệu `brand_tagline` vẫn là của dự án mẫu ("Mọi việc vào đúng nhịp."), plan này không đổi (mục 8, câu 7).
- Việc sau khi merge, trong phiên có người theo dõi (không thuộc phiên tự chạy, không có file nào trong mục 3.2): dựng lại biến bo góc, viền và màu app trên Figma nếu còn lệch `tokens.json` mới; chạy `scripts/figma-export.py` cho sáu gói thiết kế để `context/` hết "ngoài hệ thống"; cập nhật đoạn "Hệ thống thiết kế trong code hiện là của dự án mẫu" trong `docs/design-system/sticker.md`. Phiên tự chạy ghi các việc này vào mục Việc còn lại của nhật ký.
- Đề xuất thứ tự chạy: T-007 chạy đầu tiên trong chuỗi Sticker, trước mọi task phụ thuộc. T-009 (`:core:poses`) không phụ thuộc T-007, có thể xếp trước để phiên có task độc lập.

## 7. Điều kiện dừng riêng

1. Mã băm của một trong năm file tải về không khớp mục 3.1, hoặc commit 84745e5b96261ae5f8c6c856e262fe78d1d6efdd không còn tệp ở đường dẫn đã ghi.
2. Robolectric không vẽ được phông trong `res/font` (ảnh vẫn ra Roboto hay Inter), hoặc chữ tiếng Việt hiện ô vuông.
3. `captureToImage` không chạy được trong Robolectric chế độ `NATIVE` (lỗi hay ảnh rỗng): dừng, không thay test đọc điểm ảnh bằng cách khác.
4. `LoadingScreenTest` hay `SampleScreenTest` thất bại sau khi đổi hệ thống (ví dụ nút "Thử lại" ra ngoài màn hình thấp): dừng, không sửa code hay test của feature.
5. Ảnh chuẩn đổi ngoài 15 ảnh "Ghi lại" ở mục 3.2, hoặc bố cục màn mẫu, màn loading đổi ngoài màu, phông, logo, nút và thanh tiến trình.
6. Cần đổi chữ ký công khai của `PrimaryButton`, `ProgressBar` (ngoài tham số `borderColor` thêm ở cuối) hay `BrandLogo`.
7. Bóng `stickerShadow` bị cắt bởi vùng cắt của `Button` mà không sửa được bằng thứ tự modifier: dừng, không tự vẽ nút bằng `Box` thay `Button`.

## 8. Câu hỏi cho người duyệt

1. **Chiều cao nút: 56 hay 60.** `sticker.md` và README thiết kế ghi 56; `context/` của mọi màn ghi khung nút 60 (lề 16 + chữ 24 + lề 16, cộng viền 2 mỗi phía tính vào bố cục trên Figma). Đề xuất: 56 như hai tài liệu, viền nằm trong 56; sau đó tắt "strokes included in layout" trên Figma để `context/` ra 56 khi xuất lại.
2. **Bảy vai trò trước đây ghi "xem Figma" trong `sticker.md`.** Đã giải quyết khi viết plan: AI lấy giá trị từ biến `color` trên Figma (do AI tạo ở bước thiết kế) và ghi vào `sticker.md`, mục 1, và bảng ở mục 3.1. Không cần trả lời.
3. **Kiểu `SecondaryButton`.** Chưa màn nào trên thiết kế dùng nút phụ. Đề xuất: nền `surface`, chữ `onSurface`, viền `outline` 2, không bóng, cùng cỡ với nút chính.
4. **Hình trong logo tạm.** Thiết kế dùng hình dáng người (`pose-outline`, 40 x 80, nét 3), repo không có SVG. Đề xuất: giữ `brand_mark.xml` hiện có, tô `onSecondaryContainer`, cho tới khi có icon app. Nếu anh muốn hình dáng người ngay, xuất SVG vào `docs/features/loading/design/handoff/` trước, AI thêm vào plan.
5. **Tải phông trong phiên tự chạy.** Lệnh ở mục 3.1 tải từ `raw.githubusercontent.com`, có kiểm tra mã băm; T-002 đã tải phông Inter từ GitHub trong phiên tự chạy và qua. Đề xuất: để phiên tải. Nếu môi trường chặn tên miền này, phiên coi là lỗi hạ tầng; anh có thể chép sẵn bốn file vào nhánh trước khi chạy.
6. **Bản phông tĩnh và xóa Inter.** Bản tĩnh `BricolageGrotesque-*.ttf` là một cỡ quang học cố định; Figma dùng bản biến thiên nên chữ rất lớn có thể khác nét đôi chút (README cho phép khác "cách vẽ chữ"). Đề xuất: chấp nhận; xóa ba file Inter cho APK nhẹ hơn khoảng 1,2 MB.
7. **Khẩu hiệu.** `brand_tagline` trong `:core:designsystem` vẫn là của dự án mẫu, thiết kế ghi "Tạo dáng theo nét viền, chụp đẹp ngay.". Đề xuất: plan này không đổi; T-008 quyết định đặt khẩu hiệu ở module feature (cùng 7 ngôn ngữ) hay đổi trong core.
8. **Nút khi tắt.** Thiết kế không có trạng thái tắt. Đề xuất: màu tắt mặc định của Material 3, viền `outlineVariant`, không bóng.

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
