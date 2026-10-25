---
ticket: T-050
title: Chữ của camera và màn xem lại theo ngôn ngữ đã chọn
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 6                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 8M           # cỡ M (5.0M, scripts/token-usage.py estimate) cộng khoảng 60%: 83 chuỗi dịch sang 6 ngôn ngữ (12 tệp) và 12 tệp test đổi qualifiers
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-25
---

# PLAN T-050: CHỮ CỦA CAMERA VÀ MÀN XEM LẠI THEO NGÔN NGỮ ĐÃ CHỌN

## 1. Tóm tắt

`values/` của `:feature:camera` đang viết bằng tiếng Việt, không có thư mục ngôn ngữ nào khác, nên đổi ngôn ngữ thì camera và màn xem lại vẫn tiếng Việt. Plan làm theo đúng mẫu của settings và onboarding:
- `values/` chuyển sang tiếng Anh;
- chữ tiếng Việt chuyển nguyên sang `values-vi/`;
- AI dịch thêm 5 ngôn ngữ phát hành còn lại;
- một test chặn `CameraStringsTest` kiểm đủ bản dịch.

Các test giao diện đang dựa vào chữ tiếng Việt được thêm `vi` vào `qualifiers`. Chạy thử trên code hiện tại (mục 4.1) cho thấy: thêm `vi` vào cả 25 chỗ thì 379 test của module vẫn qua, mọi ảnh chuẩn khớp. Test chữ mới thất bại đúng lý do.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/settings/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khi bấm "Ngôn ngữ" trên Android 13 trở lên thì trang ngôn ngữ của app trong cài đặt máy mở ra; chọn xong quay lại thì app hiện ngôn ngữ mới | Cơ chế đổi ngôn ngữ đã chạy đúng (đã thử trên máy thật Xiaomi, Android 15, ngày 2026-10-25, ticket T-050). Phần thiếu là tài nguyên chữ của camera và màn xem lại: khi app ở ngôn ngữ `X` trong 7 ngôn ngữ phát hành thì mọi chữ của hai màn này hiện bằng `X`; ngôn ngữ khác thì hiện tiếng Anh |
| KB-4 | Khi bấm "Ngôn ngữ" trên máy dưới Android 13 thì một hộp chọn trong app hiện "Theo hệ thống" và 7 ngôn ngữ phát hành; chọn xong app đổi ngôn ngữ ngay | Như trên: `MainActivity` đã gắn ngôn ngữ vào context; camera chỉ cần có tài nguyên đủ 7 ngôn ngữ |

Thiết kế tham chiếu: `features/camera/design/README.md`, mục 2 (bảng chữ tiếng Việt), các ảnh trong `design/screens/` (18 ảnh `CameraScreen_*`, `ReviewScreen_*`, đều tiếng Việt). Plan không đổi thiết kế: ảnh thiết kế vẫn so với screenshot test chạy ở `vi`.

Quyết định đã có: spec onboarding, câu hỏi 1 (AI dịch, người biết tiếng duyệt trước khi phát hành); spec settings, KB-4 và câu hỏi 2 (7 ngôn ngữ phát hành: `en`, `hi`, `pt-BR`, `es-419`, `id`, `th`, `vi`).

ADR liên quan: ADR-0008 (hệ thống thiết kế), ADR-0020 (thiết kế và screenshot test).

## 3. Giải pháp

### 3.1. Cách làm

**Tài nguyên chữ.** Làm theo mẫu `:feature:settings` (plan T-025, mục 3.1) và `:feature:onboarding`.
- `values/strings.xml` (69 chuỗi) và `values/strings_review.xml` (19 chuỗi) chuyển sang tiếng Anh. Giữ nguyên tên chuỗi, thứ tự, chú thích và tham số.
- Bốn chuỗi chỉ có định dạng, không có chữ, thêm `translatable="false"` như `loading_percent`: `camera_timer_label` (`%1$ds`), `camera_zoom_selected` (`%1$sx`), `camera_step_count` (`%1$d/%2$d`), `camera_adjust_percent` (`%1$d%%`). `review_album_name` đã là `translatable="false"`. Các tệp ngôn ngữ không chép năm chuỗi này.
- `values-vi/strings.xml` và `values-vi/strings_review.xml`: chép nguyên chữ tiếng Việt hiện có của mọi chuỗi dịch được, không sửa chữ nào. Đây là bảng chữ của thiết kế.
- `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in` (tiếng Indonesia, `id` đọc `values-in`), `values-th`: mỗi thư mục hai tệp cùng tên, đủ mọi chuỗi dịch được. Phiên dịch theo các quy tắc sau:
  - Giữ đúng tham số (`%1$s`, `%1$d`, `%%`) và dấu `\'`. `CameraStringsTest.everyTranslation_keepsPlaceholders` kiểm điều này.
  - Bản tiếng Anh viết từ bảng chữ tiếng Việt, cùng giọng với settings và onboarding. Dùng lại đúng chữ đã có, ví dụ "Try again", "Settings", "Cancel".
  - Bản dịch của 5 ngôn ngữ dịch từ bản tiếng Anh, có đối chiếu bản tiếng Việt. Dùng lại cách gọi đã có trong `values-<ngôn ngữ>` của settings và onboarding, ví dụ cách dịch "camera", "pose", "outline".
  - Nhãn nút và chip ngắn (bảng chỉnh nét viền: "Cỡ", "Lật", "Ẩn"; chip nhóm; nút của màn xem lại) giữ trong khoảng gấp 1,5 lần độ dài chữ tiếng Anh nếu được. Ảnh mới ở mục 4 kiểm chữ dài nhất.
  - Mô tả cho trình đọc màn hình (`camera_cd_*`) dịch đủ, không để tiếng Anh.

**Test.** Mỗi module có test chữ riêng, không dùng chung hàm giữa các module, giống `SettingsStringsTest` và `OnboardingStringsTest`. T-051 làm tương tự cho `:feature:poses` và `:core:designsystem` (mục 6).
- `CameraStringsTest` (mới) đọc mọi chuỗi của module qua `R.string::class.java.fields`. Dự án đặt `android.nonTransitiveRClass=true`, nên `R` chỉ có chuỗi của chính module. Ba test:
  - `english_isDefault`: ở `en` và `fr` (ngôn ngữ không có thư mục), không chuỗi nào có chữ cái tiếng Việt. Cách dò: `java.text.Normalizer.normalize(text, NFD)` rồi tìm dấu kết hợp `[\u0300-\u0323]` hoặc `[đĐ]`; cách này bắt cả chữ hai dấu (ậ, ẩ, ỡ, ở, ế, ộ, ừ…), một bảng chữ có dấu viết sẵn thì sót.
  - `everyReleaseLanguage_isTranslated`: ở `hi`, `pt-BR`, `es-419`, `es-MX` (đọc `values-b+es+419`), `id`, `th`, `vi`, mỗi chuỗi không rỗng. Chuỗi khác tiếng Anh, trừ 5 chuỗi `LANGUAGE_NEUTRAL` ở trên và danh sách `SAME_AS_ENGLISH`. Danh sách này ban đầu rỗng. Phiên chỉ được thêm cặp (ngôn ngữ, chuỗi) mà chính từ đó đã giữ nguyên tiếng Anh trong `values-<ngôn ngữ>` của settings hay onboarding (ví dụ `es-419` "General"), ghi lý do trong chú thích như `SettingsStringsTest`, và liệt kê từng cặp trong nhật ký thực hiện.
  - `everyTranslation_keepsPlaceholders`: mỗi ngôn ngữ có đúng bộ tham số như tiếng Anh.
- Mười hai lớp test giao diện của module được thêm `vi-` vào đầu mọi `qualifiers`, kể cả hằng `SHORT_SCREEN` và `@Config` ở từng test. `@Config` ở từng test thay hẳn `qualifiers` của lớp, nên phải đổi cả hai. Lý do của từng tệp ở mục 3.2. Đây là sửa test, chỉ được làm đúng các tệp đó. Các test này kiểm chữ tiếng Việt theo bảng chữ của thiết kế (`onNodeWithText("Chụp lại")`, mô tả "Bước trước"…), kích thước đo theo chữ tiếng Việt, và ảnh chuẩn so với ảnh thiết kế tiếng Việt. Giữ ở `vi` thì các test kiểm đúng điều cũ, ảnh chuẩn không đổi.
- Ba test mới ở `pt-rBR`, khung thấp `w360dp-h640dp-xxhdpi`, kiểm chữ dài không đẩy nút ra khỏi màn hay đè lên nhau:
  - `ReviewScreenTest.smallPortuguese_saveError_primaryButtonVisible` dùng lại `assertFitsOnShortScreen()`;
  - `CameraScreenTest.smallPortuguese_permissionDenied_buttonVisible` tìm nút bằng `getString(R.string.camera_permission_action)`, không ghi chữ cứng;
  - `CameraScreenOverlayTest.smallPortuguese_adjustPanel_labelsFit`: bảng chỉnh nét viền đang mở, bốn nút "Lật", "Khóa", "Ẩn", "Đặt lại" (`AdjustAction` của `:core:designsystem`) là chỗ dễ tràn nhất. Nhãn của `AdjustAction` một dòng (`softWrap = false`, rộng không giới hạn) và đã xóa ngữ nghĩa (`clearAndSetSemantics`), nên test đo chiều rộng nhãn bằng `rememberTextMeasurer()` với `MaterialTheme.typography.labelMedium` trong cây giao diện của test, đặt nhãn giữa nút cùng tên (tìm bằng mô tả, `performScrollTo()`). Kiểm: mỗi nhãn nằm trong bảng, và hai nhãn cạnh nhau trên cùng hàng không đè nhau. Để đo được, `setScreen` của lớp nhận thêm tham số `probe: @Composable () -> Unit = {}` gọi trong `AppTheme`, không đổi các test khác.
- Một ảnh chuẩn mới: `ReviewScreenScreenshotTest.saveError_portuguese_small` chụp `src/test/screenshots/ReviewScreen_save-error-pt-br_small.png`, `@Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")`, cùng nội dung với `saveError_small`. Đây là trạng thái nhiều chữ nhất của hai màn: thông báo lỗi, ba nút và nút chính. Tiếng Bồ Đào Nha thường dài nhất trong 7 ngôn ngữ.

**Không đổi:** code Kotlin của `src/main`, cách đổi ngôn ngữ của `:app`, thiết kế, kịch bản emulator.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/res/values/strings.xml` | Chữ tiếng Anh; `translatable="false"` cho `camera_timer_label`, `camera_zoom_selected`, `camera_step_count`, `camera_adjust_percent` |
| Sửa | `feature/camera/src/main/res/values/strings_review.xml` | Chữ tiếng Anh |
| Tạo | `feature/camera/src/main/res/values-vi/strings.xml` | Chữ tiếng Việt hiện có, chép nguyên |
| Tạo | `feature/camera/src/main/res/values-vi/strings_review.xml` | Như trên |
| Tạo | `feature/camera/src/main/res/values-hi/strings.xml` | Tiếng Hindi |
| Tạo | `feature/camera/src/main/res/values-hi/strings_review.xml` | Tiếng Hindi |
| Tạo | `feature/camera/src/main/res/values-pt-rBR/strings.xml` | Tiếng Bồ Đào Nha (Brasil) |
| Tạo | `feature/camera/src/main/res/values-pt-rBR/strings_review.xml` | Tiếng Bồ Đào Nha (Brasil) |
| Tạo | `feature/camera/src/main/res/values-b+es+419/strings.xml` | Tiếng Tây Ban Nha (Mỹ Latinh) |
| Tạo | `feature/camera/src/main/res/values-b+es+419/strings_review.xml` | Tiếng Tây Ban Nha (Mỹ Latinh) |
| Tạo | `feature/camera/src/main/res/values-in/strings.xml` | Tiếng Indonesia |
| Tạo | `feature/camera/src/main/res/values-in/strings_review.xml` | Tiếng Indonesia |
| Tạo | `feature/camera/src/main/res/values-th/strings.xml` | Tiếng Thái |
| Tạo | `feature/camera/src/main/res/values-th/strings_review.xml` | Tiếng Thái |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraStringsTest.kt` | Ba test của mục 3.1 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenTest.kt` | `vi-` ở `@Config` của lớp và `SHORT_SCREEN`: test kiểm chữ tiếng Việt ("Đang mở camera…", "Mở cài đặt", mô tả "Hủy đếm ngược"). Thêm `smallPortuguese_permissionDenied_buttonVisible` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenPoseTest.kt` | `vi-` ở lớp và `SHORT_SCREEN`: kiểm "Tất cả", "Bước trước", "Bước sau", "Ẩn thẻ bước", "Thử lại" |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenMatchTest.kt` | `vi-` ở lớp, `SHORT_SCREEN` và hai `@Config` khung mở gập: kiểm chữ gợi ý ("Nâng tay trái cao hơn"…) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenOverlayTest.kt` | `vi-` ở lớp và `SHORT_SCREEN`: kiểm mô tả "Ẩn tư thế", "Khóa lớp phủ", chữ "Đã khóa lớp phủ". `setScreen` thêm tham số `probe`; thêm `smallPortuguese_adjustPanel_labelsFit` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenUnfoldedTest.kt` | `vi-` ở lớp và `@Config` khung `w673dp-h891dp`: số đo bố cục mở gập lấy từ thiết kế tiếng Việt (`design/context/`), chữ khác độ dài thì kích thước nút chữ khác |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraLastPhotoTest.kt` | `vi-` ở lớp: cùng màn camera, giữ một ngôn ngữ cho mọi test giao diện của module, để thứ tự chạy test hay chữ mới không làm đổi số đo |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | `vi-` ở hai `@Config` của lớp và `SHORT_SCREEN`: ảnh chuẩn so với ảnh thiết kế tiếng Việt |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraUnfoldedScreenshotTest.kt` | `vi-` ở lớp: như trên |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenTest.kt` | `vi-` ở lớp và `SHORT_SCREEN`: kiểm "Xem lại", "Chụp lại", "Lưu vào thư viện", "Đóng". Thêm `smallPortuguese_saveError_primaryButtonVisible` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenUnfoldedTest.kt` | `vi-` ở hai `@Config` của lớp: kiểm "Lưu ảnh", "Đang lưu…", "Tư thế tiếp" |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewRouteTest.kt` | `vi-` ở lớp: chạm "Chụp lại", "Bỏ ảnh" |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenScreenshotTest.kt` | `vi-` ở hai `@Config` của lớp và `@Config` của `saveError_small`. Thêm `saveError_portuguese_small` |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_save-error-pt-br_small.png` | Ảnh chuẩn mới, `scripts/check.sh` tự ghi |
| Sửa | `docs/features/camera/design/review/` | Ảnh ghép và `summary.md` do `scripts/design-compare.sh camera` sinh lại (`docs/ops/design-workflow.md`, mục 4); không sửa tay |

Không có tệp nào khác. Đã tìm nơi đọc chữ của camera ngoài module (L-020):
- `grep` chữ tiếng Việt và `camera.R.string` trong `app/src`, `core/`, các feature khác: không có.
- Ba test emulator của `app/src/androidTest` không kiểm chữ.
- Kịch bản `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt` chỉ dùng `id=`/`idp=`.
- Bộ chọn `str=` của `scripts/device-scenario.py` đọc `values/strings.xml` (ngôn ngữ mặc định). Không kịch bản nào dùng `str=` cho chuỗi camera, và emulator CI chạy `en_US` (log của lần chạy 40179423644), nên camera trên emulator sẽ hiện tiếng Anh mà không kịch bản nào lỗi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-4: ngôn ngữ ngoài 7 ngôn ngữ phát hành thì hiện tiếng Anh, `values/` không còn tiếng Việt | Unit test (Robolectric) | `CameraStringsTest.english_isDefault` |
| KB-4: đủ 7 ngôn ngữ, mọi chuỗi có bản dịch | Unit test (Robolectric) | `CameraStringsTest.everyReleaseLanguage_isTranslated` |
| Bản dịch giữ đúng tham số | Unit test (Robolectric) | `CameraStringsTest.everyTranslation_keepsPlaceholders` |
| Tiếng Việt giữ đúng bảng chữ của thiết kế | Compose UI test và screenshot test có sẵn chạy ở `vi` | 12 lớp ở mục 3.2 qua mà không sửa gì ngoài `qualifiers`; `verifyRoborazziDebug` qua với mọi ảnh chuẩn cũ (không ghi lại ảnh nào) |
| `values-vi` là chữ cũ chép nguyên | Lệnh kiểm ở bước 5 | `python3` so cặp tên và chữ giữa `git show origin/main:feature/camera/src/main/res/values/<tệp>` và `values-vi/<tệp>`, bỏ qua 5 chuỗi `translatable="false"`: phải trùng hết. Ghi kết quả vào nhật ký |
| Chữ dài không đẩy nút ra khỏi màn thấp hay đè nhau (L-028) | Compose UI test | `ReviewScreenTest.smallPortuguese_saveError_primaryButtonVisible`, `CameraScreenTest.smallPortuguese_permissionDenied_buttonVisible`, `CameraScreenOverlayTest.smallPortuguese_adjustPanel_labelsFit` |
| Màn hình thấp ở ngôn ngữ chữ dài (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, `pt-rBR` | `ReviewScreen_save-error-pt-br_small.png`; phiên mở ảnh, xem chữ không bị cắt hay tràn ở nút và thông báo lỗi, ghi nhận xét vào nhật ký |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, chạy ở `vi` (ảnh chuẩn không đổi); chạy `scripts/design-compare.sh camera` sau `check.sh`, commit `docs/features/camera/design/review/` cùng PR và dán `review/summary.md` vào mô tả PR (`docs/ops/design-workflow.md`, mục 4; không chặn merge). Ảnh chuẩn không đổi nên tỷ lệ khác biệt phải bằng tỷ lệ hiện có; tỷ lệ nào đổi rõ thì ghi vào "Cần người kiểm tra" của báo cáo, không dừng | 18 ảnh `CameraScreen_*`, `ReviewScreen_*` của `design/screens/` |
| Ảnh thiết kế của feature settings | Ảnh thiết kế của ticket khác (T-025): `SettingsScreen_default_dark`, `SettingsScreen_default_light`, `SettingsScreen_theme-picker_dark`, `SettingsScreen_theme-picker_light`. Ticket này thuộc feature settings (KB-4) nhưng chỉ đổi chữ của camera; ảnh của settings không đổi | Không chạm |
| KB-4, đổi ngôn ngữ trên máy: camera và màn xem lại hiện đúng ngôn ngữ | Kiểm tra thủ công (máy thật: camera thật) | Trên bản staging: đổi app sang English, Português, हिन्दी trong cài đặt máy (Android 13 trở lên) hay hộp chọn của app (dưới 13), mở camera, chụp, xem màn xem lại; chữ đổi theo |
| Bản dịch đúng nghĩa, tự nhiên | Kiểm tra thủ công (người: người biết tiếng duyệt bản dịch, spec onboarding câu hỏi 1) | Người biết tiếng đọc `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th` trước khi phát hành |

**Thứ tự test viết trước (bước 3):** commit test gồm `CameraStringsTest`, hai test `smallPortuguese_*`, test ảnh `saveError_portuguese_small`, và phần đổi `vi-` của 12 lớp. Kết quả trên code hiện tại (mục 4.1): `english_isDefault` và `everyReleaseLanguage_isTranslated` thất bại đúng lý do; `everyTranslation_keepsPlaceholders` và ba test `smallPortuguese_*` qua ngay (đối chứng: trên code cũ mọi ngôn ngữ đều đọc `values/` tiếng Việt, nên chúng chỉ có ý nghĩa sau khi có bản dịch). 12 lớp có `vi-` vẫn qua và ảnh chuẩn khớp, chứng minh việc đổi `qualifiers` không đổi điều test kiểm. **Ảnh mới `ReviewScreen_save-error-pt-br_small.png`:** test chụp ảnh được commit ở bước 3, chưa có ảnh chuẩn. `scripts/check.sh` tự ghi ảnh còn thiếu, nên nếu chạy ở bước 3 nó ghi ảnh khi chưa có `values-pt-rBR` (chữ tiếng Việt), và bước 4 so lệch. Vì vậy:
- Bước 3 chỉ chạy các lệnh Gradle của test (`./gradlew :feature:camera:testDebugUnitTest --tests ...`), không chạy `check.sh`.
- Bước 4, sau khi có bản dịch và trước khi ghi: xóa `feature/camera/src/test/screenshots/ReviewScreen_save-error-pt-br_small.png` nếu đã có, rồi ghi bằng `./gradlew :feature:camera:recordRoborazziDebug` (plan cho phép ghi đúng ảnh này, không ảnh cũ nào đổi: `git status --short feature/camera/src/test/screenshots/` chỉ có tệp mới này). Mở ảnh, xem chữ là tiếng Bồ Đào Nha và không bị cắt hay tràn, rồi mới chạy `check.sh` (bước 5).
- Ba test bố cục `smallPortuguese_*` không phụ thuộc thứ tự bước: chúng không có ảnh chuẩn, chỉ đo bố cục lúc chạy, nên qua ở bước 3 (đối chứng, chữ tiếng Việt) và đo chữ Bồ Đào Nha thật từ bước 4.

**Ngoại lệ viết test trước:** bản dịch là tài nguyên chữ. Test chặn (`CameraStringsTest`) viết trước và thất bại trước, nhưng đúng nghĩa của từng câu dịch không kiểm tự động được; người biết tiếng duyệt (dòng cuối của bảng trên).

### 4.1. Chạy thử

- **Đã chạy thử** (2026-10-25, worktree tạm từ `origin/main` `0dfd2a4`, không commit; L-075):
  - (a) `CameraStringsTest` đúng như mục 3.1, tệp tạm.
  - (b) Thêm `vi-` vào cả 25 chỗ `qualifiers`/`SHORT_SCREEN` của 12 lớp ở mục 3.2 (`sed` thay `qualifiers = "w` thành `qualifiers = "vi-w`, `SHORT_SCREEN = "w` thành `SHORT_SCREEN = "vi-w`; sau khi thay không còn chỗ nào thiếu).
  - (c) Ba test `smallPortuguese_*` đúng như mục 3.1 (với tham số `probe` của `setScreen`).
- **Lệnh:** `./gradlew --offline :feature:camera:testDebugUnitTest --tests '*CameraStringsTest'`; `./gradlew --offline :feature:camera:verifyRoborazziDebug` với (b); `./gradlew --offline :feature:camera:testDebugUnitTest --tests '*CameraScreenTest.smallPortuguese*' --tests '*ReviewScreenTest.smallPortuguese*'`, rồi `--tests '*CameraStringsTest' --tests '*CameraScreenOverlayTest.smallPortuguese*'` sau khi đổi cách dò tiếng Việt. Plan không dùng kỹ thuật mới (E là 1), nên không cần chạy `scripts/check.sh`; phiên chạy `check.sh` như mọi task.
- **Kết quả thật:**
  - `CameraStringsTest.english_isDefault`: **thất bại đúng lý do**, `en camera_adjust_flip is Vietnamese` ("Lật", chuỗi tiếng Việt đầu tiên theo thứ tự của `R.string`; bảng chữ có dấu viết sẵn của lần chạy đầu sót chữ hai dấu nên báo muộn ở `camera_adjust_lock`, đã đổi sang cách NFD).
  - `CameraStringsTest.everyReleaseLanguage_isTranslated`: **thất bại đúng lý do**, `hi camera_adjust_flip is not translated. Actual: Lật`.
  - `CameraStringsTest.everyTranslation_keepsPlaceholders`: **đối chứng, qua**.
  - (b): `verifyRoborazziDebug` **qua**, 379 test của module, mọi ảnh chuẩn khớp, không ảnh nào đổi.
  - `ReviewScreenTest.smallPortuguese_saveError_primaryButtonVisible`, `CameraScreenTest.smallPortuguese_permissionDenied_buttonVisible`, `CameraScreenOverlayTest.smallPortuguese_adjustPanel_labelsFit`: **đối chứng, qua** trên code cũ (chưa có `values-pt-rBR`, nên đo chữ tiếng Việt). Sau bước 4 hai test này đo chữ Bồ Đào Nha thật. Thất bại thì sửa bản dịch cho ngắn lại, không sửa test hay bố cục (`AdjustAction` thuộc `:core:designsystem`, ngoài mục 3.2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có (chỉ tài nguyên chữ và test của một module feature).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Nhiều phần trong một module: 14 tệp tài nguyên, 13 tệp test, một ảnh chuẩn; không đổi giao diện công khai |
| B. Độ rõ của yêu cầu | 1 | Spec và cách làm rõ (mẫu settings, onboarding); câu chữ tiếng Anh và bản dịch do AI tự quyết, người duyệt sau |
| C. Khả năng kiểm chứng tự động | 1 | Đủ bản dịch, tham số, bố cục ở chữ dài có test; đúng nghĩa của bản dịch cần người |
| D. Ảnh hưởng chéo | 1 | Không chạm file ngoài module; cùng mẫu với T-051 nhưng khác module |
| E. Độ mới của kỹ thuật | 1 | Lặp lại mẫu settings và onboarding; mới chỉ có cách đọc mọi chuỗi qua `R.string` bằng reflection, đã chạy thử |
| **Tổng** | **6** | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** không có. Các tệp đều trong `feature/camera/`.
- **Task khác có thể xung đột:**
  - T-051 (chữ của `:feature:poses` và `:core:designsystem`, cùng mẫu): khác module, không chung tệp.
    - Thỏa thuận: mỗi module một test chữ riêng (`CameraStringsTest` ở đây; `PosesStringsTest` và test của `:core:designsystem` ở T-051), không tạo hàm dùng chung giữa các module, để test của module này không phụ thuộc module kia.
    - Nhãn chip "Tất cả" của camera (`camera_chip_all`) và chip của thư viện (`:feature:poses`) là hai chuỗi riêng. Hai plan dùng cùng cách dịch cho "All", "Saved", "Recent" nếu có: task chạy sau đọc `values-<ngôn ngữ>` của task đã merge trước khi dịch.
    - Lưu ý cho T-051: kịch bản `T-022`, `T-024` dùng `str=poses_*`, và `str=` đọc `values/`. Sau T-051 thì `values/` là tiếng Anh, và emulator CI chạy `en_US`, nên vẫn khớp.
  - T-052, T-053 (script của cổng chất lượng): không chung tệp.
- **Đề xuất thứ tự chạy:** độc lập. Chạy trước hay sau T-051 đều được; task chạy sau tạo nhánh từ nhánh chính mới nhất. Phiên có thay đổi app nên `device-session` cuối phiên chạy; camera trên emulator sẽ hiện tiếng Anh, kịch bản T-020 không đổi.

## 7. Điều kiện dừng riêng

1. Một test có sẵn của `:feature:camera` ngoài 12 lớp ở mục 3.2 thất bại sau khi đổi tài nguyên, hoặc một trong 12 lớp cần sửa thêm ngoài `qualifiers` và các test mới ở mục 3.2: dừng. Mục 4.1 cho thấy không cần.
2. `verifyRoborazziDebug` báo một ảnh chuẩn cũ lệch: dừng. Không ghi lại ảnh chuẩn cũ.
3. Lint báo `MissingTranslation` hay `ExtraTranslation` mà không sửa được bằng cách thêm hay bỏ chuỗi trong tệp ngôn ngữ của mục 3.2: dừng. Không tắt quy tắc lint.

## 8. Câu hỏi cho người duyệt

1. **Câu chữ tiếng Anh của camera và màn xem lại.** Thiết kế chỉ có bảng chữ tiếng Việt. **Đề xuất của AI:** AI viết bản tiếng Anh theo giọng của settings và onboarding, và dán bảng ba cột (tên chuỗi, tiếng Việt, tiếng Anh) vào mô tả PR để người xem sau. Bản tiếng Anh không chặn merge, sửa được ở PR sau.
2. **Ảnh `_small` ở ngôn ngữ chữ dài.** **Đề xuất của AI:** một ảnh `ReviewScreen_save-error-pt-br_small.png` (trạng thái nhiều chữ nhất), cùng ba test bố cục ở `pt-BR`. Phương án khác: thêm `CameraScreen_permission-denied-hi_small.png` cho chữ Devanagari cao hơn (thêm một ảnh chuẩn).
3. **Người duyệt bản dịch.** **Đề xuất của AI:** như settings và onboarding, bản dịch merge vào staging, và báo cáo phiên ghi vào "Cần người kiểm tra" trước khi phát hành production.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-25, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: AI viết chữ tiếng Anh theo giọng của settings và onboarding, dán bảng ba cột vào mô tả PR. Câu 2: một ảnh ReviewScreen_save-error-pt-br_small.png cùng ba test bố cục ở pt-BR. Câu 3: bản dịch vào staging, người biết tiếng duyệt trước khi phát hành production. Plan qua một rà soát độc lập (0 chặn, 2 nên sửa, đã sửa hết).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
