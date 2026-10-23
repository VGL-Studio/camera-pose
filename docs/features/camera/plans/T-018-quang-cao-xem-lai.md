---
ticket: T-018
title: Quảng cáo toàn màn hình khi rời màn xem lại
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10M          # số gợi ý cỡ S là 5.0M (tạm, mới đo 2 task); chỉnh lên, lý do ở mục 5
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-018: QUẢNG CÁO TOÀN MÀN HÌNH KHI RỜI MÀN XEM LẠI

## 1. Tóm tắt

Thêm vị trí `review_interstitial` vào `:core:ads` và gọi nó khi người dùng rời màn xem lại sau khi đã lưu ảnh. Ba giới hạn của spec (không trước ảnh lưu thứ 2 của phiên, cách lần trước ít nhất 120 giây, tối đa 1 lần mỗi 3 ảnh lưu) nằm trong một lớp thuần Kotlin của `:core:ads`, đọc số từ `RemoteSettings`. `AdsController` thêm hai điều kiện: hộp thoại mất mạng đang mở thì không hiện quảng cáo, chưa có quảng cáo tải sẵn thì không bắt người dùng chờ. Feature chỉ gọi một dạng mới của `rememberShowInterstitial` có hàm báo kết quả, rồi mới về camera. Kiểm chứng bằng unit test của lớp giới hạn và của thứ tự rời màn, test Robolectric cho `rememberShowInterstitial` và `ReviewRoute`, và kiểm tra thủ công trên máy có Remote Config thật.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-9 | Khi người dùng rời màn xem lại (Chụp lại, Tư thế tiếp hay nút quay lại) sau khi đã lưu ảnh, và đủ điều kiện ở mục 5, thì một quảng cáo toàn màn hình hiện trước khi về camera | Mọi cách rời màn đi qua `ReviewEvent.Leave(savedThisVisit)` của T-016. `savedThisVisit = true` thì hỏi quảng cáo trước; khi quảng cáo đã hiện (hay bị bỏ qua) thì mới về camera. Quảng cáo phủ lên app, người dùng đóng quảng cáo là thấy camera. Mỗi lần rời màn sau khi lưu là một "điểm dừng tự nhiên" của vị trí; một lần xem lại lưu nhiều nhất một ảnh, nên số điểm dừng bằng số ảnh lưu |
| KB-9 | Khi chưa lưu ảnh nào trong lần xem lại đó thì không hiện quảng cáo | `savedThisVisit = false` (bỏ ảnh, hoặc mở ảnh cũ từ ô ảnh vừa chụp) thì về camera ngay, không gọi quảng cáo và không tính điểm dừng |
| KB-9 | Khi cờ `ads_review_interstitial_enabled` tắt hoặc người dùng chưa đồng ý quảng cáo theo hộp thoại đồng ý thì không hiện quảng cáo (ADR-0013, ADR-0014) | Đã có sẵn trong `AdsController`: `canRequestAds` (đồng ý) và `isEnabled(placement)` (khóa `ads_<key>_enabled`, mặc định tắt). Plan đưa hai điều kiện này vào hàm quyết định thuần Kotlin để có test |
| KB-9 | Khi ở camera và trong lúc xem lại thì không có quảng cáo nào trên màn hình | Không thêm `AdSlot` nào vào camera hay màn xem lại; quảng cáo chỉ được gọi trong lúc rời màn |
| Spec mục 5 | Không trước ảnh lưu thứ 2 của phiên; cách lần trước ít nhất 120 giây; tối đa 1 lần mỗi 3 ảnh lưu; các con số trong `RemoteSettings` | Ba khóa Remote Config riêng của vị trí, mặc định 2, 120, 3. "Phiên" là một lần chạy của app (từ khi mở tới khi hệ thống đóng tiến trình), đếm trong bộ nhớ: xem câu hỏi 1. Rào chắn chung của ADR-0013, ADR-0014 vẫn áp dụng: 45 giây giữa hai quảng cáo toàn màn hình bất kỳ, chờ người dùng ngừng chạm 1 giây |
| Ticket | Không hiện khi hộp thoại mất mạng đang mở | `AdsController` đọc `NetworkStatus.dialogShown` của `:core:network`, trước khi chờ và ngay trước khi hiện. `NetworkStatus` đã ghi rõ ý này trong chú thích ("no full-screen ad shows") |

Thiết kế tham chiếu: `features/camera/design/README.md` (dòng về `review_interstitial`: hiện khi rời `ReviewScreen` sau khi đã lưu ảnh, trước khi về camera; không có vùng quảng cáo trên `CameraScreen` và `ReviewScreen`). Không có ảnh thiết kế riêng: quảng cáo do SDK vẽ.

ADR liên quan: ADR-0001 (`:core:ads` phụ thuộc `:core:network`, core chỉ phụ thuộc core), ADR-0003, ADR-0004, ADR-0013 (feature chỉ dùng `rememberShowInterstitial`; gọi ở điểm dừng tự nhiên), ADR-0014 (vị trí mới mặc định tắt; không đổi các mặc định có sẵn).

## 3. Giải pháp

### 3.1. Cách làm

**Trong `:core:ads` (module dùng chung, điểm chặn 7):**

- `AdPlacement`: thêm `REVIEW_INTERSTITIAL("review_interstitial", AdFormat.INTERSTITIAL),` đúng một dòng như dòng có sẵn. `scripts/release.sh` (bước 3) đọc danh sách vị trí bằng biểu thức `^\s*TEN("key", AdFormat.X),?$`, nên không thêm tham số nào vào enum; giới hạn riêng của vị trí nằm ở `AdsSettings`.
- `AdsSettings`: ba khóa theo vị trí và mặc định của chúng:
  - `placementFirstBreakKey(p) = "ads_${p.key}_first_break"`: điểm dừng đầu tiên của phiên được phép hiện (2 với `review_interstitial`: không trước ảnh lưu thứ 2).
  - `placementBreaksBetweenKey(p) = "ads_${p.key}_breaks_between"`: số điểm dừng tối thiểu giữa hai lần hiện (3: tối đa 1 lần mỗi 3 ảnh lưu).
  - `placementMinIntervalKey(p) = "ads_${p.key}_min_interval_seconds"`: số giây tối thiểu giữa hai lần hiện của vị trí này (120).
  - `fun breakCapDefaults(p): BreakCap`: `REVIEW_INTERSTITIAL` trả `BreakCap(firstBreak = 2, breaksBetween = 3, minIntervalSeconds = 120)`; vị trí khác trả `BreakCap.NONE` (1, 1, 0), tức là không thêm giới hạn nào ngoài rào chắn chung.
- Tạo `InterstitialBreaks.kt`, thuần Kotlin, không Android (như `FullScreenAdGuard`):
  - `data class BreakCap(firstBreak: Int, breaksBetween: Int, minIntervalSeconds: Long)` với `NONE`.
  - `class InterstitialBreaks`: nhớ theo từng vị trí số điểm dừng của phiên và lần hiện gần nhất (số thứ tự điểm dừng, thời điểm theo đồng hồ đơn điệu). `onBreak(p): Int` tăng và trả số thứ tự; `allows(p, breakNumber, nowMillis, cap): Boolean`; `onShown(p, breakNumber, nowMillis)`.
  - `allows` đúng khi: `breakNumber >= cap.firstBreak`; và chưa hiện lần nào trong phiên, hoặc `breakNumber - lastShownBreak >= cap.breaksBetween` và `nowMillis - lastShownAt >= cap.minIntervalSeconds * 1000`.
  - `fun interstitialAllowed(canRequestAds: Boolean, placementEnabled: Boolean, networkDialogShown: Boolean): Boolean`: hàm thuần, đúng khi cả ba điều kiện cho phép.
- `AdsController`:
  - Nhận thêm `NetworkStatus` (đã bind trong `NetworkBindingsModule` của `:core:network`, singleton). `core/ads/build.gradle.kts` thêm `implementation(project(":core:network"))`; `:core:network` không phụ thuộc `:core:ads`, không có vòng phụ thuộc.
  - `showInterstitial(activity, placement)` theo thứ tự mới:
    1. `val breakNumber = breaks.onBreak(placement)`: mỗi lần gọi là một điểm dừng, đếm cả khi sau đó không hiện.
    2. `interstitialAllowed(canRequestAds.value, isEnabled(placement).first(), networkStatus.dialogShown.value)` sai thì trả `false`.
    3. Đọc `BreakCap` của vị trí từ `RemoteSettings` (ba khóa trên, mặc định `breakCapDefaults`); `breaks.allows(...)` sai thì trả `false`.
    4. Chưa có quảng cáo tải sẵn của vị trí thì gọi tải sẵn và trả `false` ngay. Trước đây bước này nằm sau vòng chờ người dùng ngừng chạm; đưa lên trước để người dùng không phải chờ tới 3 giây cho một quảng cáo không có.
    5. Vòng chờ của `FullScreenAdGuard` giữ nguyên (45 giây chung, ngừng chạm 1 giây, chờ tối đa 3 giây).
    6. Hộp thoại mất mạng vừa mở trong lúc chờ (`dialogShown.value`) thì trả `false`.
    7. Hiện quảng cáo, `guard.onShown(now)`, `breaks.onShown(placement, breakNumber, now)`, trả `true`.
  - Lý do trả `false` ghi bằng `Log.i(TAG, "Interstitial <key> skipped: <reason>")`, như các dòng log có sẵn của lớp (lớp này dùng `android.util.Log` từ dự án mẫu; không đổi cách ghi log của `:core:ads` trong task này).
- `AdSlot.kt`: thêm một dạng của `rememberShowInterstitial`:
  - `fun rememberShowInterstitial(placement: AdPlacement, onFinished: (shown: Boolean) -> Unit): () -> Unit`.
  - Hành động trả về chạy `showInterstitial` rồi gọi `onFinished(kết quả)` trên luồng chính (coroutine của `rememberCoroutineScope` trên `Dispatchers.Main.immediate`), đúng một lần mỗi lần gọi.
  - Không có `LocalAdsController` hay `LocalActivity` (test, preview) thì gọi `onFinished(false)` ngay. `onFinished` đi qua `rememberUpdatedState`.
  - Dạng cũ `rememberShowInterstitial(placement)` giữ nguyên chữ ký và hành vi, gọi lại dạng mới với `onFinished = {}`. Tên vẫn là `rememberShowInterstitial`, đúng quy tắc của ADR-0013 (feature chỉ dùng `AdSlot`, `rememberShowInterstitial`, `PrivacyOptionsButton`).

**Trong `:feature:camera`:**

- Tạo `review/ReviewExit.kt`, hai hàm thuần, nơi duy nhất quyết định thứ tự rời màn (T-019 thêm việc hỏi đánh giá vào đây):
  - `fun leaveReview(savedThisVisit: Boolean, showAd: () -> Unit, goBack: () -> Unit)`: đã lưu thì `showAd()` (kết quả quay về `afterReviewAd`), chưa lưu thì `goBack()` ngay.
  - `fun afterReviewAd(shown: Boolean, goBack: () -> Unit)`: gọi `goBack()`, dù quảng cáo hiện hay không.
- `ReviewScreen.kt`, trong `ReviewRoute`:
  - `val showAd = rememberShowInterstitial(AdPlacement.REVIEW_INTERSTITIAL) { shown -> afterReviewAd(shown, currentOnLeave) }`.
  - Sự kiện `ReviewEvent.Leave` gọi `leaveReview(event.savedThisVisit, showAd, currentOnLeave)` thay cho `currentOnLeave()`.
  - `onFinished` chạy trên luồng chính, nên `navController.popBackStack()` của `AppNavHost` chạy trên luồng chính (L-030). Quảng cáo hiện thì `Activity` dừng, `repeatOnLifecycle` ngừng gom sự kiện, nhưng coroutine của `rememberCoroutineScope` vẫn chạy tới `onFinished`, nên lệnh về camera không bị mất.
  - Trong lúc chờ quảng cáo (tối đa khoảng 3 giây), `ReviewViewModel` đã đặt `left = true`, nên bấm lại các nút không làm gì.
- `ReviewViewModel`, `ReviewUiState`, `AppNavHost` không đổi.

Mẫu có sẵn: `FullScreenAdGuard` và `FullScreenAdGuardTest` (luật quảng cáo thuần Kotlin, test bằng đồng hồ giả); `PrivacyOptionsButtonTest` (thành phần của `:core:ads` không có controller trong test).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Thêm `REVIEW_INTERSTITIAL("review_interstitial", AdFormat.INTERSTITIAL),` một dòng; không đụng dòng khác (T-023 thêm hai vị trí banner vào cùng file) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsSettings.kt` | Ba hàm khóa theo vị trí, `breakCapDefaults` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialBreaks.kt` | `BreakCap`, `InterstitialBreaks`, `interstitialAllowed` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsController.kt` | Nhận `NetworkStatus`; thứ tự mới của `showInterstitial` (mục 3.1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlot.kt` | Dạng `rememberShowInterstitial(placement, onFinished)`; dạng cũ gọi dạng mới |
| Sửa | `core/ads/build.gradle.kts` | `implementation(project(":core:network"))` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialBreaksTest.kt` | Test giới hạn theo điểm dừng và thời gian, và `interstitialAllowed` (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ShowInterstitialTest.kt` | Robolectric: không có controller thì `onFinished(false)` đúng một lần |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsSettingsTest.kt` | Thêm test mặc định của vị trí mới; test có sẵn giữ nguyên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | `configuredUnits_areParsedPerPlacementKey` dựng chuỗi cấu hình có mã cho mọi `AdPlacement.entries` (thêm một mục `other_key` và giữ khoảng trắng để vẫn kiểm cách đọc), thay cho chuỗi chỉ có `sample_bottom_banner`. Lý do: thêm vị trí thì `missing` không còn rỗng; cách viết mới không phải sửa lại khi T-023 thêm vị trí. Điều test kiểm (đọc đúng theo khóa, không thiếu vị trí nào) giữ nguyên Nếu plan kia (T-018 hay T-023) đã merge trước thì test đã sinh chuỗi từ `entries`: giữ nguyên phần đó, chỉ thêm khẳng định của plan này. |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewExit.kt` | `leaveReview`, `afterReviewAd` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreen.kt` | `ReviewRoute` nối `rememberShowInterstitial(REVIEW_INTERSTITIAL, onFinished)` với `leaveReview` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewExitTest.kt` | Unit test thứ tự rời màn |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewRouteTest.kt` | Robolectric: `ReviewRoute` với `ReviewViewModel` dựng bằng bản giả, rời màn sau khi lưu và sau khi bỏ ảnh đều về camera đúng một lần |
| Sửa | `docs/features/camera/tickets/T-018-quang-cao-xem-lai.md` | Trường `plan:` trỏ tới plan này |

Nơi đọc các file bị sửa (L-020):

- `AdPlacement.kt`: `scripts/release.sh` đọc danh sách vị trí từ commit phát hành (không sửa, không chạy file này; dòng mới giữ đúng mẫu biểu thức của nó). Trước bản phát hành có `review_interstitial`, dev phải thêm mã quảng cáo thật của vị trí vào `adUnits` ở thư mục phát hành, nếu không `release.sh` từ chối (việc của dev, ghi ở mục 8). `scripts/tests/run.sh` dựng repo giả riêng cho `release.sh`, không đọc file thật. `AdUnitIdsTest.kt` (trong danh sách). `AdSlot.kt`, `AdsController.kt`, `AdUnitIds.kt` dùng enum, không liệt kê từng vị trí. `feature/sample/.../SampleScreen.kt` dùng `SAMPLE_BOTTOM_BANNER`, không đổi.
- `AdsSettings.kt`: chỉ `AdsController.kt`, `AdSlot.kt` và `AdsSettingsTest.kt` dùng; giá trị có sẵn không đổi.
- `AdsController.kt`: thêm tham số tiêm, chỉ Hilt dựng lớp; không test nào dựng `AdsController`. `app/.../MainActivity.kt` tiêm và gọi `gatherConsent`, `onUserInteraction`, không đổi.
- `AdSlot.kt`: dạng cũ của `rememberShowInterstitial` giữ chữ ký; hiện không feature nào gọi nó.
- `core/ads/build.gradle.kts`: `checkModuleDependencies` của `build.gradle.kts` gốc kiểm, phụ thuộc core vào core là hợp lệ.
- `ReviewScreen.kt`: `ReviewScreenTest.kt`, `ReviewScreenScreenshotTest.kt` gọi `ReviewScreen` không trạng thái, không gọi `ReviewRoute`; ảnh chuẩn không đổi. `ReviewNavigation.kt` gọi `ReviewRoute(onLeave)`, chữ ký không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có cờ tính năng mới. Vị trí bật tắt bằng khóa `ads_review_interstitial_enabled`, mặc định tắt (ADR-0014), đúng bảng cờ của spec mục 8.
- Quảng cáo (điểm chặn 1): vị trí mới `review_interstitial`, toàn màn hình, đúng spec mục 5. Khóa Remote Config mới của vị trí: `ads_review_interstitial_enabled` (mặc định tắt), `ads_review_interstitial_first_break` (2), `ads_review_interstitial_breaks_between` (3), `ads_review_interstitial_min_interval_seconds` (120). Không đổi `ads_interstitial_min_interval_seconds` (45) hay mặc định nào của ADR-0014. Bản staging không nhận Remote Config (ADR-0012, ADR-0014) nên không hiện quảng cáo này: xem câu hỏi 2.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- Dữ liệu trên máy: không có khóa DataStore mới. Mốc quảng cáo của phiên nằm trong bộ nhớ của `AdsController` (câu hỏi 1).
- **Quyền truy cập:** Không thêm. Quảng cáo toàn màn hình dùng SDK đã có.
- **Thư viện và SDK:** Không thêm. `play-services-ads` đã có ở `:core:ads` (`libs.play.services.ads`); thêm phụ thuộc module `:core:network` (của dự án), không phải thư viện. `NetworkStatus` và `StateFlow` đã có trên classpath của `:core:network` (Kotlin coroutines, do convention plugin thêm).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** test Robolectric của `ReviewRoute` có thể không dựng được màn (trình chọn quyền `rememberLauncherForActivityResult`, giải mã ảnh trong `produceState`) hoặc chập chờn.
- **Điều kiện chuyển:** `ReviewRouteTest` vẫn thất bại vì lỗi dựng màn (không phải vì thiếu code) sau 2 lần sửa.
- **Cách làm thay:** giữ `ReviewExitTest` (thứ tự rời màn) và `ShowInterstitialTest` (dạng mới gọi `onFinished(false)` khi không có controller), bỏ `ReviewRouteTest`, ghi vào nhật ký và thêm bước kiểm tra thủ công 1 ở mục 4 vào danh sách "Cần người xem". Tiêu chí chấp nhận không đổi. File `ReviewRouteTest.kt` khi đó không được tạo.

## 4. Kiểm thử

Test viết trước code. Bước 3 (commit test): mọi test dưới đây, cùng chữ ký trống của `BreakCap`, `InterstitialBreaks`, `interstitialAllowed`, `leaveReview`, `afterReviewAd`, dạng mới của `rememberShowInterstitial` và `AdPlacement.REVIEW_INTERSTITIAL` để test biên dịch được (thân hàm `TODO()` hay trả giá trị sai), để test thất bại vì chưa có hành vi, không phải vì lỗi biên dịch. Bước 4 (commit code): phần còn lại.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-9, tiêu chí 1: rời màn sau khi lưu thì quảng cáo được hỏi trước, về camera sau | Unit test | `ReviewExitTest.saved_asksAdFirst_thenGoesBackAfterAd`: ghi thứ tự gọi vào một danh sách; `leaveReview(true, showAd, goBack)` chỉ gọi `showAd`, chưa gọi `goBack`; `afterReviewAd(true, goBack)` gọi `goBack` đúng một lần |
| KB-9, tiêu chí 1: quảng cáo không hiện được thì vẫn về camera | Unit test | `ReviewExitTest.adNotShown_stillGoesBack`: `afterReviewAd(false, goBack)` gọi `goBack` đúng một lần |
| KB-9, tiêu chí 2: chưa lưu thì không hỏi quảng cáo | Unit test | `ReviewExitTest.notSaved_goesBackWithoutAd`: `leaveReview(false, ...)` gọi `goBack` một lần, `showAd` không lần nào |
| KB-9, nối trong `ReviewRoute` | Compose UI test (Robolectric) | `ReviewRouteTest.savedThenRetake_leavesOnce`: dựng `ReviewViewModel` bằng bản giả của `ReviewFakes.kt` với `SavedStateHandle` đã có `review_saved_uri` và `review_path` trỏ tới một file tồn tại (trạng thái `SAVED`), `setContent { ReviewRoute(onLeave = { leaves++ }, viewModel = vm) }` không có `LocalAdsController`; bấm "Chụp lại"; `waitForIdle`; `leaves == 1`. `ReviewRouteTest.discardThenLeave_leavesOnce`: trạng thái `NOT_SAVED`, bấm "Chụp lại", bấm "Bỏ ảnh"; `leaves == 1`. Đối chứng: trước bước 4, `leaveReview` trống (không gọi `goBack`) nên `leaves == 0`, test thất bại đúng lý do |
| KB-9, tiêu chí 3: cờ tắt, chưa đồng ý, hộp thoại mất mạng mở thì không hiện | Unit test | `InterstitialBreaksTest.allowed_onlyWithConsentFlagAndNoNetworkDialog`: `interstitialAllowed(true, true, false)` đúng; mỗi trường hợp đổi một điều kiện (`false, true, false`), (`true, false, false`), (`true, true, true`) đều sai. Đặt trường hợp đúng cạnh các trường hợp sai, để hàm trống trả hằng số nào cũng thất bại (L-035) |
| Spec mục 5: không trước ảnh lưu thứ 2 của phiên | Unit test | `InterstitialBreaksTest.firstBreak_isSkipped_secondAllowed`: cap (2, 3, 120); điểm dừng 1 không cho phép, điểm dừng 2 cho phép |
| Spec mục 5: tối đa 1 lần mỗi 3 ảnh lưu | Unit test | `InterstitialBreaksTest.afterShown_nextAllowedThreeBreaksLater`: hiện ở điểm dừng 2 lúc 0 ms; điểm dừng 3 và 4 (lúc 10 phút) không cho phép; điểm dừng 5 (lúc 10 phút) cho phép |
| Spec mục 5: cách lần trước ít nhất 120 giây | Unit test | `InterstitialBreaksTest.afterShown_waitsMinInterval`: hiện ở điểm dừng 2 lúc 0 ms; điểm dừng 5 lúc 119 999 ms không cho phép, lúc 120 000 ms cho phép |
| Mỗi vị trí đếm riêng | Unit test | `InterstitialBreaksTest.placements_countSeparately`: `onBreak` của hai vị trí khác nhau trả 1 và 1 (dùng `REVIEW_INTERSTITIAL` và `SAMPLE_BOTTOM_BANNER` chỉ làm khóa của bảng đếm) |
| Vị trí không có giới hạn riêng | Unit test | `InterstitialBreaksTest.noneCap_allowsEveryBreak`: `BreakCap.NONE`, điểm dừng 1, 2, 3 liền nhau lúc 0 ms đều cho phép sau mỗi lần hiện |
| Mặc định là quyết định kinh doanh (ADR-0014) | Unit test | `AdsSettingsTest.reviewInterstitialCapDefaults`: `breakCapDefaults(REVIEW_INTERSTITIAL) == BreakCap(2, 3, 120)`; `breakCapDefaults(SAMPLE_BOTTOM_BANNER) == BreakCap.NONE`; khóa `placementFirstBreakKey(REVIEW_INTERSTITIAL) == "ads_review_interstitial_first_break"`, tương tự hai khóa kia. Test có sẵn (vị trí mặc định tắt, 45 giây) giữ nguyên |
| Vị trí mới hợp lệ cho Remote Config và có mã thử | Unit test có sẵn | `AdUnitIdsTest.everyPlacementKey_isUniqueAndUsableInRemoteConfig`, `googleTestUnits_existForEveryPlacement` chạy trên `AdPlacement.entries`, tự gồm vị trí mới; `configuredUnits_areParsedPerPlacementKey` sửa như mục 3.2 |
| Dạng mới của `rememberShowInterstitial` trong test và preview | Compose UI test (Robolectric) | `ShowInterstitialTest.withoutAdsController_reportsNotShownOnce`: `setContent { action = rememberShowInterstitial(REVIEW_INTERSTITIAL) { results += it } }`, gọi `action()` trên luồng UI, `waitForIdle`; `results == listOf(false)`. Đối chứng: trước bước 4, dạng mới trống không gọi `onFinished`, `results` rỗng |
| Dạng mới chỉ nhận vị trí toàn màn hình | Unit test | `ShowInterstitialTest.bannerPlacement_isRejected`: gọi với `SAMPLE_BOTTOM_BANNER` ném `IllegalArgumentException`, như dạng cũ |
| KB-9, tiêu chí 4: không có quảng cáo ở camera và màn xem lại | Kiểm tra code và screenshot test có sẵn | Không thêm `AdSlot`; 7 ảnh chuẩn camera và 5 ảnh chuẩn màn xem lại không đổi (`scripts/check.sh` so ảnh) |
| Quảng cáo thật hiện đúng lúc, đúng giới hạn | Kiểm tra thủ công | Cần một bản build nhận được Remote Config (project Firebase staging thật, hoặc bản nội bộ khi dev có): 1) bật `ads_review_interstitial_enabled`; 2) chụp, lưu, bấm "Chụp lại": không có quảng cáo (ảnh lưu thứ 1); 3) chụp, lưu, "Tư thế tiếp": quảng cáo thử của Google hiện, đóng thì thấy camera với tư thế kế tiếp; 4) lưu thêm 2 ảnh: không có quảng cáo; ảnh thứ 5 sau ít nhất 120 giây kể từ quảng cáo trước: có quảng cáo; 5) chụp, bấm "Chụp lại", "Bỏ ảnh": không có quảng cáo; 6) tắt mạng tới khi hộp thoại mất mạng hiện: không có quảng cáo phủ lên hộp thoại; 7) logcat có `Interstitial review_interstitial skipped: <lý do>` ở các lần không hiện |

Màn hình thấp, đúng thiết kế, chữ đúng bảng chữ: không áp dụng, task không đổi giao diện (không có ảnh thiết kế hay ảnh chuẩn mới; ảnh có sẵn không đổi).

**Ngoại lệ viết test trước:** phần nối trong `AdsController.showInterstitial` (đọc `RemoteSettings`, `NetworkStatus`, thứ tự các bước, gọi SDK) không có unit test: lớp gọi thẳng SDK quảng cáo và `SystemClock`, dự án chưa có test nào dựng nó (như `PrivacyOptionsButtonTest` đã ghi). Mọi quyết định của nó nằm trong `interstitialAllowed`, `InterstitialBreaks` và `FullScreenAdGuard`, đều có test; phần nối được kiểm bằng kiểm tra thủ công ở dòng cuối bảng trên. Người duyệt cần đồng ý ngoại lệ này.

### 4.1. Chạy thử

Không cần (E dưới 2). Quảng cáo toàn màn hình, rào chắn thuần Kotlin và cách `:core:ads` không có controller trong test đều đã có trong dự án; plan chỉ thêm biến thể.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (thêm vị trí quảng cáo và tần suất của nó), 7 (sửa module core dùng chung `:core:ads`, thêm phụ thuộc vào `:core:network`). Rủi ro Cao, cần duyệt chéo, người phụ trách sản phẩm biết trước khi duyệt (risk-scoring.md, mục 3).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:feature:camera`), đổi giao diện công khai của `:core:ads` (dạng mới của `rememberShowInterstitial`, vị trí mới) |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận, đủ con số, nhưng "phiên" chưa được định nghĩa, và bảng dữ liệu của spec ghi "mốc quảng cáo của phiên" giữ tới khi xóa dữ liệu, khác với cách plan đề xuất (câu hỏi 1) |
| C. Khả năng kiểm chứng tự động | 2 | Luật và thứ tự rời màn có test; quảng cáo thật chỉ kiểm được bằng tay trên bản nhận Remote Config, mà bản staging hiện không nhận (câu hỏi 2) |
| D. Ảnh hưởng chéo | 2 | `AdPlacement.kt`, `AdUnitIdsTest.kt` cũng được T-023 sửa; `ReviewScreen.kt` cũng được T-019 và có thể T-020 sửa |
| E. Độ mới của kỹ thuật | 1 | Biến thể của rào chắn và quảng cáo toàn màn hình đã có |
| **Tổng** | 10 | **Mức: Cao** (đồng thời chạm điểm chặn) |

`token_estimate`: số gợi ý cỡ S là 5.0M, mới đo 2 task. Plan sửa hai module, có 4 file test và một test Robolectric dựng `ReviewRoute`, nên ghi 10M.

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `:core:ads` (mọi app của dự án mẫu dùng): `AdPlacement`, `AdsSettings`, `AdsController`, `AdSlot`. Hành vi của vị trí có sẵn không đổi, trừ một điểm: chưa có quảng cáo tải sẵn thì `showInterstitial` trả `false` ngay thay vì chờ người dùng ngừng chạm trước. Thay đổi có ích cho mọi app (giới hạn theo điểm dừng, không hiện khi hộp thoại mất mạng mở, không chờ khi không có quảng cáo): đề xuất về dự án mẫu theo `docs/ops/from-app.md`, ghi trong mô tả PR.
  - `core/ads/build.gradle.kts`: `:core:ads` phụ thuộc thêm `:core:network`.
- Task khác trong sprint có thể xung đột:
  - T-023 (banner thư viện): cùng thêm dòng vào `AdPlacement.kt`, cùng có thể sửa `AdUnitIdsTest.kt`. Sau T-018, `configuredUnits_areParsedPerPlacementKey` dựng cấu hình từ `AdPlacement.entries`, nên T-023 không phải sửa test đó nữa. Chạy lần lượt trong một phiên, task sau gộp nhánh chính trước khi sửa.
  - T-019 (hỏi đánh giá): dựng trên `ReviewExit.kt` và `ReviewRoute` của plan này, phải chạy sau T-018.
  - T-020 (máy gập): có thể sửa `ReviewScreen.kt` (bố cục `ReviewScreen`), plan này chỉ sửa `ReviewRoute` trong cùng file. Chạy lần lượt.
- Đề xuất thứ tự chạy: sau T-016 (đã merge). Trước T-019. Thứ tự với T-023 không quan trọng, chỉ cần lần lượt.

## 7. Điều kiện dừng riêng

- `scripts/release.sh` hay test của nó đọc `AdPlacement.kt` theo cách khác mục 3.2 đã ghi: dừng, không sửa `release.sh`.
- Thêm `implementation(project(":core:network"))` vào `:core:ads` làm `checkModuleDependencies` báo lỗi hay tạo vòng phụ thuộc: dừng, không chuyển phần kiểm hộp thoại mất mạng sang feature khi chưa có người duyệt.

## 8. Câu hỏi cho người duyệt

1. **"Phiên" của giới hạn quảng cáo là gì, và có lưu trên máy không?** Spec mục 5 ghi "không trước ảnh lưu thứ 2 của phiên"; bảng dữ liệu mục 6 ghi "mốc quảng cáo của phiên" giữ tới khi xóa dữ liệu app.
   - Phương án A (AI đề xuất): phiên là một lần chạy của app, từ lúc mở tới khi hệ thống đóng tiến trình; số điểm dừng và lần hiện gần nhất nằm trong bộ nhớ của `AdsController`. Đơn giản, không có khóa DataStore mới, khớp với cách rào chắn 45 giây đang làm. Mở lại app sau khi bị đóng thì đếm lại từ đầu, nên ảnh lưu đầu tiên của lần mở mới không có quảng cáo.
   - Phương án B: lưu số đếm và thời điểm vào DataStore; phiên hết khi app không dùng quá 30 phút (giống cách Firebase Analytics tính phiên). Khớp chữ của spec hơn, nhưng thêm khóa DataStore, đồng hồ thật thay cho đồng hồ đơn điệu, và cần plan phiên bản 2.
   - **Đã trả lời** (Tuấn, 2026-10-23): phương án A. Spec camera đã sửa cho khớp (PR #64): mục 5 định nghĩa phiên, mục 6 ghi mốc quảng cáo của phiên nằm trong bộ nhớ, không lưu; spec xác nhận lại 2026-10-23.
2. **Thử quảng cáo trên máy thế nào?** Bản staging dùng file Firebase giả, không nhận Remote Config, nên vị trí mặc định tắt và quảng cáo này không bao giờ hiện trên staging (ADR-0014, Hệ quả). Đề xuất của AI: giữ nguyên trong task này; kiểm tra thủ công ở mục 4 làm khi nhóm có project Firebase staging thật, ghi vào danh sách "Cần người xem" của phiên. Nếu muốn thấy quảng cáo trên staging ngay, cần một cách bật vị trí riêng cho staging (giống `StagingFeatureFlagsModule`), là thay đổi của `:core:ads` và vỏ app, nên đề xuất thành ticket riêng và đưa về dự án mẫu.
3. **Thứ tự hiện quảng cáo và về camera.** Plan gọi quảng cáo, rồi ngay khi quảng cáo đã mở (hoặc bị bỏ qua) thì về camera bên dưới quảng cáo; người dùng đóng quảng cáo là thấy camera. Rào chắn chung bắt chờ người dùng ngừng chạm 1 giây, nên sau khi bấm "Chụp lại" người dùng thấy màn xem lại đứng yên khoảng 1 giây trước khi quảng cáo hiện (chỉ khi có quảng cáo tải sẵn và đủ giới hạn; các trường hợp khác về camera ngay). Đề xuất của AI: chấp nhận, không thêm vòng chờ hay hiệu ứng.
4. **Việc của dev trước bản phát hành có `review_interstitial`** (không phải việc của AI): tạo mã quảng cáo toàn màn hình thật cho vị trí trên AdMob, thêm `review_interstitial=<mã>` vào `adUnits` của thư mục phát hành (`release.sh` từ chối nếu thiếu), và bật `ads_review_interstitial_enabled` trên Remote Config production khi người phụ trách sản phẩm quyết định.
5. **Người phụ trách sản phẩm** xác nhận các mặc định 2, 3, 120 giây (đúng spec mục 5) và việc giữ rào chắn chung 45 giây giữa hai quảng cáo toàn màn hình bất kỳ.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: phương án A (phiên là một lần chạy của app; spec đã sửa ở PR #64).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
