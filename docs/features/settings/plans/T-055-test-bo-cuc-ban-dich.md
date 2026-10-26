---
ticket: T-055
title: Bố cục và nghĩa của bản dịch ở bảng chỉnh nét viền và nút chụp; "Độ mờ" thành "Độ đậm"
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 4                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: low              # low | medium | high
token_estimate: 10M          # cỡ S (5.8M, scripts/token-usage.py estimate) cộng khoảng 70%: RUN-20261026-01 cho task S thật 8.1M và 11.4M khi đã tính phiên con (L-073); hai module, ghi lại hai ảnh chuẩn, design-compare
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-055: BỐ CỤC VÀ NGHĨA CỦA BẢN DỊCH Ở BẢNG CHỈNH NÉT VIỀN VÀ NÚT CHỤP; "ĐỘ MỜ" THÀNH "ĐỘ ĐẬM"

## 1. Tóm tắt

Plan làm bốn ghi chú rà soát của T-050 và T-051:
- test bố cục bảng chỉnh nét viền ở `es-419`, `id`, `hi`, `th` (khung thấp);
- test nhãn con trượt chặn từ ngược nghĩa, gồm tiếng Thái ở mọi vị trí và tiếng Việt "mờ";
- test nút chính tiếng Bồ Đào Nha của màn chi tiết tư thế một dòng;
- nhãn tiếng Việt "Độ mờ" thành "Độ đậm".

Chạy thử trên code hiện tại (mục 4.1): test tiếng Indonesia bắt được lỗi thật, "Sembunyikan" đè lên "Kunci" 0,33 dp, nên plan đổi bản dịch thành "Sembunyi". Ba test thất bại đúng lý do, sáu test là test chặn qua ngay (có đối chứng). Đổi "Độ đậm" làm đúng hai ảnh chuẩn lệch: `CameraScreen_adjust_dark.png`, `CameraScreen_adjust_small.png`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Settings KB-4 | Khi bấm "Ngôn ngữ" trên Android 13 trở lên thì trang ngôn ngữ của app trong cài đặt máy mở ra; chọn xong quay lại thì app hiện ngôn ngữ mới | Đã làm ở T-050, T-051. Ticket này kiểm thêm: ở mỗi ngôn ngữ phát hành, nhãn bảng chỉnh nét viền không tràn hay đè nhau trên màn thấp; nút chính của chi tiết tư thế tiếng Bồ Đào Nha một dòng; nhãn con trượt độ đậm không đọc ngược nghĩa |
| Camera KB-3 | Khi bấm nút chỉnh thì bảng chỉnh hiện trên khung xem trước, có thanh trượt "Độ mờ" (20% đến 100%) và "Cỡ" (40% đến 160%)… | Con trượt chỉnh độ đậm của nét viền (`OverlaySettings.opacity`, mặc định 1.0, khoảng 0.2 đến 1.0): 100% là đậm nhất. "Độ mờ 100%" đọc thành "mờ hẳn", ngược nghĩa. Tuấn chọn "Độ đậm" (2026-10-26). PR thiết kế trước phiên sửa câu chữ của tiêu chí này trong spec camera (spec sang `changed`, người xác nhận lại) |

Thiết kế tham chiếu: `features/camera/design/README.md`, mục 2 (bảng chữ: "Camera, bảng chỉnh", "Thanh trượt"); ảnh và context `CameraScreen_adjust_dark`. Theo quyết định của Tuấn, phiên có người theo dõi sửa chữ này trên Figma và chạy `scripts/figma-export.py <link section> camera` trong một PR thiết kế riêng, merge trước phiên. Plan chỉ làm phần code và giả định PR đó đã merge (mục 7, điều kiện 1).

Chuỗi đi cùng con số (L-077):
- `camera_adjust_opacity` đi với `camera_adjust_percent` (`%1$d%%`), 20% đến 100%; 100% là nét viền đậm nhất. Từ ngược nghĩa: "trong suốt" (`transparency`, các bản dịch onboarding dùng), tiếng Việt "mờ".
- `camera_adjust_size` ("Cỡ"), 40% đến 160%, 160% là lớn nhất. "Cỡ", "Size", "Tamanho"… không có chiều ngược, không cần test.

Quyết định đã có: spec onboarding, câu hỏi 1 (AI dịch, người biết tiếng duyệt trước khi phát hành); plan T-050 mục 8 (AI viết bản dịch, báo cáo ghi vào "Cần người kiểm tra").

ADR liên quan: ADR-0008, ADR-0020 (thiết kế và screenshot test).

## 3. Giải pháp

### 3.1. Cách làm

**Test nhãn con trượt** (`CameraStringsTest.opacityLabel_isNotTransparency`, đổi):
- Danh sách `TRANSPARENCY` đổi tên thành `OPPOSITE_WORDS` (từ ngược nghĩa trên con trượt), thêm `"vi" to "mờ"` và `"vi" to "trong suốt"`. Các cặp cũ giữ nguyên.
- Cách dò: ngôn ngữ có dấu cách giữa các từ giữ cách so cả từ hiện có (`(?<![\p{L}\p{M}])` đứng trước, để "अपारदर्शिता" của tiếng Hindi không khớp "पारदर्शिता"). Tiếng Thái (`th`) so ở mọi vị trí, vì tiếng Thái viết liền các từ.
- Chú thích của test nêu lý do và plan T-055.

**Test bố cục bảng chỉnh** (`CameraScreenOverlayTest`):
- Thân test `smallPortuguese_adjustPanel_labelsFit` chuyển nguyên sang hàm riêng `assertAdjustLabelsFit()`. Test cũ giữ tên và `@Config`, chỉ gọi hàm này. Không đổi điều test kiểm.
- Bốn test mới gọi cùng hàm, mỗi test một `@Config`:
  - `smallSpanish_adjustPanel_labelsFit`: `b+es+419-w360dp-h640dp-xxhdpi`;
  - `smallIndonesian_adjustPanel_labelsFit`: `in-w360dp-h640dp-xxhdpi`;
  - `smallHindi_adjustPanel_labelsFit`: `hi-w360dp-h640dp-xxhdpi`;
  - `smallThai_adjustPanel_labelsFit`: `th-w360dp-h640dp-xxhdpi`.
- Qualifier `b+es+419` và `in` đã chạy được trên Robolectric (mục 4.1).
- Test `panel_showsSlidersAndActions_andHidesZoom` đổi chữ khẳng định "Độ mờ" thành "Độ đậm" (theo bảng chữ mới của thiết kế).

**Test nút chính tiếng Bồ Đào Nha** (`PoseDetailScreenTest`, thêm `smallScreen_portuguese_buttonOneLine`):
- `@Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")`, cùng nội dung với `smallScreen_portuguese_buttonFullyVisible`.
- Lấy `TextLayoutResult` của chữ nút bằng `onNodeWithText("Fotografar com esta pose", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }`.
- Kiểm `lineCount` bằng 1. Import thêm `SemanticsActions`, `performSemanticsAction`, `TextLayoutResult`, đều đã có trên classpath test (đã chạy thử).

**Tài nguyên chữ** (bước 4):
- `feature/camera/src/main/res/values-vi/strings.xml`: `camera_adjust_opacity` từ "Độ mờ" thành "Độ đậm".
- `feature/camera/src/main/res/values-in/strings.xml`: `camera_adjust_hide` từ "Sembunyikan" thành "Sembunyi". Đo ở mục 4.1: "Sembunyi" nằm ở 206,2 đến 263,8 dp, cách "Kunci" 10 dp và mép bảng (275 dp) 11 dp. Câu hỏi cho người biết tiếng ở mục 8.
- Không đổi `values/` và các ngôn ngữ khác.

**Ảnh chuẩn** (bước 4): đúng hai ảnh lệch vì "Độ đậm", mỗi ảnh 1375 điểm ảnh (mục 4.1). Plan cho phép ghi lại đúng hai ảnh này:
- `feature/camera/src/test/screenshots/CameraScreen_adjust_dark.png`;
- `feature/camera/src/test/screenshots/CameraScreen_adjust_small.png`.

Cách ghi: sau khi sửa `values-vi`, chạy `./gradlew :feature:camera:recordRoborazziDebug`, rồi `git status --short feature/camera/src/test/screenshots/` chỉ được có hai tệp này. Mở cả hai ảnh, xem nhãn "Độ đậm" đủ chữ, không đè "70%", rồi mới chạy `check.sh`.

**Ảnh so sánh thiết kế:** sau `check.sh`, chạy `scripts/design-compare.sh camera`, commit `docs/features/camera/design/review/` cùng PR (`docs/ops/design-workflow.md`, mục 4; không chặn merge). PR thiết kế đã thay `design/screens/CameraScreen_adjust_dark.png` bằng ảnh có "Độ đậm", nên tỷ lệ khác biệt của ảnh này phải bằng hoặc thấp hơn hiện có. Cao hơn rõ thì ghi vào "Cần người kiểm tra" của báo cáo, không dừng.

**Không đổi:**
- code Kotlin của `src/main`;
- `:core:designsystem`, kể cả `AdjustPanelTest`, nơi "Độ mờ" chỉ là nhãn mẫu truyền vào thành phần;
- thiết kế, spec, kịch bản emulator.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/res/values-vi/strings.xml` | `camera_adjust_opacity`: "Độ đậm" |
| Sửa | `feature/camera/src/main/res/values-in/strings.xml` | `camera_adjust_hide`: "Sembunyi" |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraStringsTest.kt` | `OPPOSITE_WORDS` thay `TRANSPARENCY`, thêm hai cặp `vi`; tiếng Thái so ở mọi vị trí |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenOverlayTest.kt` | Hàm `assertAdjustLabelsFit()`; bốn test bố cục mới; "Độ đậm" ở `panel_showsSlidersAndActions_andHidesZoom` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Test `smallScreen_portuguese_buttonOneLine` và ba import |
| Sửa | `feature/camera/src/test/screenshots/CameraScreen_adjust_dark.png` | Ghi lại, nhãn "Độ đậm" |
| Sửa | `feature/camera/src/test/screenshots/CameraScreen_adjust_small.png` | Ghi lại, nhãn "Độ đậm" |
| Sửa | `docs/features/camera/design/review/` | Ảnh ghép và `summary.md` do `scripts/design-compare.sh camera` sinh lại; không sửa tay |

Không có tệp nào khác. Đã tìm nơi đọc hai chuỗi này (L-020):
- `grep -rn 'camera_adjust_opacity\|camera_adjust_hide\|Độ mờ\|Sembunyikan'` trong `app/`, `core/`, `feature/`, `scripts/`: chỉ có `OverlayTools.kt` (gọi `stringResource`), các tệp `values*` và hai tệp test ở trên. Ngoài ra có `AdjustPanelTest` của `:core:designsystem`, nơi "Độ mờ" là nhãn mẫu, không đọc tài nguyên của camera.
- Không kịch bản `deviceScenarios` nào và không chỗ nào của `scripts/tests/run.sh` dùng hai chuỗi này. `str=` đọc `values/`, mà `values/` không đổi.
- `verifyRoborazziDebug` của `:feature:camera` khi đổi `values-vi` chỉ báo hai ảnh ở trên lệch (mục 4.1). `:feature:poses` không có ảnh nào đổi, vì chỉ thêm test không chụp ảnh.

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
| Nhãn con trượt không đọc ngược nghĩa ở mọi ngôn ngữ (L-077) | Unit test (Robolectric) | `CameraStringsTest.opacityLabel_isNotTransparency` với `OPPOSITE_WORDS` |
| Nhãn bảng chỉnh nét viền không tràn hay đè nhau trên màn thấp (L-028) | Compose UI test | `CameraScreenOverlayTest.smallSpanish_adjustPanel_labelsFit`, `smallIndonesian_adjustPanel_labelsFit`, `smallHindi_adjustPanel_labelsFit`, `smallThai_adjustPanel_labelsFit`; `smallPortuguese_adjustPanel_labelsFit` giữ nguyên điều kiểm |
| Nút chính tiếng Bồ Đào Nha một dòng trên màn thấp | Compose UI test | `PoseDetailScreenTest.smallScreen_portuguese_buttonOneLine` |
| Chữ đúng bảng chữ mới của thiết kế ("Độ đậm") | Compose UI test | `CameraScreenOverlayTest.panel_showsSlidersAndActions_andHidesZoom` |
| Giao diện bảng chỉnh ở tiếng Việt | Screenshot test | `CameraScreen_adjust_dark.png`, `CameraScreen_adjust_small.png` ghi lại; phiên mở ảnh, ghi nhận xét vào nhật ký |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, chạy ở `vi`; `scripts/design-compare.sh camera` sau `check.sh`, commit `docs/features/camera/design/review/` cùng PR, dán `review/summary.md` vào mô tả PR (không chặn merge) | 18 ảnh `CameraScreen_*`, `ReviewScreen_*` của `docs/features/camera/design/screens/`; chỉ `CameraScreen_adjust_dark` đổi |
| Ảnh thiết kế của feature settings | Ảnh thiết kế của ticket khác (T-025): `SettingsScreen_default_dark`, `SettingsScreen_default_light`, `SettingsScreen_theme-picker_dark`, `SettingsScreen_theme-picker_light`. Ticket này thuộc feature settings (KB-4) nhưng chỉ đổi chữ của camera | Không chạm |
| Nhãn "Độ đậm" và "Sembunyi" trên máy | Kiểm tra thủ công (máy thật: camera thật) | Trên bản staging, app ở Tiếng Việt rồi Bahasa Indonesia: mở camera, bấm nút chỉnh; con trượt ghi "Độ đậm", bốn nút không đè chữ nhau |
| Bản dịch "Sembunyi" tự nhiên | Kiểm tra thủ công (người: người biết tiếng duyệt bản dịch, spec onboarding câu hỏi 1) | Người biết tiếng Indonesia xem "Sembunyi" (mục 8, câu 1) |

**Thứ tự test viết trước (bước 3):** commit test gồm phần đổi của `CameraStringsTest`, `CameraScreenOverlayTest` (hàm, bốn test, "Độ đậm") và `PoseDetailScreenTest`. Bước 3 chỉ chạy lệnh Gradle của test, không chạy `check.sh`:

```sh
./gradlew :feature:camera:testDebugUnitTest --tests '*CameraStringsTest' --tests '*CameraScreenOverlayTest.small*' --tests '*CameraScreenOverlayTest.panel_showsSlidersAndActions_andHidesZoom'
./gradlew :feature:poses:testDebugUnitTest --tests '*PoseDetailScreenTest.smallScreen_portuguese*'
```

Kết quả phải đúng như mục 4.1:
- ba test thất bại đúng lý do: `opacityLabel_isNotTransparency`, `smallIndonesian_adjustPanel_labelsFit`, `panel_showsSlidersAndActions_andHidesZoom`;
- các test còn lại qua.

Bước 4 sửa hai chuỗi, ghi hai ảnh chuẩn như mục 3.1, rồi chạy `check.sh` (bước 5).

**Test chặn qua ngay** (có đối chứng ở mục 4.1, L-035):
- `smallSpanish_*`, `smallHindi_*`, `smallThai_*` và `smallPortuguese_*` (cũ): nhãn hiện có vừa bảng; chúng chặn bản dịch sau này làm tràn.
- `smallScreen_portuguese_buttonOneLine`: chữ hiện có một dòng. Đối chứng: thay chữ nút bằng chuỗi dài hơn thì test thất bại.
- Phần tiếng Thái của `opacityLabel_isNotTransparency`: "ความทึบ" hiện có không chứa từ ngược nghĩa. Đối chứng: "ปรับความโปร่งใส" làm test mới thất bại, test cũ thì qua.

**Ngoại lệ viết test trước:** Không có. Đổi hai chuỗi có test thất bại trước; hai ảnh chuẩn ghi lại theo plan.

### 4.1. Chạy thử

- **Đã chạy thử** (2026-10-26, worktree tạm từ `origin/main` `3bf61da`, không commit; L-029, L-075): các test đúng như mục 3.1, có thêm dòng `println` để in số đo; rồi đổi tạm tài nguyên theo từng bước dưới đây.
- **Lệnh:** `./gradlew --offline :feature:camera:testDebugUnitTest --tests '*CameraStringsTest' --tests '*CameraScreenOverlayTest.small*' --tests '*CameraScreenOverlayTest.panel_showsSlidersAndActions_andHidesZoom' :feature:poses:testDebugUnitTest --tests '*PoseDetailScreenTest.smallScreen_portuguese*' --continue`; `./gradlew --offline :feature:camera:verifyRoborazziDebug` sau khi đổi `values-vi`. Plan không dùng kỹ thuật mới (E là 0), nên không cần `scripts/check.sh`; phiên chạy `check.sh` như mọi task.
- **Kết quả thật trên code hiện tại:**
  - `opacityLabel_isNotTransparency`: **thất bại đúng lý do**, `vi camera_adjust_opacity reads the other way round: Độ mờ`.
  - `smallIndonesian_adjustPanel_labelsFit`: **thất bại đúng lý do**, `Kunci 163.83333..196.16667 overlaps Sembunyikan 195.83333..274.16666`. Đây là lỗi thật: "Sembunyikan" đè lên "Kunci" 0,33 dp và chỉ cách mép phải của bảng (275 dp) 0,83 dp.
  - `panel_showsSlidersAndActions_andHidesZoom`: **thất bại đúng lý do**, `AssertionError: Độ đậm`.
  - **Test chặn, qua:** số đo nhãn hàng 1 (Lật, Khóa, Ẩn) và hàng 2 (Đặt lại); bảng từ 85 đến 275 dp.
    - `smallPortuguese_*`: Espelhar 100,0 đến 150,0; Travar 162,0 đến 198,0; Ocultar 213,8 đến 256,2; Redefinir 98,7 đến 151,3.
    - `smallSpanish_*`: Voltear 104,3 đến 145,7; Bloquear 154,0 đến 206,0; Ocultar 213,8 đến 256,2; Restablecer 90,5 đến 159,5. "Restablecer" ở hàng 2 một mình nên không đè.
    - `smallHindi_*`: पलटें 114,7 đến 135,3; लॉक 170,0 đến 190,0; छिपाएं 221,2 đến 248,8; रीसेट 114,0 đến 136,0.
    - `smallThai_*`: พลิก 113,3 đến 136,7; ล็อก 169,0 đến 191,0; ซ่อน 223,7 đến 246,3; รีเซ็ต 112,3 đến 137,7.
  - `smallScreen_portuguese_buttonOneLine`: **test chặn, qua**, `lineCount` bằng 1. Đối chứng: chữ nút tạm là "Fotografar agora mesmo com esta pose escolhida" thì thất bại, `lines expected:<1> but was:<2>`.
  - **Phần tiếng Thái** (tạm đặt `values-th` là "ปรับความโปร่งใส"):
    - test mới **thất bại**, `th camera_adjust_opacity reads the other way round: ปรับความโปร่งใส`;
    - test cũ trên `origin/main` **qua** (không bắt được), đúng như ghi chú rà soát.
- **Kết quả sau khi đổi tạm tài nguyên như bước 4** (`values-vi` "Độ đậm", `values-in` "Sembunyi"):
  - Mọi test mới và test đổi đều qua. "Sembunyi" 206,2 đến 263,8 dp.
  - `verifyRoborazziDebug` của `:feature:camera`: 388 test, đúng 2 thất bại vì ảnh lệch, `CameraScreen_adjust_dark.png` và `CameraScreen_adjust_small.png`, mỗi ảnh 1375 điểm ảnh. Mọi ảnh khác 0 điểm ảnh khác.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có (hai chuỗi, test của hai module feature, hai ảnh chuẩn).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai chuỗi, ba tệp test, hai ảnh chuẩn; không đổi code `src/main` |
| B. Độ rõ của yêu cầu | 1 | Bốn điểm rõ; chữ "Sembunyi" do AI chọn, người biết tiếng duyệt sau |
| C. Khả năng kiểm chứng tự động | 1 | Bố cục, nghĩa ngược và chữ có test; ảnh chuẩn ghi lại cần người xem |
| D. Ảnh hưởng chéo | 1 | Phụ thuộc PR thiết kế (Figma, `design/screens/`, spec camera) merge trước phiên |
| E. Độ mới của kỹ thuật | 0 | Lặp lại cách đo của T-050; `GetTextLayoutResult` là API ngữ nghĩa có sẵn, đã chạy thử |
| **Tổng** | **4** | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** không có. `AdjustAction` của `:core:designsystem` không đổi.
- **Task khác có thể xung đột:**
  - PR thiết kế của phiên có người theo dõi (Figma, `figma-export.py camera`, `design/README.md`, spec camera KB-3) phải merge trước. Nó sửa `docs/features/camera/design/screens/` và `context/`; T-055 chỉ sinh lại `design/review/`.
  - Các plan khác của phiên sau (trình chạy kịch bản, khóa hướng dọc, lời báo máy chủ): chưa thấy chung tệp tại lúc viết. Plan khóa hướng dọc có thể đổi `CameraUnfoldedScreenshotTest` hay ảnh `_unfolded`; T-055 không đổi ảnh `_unfolded` nào. `run-check.py` kiểm khi lập danh sách.
- **Đề xuất thứ tự chạy:** độc lập, chạy lúc nào cũng được sau khi PR thiết kế đã merge. Phiên có thay đổi app nên `device-session` cuối phiên chạy; kịch bản không dùng hai chuỗi này.

## 7. Điều kiện dừng riêng

1. Trước bước 3, PR thiết kế chưa merge: `grep -c 'Độ đậm' docs/features/camera/design/README.md docs/features/camera/design/context/CameraScreen_adjust_dark.md docs/features/camera/spec.md` có tệp ra 0, hoặc spec camera không ở `confirmed`. Dừng, không làm task.
2. Sau khi đổi `values-vi`, `verifyRoborazziDebug` báo ảnh chuẩn nào khác hai ảnh ở mục 3.2 lệch: dừng. Không ghi lại ảnh khác.
3. Một test bố cục mới vẫn thất bại sau bước 4 vì một nhãn khác "Sembunyikan": dừng, ghi số đo. Không đổi bản dịch ngoài hai chuỗi của mục 3.2, không sửa test hay `AdjustAction`.

## 8. Câu hỏi cho người duyệt

1. **Nhãn "Ẩn" tiếng Indonesia.** "Sembunyikan" đè lên "Kunci" trên màn thấp (mục 4.1). **Đề xuất của AI:** "Sembunyi" (ngắn hơn, vẫn hiểu là "ẩn"), người biết tiếng duyệt trước khi phát hành. Phương án khác: giữ "Sembunyikan" và đổi `AdjustAction` để nhãn dài tự xuống dòng hay co chữ (sửa `:core:designsystem`, ảnh hưởng mọi nút của bảng, ticket riêng).
2. **"Try again" tiếng Bồ Đào Nha chưa thống nhất:** camera "Tentar de novo", poses "Tentar novamente", settings "Tente de novo". **Đề xuất của AI:** không đổi trong T-055; người biết tiếng chọn một cách khi duyệt bản dịch, sửa bằng PR riêng.
3. **Nhãn mẫu "Độ mờ" trong `AdjustPanelTest` của `:core:designsystem`.** Đó là chữ mẫu truyền vào thành phần, không phải chữ của app. **Đề xuất của AI:** giữ nguyên, để T-055 không chạm `:core:designsystem`.
4. **Câu chữ spec camera KB-3 và bảng lưu trữ** ("Độ mờ, cỡ, lật…", dòng 216). **Đề xuất của AI:** PR thiết kế trước phiên đổi cả hai chỗ thành "Độ đậm" và chuyển spec camera sang `changed`; người xác nhận lại `confirmed` trước khi duyệt danh sách chạy.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** <Duyệt / Duyệt có điều kiện / Yêu cầu sửa / Từ chối>
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
