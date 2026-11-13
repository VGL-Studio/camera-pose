---
ticket: T-095
title: Logo màn loading dùng biểu tượng app
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn 7, không tính tổng
risk_level: high             # low | medium | high
token_estimate: 7.1M         # scripts/token-usage.py estimate, cỡ S
blocking_points: [core]      # điểm 7: module core dùng chung (:core:designsystem)
run_order: RUN-20261113-01, 1
reviewer: Nhật
cross_reviewer: Nhật           # nhóm một dev (risk-scoring.md, mục 3)
approved_at: 2026-11-13
lane:                        # phiên tự chạy
---

# PLAN T-095: LOGO MÀN LOADING DÙNG BIỂU TƯỢNG APP

## 1. Tóm tắt

`BrandLogo` của `:core:designsystem` đổi từ ô vàng có viền, bóng sticker và hình người nét đen thành ảnh biểu tượng app lấp kín ô 112 dp, bo `Radius.sheet`, không viền, không bóng. Ảnh là `brand_logo.webp` (448 x 448 px, khoảng 29 KB) chép từ `docs/brand/app-icon/brand-logo-448.webp`, đã tạo sẵn trong PR của plan này. Kiểm bằng test điểm ảnh ở `ComponentsTest` (nền là ảnh, không viền, không bóng), test vị trí có sẵn của màn loading, và screenshot test cùng tên bốn ảnh thiết kế.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/loading/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Khi màn hình loading hiện (đang tải hay báo lỗi, giao diện sáng hay tối) thì logo là biểu tượng app, cùng hình với biểu tượng trên màn hình chính: nền cam, máy ảnh và ảnh tư thế hiện trọn, bo góc, không viền, không bóng, cỡ 112 dp, cùng chỗ như logo cũ; không còn hình người nét đen trên ô vàng | Ảnh nguồn là `play-store-512.png` (ảnh vuông, nền cam kín mép), thu về 448 px (112 dp ở `xxxhdpi`). Bo góc bằng cách cắt ô theo `RoundedCornerShape(Radius.sheet)`, như Figma (bo 24). Logo giữ đầu ở 200 dp, cao 112 dp như cũ |
| KB-1 | Khi mở app thì màn hình loading hiện đầu tiên, có logo, tên app, khẩu hiệu, ... đúng thiết kế | Chỉ phần logo đổi; mọi phần khác giữ như ảnh chuẩn hiện có. Screenshot test cùng tên bốn ảnh thiết kế mới |

Thiết kế tham chiếu: `features/loading/design/README.md` (duyệt 2026-11-13), ảnh `design/screens/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`.

ADR liên quan: ADR-0008 (`:core:designsystem` là bản gốc của hệ thống thiết kế; mỗi app thay logo của mình), ADR-0020 (screenshot test cùng tên ảnh thiết kế), ADR-0031 (mục 3.5; xóa test cũ chờ người duyệt, câu hỏi 4).

## 3. Giải pháp

### 3.1. Cách làm

1. Chép `docs/brand/app-icon/brand-logo-448.webp` thành `core/designsystem/src/main/res/drawable-nodpi/brand_logo.webp` (`cp`, không xử lý ảnh trong phiên). Ảnh đã tạo trong phiên có người theo dõi bằng Pillow: `play-store-512.png` chuyển RGB, thu về 448 x 448 bằng LANCZOS, WebP chất lượng 90. Đặt ở `drawable-nodpi` để Android không co giãn lại; ô 112 dp ở `xxxhdpi` là 448 px, ở mật độ thấp hơn ảnh được thu nhỏ khi vẽ.
2. `BrandLogo.kt`:

```kotlin
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.brand_logo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(TileSize)
            .clip(RoundedCornerShape(Radius.sheet)),
    )
}
```

   Bỏ `MarkWidth`, `MarkHeight`, `stickerShadow`, `border`, `background` và các import không còn dùng. Giữ `contentDescription = null` vì tên app hiện ngay dưới logo. KDoc mới: "App logo: the launcher icon artwork, clipped to `Radius.sheet`, no outline and no shadow. Each app replaces `res/drawable-nodpi/brand_logo.webp` with its own artwork (ADR-0008)."
3. `brand_mark.xml` giữ lại, vì còn là icon của `PrimaryButton` trong `DesignSystemCatalog.kt` và `ComponentsTest.kt`. Sửa chú thích dòng 2 thành "Sample icon for the catalog's PrimaryButton; not the brand logo (BrandLogo draws brand_logo.webp)."
4. Không đổi `LoadingScreen.kt`: logo vẫn gọi `BrandLogo(modifier = Modifier.testTag(LoadingTestTags.LOGO))`.
5. Ghi lại ảnh chuẩn (mục 4), chạy `scripts/design-compare.sh loading`, cập nhật mục 8 của `design/README.md` (bảng tỷ lệ khác, nhận xét, bỏ câu "logo tạm").

Mẫu làm theo: ảnh WebP trong `drawable-nodpi` vẽ bằng `Image` như `OnboardingIllustration.kt` và `res/drawable-nodpi/onboarding_art_*.webp` (`:feature:onboarding`, ảnh chuẩn do Robolectric `NATIVE` vẽ); `Image` với `ContentScale.Crop` như ô ảnh vừa chụp ở `CameraControls.kt` (`:feature:camera`) và ảnh tư thế ở `PoseDetailComponents.kt` (`:feature:poses`); đo màu điểm ảnh bằng `pixelAt` có sẵn của `ComponentsTest`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/designsystem/src/main/res/drawable-nodpi/brand_logo.webp` | Chép nguyên `docs/brand/app-icon/brand-logo-448.webp`; khi chuyển sang dự phòng (mục 3.4) thì không giữ file này, chỉ có `brand_logo.png` |
| Tạo | `core/designsystem/src/main/res/drawable-nodpi/brand_logo.png` | (dự phòng) Chép `docs/brand/app-icon/play-store-512.png`, mục 3.4 |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/BrandLogo.kt` | Vẽ ảnh `brand_logo`, cắt bo `Radius.sheet`, bỏ viền, bóng, nền; KDoc mới |
| Sửa | `core/designsystem/src/main/res/drawable/brand_mark.xml` | Chỉ chú thích dòng 2 |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/ComponentsTest.kt` | Xóa `brandLogo_fill_isSecondaryContainer` khi người duyệt chấp nhận câu hỏi 4 (mục 3.5, 8); thêm `brandLogo_fill_isAppIconArtwork`, `brandLogo_hasNoOutline`, `brandLogo_hasNoShadow` (mục 4) |
| Sửa | `core/designsystem/src/test/screenshots/DesignSystem_brand_light.png` | Ghi lại ảnh chuẩn |
| Sửa | `core/designsystem/src/test/screenshots/DesignSystem_brand_dark.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_progress_light.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_progress_dark.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_failed_light.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_failed_dark.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_progress_small.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_failed_small.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_failed-server_light.png` | Ghi lại ảnh chuẩn |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_failed-server_small.png` | Ghi lại ảnh chuẩn |
| Sửa | `docs/features/loading/design/review/LoadingScreen_progress_light_compare.png` | Sinh lại bằng `scripts/design-compare.sh loading` |
| Sửa | `docs/features/loading/design/review/LoadingScreen_progress_dark_compare.png` | Như trên |
| Sửa | `docs/features/loading/design/review/LoadingScreen_failed_light_compare.png` | Như trên |
| Sửa | `docs/features/loading/design/review/LoadingScreen_failed_dark_compare.png` | Như trên |
| Sửa | `docs/features/loading/design/review/summary.md` | Như trên |
| Sửa | `docs/features/loading/design/README.md` | Mục 8: bảng tỷ lệ khác và nhận xét mới, bỏ câu "logo tạm" |

Nơi khác đọc các file bị đổi (L-020 của mẫu):

- `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` và `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingStringsTest.kt` chỉ nhắc `design/README.md` trong chú thích (mục 2, bảng chữ; mục 7, lưới 4 dp); plan chỉ sửa mục 8 nên không bị ảnh hưởng.
- `scripts/design-compare/DesignCompare.java` ghi `review/summary.md`; `scripts/tests/run.sh` kiểm `summary.md` của thư mục tạm của test, không đọc file của loading: không bị ảnh hưởng.
- `scripts/device-session.sh` và `.github/workflows/device-session.yml` đọc `summary.md` của phiên thử trong `test-sessions/`, khác file này: không bị ảnh hưởng.

Không file nào khác đọc `brand_mark.xml` hay `BrandLogo` ngoài `LoadingScreen.kt`, `DesignSystemCatalog.kt`, `ComponentsTest.kt` (`grep -rn "BrandLogo\|brand_mark" --include=*.kt --include=*.xml .`). `docs/design-system/tokens.json` không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `Image`, `ContentScale`, `clip` thuộc `androidx.compose.foundation` và `androidx.compose.ui`, đã có trên classpath biên dịch của `:core:designsystem` (module đã dùng `Box`, `background` của foundation). WebP đọc được bằng `BitmapFactory` từ API 14, minSdk 26.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** nhỏ: ảnh WebP trong `drawable-nodpi` đã vẽ được trên Robolectric `NATIVE` ở `:feature:onboarding`; còn lại khả năng `:core:designsystem` vẽ khác (cấu hình test của module).
- **Điều kiện chuyển:** `brandLogo_fill_isAppIconArtwork` vẫn thất bại sau khi viết code vì điểm ảnh trong ô trong suốt hay trùng màu nền, trong khi ảnh chuẩn vẽ ra không có ảnh logo.
- **Cách làm thay:** dùng PNG thay WebP: chép `docs/brand/app-icon/play-store-512.png` thành `core/designsystem/src/main/res/drawable-nodpi/brand_logo.png` (dự phòng) và bỏ `brand_logo.webp` khỏi commit (hai tài nguyên cùng tên khác đuôi làm build lỗi). Ảnh 512 px nặng khoảng 330 KB; ghi vào nhật ký để người quyết có thu nhỏ sau không. Test và tiêu chí giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` cho plan này: xem kết quả ở mô tả PR. Các test liên quan:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Nền logo là `secondaryContainer` `#FFC83D` (điểm 56 dp, 8 dp) | Nền logo là ảnh biểu tượng app. Ảnh luôn 4 px mỗi dp (448 px trên 112 dp); dải nền cam ở đỉnh ảnh chỉ cao khoảng 10 dp, dưới đó là khung ảnh tư thế màu kem. Đo bằng Pillow trên `brand-logo-448.webp`: hàng 20 (5 dp) là `(252, 181, 30)`; thu về 112 px thì (56, 5) cũng là `(252, 181, 30)` | `ComponentsTest.brandLogo_fill_isSecondaryContainer` | Không có màu cố định để so bằng (ảnh là dải màu, nén WebP có mất mát, thu nhỏ khi vẽ), nên câu cũ không viết lại được mà vẫn giữ phép so bằng. Đề xuất (câu hỏi 4, mục 8): người duyệt chấp nhận ngoại lệ của ADR-0031, xóa test này và thay bằng test viết trước `brandLogo_fill_isAppIconArtwork` (mục 4, dùng khoảng màu). Người duyệt không chấp nhận thì plan không được duyệt ở bản này; làm bản 2 theo phương án khác ở câu hỏi 4 |
| Logo 112 x 112 dp | Giữ | `ComponentsTest.brandLogo_is112dpSquare` | Giữ nguyên |
| Logo đầu ở 200 dp, cao 112, tên app ở 336 dp | Giữ (không có bóng hay đệm ngoài ô) | `LoadingScreenTest` (đo `logo top`, `logo height`, `app name top`) | Giữ nguyên |
| Ảnh chuẩn có logo cũ | Ảnh chuẩn có logo mới | `LoadingScreenScreenshotTest` (8 ảnh), `DesignSystemCatalogScreenshotTest.brand_light`, `brand_dark` | Ghi lại ảnh chuẩn theo mục 3.2, không đổi test |

Câu khẳng định viết lại: 0. Test bị thay chờ quyết định ở câu hỏi 4 của mục 8; câu của plan nói điều mới là tiêu chí KB-6 và mục 3.1 bước 2.

## 4. Kiểm thử

Các test đo điểm ảnh của logo ở `ComponentsTest` (`brandLogo_fill_isAppIconArtwork`, `brandLogo_hasNoOutline`, `brandLogo_hasNoShadow`) đặt `BrandLogo` trong `Box(Modifier.background(Color.White).padding(16.dp))`, gắn `testTag(LOGO)` vào logo, đo bằng `pixelAt(LOGO, x, y)` có sẵn (vẽ `decorView` ra `Bitmap`; đo được điểm ngoài node), `AppTheme(darkTheme = false)`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-6, nền là ảnh biểu tượng app, không còn ô vàng | Compose UI test (Robolectric `NATIVE`) | `ComponentsTest.brandLogo_fill_isAppIconArtwork`: điểm (56 dp, 5 dp), trong dải nền cam ở đỉnh ảnh, có đỏ ≥ 240, 160 ≤ lục ≤ 195, lam < 60 (ảnh cho `(252, 181, 30)`). Trên code cũ: `#FFC83D`, lục 200, thất bại |
| KB-6, không viền | Compose UI test | `ComponentsTest.brandLogo_hasNoOutline`: điểm (56 dp, 1 dp) không gần đen (đỏ + lục + lam > 300). Trên code cũ: viền `#111111` dày 2 dp, thất bại |
| KB-6, không bóng | Compose UI test | `ComponentsTest.brandLogo_hasNoShadow`: điểm (56 dp, 114 dp), ngay dưới ô (bóng cũ lệch 4 dp xuống dưới, phải), bằng `Color.White` của nền bọc. Trên code cũ: màu bóng sticker, thất bại |
| KB-6, bo góc | Compose UI test | Thêm vào `brandLogo_hasNoShadow`: điểm (2 dp, 2 dp) trong ô là `Color.White` (góc bị cắt). Đối chứng: qua cả trên code cũ (ô cũ cũng bo). Đột biến: bỏ `.clip` trong code mới thì điểm này thành cam của ảnh, khoảng `(253, 160, 16)`, và test đỏ; ghi kết quả đột biến vào nhật ký |
| KB-6, cỡ 112 dp, cùng chỗ | Compose UI test có sẵn | `ComponentsTest.brandLogo_is112dpSquare`, `LoadingScreenTest` (logo top 200, cao 112, tên app 336): giữ nguyên, phải qua |
| KB-1, KB-6, đúng thiết kế | Screenshot test cùng tên ảnh thiết kế | `LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png` (`LoadingScreenScreenshotTest`, khung `vi-w411dp-h891dp-xxhdpi`); ghi lại rồi `scripts/design-compare.sh loading`, rà soát độc lập theo mục 7 của mẫu thiết kế |
| Màn hình thấp | Screenshot test khung `w360dp-h640dp-xxhdpi` | `LoadingScreen_progress_small.png`, `LoadingScreen_failed_small.png`, `LoadingScreen_failed-server_small.png`: logo mới, nút "Thử lại" thấy trọn |
| Lời lỗi máy chủ | Screenshot test | `LoadingScreen_failed-server_light.png` |
| Danh mục hệ thống thiết kế | Screenshot test | `DesignSystem_brand_light.png`, `DesignSystem_brand_dark.png` |
| KB-6, giống biểu tượng trên màn hình chính khi chạy thật | Kiểm tra thủ công (người: so bằng mắt logo trên ảnh chuẩn với `docs/brand/app-icon/play-store-512.png`) | Người duyệt PR mở các ảnh chuẩn đã ghi lại |

Ghi lại ảnh chuẩn, sau khi test logo đã qua:

```
./gradlew :core:designsystem:recordRoborazziDebug :core:designsystem:testDebugUnitTest --rerun
./gradlew :feature:loading:recordRoborazziDebug :feature:loading:testDebugUnitTest --rerun
git status --short -- '*/src/test/screenshots/*'
```

Chỉ giữ 10 ảnh của mục 3.2; ảnh khác đổi thì trả về bằng `git checkout -- <ảnh>` (`CLAUDE.md`, mục Lệnh). Logo không có bóng nên không gặp lệch macOS và Linux của ảnh có bóng.

**Ngoại lệ viết test trước:** tệp `brand_logo.webp` và chú thích của `brand_mark.xml` là tài nguyên; ảnh chuẩn ghi sau khi code xong. Ba test điểm ảnh ở trên viết trước và thất bại trên code cũ (trừ câu góc bo trong `brandLogo_hasNoShadow`, là đối chứng).

### 4.1. Chạy thử

Không cần (E dưới 2): ảnh WebP trong `drawable-nodpi` vẽ bằng `Image` là mẫu đã có ở `:feature:onboarding`. Màu của điểm đo đã đọc trên chính ảnh `brand-logo-448.webp` bằng Pillow (mục 3.5, mục 4); màu trên code cũ suy từ `Shape.kt`, `AppColors.kt`, `Color.kt`, không đo bằng test tạm.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7, module core dùng chung (`:core:designsystem`, thành phần `BrandLogo`). Mức Cao, cần duyệt chéo. Tiêu chí dưới để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một thành phần và tài nguyên của `:core:designsystem`, ảnh chuẩn ở hai module; giao diện công khai của `BrandLogo` không đổi |
| B. Độ rõ của yêu cầu | 0 | Spec xác nhận, thiết kế duyệt ngày 2026-11-13, tiêu chí đo được |
| C. Khả năng kiểm chứng tự động | 1 | Màu nền, viền, bóng, cỡ, vị trí có test; "giống biểu tượng" do người xem ảnh chuẩn |
| D. Ảnh hưởng chéo | 1 | `BrandLogo` chỉ dùng ở màn loading và danh mục; không task nào khác đang sửa |
| E. Độ mới của kỹ thuật | 0 | Ảnh WebP trong `drawable-nodpi` vẽ bằng `Image` là mẫu đã có ở `:feature:onboarding` |
| **Tổng** | 4 | **Mức: Cao (chạm điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `BrandLogo` của `:core:designsystem` (bản gốc của hệ thống thiết kế, ADR-0008). Dự án mẫu dùng `brand_mark.xml` cho logo; PR của task ghi đề xuất về dự án mẫu (`docs/ops/from-app.md`): `BrandLogo` vẽ một ảnh `brand_logo` mà mỗi app thay, thay cho hình vector tô màu trên ô sticker.
- Task khác trong sprint có thể xung đột: T-093 (`SettingsScreen`) và T-094 (`ReviewScreen`) không chạm `:core:designsystem` hay màn loading.
- Đề xuất thứ tự chạy: độc lập, chạy được bất kỳ lúc nào.

## 7. Điều kiện dừng riêng

- Ảnh chuẩn ngoài 10 ảnh của mục 3.2 đổi thật (không chỉ nhiễu 1 mỗi kênh màu) sau khi ghi lại: dừng, vì `BrandLogo` có chỗ dùng mà plan chưa thấy.
- `LoadingScreenTest` đo vị trí logo hay tên app đỏ: dừng, không sửa câu khẳng định (bố cục phải giữ).

## 8. Câu hỏi cho người duyệt

1. **Đặt ảnh logo ở `:core:designsystem` hay ở `:app`?** Đề xuất: `:core:designsystem/res/drawable-nodpi/brand_logo.webp`, cùng chỗ với `brand_mark.xml` hiện nay (quy ước "mỗi app thay logo của mình" của ADR-0008), không đổi giao diện công khai của `BrandLogo`. Phương án khác: `BrandLogo(painter)` nhận ảnh từ ngoài, ảnh để ở `:feature:loading`; đổi chữ ký nên dự án mẫu phải theo.
2. **Ảnh 448 px WebP chất lượng 90 (29 KB) có đủ không?** Đề xuất: đủ cho ô 112 dp ở mọi mật độ tới `xxxhdpi`. Phương án khác: PNG 512 px (330 KB, nét hơn một chút khi phóng).
3. **Nhóm có một dev:** Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9 (`risk-scoring.md`, mục 3).

4. **Thay `brandLogo_fill_isSecondaryContainer` (ngoại lệ ADR-0031).** Câu cũ so bằng `#FFC83D`; nền mới là ảnh nên không có một màu cố định để so bằng, mà ADR-0031 không cho đổi so bằng thành khoảng. Đề xuất: chấp nhận ngoại lệ, xóa test cũ, thay bằng test viết trước `brandLogo_fill_isAppIconArtwork` (khoảng màu, thất bại trên code cũ), ghi vào mục 9. Khi chấp nhận, mục 9 ghi rõ việc xóa test đã được duyệt, để phiên không vướng điều kiện dừng 5 của `risk-scoring.md`. Không chấp nhận thì plan không được duyệt ở bản này; bản 2 chọn một cách giữ phép so bằng: so điểm ảnh vẽ ra với điểm tương ứng của chính `R.drawable.brand_logo` giải mã và thu cùng cỡ (dễ chập chờn vì bộ lọc thu nhỏ), hoặc sửa ảnh để có một vùng một màu ở điểm đo.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện chỉ có một dev, ngoại lệ của `risk-scoring.md` mục 3)
- **Ghi chú:** Nhật duyệt trong phiên có người theo dõi ngày 2026-11-13, lời của Nhật: "duyệt", sau khi AI đề xuất bốn câu trả lời ở mục 8 và nói sẽ điền mục 9 theo đó. AI ghi thay. Trả lời câu hỏi ở mục 8:
  1. Ảnh logo đặt ở `:core:designsystem/res/drawable-nodpi/brand_logo.webp`, không đổi giao diện công khai của `BrandLogo`.
  2. WebP 448 px, chất lượng 90 (29 KB).
  3. Nhật vừa duyệt vừa duyệt chéo vì nhóm một dev.
  4. **Chấp nhận ngoại lệ ADR-0031:** phiên được xóa `ComponentsTest.brandLogo_fill_isSecondaryContainer` và thay bằng test viết trước `brandLogo_fill_isAppIconArtwork` (mục 3.5, mục 4). Việc xóa này đã được duyệt, không thuộc điều kiện dừng 5 của `risk-scoring.md`; mọi test cũ khác vẫn giữ nguyên.

## 10. Nhật ký thực hiện

### RUN-20261113-01, phiên bản plan 1

- **Kết quả:** đã merge.
- **Nhánh, PR:** `claude/T-095-logo-loading`, PR #327, commit merge `6fc591f`.
- **Đã làm:** `BrandLogo` vẽ `res/drawable-nodpi/brand_logo.webp` (chép nguyên `docs/brand/app-icon/brand-logo-448.webp`), cắt bo `Radius.sheet`, bỏ viền, bóng, nền; KDoc mới. `brand_mark.xml` chỉ sửa chú thích. Ghi lại 10 ảnh chuẩn của mục 3.2; `scripts/design-compare.sh loading` 2.5% (failed), 1.4% (progress); mục 8 của `design/README.md` cập nhật. Không dùng phương án dự phòng.
- **Test viết trước:** 3 test (`brandLogo_fill_isAppIconArtwork`, `brandLogo_hasNoOutline`, `brandLogo_hasNoShadow`), thất bại đúng lý do trên code cũ: `green 200`, `outline pixel sum 51`, điểm bóng `-14275` (`#FFC83D`). Test JVM của mọi module trên khung đó: chỉ 3 test này đỏ. `brandLogo_fill_isSecondaryContainer` xóa theo mục 9 (ngoại lệ ADR-0031 đã duyệt). Đột biến bỏ `.clip`: câu góc bo đỏ, điểm `-155632` (`#FDA010`). Lúc hoàn tác đột biến, `git checkout` trả `BrandLogo.kt` về code cũ vì code mới chưa commit; viết lại ngay theo mục 3.1 và chạy lại test, qua.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 3 ghi chú. Đã sửa: nhận xét `failed` trong `design/README.md` (commit `2d96e17`). Còn lại: mô tả PR có đề xuất về dự án mẫu và kết quả `existing-tests.py`; người so logo trên ảnh chuẩn với `play-store-512.png`.
- **Kết quả CI:** `pr-check` xanh (lần chạy 43369641901), `main-guard` xanh (43371415938). `scripts/check.sh` trên máy qua (3123 test JVM, 0 lỗi).
- **Lệch so với plan:** Không có. Khi ghi lại ảnh chuẩn, 4 ảnh ngoài mục 3.2 của `:core:designsystem` (`DesignSystem_camera_components_dark`, `DesignSystem_pose_cards_light`, `DesignSystem_spacing_components_dark`, `DesignSystem_spacing_components_light`) lệch tối đa 1 mỗi kênh màu (nhiễu vẽ, mục 7 của plan không dừng), đã trả về bằng `git checkout`.
- **Việc còn lại:** đề xuất về dự án mẫu (`BrandLogo` vẽ ảnh `brand_logo` mà mỗi app thay), theo `docs/ops/from-app.md`.
- **Cần người xem:** logo trên ảnh chuẩn `feature/loading/src/test/screenshots/LoadingScreen_*.png` so với `docs/brand/app-icon/play-store-512.png`; ảnh so sánh trong `docs/features/loading/design/review/`.
