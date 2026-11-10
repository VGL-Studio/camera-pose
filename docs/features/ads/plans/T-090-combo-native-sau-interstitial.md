---
ticket: T-090
title: Bật combo native toàn màn hình sau interstitial của màn; `resume_inter` không nối combo
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10.9M        # cỡ M, số gợi ý của scripts/token-usage.py estimate (L-081)
blocking_points: [ads, core] # điểm chặn 1 (combo hiện sau interstitial, đổi tần suất quảng cáo) và 7 (`:core:ads`)
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-11-10
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-090: BẬT COMBO NATIVE TOÀN MÀN HÌNH SAU INTERSTITIAL CỦA MÀN; `RESUME_INTER` KHÔNG NỐI COMBO

## 1. Tóm tắt

`MainActivity` đặt `FullScreenAdHost()` một lần, trong `AppTheme`, sau `NetworkDialogHost` (luật HD-05), để lượt combo native toàn màn hình sau `review_interstitial` và `inter_pose_detail` được vẽ, đếm ngược và đóng được (K-01 tới K-08). `ResumeAdTurns` gọi interstitial của `resume_inter` với `combo = false` (spec ads, câu 18; lệch luật A-12 của khung, ghi ở ADR-1009 mới và đề xuất `from-app`). Bản staging bật sẵn `is_show_native_after_inter`. Kiểm bằng test Robolectric với `AdsController` thật và `FullScreenAdHost` công khai (đã chạy thử, mục 4.1: lỗi FB-20261102-01 tái hiện được trên code hiện tại), test đọc mã nguồn của `MainActivity`, bốn kịch bản emulator và mục kiểm tay mới của `manual-test.md`.

## 2. Hiểu yêu cầu

Spec ads không có kịch bản riêng (mục 3); tiêu chí lấy từ câu 18 của `features/ads/spec.md` (Nhật, 2026-11-02) và luật K-01 tới K-08, A-12, HD-05 của `docs/ops/ads-business-rules.md`.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Câu 18, ý 1 | Khi interstitial của màn (`review_interstitial`, `inter_pose_detail`) kết thúc mà `is_show_native_after_inter` bật thì có quảng cáo native toàn màn hình của `native_combo` | Khung đã nối combo cho mọi lượt `showInterstitial` của feature (T-026); app chỉ thiếu chỗ vẽ. Đặt `FullScreenAdHost()` thì lớp phủ hiện. Lệnh sau interstitial (điều hướng tới camera, rời màn xem lại) chạy khi combo đóng, như khung đã làm |
| Câu 18, ý 1 | Khi interstitial dùng thay quảng cáo mở app hay quảng cáo khi quay lại app (`open_app_inter`, `resume_inter`) kết thúc thì không có combo | `open_app_inter` đã không nối combo (`showStartupAd`, `combo = false`); plan thêm test đường hiện thật. `resume_inter` đang nối combo (A-12): đổi `ResumeAdTurns` sang `combo = false` |
| Câu 18, ý 2 | Bản staging bật sẵn combo; Firebase staging vẫn tắt được | `StagingRemoteValuesModule.globalAdSwitchesOn` thêm `IS_SHOW_NATIVE_AFTER_INTER`. Giá trị Firebase staging thắng giá trị cục bộ (ADR-1007), nên tắt được từ xa |
| Câu 18, ý 3 | Production bật khi phát hành | Không làm ở đây: mã quảng cáo `native_combo` thuộc T-075, khóa trên Firebase production là việc của người (ADR-0007, ADR-0009) |
| Câu 18, ý 4 (K-02 tới K-08) | Khi combo mở thì có vòng đếm `time_close_native` giây (mặc định 5) ở góc trên trái; hết đếm hay nạp lỗi thì nút đóng dùng được; nút Back bị chặn trong lúc đếm; nạp lỗi thì tự đóng khi hết đếm | Hành vi của khung, đã có test ở `:core:ads` (`NativeFullScreenOverlayTest`, `NativeFullScreenTurnsTest`). Plan kiểm thêm qua đúng đường mà app dùng: `AdsHost` + `FullScreenAdHost()` công khai với `AdsController` thật |
| FB-20261102-01 | Khi Firebase staging bật khóa và `resume_ad_type = inter` thì sau `resume_inter` không mở lượt combo vô hình giữ chặn quảng cáo quay lại app | Hai thay đổi cùng sửa: `resume_inter` không nối combo, và mọi combo đều có chỗ vẽ |
| `features/network/spec.md`, mục 5 | Mất mạng lúc combo đang hiện thì popup mất mạng chờ tới khi combo đóng | Combo giữ cờ "đang hiện" (`fullScreenAdShowing`) suốt từ interstitial tới lúc đóng (`NativeFullScreenTurns.afterInterstitial`); `AppAdsRoot` báo cờ cho `NetworkDialogController.setFullScreenAdShowing`. Không đổi code mạng |

Thiết kế tham chiếu: Không có thiết kế riêng (spec ads, mục 4); lớp phủ là của khung (thiết kế T-035 của dự án mẫu, K-03).

ADR liên quan: ADR-0026 (khung AdsKit), ADR-0038 (đếm ngược `time_close_native`), ADR-1007 (vế "`MainActivity` không đặt `FullScreenAdHost`" bị thay), ADR-1009 (mới, plan này tạo), ADR-0024 (popup mất mạng), ADR-0029, ADR-0031 (sửa test cũ), ADR-0013, ADR-0014.

## 3. Giải pháp

### 3.1. Cách làm

1. **Chỗ vẽ.** Trong `MainActivity.onCreate`, `setContent`: thêm `FullScreenAdHost()` ngay sau lời gọi `NetworkDialogHost(...) { AppNavHost(...) }`, vẫn trong `Surface` và `AppTheme(darkTheme = darkTheme)` (lớp phủ theo màu của app), trong `AppAdsRoot` (nơi `AdsHost` cấp `LocalAdsController`). Không đặt trong khối nội dung của `NetworkDialogHost` (câu hỏi 1, mục 8): lớp phủ và popup mạng là hai cửa sổ hộp thoại riêng; popup chờ theo cờ `fullScreenAdShowing`, không theo chỗ đặt, nên đặt ngoài giữ `NetworkDialogHost` chỉ bọc nội dung điều hướng. Chú thích một dòng dẫn HD-05, spec ads câu 18, ADR-1009. Mỗi lần `onCreate` đều đặt lại (cùng `setContent`), nên Activity dựng lại vẫn có chỗ vẽ (L-061 của mẫu); hạn đếm ngược nằm ở `NativeFullScreenTurns` theo đồng hồ đơn điệu, không bắt đầu lại (K-02).
2. **`resume_inter` không nối combo.** `ResumeAdTurns.onAppActivityResumed`: `AdFormat.INTERSTITIAL -> port.showInterstitial(activity, placement, combo = false)`, chú thích "No combo after the resume interstitial (spec ads, question 18; ADR-1009): app deviation from A-12". Giữ tham số `combo` của `ResumeAdPort.showInterstitial` (ít lệch mẫu nhất; đợt đồng bộ chỉ gộp một dòng). Sửa thẳng trong `:core:ads` của app, không thêm công tắc của khung (câu hỏi 2, mục 8). KDoc của `AdsController` (đoạn resume ad, dòng "the interstitial of `resume_inter` with the combo, which skips the `ads_interval` gate") đổi thành "the interstitial of `resume_inter`, which skips the `ads_interval` gate and, in this app, never opens the combo (spec ads, question 18)"; đoạn combo ("The startup interstitial never opens the combo") thêm "nor does the resume interstitial". Không đổi hàm nào khác của `AdsController`.
3. **`open_app_inter`.** Không đổi code: `showStartupAd` gọi `showInterstitialTurn(..., combo = false, ...)`. Test mới chạy đường hiện thật (interstitial thử được đưa vào ô, hiện rồi đóng) để chắc không có combo ở lần mở sau khi `open_ad_type` là interstitial.
4. **Bản staging.** `StagingRemoteValuesModule.globalAdSwitchesOn()` thêm `AdsSettings.IS_SHOW_NATIVE_AFTER_INTER`; KDoc bỏ `is_show_native_after_inter` khỏi câu "Not set, so off as their default", thêm "the native combo after the interstitials of the screens (spec ads, question 18; plan T-090)". Sửa `StagingRemoteValuesModuleTest` theo mục 3.5.
5. **Kịch bản emulator.** Ba tệp T-089 đã sửa (`T-058-chup-tu-chi-tiet.txt`, `T-022-chi-tiet-sang-camera.txt`, `T-024-thu-vien-mo-gap.txt`): sau mỗi cặp `wait text="Close" | id=camera_screen 20` / `tap_if text="Close"`, thêm:

   ```text
   # T-090: the native combo may follow the interstitial (spec ads, question 18); close it when it shows (K-04: a failed load closes by itself)
   wait str=ads_close_full_screen | id=camera_screen 20
   tap_if str=ads_close_full_screen
   ```

   (Mỗi tệp có đúng một chỗ: T-058 dòng 28, T-022 dòng 41, T-024 dòng 51.) Nút đóng của lớp phủ có `contentDescription` "Close ad" (`ads_close_full_screen` của `:core:ads`), không có `resource-id` vì hộp thoại là cửa sổ khác, không nhận `exposeTestTagsForUiAutomator`. `text="Close"` không khớp nút này (plan T-089, mục 3.4). Tệp mới `feature/poses/src/deviceScenarios/T-090-combo-sau-chi-tiet.txt` (mục 4).
6. **Tài liệu đi cùng PR.**
   - `docs/features/camera/spec.md` mục 5: thêm đoạn "Sau `review_interstitial` có thể có quảng cáo native toàn màn hình (combo, `native_combo`, khóa `is_show_native_after_inter`), đếm ngược `time_close_native` giây rồi đóng được; rời màn xem lại khi combo đóng (`features/ads/spec.md`, câu 18)". `docs/features/poses/spec.md` mục 5: cùng ý cho dòng `inter_pose_detail` ("camera mở khi combo đóng"). Tiêu chí cùng ý ở kịch bản: spec poses KB-4 (dòng 96, "đóng quảng cáo thì camera mở với đúng tư thế đó") thành "đóng quảng cáo, gồm cả combo nếu có, thì camera mở với đúng tư thế đó"; spec camera KB-9 (dòng 174, "một quảng cáo toàn màn hình hiện trước khi về camera") thêm "; combo native toàn màn hình có thể hiện ngay sau nó, về camera khi combo đóng (mục 5)". Đã xem cả hai spec: không còn tiêu chí nào khác nói điều xảy ra ngay sau khi đóng interstitial. Hai spec chuyển `changed`, thêm dòng lịch sử "Chép câu 18 của spec ads (T-090), chờ xác nhận", như T-074 đã làm với bốn spec (câu hỏi 3, mục 8).
   - ADR mới `docs/adr/1009-combo-native-sau-interstitial-cua-man.md` (`origin: app`, `status: accepted`, `deciders: Nhật`, nhận lúc duyệt plan theo mục 8 câu 6; cấu trúc như ADR-1008). Phần đầu `supersedes: ADR-1007, ADR-0026` kèm chú thích "ADR-1007: chỉ vế `MainActivity` không đặt `FullScreenAdHost`; ADR-0026: chỉ luật A-12 của `docs/ops/ads-business-rules.md` mà vế 'hành vi quảng cáo theo ads-business-rules.md' dẫn tới, ở vế 'rồi nối combo'; các vế khác giữ nguyên" (`docs/CLAUDE.md`, mục Khi viết ADR: không sửa ADR `origin: template`, viết ADR của app ghi rõ thay quy tắc nào và vì sao). Mục Quyết định: (a) `MainActivity` đặt `FullScreenAdHost()` (HD-05), thay vế của ADR-1007; (b) combo sau mọi interstitial của màn, không sau `open_app_inter`, `resume_inter` (spec ads câu 18); (c) app không theo vế "rồi nối combo nếu `is_show_native_after_inter` bật" của luật A-12 trong `ads-business-rules.md` mà ADR-0026 dựa vào: lệch có chủ đích, một dòng `combo = false` trong `ResumeAdTurns`, đợt đồng bộ phải giữ; (d) `docs/ops/ads-guide.md` dòng 42 ("inter của resume nối combo khi `is_show_native_after_inter` bật") và dòng 257 ("Đặt `resume_ad_type = inter` (Firebase staging): quay lại thì có inter rồi combo") không đúng với app này; hai file đó là của mẫu, không sửa (câu hỏi 5); (e) đề xuất `from-app`. Mục "Việc AI phải tuân thủ" viết thành các vế ngắn nối bằng dấu chấm phẩy. ADR-1007 không sửa (`docs/CLAUDE.md`, mục Những điều không được làm). Sinh lại `docs/foundation.md` bằng `scripts/foundation.py`.
   - `docs/ops/manual-test.md` mục 11: dòng Firebase staging "Không có `show_open_ads_first_open`, `is_show_native_after_inter`, `is_show_native_collap` đặt `true`" thành "Không có `show_open_ads_first_open`, `is_show_native_collap` đặt `true`; không có `is_show_native_after_inter` đặt `false` (T-090)"; thêm mục con "11.8 Combo native sau interstitial (T-090)" (sau 11.7) với các bước kiểm tay ở mục 4 dưới. Mục 11.1 bước 4 ("đóng quảng cáo thì về camera") thành "đóng interstitial, rồi combo nếu có (mục 11.8), thì về camera"; bước 7 ("Đóng quảng cáo thì camera mở với đúng tư thế vừa chọn") thành "Đóng interstitial, rồi combo nếu có (mục 11.8), thì camera mở với đúng tư thế vừa chọn".
   - Mô tả PR có mục đề xuất `from-app` (`docs/ops/from-app.md`): (a) khung có công tắc cho `resume_inter` nối combo hay không (mặc định như mẫu); (b) khung không mở lượt combo khi không có host nào vẽ (TS-20261102-01, P-1, việc 3). Không tự mở issue.

Mẫu làm theo: `AdsControllerHostTest` (controller thật, interstitial giả trong ô, quay lại app bằng `ActivityController`), `NativeFullScreenOverlayTest` (hộp thoại, dựng lại Activity bằng `scenario.recreate()` rồi `setContent` lại), `MainActivityForegroundWiringTest` (đọc mã nguồn `MainActivity`, bỏ chú thích và chuỗi).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Import và một lời gọi `FullScreenAdHost()` sau `NetworkDialogHost`, trong `AppTheme` (mục 3.1, bước 1) |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/MainActivityFullScreenAdHostWiringTest.kt` | Test đọc mã nguồn (mục 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ResumeAdTurns.kt` | `combo = false` cho `resume_inter`, chú thích |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsController.kt` | Chỉ KDoc (hai câu ở mục 3.1, bước 2) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ResumeAdTurnsTest.kt` | Hai câu khẳng định viết lại theo ADR-0031, đổi tên một test (mục 3.5) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerComboScopeTest.kt` | Combo sau interstitial nào, qua `AdsController` thật (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/FullScreenAdHostComboTest.kt` | `AdsHost` + `FullScreenAdHost()` với controller thật: lớp phủ, đóng, Back, dựng lại, ra nền (mục 4) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Bật `IS_SHOW_NATIVE_AFTER_INTER`, KDoc |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Dữ liệu (ADR-0029), một câu viết lại (ADR-0031), test viết trước mới `combo_isOnAtStaging`, KDoc lớp (mục 3.5) |
| Sửa | `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` | Đóng lớp phủ combo khi có (mục 3.1, bước 5) |
| Sửa | `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` | Như trên |
| Sửa | `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` | Như trên |
| Tạo | `feature/poses/src/deviceScenarios/T-090-combo-sau-chi-tiet.txt` | Kịch bản combo sau `inter_pose_detail` (mục 4) |
| Sửa | `docs/features/camera/spec.md` | Mục 5, KB-9 (dòng 174), `status: changed`, dòng lịch sử |
| Sửa | `docs/features/poses/spec.md` | Mục 5, KB-4 (dòng 96), `status: changed`, dòng lịch sử |
| Tạo | `docs/adr/1009-combo-native-sau-interstitial-cua-man.md` | ADR của app thay vế `FullScreenAdHost` của ADR-1007 và vế nối combo của luật A-12 mà ADR-0026 dựa vào |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` |
| Sửa | `docs/ops/manual-test.md` | Mục 11: dòng Firebase staging, mục 11.1 bước 4 và 7, mục mới 11.8 |
| Sửa | `docs/features/ads/plans/T-090-combo-native-sau-interstitial.md` | Nhật ký thực hiện |
| Sửa | `docs/features/ads/tickets/T-090-bat-combo-native-toan-man-hinh.md` | `status` theo `auto-run.md` |

Nơi đọc các file bị đổi (L-020 của mẫu): `StagingRemoteValuesModule` chỉ được `StagingRemoteValuesModuleTest` và Hilt đọc (`grep -rln StagingRemoteValuesModule scripts .github */src/test app/src/test*`); `LocalRemoteValuesSourceSetTest` chỉ kiểm source set, không đọc giá trị. Bảng khóa `docs/remote-config-keys.md` và `docs/remote-config-template.json` ghi mặc định của khóa (`false`), không đổi vì mặc định không đổi. `MainActivity.kt` được `MainActivityForegroundWiringTest` đọc theo ký hiệu `observeAppForeground`, `onAppRestored`: không ảnh hưởng. Ba tệp kịch bản được `scripts/device-scenario.py` và `device-session.yml` đọc; cú pháp kiểm bằng `check` (mục 4). Không có ảnh chuẩn nào đổi: lớp phủ là hộp thoại, không vẽ khi `LocalAdsController` rỗng (test, preview). Các nơi khác mà `scripts/plan-check.py` dò (L-020): `scripts/tests/run.sh` đọc `MainActivity.kt` để đếm `exposeTestTagsForUiAutomator()` (đúng 1) và `testTag(APP_ROOT_TAG)` (đúng 1), plan không thêm lời gọi nào của hai thứ đó (lớp phủ là cửa sổ của `:core:ads`, không gọi `exposeTestTagsForUiAutomator`); `run.sh` so `docs/foundation.md` với các ADR, nên `foundation.md` sinh lại cùng ADR-1009 (có trong bảng); `run.sh` chỉ nhắc `spec.md` của repo giả tạo trong thư mục tạm, không đọc hai spec của app; `scripts/status.py` đọc `status` trong phần đầu của mọi spec để vẽ trang tiến độ, `changed` là trạng thái hợp lệ, không cần sửa script; `.github/workflows/pr-check.yml` chỉ nhắc `foundation.md` trong chú thích (PR không có code chạy `run.sh`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Khóa Remote Config đã có: `is_show_native_after_inter` (K-01), `time_close_native` (K-02); không thêm khóa.
- Quảng cáo: vị trí toàn app `native_combo` (`AdPlacement.NATIVE_AFTER_INTER`, đã có từ T-074, không có `is_show_<mã>`), theo spec ads câu 18. Không thêm vị trí. Đổi luật: `resume_inter` không nối combo.
- Sự kiện phân tích: Không thêm. Combo ghi `ad_opportunity` (K-07) và các sự kiện vòng đời của khung qua `FirebaseAdMetrics` như mọi lượt native toàn màn hình (T-074); lượt combo nay thật sự hiện thay vì ghi `not_ready`.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `FullScreenAdHost` thuộc `:core:ads`, `:app` đã phụ thuộc `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Điểm kỹ thuật chưa chắc (tìm node của lớp phủ trong hộp thoại với `AdsController` thật, dựng lại Activity và ra nền giữa lúc đếm trên Robolectric) đã chạy thử qua (mục 4.1).

### 3.5. Hành vi đã có bị đổi

Từ `python3 scripts/existing-tests.py docs/features/ads/plans/T-090-combo-native-sau-interstitial.md`. Ba câu khẳng định viết lại theo ADR-0031 (đúng giới hạn 3 câu mỗi task): hai câu của `ResumeAdTurnsTest` và câu so danh sách tên của `firstOpenAndCombo_areNotSet`. Bảng `expected` và danh sách `off` của test staging là dữ liệu (ADR-0029), không tính; phần combo của test staging chuyển sang test viết trước mới `combo_isOnAtStaging`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Interstitial `resume_inter` nối combo (A-12) | Bước 2: `combo = false` cho `resume_inter` | `ResumeAdTurnsTest.interResume_showsInterstitialWithCombo`, dòng 115: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, true)), port.interShows)` | Ý định: lượt quay lại app dạng interstitial gọi `showInterstitial` đúng một lần trên Activity vừa resume, với cờ combo đúng luật của app. Đổi tên `interResume_showsInterstitialWithoutCombo`; câu mới `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, false)), port.interShows)`; giữ `assertTrue(port.appOpenShows.isEmpty())`. Đối chứng đỏ khi ý định bị phá: `AdsControllerComboScopeTest.resumeInter_closed_noCombo` (đường thật) và câu này đỏ nếu `combo = true` quay lại |
| Như trên | Như trên | `ResumeAdTurnsTest.typeChangedBeforeResume_showsNothing`, dòng 360 (câu đối chứng: lượt sau với loại mới hiện interstitial một lần) | Ý định: đổi loại trước lúc resume thì không hiện gì, lượt sau hiện đúng một interstitial. Câu mới `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, false)), port.interShows)`; các câu khác giữ |
| Staging không đặt `is_show_native_after_inter` | Bước 4: staging bật sẵn | `StagingRemoteValuesModuleTest.firstOpenAndCombo_areNotSet` | Ý định cũ có hai phần: lần mở đầu không có quảng cáo toàn màn hình (KB-1), và combo tắt; phần thứ hai trái câu 18. Đổi tên `firstOpen_isNotSet`. Dữ liệu (ADR-0029, không tính vào giới hạn của ADR-0031): danh sách `off` bỏ `AdsSettings.IS_SHOW_NATIVE_AFTER_INTER`, chỉ còn `SHOW_OPEN_ADS_FIRST_OPEN`. Câu viết lại (ADR-0031, 1 câu): câu cũ `assertEquals(listOf("show_open_ads_first_open", "is_show_native_after_inter"), off.map { it.name })` thành `assertEquals(listOf("show_open_ads_first_open"), off.map { it.name })`. Giữ nguyên: `assertTrue(byName.toString(), "show_open_ads" in byName)` và vòng `off.forEach { assertTrue("${key.name} must not be set on staging", key.name !in byName); assertEquals(false, key.default) }` (nay chạy cho một khóa, không nới vì khóa combo có test riêng dưới). Đối chứng đỏ khi ý định bị phá: đặt `SHOW_OPEN_ADS_FIRST_OPEN` ở staging thì vòng đỏ |
| (mới) | Bước 4 | Không có | Test viết trước mới `StagingRemoteValuesModuleTest.combo_isOnAtStaging` (`runBlocking`): `assertEquals("is_show_native_after_inter", AdsSettings.IS_SHOW_NATIVE_AFTER_INTER.name)`; `assertEquals(true, byName["is_show_native_after_inter"])`; `assertEquals(true, localOnly.value(AdsSettings.IS_SHOW_NATIVE_AFTER_INTER).first())`; đối chứng `assertEquals(false, AdsSettings.IS_SHOW_NATIVE_AFTER_INTER.default)` (mặc định tắt, nên staging phải đặt; L-035). Thất bại trước code: khóa không có trong giá trị cục bộ |
| Như trên (dữ liệu) | Như trên | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` | Giữ câu khẳng định; bảng `expected` thêm `"is_show_native_after_inter" to true` kèm chú thích "The native combo after the interstitials of the screens (spec ads, question 18; plan T-090)" (ADR-0029: dữ liệu của khóa plan bật). KDoc lớp: "the first open stays off and the combo is on" |
| `MainActivity` không đặt `FullScreenAdHost`; combo mở mà không vẽ (FB-20261102-01) | Bước 1 | Không có test khẳng định việc không đặt. `AppAdsRootTest` (cờ báo cho popup mạng, Activity dựng lại) không đổi | Giữ |
| KDoc `AdsController` | Bước 2 | Không có test đọc KDoc | Giữ |

Kết quả `existing-tests.py` (2026-11-10) và cách đã xem từng nhóm: cuối tài liệu này, mục "Kết quả existing-tests.py".

## 4. Kiểm thử

Test viết trước, thất bại đúng lý do trên code hiện tại (đã thấy ở mục 4.1 cho các test có dấu "đã chạy thử").

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| `resume_inter` không nối combo (câu 18), cả khi khóa bật | Unit test (JVM, port giả) | `ResumeAdTurnsTest.interResume_showsInterstitialWithoutCombo`, `typeChangedBeforeResume_showsNothing` (mục 3.5). Thất bại trước code: cờ `true` |
| Như trên, qua `AdsController` thật (đường của FB-20261102-01) | Robolectric | `AdsControllerComboScopeTest.resumeInter_closed_noCombo`: `FakeRemoteSettings` có `IS_SHOW_NATIVE_AFTER_INTER = true`, `SHOW_RESUM_ADS = true`, `RESUME_AD_TYPE = "inter"`; `start()`, `openConsentGateForTest()`, `idleFor(45_100)` (hết trần lượt khởi động); `putLoadedInterstitialForTest(RESUME_INTER, FakeInterstitialAd())`; Activity dựng sau `start()` rồi `pause().stop()`, `start().resume()`; `fake.shows == 1`; gọi `fake.fullScreenContentCallback!!.onAdDismissedFullScreenContent()`; `idleFor(2_100)`. Khẳng định: không dòng log nào bắt đầu bằng `Native combo opened`; `nativeFullScreenView.value == null`; `fullScreenAdShowing.value == false`; `resumeAdsSuppressed.value == false` (không giữ chặn quảng cáo quay lại app). Đã chạy thử: thất bại đúng lý do trên code hiện tại (log có `Native combo opened (inter gone: true)`), qua khi có bước 2 |
| `review_interstitial` và `inter_pose_detail` vẫn nối combo; lệnh sau interstitial chỉ chạy khi combo đóng | Robolectric | `AdsControllerComboScopeTest.screenInterstitial_closed_opensCombo_finishesOnClose`, chạy cho từng vị trí trong `listOf(REVIEW_INTERSTITIAL, INTER_POSE_DETAIL)` (một vòng `forEach` trong test, mỗi vòng một controller mới): khung như `AdsControllerHostTest` (`start()`, `openConsentGateForTest()`, `AdUnitIds { null }`), khóa combo và `AdsSettings.placementEnabled(placement)` bật, `putLoadedInterstitialForTest(placement, fake)`, `controller.showInterstitial(activity, placement) { finishes++ }` trên `Dispatchers.Main` như `AdsControllerHostTest.showInterstitial`; `fake.shows == 1`; đóng (`onAdDismissedFullScreenContent()`), `idleFor(2_100)`: `nativeFullScreenView.value != null`, `finishes == 0`, log có `Native combo opened`; `closeNativeFullScreen()` (nút đóng dùng được ngay vì combo nạp lỗi: không có mã vị trí, K-03): `finishes == 1`, view `null`, log có `Native combo closed (shown: false)`. Đối chứng của dòng trên. Qua ngay trên code hiện tại (hành vi của khung, giữ) |
| `open_app_inter` (lần mở sau, `open_ad_type` interstitial) không nối combo trên đường hiện thật | Robolectric | `AdsControllerComboScopeTest.startupInterstitial_shownAndClosed_noCombo`: khung như `AdsControllerHostTest` (`start()`, `openConsentGateForTest()`), `SHOW_OPEN_ADS = true`, `OPEN_AD_TYPE` là giá trị interstitial, khóa combo bật, `putLoadedInterstitialForTest(OPEN_APP_INTER, fake)`, `showStartupAd(activity, StartupAdTurn(OPEN_APP_INTER)) { results += it }`; `fake.shows == 1`; đóng; `idle()`: `results == listOf(true)` ngay; `idleFor(5_000)`: view `null`, `results` vẫn một phần tử. Qua ngay (đường đã đúng, test giữ cho đợt đồng bộ sau); bước đối chứng là test của dòng trên, cùng khung, có combo |
| Mất mạng trong lúc combo: popup chờ, đóng combo thì popup hiện (`features/network/spec.md`, mục 5) | Robolectric | `AdsControllerComboScopeTest.screenInterstitial_showingFlag_noGapUntilComboCloses`: phần kiểm chính là test có sẵn `FullScreenAdPresenceTest.publishAfter_closeThenBegin_publishesNoFalse` (cờ không phát `false` khi lượt interstitial đóng rồi combo nhận cờ ngay). Test mới kiểm trên controller thật: ghi đồng bộ giá trị của `controller.fullScreenAdShowing.value` ngay sau từng bước (trước khi hiện, sau `fake.show`, ngay sau `onAdDismissedFullScreenContent()` trên cùng stack, sau `idleFor(2_100)` khi combo đã mở, sau `closeNativeFullScreen()`): đúng `[false, true, true, true, false]`. Thêm một job `launch(Dispatchers.Main.immediate) { flag.collect { values += it } }` (thu không dùng hàng đợi, nên mọi lần đổi được nhận ngay trên luồng chính) và khẳng định `values == listOf(false, true, false)`; hủy job ở cuối test (`job.cancel()` trong `finally`). Bước đối chứng lúc viết test (ghi vào nhật ký, không commit): tạm bỏ `publishAfter` của `FullScreenAdPresence` thì câu `values` đỏ vì có `false` ở giữa; không đỏ thì test không kiểm được khoảng hở, dựa vào `FullScreenAdPresenceTest` và ghi vào nhật ký. Phần còn lại của chuỗi đã có test: `AppAdsRootTest.fullScreenAdShowing_isReportedToNetworkDialog` (cờ tới `setFullScreenAdShowing`), `NetworkDialogControllerTest` (popup chờ khi cờ bật, hiện khi cờ tắt). Kiểm tay ở bước 5 dưới |
| Khi lượt combo mở thì lớp phủ có trong cây giao diện; đóng thì về màn cũ, `Native combo closed` được ghi | Robolectric, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, `createAndroidComposeRule<ComponentActivity>()` | `FullScreenAdHostComboTest.combo_overlayInTree_closeReturnsToScreen`: nội dung `AdsHost(controller) { Box(Modifier.fillMaxSize().testTag(SCREEN)); FullScreenAdHost() }` (đúng hai composable công khai mà `MainActivity` dùng); `openConsentGateForTest()`, `putPreloadedNativeFullScreenForTest(NATIVE_AFTER_INTER, FakeLoadedNativeAd(FakeNativeAdAssets.Design))`; `beginInterstitialTurn(REVIEW_INTERSTITIAL, id, combo = true) { finishes++ }` rồi `onInterstitialClosed`; `idleFor(2_100)` (`shadowOf(mainLooper).idleFor` rồi `waitForIdle`): một node `NativeFullScreenAdTestTags.COUNTDOWN`, node `SCREEN` vẫn có, `finishes == 0`, `fullScreenAdShowing == true`; `idleFor(5_100)`; `onNodeWithTag(CLOSE).performClick()`: `finishes == 1`, không còn node `CLOSE`, node `SCREEN` vẫn có, `fullScreenAdShowing == false`, log có đúng `Native combo closed (shown: true)`. Đã chạy thử (qua, vì khung đã đúng; phần mới của app là chỗ đặt, kiểm ở dòng `MainActivity` dưới) |
| Nút Back trong lúc đếm không đóng; hết đếm thì đóng (K-08) | Robolectric | `FullScreenAdHostComboTest.back_duringCountdown_ignored_afterCountdown_closes`: như trên tới lúc có `COUNTDOWN`; Back trên hộp thoại (`ShadowDialog.getLatestDialog() as ComponentDialog`, `onBackPressedDispatcher.onBackPressed()`, như `NativeFullScreenOverlayTest.pressBackOnDialog`): vẫn có `COUNTDOWN`, `finishes == 0`; `idleFor(5_100)`, Back lần nữa: `finishes == 1`, không còn `CLOSE` |
| Activity dựng lại trong lúc đếm (L-061 của mẫu) | Robolectric | `FullScreenAdHostComboTest.recreate_duringCountdown_overlayKept_deadlineKept`: mở combo, `idleFor(1_000)`, `scenario.recreate()` rồi `onActivity { it.setContent { Shell() } }` (như `MainActivity` đặt lại nội dung trong `onCreate`): một node `COUNTDOWN`, `finishes == 0`; `idleFor(4_500)` (lớp phủ mở ở khoảng giây 2,0 của `idleFor(2_100)`, nên nay là khoảng 5,6 giây sau lúc mở): có node `CLOSE`, không còn `COUNTDOWN`; hạn bị bắt đầu lại lúc dựng lại thì phải tới khoảng 6,0 giây sau lúc mở mới có nút, nên test đỏ; bấm: `finishes == 1`. Đã chạy thử (bản gần đúng, qua) |
| App ra nền giữa lúc đếm rồi trở lại (L-091 của mẫu) | Robolectric | `FullScreenAdHostComboTest.background_duringCountdown_resumedPastDeadline_closes`: mở combo, `moveToState(CREATED)`, `idleFor(6_000)`, `moveToState(RESUMED)`: một node `CLOSE` (có quảng cáo nên không tự đóng), `finishes == 0`; bấm: `finishes == 1`. Đã chạy thử (qua) |
| `MainActivity` đặt `FullScreenAdHost()` đúng một lần, trong `AppTheme`, sau `NetworkDialogHost` | Unit test đọc mã nguồn | `MainActivityFullScreenAdHostWiringTest.mainActivity_placesFullScreenAdHostOnce`: đọc `src/main/kotlin/.../MainActivity.kt` (thư mục làm việc của unit test là thư mục module), bỏ chú thích và chuỗi (`codeOnly`, chép từ `MainActivityForegroundWiringTest`, như nó chép từ `LeaveAppCallSitesTest`); đối chứng `class MainActivity` có trong code; `Regex("""\bFullScreenAdHost\(\s*\)""")` khớp đúng 1 lần; vị trí khớp nằm trong ngoặc của `override fun onCreate(`, trong ngoặc của lambda sau `AppTheme(darkTheme = darkTheme)`, và sau dấu `}` đóng lambda nội dung cuối của lời gọi `NetworkDialogHost(`: từ vị trí khớp `\bNetworkDialogHost\(`, đếm ngoặc tròn tới `)` đóng danh sách tham số, rồi lấy `{` kế tiếp (lambda nội dung đặt sau ngoặc) và đếm ngoặc nhọn tới `}` của nó (hàm `bodyOf` đếm ngoặc như test kia). Chỗ đặt trong lambda nội dung (phương án khác của câu hỏi 1) thì test đỏ; nếu người duyệt chọn phương án đó, câu này đổi thành "trong lambda nội dung của `NetworkDialogHost`, sau lời gọi `AppNavHost(`". Quét theo ký hiệu, không theo chuỗi con (L-062 của mẫu): chú thích nhắc `FullScreenAdHost` không làm test đỏ hay xanh sai. Thất bại trước code: 0 lần khớp |
| Bản staging bật combo, Firebase staging vẫn tắt được | Unit test | `StagingRemoteValuesModuleTest.combo_isOnAtStaging`, `firstOpen_isNotSet`, `localValues_areExactlyTheAppSet` (mục 3.5). Thất bại trước code: khóa không có trong giá trị cục bộ. "Firebase thắng giá trị cục bộ" đã có test của `DefaultRemoteSettings` (ADR-1007), không đổi |
| Khóa tắt giữa chừng (L-061 của mẫu): khóa đọc lúc interstitial hoàn tất (K-01) | Unit test có sẵn | `NativeFullScreenTurnsTest` (dòng 106: bật lúc mở lượt, tắt lúc hoàn tất thì không có combo; `comboKeyOff_whileOpen_staysUntilClosed_thenNoNextCombo`). Không đổi, không viết thêm |
| Ba kịch bản emulator có sẵn đi qua combo khi nó hiện | Kiểm tra thủ công (emulator: `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`, `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`) | `python3 scripts/device-scenario.py check <ba tệp>` trả mã 0. `device-session` cuối phiên: ba dòng `đạt`; trong `scenarios.txt`, bước `tap_if str=ads_close_full_screen` có dòng `note` kèm tọa độ và nhãn `Close ad` (combo hiện, được đóng) hay không có tọa độ (không có interstitial nên không có combo, hay combo nạp lỗi đã tự đóng) |
| Combo sau `inter_pose_detail`: lớp phủ hiện, đóng được, camera mở; không kẹt | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-090-combo-sau-chi-tiet.txt`) | Tệp mới, nội dung ở dưới. `python3 scripts/device-scenario.py check feature/poses/src/deviceScenarios/T-090-combo-sau-chi-tiet.txt` trả mã 0. `device-session`: `đạt`, hay `bỏ qua (môi trường)` khi emulator không có interstitial (`need`). Ảnh `combo.png` cho người xem |
| Combo sau `review_interstitial` (rời màn xem lại sau ảnh lưu thứ 2, rồi mỗi 3 ảnh) | Kiểm tra thủ công (máy thật: camera thật; luật đếm ảnh lưu cần chụp và lưu nhiều ảnh) | Bước 2 của kịch bản kiểm tay dưới |
| Đếm ngược, nút đóng, Back trên máy với quảng cáo thử thật; không có combo sau `resume_inter` | Kiểm tra thủ công (máy thật: quảng cáo thật; Remote Config: `resume_ad_type = inter` đặt trên Firebase staging) | Bước 3, 4 dưới |
| Tài liệu: hai spec, ADR-1009, `manual-test.md` | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc; Nhật xác nhận lại hai spec `changed`; ADR-1009 đã `accepted` từ lúc duyệt plan |

**Tệp kịch bản mới** `feature/poses/src/deviceScenarios/T-090-combo-sau-chi-tiet.txt`:

```text
ticket: T-090
criterion: Sau inter_pose_detail có combo native toàn màn hình; đóng được hay tự đóng khi nạp lỗi (K-03, K-04), rồi camera mở (spec ads, câu 18)
timeout: 240
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
# T-083: page 1 may hold Next up to 5 s while its ad card waits; four rounds for at most four pages and one spare
wait id=onboarding_next enabled=true | id=onboarding_enable_camera | id=camera_screen 15
tap_if id=onboarding_next enabled=true
wait id=onboarding_next enabled=true | id=onboarding_enable_camera | id=camera_screen 15
tap_if id=onboarding_next enabled=true
wait id=onboarding_next enabled=true | id=onboarding_enable_camera | id=camera_screen 15
tap_if id=onboarding_next enabled=true
wait id=onboarding_next enabled=true | id=onboarding_enable_camera | id=camera_screen 15
tap_if id=onboarding_next enabled=true
tap_if id=onboarding_enable_camera
wait id=camera_screen 15
need id=camera_poses 15
tap id=camera_poses
wait id=poses_grid 20
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
tap str=poses_detail_shoot
# The combo follows only an interstitial that showed (K-01): no interstitial on this emulator is the environment
need text="Close" 20
tap text="Close"
# Close button at once when the combo load fails (K-03), else after time_close_native (5 s); a failed load closes by itself (K-04)
wait str=ads_close_full_screen | id=camera_screen 20
shot combo
tap_if str=ads_close_full_screen
wait id=camera_screen 15
expect_no str=ads_close_full_screen
shot camera-after-combo
```

Tệp không khẳng định lớp phủ phải hiện: emulator CI không có quảng cáo native thử ổn định, combo nạp lỗi thì tự đóng sau 5 giây và có thể đóng trước lần đọc màn hình kế tiếp (ticket, "Kịch bản emulator"). Phần "có lớp phủ" kiểm bằng test Robolectric ở trên; tệp kiểm không kẹt (lỗi của FB-20261102-01) và camera mở sau đó.

**Kịch bản kiểm tay (máy thật, bản staging từ nhánh chính sau merge; mục mới 11.8 của `manual-test.md`).** Firebase staging không đặt `is_show_native_after_inter = false`. Xem log `adb logcat -v threadtime | grep ' VGL\.Ads'`. Dòng log dẫn tới code: `Native combo opened (inter gone: <true|false>)` ghi ở `NativeFullScreenTurns.afterInterstitial` khi khóa bật lúc interstitial hoàn tất; `Native combo closed (shown: <true|false>)` ghi ở `closeTurn` khi lớp phủ đóng (nút, Back, tự đóng khi nạp lỗi).

1. Đồng ý quảng cáo, mở thư viện, chạm thẻ, bấm "Chụp với tư thế này".
   - [ ] Interstitial thử hiện; đóng nó thì có quảng cáo native toàn màn hình với vòng 5, 4, 3, 2, 1 ở góc trên trái; camera chưa mở. Log `Native combo opened`.
   - [ ] Trong lúc đếm, Back không làm gì. Hết đếm có nút X ("Close ad"); bấm X (hay Back) thì camera mở với đúng tư thế vừa chọn; log `Native combo closed (shown: true)`.
   - [ ] Quảng cáo native chưa về: khung chờ toàn màn hình, vẫn đóng được sau 5 giây; nạp lỗi thì tự đóng khi hết đếm (`Native combo closed (shown: false)`). Không lần nào kẹt ở màn trống.
2. Ở camera, chụp và lưu ảnh thứ 2 của phiên rồi rời màn xem lại (luật `review_interstitial`; cách nhịp `ads_interval` 45 giây từ quảng cáo trước).
   - [ ] Interstitial rồi combo; đóng combo thì về camera như rời màn xem lại thường.
3. Firebase staging đặt `resume_ad_type = inter` (nhớ trả lại `aoa` sau bước này). Ở camera, bấm Home rồi mở lại app (sau trần 45 giây của lượt khởi động).
   - [ ] Có interstitial `resume_inter` (`Resume ad SHOW: resume_inter`); đóng nó thì về đúng màn cũ, **không** có combo, không có dòng `Native combo opened` sau `Interstitial shown: resume_inter`. Bấm Home rồi mở lại lần nữa (sau 45 giây): lại có quảng cáo quay lại app, không có `Resume ads held: full_screen_page` kẹt từ lần trước.
4. Đóng app hẳn, mở lại (lần mở thứ hai trở đi) khi Firebase staging đặt `open_ad_type` dạng interstitial.
   - [ ] Interstitial lúc mở app hiện; đóng thì vào app, không có combo.
5. Trong lúc combo đang đếm, tắt Wi-Fi và dữ liệu.
   - [ ] Popup mất mạng chưa hiện khi combo còn trên màn; đóng combo thì popup hiện (`features/network/spec.md`, mục 5). Bật mạng, "Thử lại": popup đóng, camera mở.
6. Firebase staging đặt `is_show_native_after_inter = false` rồi mở lại app hai lần (để giá trị mới được nạp).
   - [ ] Interstitial `inter_pose_detail` đóng là camera mở ngay, không có combo (Firebase thắng giá trị cục bộ của staging, ADR-1007). Trả khóa về như cũ (xóa giá trị trên Firebase staging).

**Ngoại lệ viết test trước:** tài liệu (hai spec, ADR-1009, `foundation.md`, `manual-test.md`), KDoc của `AdsController`, và bốn tệp kịch bản emulator (chạy ở `device-session` cuối phiên, không "thất bại trước" được; `docs/CLAUDE.md`, mục Khi viết plan). Các test qua ngay trên code hiện tại vì kiểm hành vi của khung mà app giữ (`screenInterstitial_closed_opensCombo_finishesOnClose`, `startupInterstitial_shownAndClosed_noCombo`, `screenInterstitial_showingFlag_noGapUntilComboCloses`, bốn test của `FullScreenAdHostComboTest`) là test giữ hành vi qua đường app dùng; phần đỏ trước code của plan là `resumeInter_closed_noCombo`, hai câu của `ResumeAdTurnsTest`, `MainActivityFullScreenAdHostWiringTest` và `StagingRemoteValuesModuleTest`. Không có `FullScreenAdHost` trong `MainActivity`, `FullScreenAdHostComboTest` vẫn xanh, nên chỗ đặt chỉ được giữ bởi test đọc mã nguồn: `:app` chưa dựng được `MainActivity` trong test (không có Hilt cho test) và hàm tạo của `AdsController` là `internal` (câu hỏi 4, mục 8).

**Đối chiếu `docs/CLAUDE.md`, mục Khi viết plan** (điều áp dụng):

- Mẫu plan, danh sách file đầy đủ: mục 3.2. File nơi khác đọc: mục 3.2, đoạn cuối.
- Sửa code Kotlin ngoài test: mục 3.5 từ `existing-tests.py`.
- Mỗi tiêu chí có cách kiểm: bảng trên. Lỗi đo được (FB-20261102-01): đã tái hiện bằng test tạm trên code hiện tại, mục 4.1 (L-029).
- Kỹ thuật lần đầu (E từ 2): không (E = 1); vẫn chạy thử vì là lần đầu dựng `FullScreenAdHost` công khai với controller thật, mục 4.1.
- Quyền, thư viện: không thêm, mục 3.3. Không thêm thư viện hay module nên không cần `assembleStagingRelease`.
- Quy tắc `check.sh`: không có ảnh chuẩn mới hay `captureRoboImage`.
- File R2, kho Maven, trợ năng, dịch chữ, chép code từ mẫu, `required-assets.txt`: không áp dụng.
- Phương án dự phòng: không cần (mục 3.4).
- Bước tay bật tắt cờ trên Firebase staging (L-140 của mẫu): `is_show_native_after_inter` nằm ở `StagingRemoteValuesModule` (giá trị cục bộ, Firebase ghi đè được), không ở `StagingFeatureFlagsModule`, nên bước 6 tắt được bằng Firebase. Dòng log của mỗi bước dẫn tới code ở trên.
- Tiêu chí chạy được trên emulator: bốn tệp kịch bản, mỗi dòng kiểm tay ghi nơi chạy.
- Giá trị mặc định (L-035): `combo_isOnAtStaging` có câu đối chứng mặc định `false`.
- Mọi đường tới lượt (L-072 của mẫu): ba đường mở lượt interstitial (`showInterstitial` của feature, `showStartupAd`, `ResumeAdTurns`) mỗi đường một test ở `AdsControllerComboScopeTest`; các đường kết thúc lượt (đóng, lỗi hiện, mất quảng cáo, kẹt, mất Activity, quá trần) đã có ở `AdsControllerNativeComboTest`.
- Trạng thái ở vỏ app (L-061, L-091 của mẫu): test dựng lại Activity và ra nền trong lúc đếm; khóa tắt giữa chừng có test sẵn.
- Test quét mã nguồn (L-062 của mẫu): theo ký hiệu, trong code đã bỏ chú thích và chuỗi.
- File dùng chung với plan khác: `AdsController.kt` (T-092), mục 6.
- Lệnh có tệp tạm: dùng `${TMPDIR:-/tmp}` (mục 4.1).
- Plan có xóa hay đổi file mà plan khác dẫn (L-101): không xóa file nào; ba tệp kịch bản đổi thêm dòng, không đổi tên.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-10, worktree `plan-T-090` ở commit `519ba3f` (nhánh chính), hai test tạm không commit trong `core/ads/src/test/.../core/ads/`: `T090TrialResumeTest.resumeInter_closed_noCombo` (bản đầu của test cùng tên ở mục 4) và `T090TrialHostTest` (ba test: lớp phủ trong cây rồi đóng, dựng lại Activity trong lúc đếm, ra nền trong lúc đếm; bản đầu của `FullScreenAdHostComboTest`). Đã xóa sau khi chạy.
- **Lệnh 1, trên code hiện tại:** `./gradlew --console=plain :core:ads:testDebugUnitTest --tests '*T090Trial*'` (29 giây, chỉ để xem lý do thất bại).
- **Kết quả 1:** 4 test, 1 thất bại đúng lý do: `resumeInter_closed_noCombo` đỏ ở câu "không có `Native combo opened`"; log thu được `Resume ad SHOW: resume_inter, Resume ad shown on resume: resume_inter, In-screen ads paused (full-screen ad on screen), Interstitial shown: resume_inter, Full-screen turn finished: closed, Resume ads held: full_screen_page, Reload resume_inter in 2000 ms, Native combo opened (inter gone: true)`, trùng chuỗi log của FB-20261102-01. Ba test của `T090TrialHostTest` qua (0,06 giây, 4,0 giây, 2,1 giây): node của lớp phủ trong hộp thoại tìm được bằng `onAllNodesWithTag`, `shadowOf(mainLooper).idleFor` đẩy được đồng hồ của `NativeFullScreenTurns`, dựng lại và ra nền giữ lượt.
- **Lệnh 2, có thay đổi tạm của mục 3.1** (bước 1 `FullScreenAdHost()` trong `MainActivity`, bước 2 `combo = false`, hai câu của `ResumeAdTurnsTest` đổi thành `false`; chưa có các test khác của mục 4): `source ~/.local/share/camera-pose/env.sh && scripts/check.sh > "${TMPDIR:-/tmp}/t090-check.log" 2>&1`.
- **Kết quả 2:** mã thoát 0, 8 phút 20 giây (01:19:24 tới 01:27:44 UTC); dòng cuối `== Cổng chất lượng: QUA`; `Test JVM: 2878 test, 0 lỗi. Ảnh screenshot: 167` (`./app: 114 test`, `./core/ads: 1403 test`, các module khác 0 lỗi); lint `BUILD SUCCESSFUL`. Báo cáo test của `:core:ads`: `T090TrialResumeTest.resumeInter_closed_noCombo` qua khi có bước 2 (13,6 giây, lần chạy đầu của lớp trong JVM), ba test của `T090TrialHostTest` qua; `ResumeAdTurnsTest.interResume_showsInterstitialWithCombo` và `typeChangedBeforeResume_showsNothing` qua với câu khẳng định mới (`false`). Không ảnh chuẩn nào đổi khi có `FullScreenAdHost()` trong `MainActivity`; `scripts/tests/run.sh` (trong `check.sh`) qua với `MainActivity` đã sửa.
- **Lệnh 3, kịch bản emulator:** bốn tệp tạm theo mục 3.1, bước 5 và nội dung tệp mới ở mục 4 (chép nguyên từ plan này): `python3 scripts/device-scenario.py check feature/poses/src/deviceScenarios/T-090-combo-sau-chi-tiet.txt feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`.
- **Kết quả 3:** mã thoát 0, không in lỗi (`str=ads_close_full_screen` có trong `strings.xml` của `:core:ads`). Mỗi tệp cũ có đúng một chỗ `tap_if text="Close"`. Chưa chạy trên emulator: `device-session` của PR hay cuối phiên chạy.
- **Dọn:** mọi thay đổi tạm đã trả lại bằng `git checkout --`, hai test tạm và tệp kịch bản tạm đã xóa; `git status --short` chỉ còn plan và ticket.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: combo native toàn màn hình thật sự hiện sau hai interstitial của màn, đổi số quảng cáo người dùng thấy; `resume_inter` bớt combo) và 7 (module core `:core:ads`). Vỏ app (`MainActivity`) cũng bị chạm. Mức **Cao**, `risk_score` để trống.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Nhiều module: `:app` (main, staging, test), `:core:ads` (main, test), kịch bản của `:feature:camera`, `:feature:poses`, tài liệu |
| B. Độ rõ của yêu cầu | 2 | Spec ads câu 18 đã xác nhận, nhưng mục 8 còn nhiều câu hỏi ảnh hưởng tới cách làm (chỗ đặt, sửa khung hay công tắc, ADR-1009, cách test vỏ app, chỉ số 3 quảng cáo mỗi 10 phút) |
| C. Khả năng kiểm chứng tự động | 2 | Logic và giao diện của lượt có test JVM với controller thật, nhưng chỗ đặt trong `MainActivity` chỉ có test đọc mã nguồn; combo sau `review_interstitial`, quảng cáo thật (đếm ngược, Back) và mất mạng giữa combo chỉ kiểm tay trên máy thật |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi mà camera và thư viện dựa vào: điều hướng sau `inter_pose_detail` và sau `review_interstitial` chờ combo đóng; popup mất mạng chờ lâu hơn; ba kịch bản emulator của task khác phải sửa; `AdsController.kt` cũng là file của T-092 |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn (`AdsControllerHostTest`, `NativeFullScreenOverlayTest`, `MainActivityForegroundWiringTest`); đã chạy thử |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn 1 và 7; tổng cũng từ 9) |

Plan chạm quảng cáo: Nhật (người phụ trách sản phẩm) cần biết trước khi duyệt (`risk-scoring.md`, mục 3). Combo làm dài thêm mỗi lần interstitial: theo dõi giữ chân ngày 1 sau khi phát hành (ticket).

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `MainActivity` (vỏ app), `:core:ads` (`ResumeAdTurns`, KDoc `AdsController`), `StagingRemoteValuesModule`, ba kịch bản emulator của T-058, T-022, T-024, spec camera và poses, `manual-test.md`.
- Task khác có thể xung đột: T-092 (`docs/features/onboarding/plans/T-092-trang-3-cho-quang-cao-trang-4.md`; phiên bản 2 đang `draft` ở PR #309) sửa `AdsController.kt`; plan này chỉ đổi hai câu KDoc ở phần đầu lớp, xa phần T-092 đổi, nhưng `run-check.py` vẫn báo cùng sửa một file. Combo bật ở staging cũng làm dài thêm mọi kịch bản emulator đi qua `inter_pose_detail`: tệp kịch bản mới của task khác viết sau T-090 dùng mẫu hai dòng ở mục 3.1, bước 5.
- Đề xuất thứ tự chạy: sau T-092 (cùng file `AdsController.kt`), hoặc ở danh sách chạy khác. Không chạy cùng danh sách với task nào sửa `MainActivity.kt`, `StagingRemoteValuesModule.kt` hay ba tệp kịch bản trên.
- Dự án mẫu: lệch A-12 (`resume_inter` không nối combo) và chỗ đặt `FullScreenAdHost` ghi ở ADR-1009; đợt đồng bộ sau (`docs/ops/to-app.md`) phải giữ dòng `combo = false` của `ResumeAdTurns`. `docs/ops/ads-business-rules.md` (A-12, K-01) và `docs/ops/ads-guide.md` (dòng quảng cáo khi quay lại app) là file lấy nguyên từ mẫu, không sửa (câu hỏi 5, mục 8).

## 7. Điều kiện dừng riêng

- Ảnh chuẩn screenshot nào đổi: dừng (plan không cho ghi lại ảnh chuẩn nào).
- Cần đổi hành vi của khung ngoài một dòng `combo = false` của `ResumeAdTurns` (ví dụ đổi chữ ký `ResumeAdPort`, `NativeFullScreenTurns`, hay luật K-01 tới K-08) để test qua: dừng.
- Kịch bản emulator có sẵn khác ngoài ba tệp ở mục 3.2 lỗi vì combo trong `device-session` của PR hay cuối phiên: ghi vào báo cáo phiên, không tự sửa tệp đó.

## 8. Câu hỏi cho người duyệt

1. **Đặt `FullScreenAdHost()` trong hay ngoài `NetworkDialogHost`?** (ticket, Chỗ vẽ.) Hai cách cùng chạy: lớp phủ và popup mạng là hai cửa sổ hộp thoại riêng, popup chờ theo cờ `fullScreenAdShowing` (combo giữ cờ suốt lượt), không theo chỗ đặt; quảng cáo trong màn tạm dừng theo cùng cờ (O-10). Đề xuất: **ngoài**, ngay sau `NetworkDialogHost`, trong `AppTheme`, để `NetworkDialogHost` chỉ bọc nội dung điều hướng và test đọc mã nguồn có một vị trí rõ. Phương án khác: trong khối nội dung, sau `AppNavHost` (đúng chữ "sau `AppNavHost`" của HD-05), không khác gì về hành vi. Trả lời: ngoài, ngay sau `NetworkDialogHost` (Nhật, 2026-11-10).
2. **Sửa thẳng `:core:ads` hay thêm công tắc của khung?** Đề xuất: **sửa thẳng một dòng** (`combo = false` trong `ResumeAdTurns`), ghi ở ADR-1009, và đề xuất về dự án mẫu một công tắc (mặc định như mẫu, ví dụ thuộc tính của vị trí `RESUME_INTER` hay một khóa) bằng issue `from-app`. Lý do: công tắc thêm nhiều code lệch mẫu hơn một dòng, và mẫu nhận công tắc thì đợt đồng bộ sau thay dòng này bằng cách của mẫu. Phương án khác: thêm công tắc ở app ngay (lệch mẫu nhiều hơn, dễ nhận về mẫu nguyên văn). Trả lời: sửa thẳng một dòng, đề xuất công tắc về dự án mẫu bằng issue `from-app` (Nhật, 2026-11-10).
3. **Hai spec camera và poses chuyển `changed`.** Câu 18 của spec ads ghi chép vào mục 5 của hai spec trong PR của T-090; `docs/CLAUDE.md` không cho sửa spec `confirmed` mà không chuyển trạng thái. Đề xuất: làm như T-074 (sửa mục 5, chuyển `changed`, dòng lịch sử, Nhật xác nhận lại sau merge). Phương án khác: Nhật sửa hai spec trước phiên, plan bỏ hai file đó khỏi mục 3.2. Trả lời: phiên sửa hai spec, chuyển `changed`; Nhật xác nhận lại ngay sau merge (Nhật, 2026-11-10).
4. **Test "vỏ app" là test đọc mã nguồn, phần hành vi ở `:core:ads`.** Ticket muốn test Robolectric của vỏ app có lớp phủ, đóng, dựng lại. `:app` chưa có Hilt cho test (`MainActivityForegroundWiringTest` cũng đọc mã nguồn vì lý do này) và hàm tạo `AdsController` là `internal`, nên `:app` không dựng được lượt combo thật. Đề xuất: hành vi kiểm ở `FullScreenAdHostComboTest` của `:core:ads` với đúng `AdsHost` + `FullScreenAdHost()` công khai mà `MainActivity` dùng, chỗ đặt kiểm bằng `MainActivityFullScreenAdHostWiringTest`; job `ui-test` (emulator, sau merge) và kịch bản T-090 bổ sung. Phương án khác: thêm Hilt test cho `:app` (thư viện test mới, task riêng). Trả lời: hành vi kiểm ở `:core:ads`, chỗ đặt kiểm bằng test đọc mã nguồn (Nhật, 2026-11-10).
5. **Hai tài liệu lấy từ mẫu còn ghi `resume_inter` nối combo** (`docs/ops/ads-business-rules.md` A-12, `docs/ops/ads-guide.md` dòng quảng cáo khi quay lại app). Đề xuất: không sửa (file của mẫu, đồng bộ đè), lệch ghi ở ADR-1009 và `foundation.md` (AI đọc trước mọi task). Phương án khác: thêm chú thích "camera-pose: không nối combo (ADR-1009)" vào hai dòng đó, đợt đồng bộ phải giữ. Trả lời: không sửa hai file của mẫu, lệch ghi ở ADR-1009 (Nhật, 2026-11-10).
6. **Nhận ADR-1009 `accepted` lúc duyệt plan?** ADR-1009 thay một vế của ADR-1007 và vế "rồi nối combo" của luật A-12 mà ADR-0026 dựa vào (mục 3.1, bước 6). Đề xuất: Nhật ghi `accepted` (và `deciders`) khi duyệt plan, để phiên tạo ADR với `status: accepted` và `foundation.md` sinh ra có vế mới ngay; không thì phiên tạo `proposed` và Nhật chuyển sau merge. Trả lời: nhận `accepted` ngay; phiên tạo ADR-1009 với `status: accepted`, `deciders: Nhật` (Nhật, 2026-11-10).
7. **Chỉ số "tối đa 3 quảng cáo toàn màn hình mỗi 10 phút dùng"** (spec camera, mục 1, dòng 19) có tính combo không? Combo là quảng cáo toàn màn hình thứ hai trong cùng một lần interstitial. Đề xuất: đếm mỗi cụm interstitial + combo là một lần (combo không tự có cơ hội riêng, luôn đi sau interstitial và đẩy mốc `ads_interval` một lần, K-05); Nhật chốt, ghi vào spec camera khi xác nhận lại (câu hỏi 3). Trả lời: mỗi cụm interstitial cộng combo đếm là một lần; ghi vào spec camera khi xác nhận lại (Nhật, 2026-11-10).
8. **FB-20261102-01 còn `status: new`.** Plan không sửa phản hồi (ngoài phạm vi). Đề xuất: người phân loại (`/triage-feedback`) chuyển `planned`, liên kết T-090, trước hay cùng lúc duyệt plan. Trả lời: chuyển FB-20261102-01 sang `planned` cùng PR plan này (Nhật, 2026-11-10).
9. **Bản staging đang phát và Firebase staging.** FB-20261102-01 đề xuất tắt `is_show_native_after_inter` trên Firebase staging tới khi có T-090. Sau merge T-090, khóa trên Firebase staging nên để trống (dùng giá trị cục bộ `true`) hay đặt `true`? Đề xuất: để trống, như `manual-test.md` mục 11 mới ("không đặt `false`"); người kiểm và ghi vào danh sách chạy trước khi phát bản staging có T-090. Trả lời: xóa khóa trên Firebase staging sau khi T-090 merge, để trống; người làm và ghi vào danh sách chạy (Nhật, 2026-11-10).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt (Nhật, 2026-11-10, trong phiên có người theo dõi).
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn (duyệt bằng lời, Nhật báo lại trong phiên, AI ghi).
- **Ghi chú:** Chín câu ở mục 8 trả lời theo đề xuất của AI (ghi tại từng câu). ADR-1009 tạo với `status: accepted`. Rà soát độc lập: 0 chặn, 6 nên sửa (đã sửa hết), 9 ghi chú. Cùng sửa `AdsController.kt` với T-092: chạy T-090 sau T-092, hay ở danh sách khác.

## 10. Nhật ký thực hiện

## Kết quả existing-tests.py

Chạy lại sau khi sửa plan theo rà soát độc lập: `python3 scripts/existing-tests.py docs/features/ads/plans/T-090-combo-native-sau-interstitial.md` (2026-11-10). File của plan: `MainActivity.kt`, `ResumeAdTurns.kt`, `AdsController.kt`, `StagingRemoteValuesModule.kt`; 271 test có sẵn dùng tên hay tag của chúng; plan đã nhắc 13 test. Bản plan mới nhắc `start()` (khung dựng của `AdsControllerHostTest`), nên script ghép thêm nhiều test chỉ trùng chữ `start`.

- **Cần xem trước (8):** `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`, `RewardedActionTest.longContent_small_buttonsInsideWindow`, `RewardedActionTest.noThanksText_usesOnSurfaceVariant_light_and_dark`, `RewardedActionTest.watchButton_is56dpHigh_fullCardWidth`, `MatchHintTest.height56_circle40`, `SettingsComponentsTest.settingsRow_iconColor_isOnSurfaceVariant`, `CameraScreenOverlayTest.smallThai_adjustPanel_labelsFit`, `PosesScreenTest.widthChange_keepsScrollAndSelectedChip`. Cả 8 chỉ trùng chữ `start` (lề `start`, `ActivityController.start()`); đo giao diện không đi qua phần plan đổi. Giữ.
- **Nên xem (144):** 112 test chỉ trùng `start`: không đi qua phần plan đổi, giữ. 32 test còn lại là nhóm đã xem ở lượt trước, cùng kết luận:
  - `MainActivityForegroundWiringTest` (2), `ScreenOrientationTest`, `MergedManifestTest`: đọc `MainActivity` theo ký hiệu khác (`observeAppForeground`, `onAppRestored`, manifest); thêm một lời gọi không đổi chúng. Giữ.
  - `AdsControllerFunnelTest`, `AdsControllerHostTest`, `AdsControllerInScreenWindowTest`, `AdsControllerNativeComboTest` (trùng `onInterstitialClosed`, `showInterstitial`, `showStartupAd`, `AdsController`): lượt interstitial của feature (`GALLERY_INTERSTITIAL`), lượt khởi động (`combo = false`) và lượt native độc lập; không lượt nào đi qua `ResumeAdTurns`. Hành vi giữ nguyên, kể cả `interstitial_hostGone_comboOn_noNativeFullScreen` và `showStartupAd_blocked_neverOpensCombo`. Giữ.
  - `NativeEligibleCoverageTest.everyFileDrawingANativeAd_reportsEligible`: quét `src/main` của `:core:ads`, có ngoại lệ `NativeFullScreenAdHost.kt`; `MainActivity` ở `:app`, ngoài phạm vi quét. Giữ.
  - `NativeFullScreenOverlayTest.recreate_keepsDeadline_notRestarted`: chỉ nhắc `MainActivity` trong chú thích. Giữ.
  - Còn lại (`AdNetworkLogWiringTest`, `AdsSdkStartTest`, `FullScreenAdGuardTest`, `FullScreenAdsTest`, `FullScreenPreloadSlotTest`, `RemovedPacingRulesTest`, `RewardedActionTest`, `RewardedTurnsTest`, `NetworkDialogControllerTest`, `PhotoStoreTest`, `LoadingRouteTest`): chỉ trùng tên lớp hay `onCreate`. Giữ.
- Hai test của `ResumeAdTurnsTest` và `StagingRemoteValuesModuleTest.firstOpenAndCombo_areNotSet` có câu khẳng định đổi: mục 3.5.
- 106 test chỉ dùng tên mà không thuộc hai nhóm trên: không xem từng test; `scripts/check.sh` ở mục 4.1 chạy cả bộ với thay đổi của mục 3.1.

### RUN-20261110-01, phiên bản plan 1

- **Kết quả:** đã merge.
- **Nhánh, PR:** `claude/T-090-combo-native-sau-inter`, PR #313, merge 05:09 UTC (commit `308161e`).
- **Đã làm:** `MainActivity` đặt `FullScreenAdHost()` ngoài `NetworkDialogHost`, trong `AppTheme`; `ResumeAdTurns` `combo = false`; KDoc `AdsController`; staging bật `is_show_native_after_inter`; ba kịch bản thêm bước đóng combo, kịch bản mới `T-090-combo-sau-chi-tiet.txt`; ADR-1009 `accepted`, `foundation.md` sinh lại; spec camera, poses `changed`; `manual-test.md` 11.1, 11.8. Phiên con làm bước 3 tới 5 trong worktree.
- **Test viết trước:** commit `7da7068`, 11 test mới hay sửa, 6 đỏ đúng lý do; 7 test về hành vi khung qua ngay như plan ghi. Đối chứng `publishAfter`: tạm bỏ thì `noGapUntilComboCloses` đỏ. File test không đổi sau commit test.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 6 ghi chú; 7 đột biến trên bản sao đều bị test bắt.
- **Kết quả CI:** `scripts/check.sh` trên máy QUA (2916 test JVM, 0 lỗi); `pr-check` xanh (lần chạy 42759036119).
- **Lệch so với plan:** `startupInterstitial_shownAndClosed_noCombo` dựng mốc lần mở đầu để là "lần mở sau" (chi tiết dựng trong commit test); ADR-1009 thêm hai vế rút từ plan mục 3.1, 7. Câu khẳng định cũ viết lại (ADR-0031): đúng ba câu của mục 3.5 (PR #313, mục Test cũ viết lại).
- **Việc còn lại:** Nhật xóa khóa `is_show_native_after_inter` trên Firebase staging; Nhật xác nhận lại spec camera và poses; kiểm tay `manual-test.md` 11.8; mở issue `from-app` (công tắc `resume_inter` nối combo; khung không mở lượt combo khi không có host vẽ); FB-20261102-01 chuyển `fixed` sau kiểm tay.
