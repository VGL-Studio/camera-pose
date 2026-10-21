---
ticket: T-004
title: Đủ vai trò màu thương hiệu và giãn cách chữ như thiết kế
status: done
version: 1
risk_score:
risk_level: high
blocking_points: [core]
run_order: 1
reviewer: Nhật
cross_reviewer: Nhật (vì là dự án mẫu)
approved_at: 2026-10-17
---

# PLAN T-004: ĐỦ VAI TRÒ MÀU THƯƠNG HIỆU VÀ GIÃN CÁCH CHỮ NHƯ THIẾT KẾ

## 1. Tóm tắt

Gán màu thương hiệu cho hai vai trò còn màu mặc định (`tertiaryContainer`, `onTertiaryContainer`), đặt giãn cách chữ 0 cho mọi kiểu chữ như thiết kế Figma, và cho `tokens.json` liệt kê đủ mọi vai trò màu của `ColorScheme`, có test giữ điều đó, để không vai trò nào lọt qua với giá trị mặc định (L-024). Kiểm chứng bằng `DesignTokensTest` (viết trước), ảnh chuẩn screenshot ghi lại và ảnh so sánh với thiết kế của màn hình loading.

## 2. Hiểu yêu cầu

Ticket làm tiếp phần hệ thống thiết kế cho các kịch bản sau trong `features/loading/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Màn hình loading đúng thiết kế | Chữ trên thiết kế không có giãn cách (Inter, giãn cách 0); app hiện dùng giãn cách của Material 3 nên chữ nhỏ rộng hơn (rà soát độc lập PR #40) |
| KB-2 | Giao diện lỗi đúng thiết kế | Như trên, nhất là lời hướng dẫn và chân trang |

Thiết kế tham chiếu: `features/loading/design/README.md`, `design/context/` (cột `ls=0.0` của mọi chữ trong số liệu Figma), ảnh trong `design/screens/`, kết quả đối chiếu ở mục 8 của README.

ADR liên quan: ADR-0008, ADR-0020. Bài học: L-024 (retro RUN-20261017-01, 2026-10-17).

## 3. Giải pháp

### 3.1. Cách làm

**Màu (`Color.kt`).** Thêm vào `lightColorScheme(...)` và `darkColorScheme(...)`:

| Vai trò | Sáng | Tối | Nguồn |
| --- | --- | --- | --- |
| `tertiaryContainer` | #C3E8FB | #264B5B | AI, cùng tông với `tertiary` (#3F6374, #A7CCE0) |
| `onTertiaryContainer` | #001F2A | #C3E8FB | AI |

Các vai trò khác giữ nguyên giá trị hiện có (theo plan T-002, mục 3.1). `scrim` giữ màu đen mặc định, vì đó là lớp phủ tối, không thuộc thương hiệu.

**Chữ (`Type.kt`).** Mọi kiểu chữ trong `AppTypography` thêm `letterSpacing = 0.sp` cạnh `fontFamily = InterFamily`. Cỡ chữ, chiều cao dòng, độ đậm giữ nguyên. Sửa chú thích của `AppTypography`: "Material 3 type scale in Inter, without letter spacing, as in the design (ADR-0020)".

**`tokens.json` đủ mọi vai trò.** Trong `DesignTokensTest`:

- Hàm `colors(scheme)` liệt kê đủ 35 vai trò, theo thứ tự: 21 vai trò hiện có, chèn `tertiaryContainer`, `onTertiaryContainer` ngay sau `onTertiary`, rồi thêm sau `outline`: `outlineVariant`, `inversePrimary`, `inverseSurface`, `inverseOnSurface`, `surfaceTint`, `surfaceBright`, `surfaceDim`, `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`. Tách danh sách tên và màu ra hàm `internal fun colorRoles(scheme: ColorScheme): List<Pair<String, Color>>` để test mới dùng.
- Test mới `colorRoles_listEveryRoleOfColorScheme`: lấy mọi hàm getter của `ColorScheme` qua Java reflection (tên bắt đầu bằng `get`, không tham số, kiểu trả về `long`, vì `Color` là value class; tên dạng `getPrimary-0d7_KjU`), bỏ tiền tố `get` và phần sau dấu `-`, đổi chữ đầu thành chữ thường, bỏ `scrim`; so với tập tên của `colorRoles(LightColors)`. Phiên bản Material 3 sau này thêm vai trò mới thì test này đỏ, buộc plan nâng cấp chọn màu cho vai trò đó.

**`ColorCatalog`.** Thêm dòng `tertiaryContainer` (trên `onTertiaryContainer`) sau dòng `tertiary`.

**Ảnh và đối chiếu thiết kế.** Ghi lại ảnh chuẩn ở mục 3.2 bằng `./gradlew recordRoborazziDebug`; giãn cách chữ đổi mọi ảnh có chữ. Chạy `scripts/design-compare.sh loading`, commit `design/review/`, thêm vào `design/README.md`, mục 8, một khối "Sau T-004" với bảng của `review/summary.md` và nhận xét của rà soát độc lập (chữ nhỏ có còn rộng hơn thiết kế không).

**Nơi khác đọc các file trên** (docs/CLAUDE.md, mục Khi viết plan; L-020), không cần sửa:

- `docs/design-system/tokens.json`: `core/designsystem/build.gradle.kts` chỉ truyền đường dẫn file cho `DesignTokensTest`; `scripts/figma-export.py` đọc file khi người lấy thiết kế (nhiều vai trò hơn chỉ làm cột token của `context/` có thêm tên), và test của nó trong `scripts/tests/run.sh` dùng `scripts/tests/fixtures/tokens.json` cố định.
- `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt`: chỉ nhắc `tokens.json` trong chú thích.
- `docs/features/loading/design/README.md`: `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` chỉ nhắc mục 2 (bảng chữ) trong chú thích; plan chỉ sửa mục 8.
- `design/review/summary.md`: `scripts/design-compare/DesignCompare.java` là chương trình sinh ra file này, không đọc nó.

Thứ tự làm: sửa tay `tokens.json` (mục 4), viết test mới, chạy để thấy hai test thất bại đúng lý do; rồi sửa code; sinh lại `tokens.json` và so với bản sửa tay; ghi lại ảnh; `scripts/check.sh`; `scripts/design-compare.sh loading`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Color.kt` | `tertiaryContainer`, `onTertiaryContainer` sáng và tối |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/theme/Type.kt` | `letterSpacing = 0.sp` cho mọi kiểu chữ, chú thích |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | Dòng `tertiaryContainer` trong `ColorCatalog` |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignTokensTest.kt` | `colorRoles` đủ 35 vai trò, test `colorRoles_listEveryRoleOfColorScheme` |
| Sửa | `docs/design-system/tokens.json` | Sửa tay trước (mục 4), sau đó sinh lại bằng `DesignTokensTest` |
| Ghi lại | `core/designsystem/src/test/screenshots/DesignSystem_colors_light.png`, `DesignSystem_colors_dark.png`, `DesignSystem_typography_light.png`, `DesignSystem_spacing_components_light.png`, `DesignSystem_brand_light.png`, `DesignSystem_brand_dark.png` | Màu, giãn cách chữ |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`, `SampleScreen_disabled_light.png` | Giãn cách chữ |
| Ghi lại | `feature/loading/src/test/screenshots/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png` | Giãn cách chữ |
| Ghi lại | `docs/features/loading/design/review/LoadingScreen_progress_light_compare.png`, `LoadingScreen_progress_dark_compare.png`, `LoadingScreen_failed_light_compare.png`, `LoadingScreen_failed_dark_compare.png`, `summary.md` | Sinh bởi `scripts/design-compare.sh loading` |
| Sửa | `docs/features/loading/design/README.md` | Chỉ mục 8: khối "Sau T-004" |

Không sửa code của `:feature:loading`, `:feature:sample`, `build.gradle.kts` hay `gradle/libs.versions.toml`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Mọi vai trò màu có giá trị thương hiệu, giãn cách chữ 0 | Unit test | `DesignTokensTest.tokensFile_matchesTheDesignSystem`: viết trước bằng cách sửa tay `tokens.json`: hai khối `color` có đủ 35 vai trò theo thứ tự ở mục 3.1 (33 vai trò lấy đúng giá trị đang có trong `Color.kt`, hai vai trò mới theo bảng ở mục 3.1), mọi `letterSpacingSp` là `0.0`. Test thất bại cho tới khi code đổi; sau đó sinh lại bằng `-PdesignTokens.update=true`, file sinh ra phải trùng file sửa tay |
| Không vai trò nào bị bỏ sót (L-024) | Unit test | `DesignTokensTest.colorRoles_listEveryRoleOfColorScheme`: tập tên của `colorRoles(LightColors)` bằng tập vai trò màu của `ColorScheme` trừ `scrim`. Viết trước: thất bại vì `colorRoles` mới có 21 vai trò |
| Giao diện catalog, màn hình mẫu, màn hình loading | Screenshot test | Ghi lại đúng 13 ảnh ở mục 3.2, không ảnh nào khác đổi; `scripts/check.sh` xanh |
| Gần thiết kế hơn | Ảnh so sánh, rà soát độc lập | `scripts/design-compare.sh loading`; rà soát xem chữ ở chân trang và lời hướng dẫn khi lỗi có còn rộng hơn thiết kế không, theo mục 7 của `design/README.md` |
| Màu `tertiaryContainer` hợp tông | Kiểm tra thủ công | Mở `DesignSystem_colors_light.png`, `DesignSystem_colors_dark.png`: dòng `tertiaryContainer` cùng tông xanh xám với `tertiary`, chữ trên nền đọc được |

**Ngoại lệ viết test trước:** ảnh chuẩn screenshot và ảnh so sánh không viết trước được; màu và giãn cách chữ được hai test unit ở trên kiểm tra trước.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core dùng chung `:core:designsystem`). Rủi ro cao, cần duyệt chéo. Điểm dưới đây để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Một module core, nhưng đổi ảnh của mọi màn hình có chữ ở ba module |
| B. Độ rõ của yêu cầu | 0 | Thiết kế ghi giãn cách 0; hai màu mới do AI đề xuất, người duyệt chốt (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Giá trị kiểm bằng test; độ giống thiết kế xem ảnh so sánh |
| D. Ảnh hưởng chéo | 2 | Ghi lại ảnh của `:feature:sample`, `:feature:loading`, không đổi code của chúng |
| E. Độ mới của kỹ thuật | 1 | Reflection trên value class của Compose là biến thể nhỏ, chưa có trong dự án |
| **Tổng** | 7 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AppTypography`, `LightColors`, `DarkColors`, `tokens.json`, ảnh catalog.
- Task khác trong sprint có thể xung đột: Không có.
- Đề xuất thứ tự chạy: chạy một mình hoặc sau các task không chạm ảnh chuẩn, vì task này ghi lại ảnh của mọi màn hình.

## 7. Điều kiện dừng riêng

1. `colorRoles_listEveryRoleOfColorScheme` thấy vai trò ngoài 35 vai trò ở mục 3.1 (phiên bản Material 3 có thêm vai trò): dừng, plan mới chọn màu cho vai trò đó.
2. Ảnh chuẩn đổi ngoài 13 ảnh ở mục 3.2, hoặc bố cục màn hình nào đổi ngoài độ rộng của chữ.

## 8. Câu hỏi cho người duyệt

1. **Màu `tertiaryContainer` sáng #C3E8FB, tối #264B5B và `onTertiaryContainer` sáng #001F2A, tối #C3E8FB** (AI đề xuất, cùng tông với `tertiary`). Hiện chưa màn hình nào dùng hai vai trò này. Đề xuất: duyệt; khi thiết kế cần thì chỉnh qua plan mới.
2. **Giãn cách 0 cho mọi kiểu chữ**, kể cả kiểu chữ chưa có trên thiết kế (`display*`, `labelMedium`). Đề xuất: đồng ý, để thống nhất với Figma, nơi hệ thống thiết kế sẽ được dựng lại từ `tokens.json` mới.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (vì đây là dự án mẫu)
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261017-02, phiên bản plan 1

- **Kết quả:** đã merge.
- **Nhánh, PR:** `claude/T-004-mau-va-gian-chu`, PR #43, merge thành commit 050997e trên nhánh chính.
- **Đã làm:** đủ mục 3.1. `Color.kt` thêm `tertiaryContainer`, `onTertiaryContainer` sáng và tối theo bảng. `Type.kt` đặt `letterSpacing = 0.sp` cho cả 15 kiểu chữ, sửa chú thích. `ColorCatalog` thêm dòng `tertiaryContainer`. `DesignTokensTest` tách `colorRoles` với 35 vai trò, thêm `colorRoles_listEveryRoleOfColorScheme`. Ghi lại đúng 13 ảnh chuẩn ở mục 3.2, không ảnh nào khác đổi. Chạy `scripts/design-compare.sh loading`, commit `design/review/`, thêm khối "Sau T-004" vào mục 8 của `design/README.md`. 24 file thay đổi, đều nằm trong mục 3.2.
- **Test viết trước:** commit 8af8d26 gồm `tokens.json` sửa tay (35 vai trò mỗi chế độ, mọi `letterSpacingSp` là `0.0`) và test reflection, với `colorRoles` còn 21 vai trò. Cả hai test thất bại đúng lý do: file lệch giá trị; tập vai trò của `ColorScheme` có 35 tên, `colorRoles` mới có 21. Reflection không thấy vai trò nào ngoài 35 vai trò của plan (điều kiện dừng 7.1 không xảy ra). Sau khi sửa code, `tokens.json` sinh lại bằng `-PdesignTokens.update=true` trùng từng byte với bản sửa tay. Một lần sửa chi tiết test trước commit, không đổi điều test kiểm tra: xuống dòng cho ktlint.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa (khối "Sau T-004" trong `design/README.md` chưa có lúc rà soát; đã thêm), 6 ghi chú. Chân trang và dòng "Đang chuẩn bị không gian của bạn…" nay rộng gần bằng thiết kế. Lời hướng dẫn khi lỗi và nhãn tiến trình vẫn rộng hơn khoảng 8%, chỉ còn do cỡ chữ 14 sp so với 13 sp (mục 6 của thiết kế cho phép). Lời hướng dẫn nay xuống dòng sau "kiểm tra mạng" thay vì sau "kiểm tra" như thiết kế, vẫn 2 dòng. Bố cục không đổi. Test reflection chỉ so tên vai trò, gán nhầm thuộc tính chỉ bị test so `tokens.json` bắt. `tertiaryContainer` sáng tươi hơn `tertiary` nhưng cùng họ màu.
- **Kết quả CI:** một vòng, xanh: `changes`, `build-and-test`, `ui-test`.
- **Đối chiếu thiết kế:** điểm ảnh khác 2,4% (đang tải, sáng và tối, trước là 2,5%), 5,2% (lỗi, sáng), 5,6% (lỗi, tối), không đổi.
- **Lệch so với plan:** Không có.
- **Việc còn lại:** kiểm tra thủ công màu `tertiaryContainer` (mục 4).
- **Cần người xem:** `DesignSystem_colors_light.png`, `DesignSystem_colors_dark.png`; bốn ảnh ghép trong `docs/features/loading/design/review/`; chỗ xuống dòng của lời hướng dẫn khi lỗi.
