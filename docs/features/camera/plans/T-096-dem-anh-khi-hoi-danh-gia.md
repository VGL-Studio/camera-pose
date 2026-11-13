---
ticket: T-096
title: Hỏi đánh giá app tính cả ảnh vừa lưu từ hộp thoại khi rời màn xem lại
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 5                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 7.2M         # cỡ S theo scripts/token-usage.py estimate (2026-11-13), không nâng tay
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật
cross_reviewer:                  # mức trung bình, không bắt buộc
approved_at: 2026-11-13
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-096: HỎI ĐÁNH GIÁ APP TÍNH CẢ ẢNH VỪA LƯU TỪ HỘP THOẠI KHI RỜI MÀN XEM LẠI

## 1. Tóm tắt

Khi người dùng lưu ảnh từ hộp thoại "Bỏ ảnh này?" lúc rời màn xem lại, app lưu rồi rời màn ngay, và phần hỏi đánh giá nhận số ảnh đã lưu chưa tính ảnh vừa lưu (FB-20261113-01). Test tạm trên code hiện tại xác nhận: kho đã có 3 ảnh nhưng lời hỏi nhận 2, và chính `ReviewViewModel.savedCount` (qua `stateIn`) cũng còn 2 lúc đó, không chỉ giá trị màn hình giữ qua `rememberUpdatedState`.

Sửa: sau khi ghi xong, ViewModel đọc lại số đếm từ kho (`repository.savedCount.first()`) và giữ lại; màn hình hỏi đánh giá bằng số lớn hơn giữa số đó và `savedCount.value`, đọc đúng lúc hỏi. Kiểm chứng bằng ba test Robolectric của `ReviewRoute` (lưu từ hộp thoại, bấm Lưu rồi rời màn, ghi lỗi) và hai unit test của `ReviewViewModel` dùng kho giả có số đếm cập nhật trễ; đường bấm Lưu rồi mới rời màn giữ nguyên.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-10 | Khi người dùng đã lưu ít nhất 3 ảnh (tính từ khi cài) và rời màn xem lại sau khi đã lưu ảnh ở lần xem lại đó thì app gọi hộp đánh giá của Google Play, nếu lần gọi trước đã cách ít nhất 30 ngày | "Đã lưu ít nhất 3 ảnh" tính cả ảnh vừa lưu ở lần xem lại đó, bất kể lưu bằng nút Lưu hay bằng nút "Lưu ảnh" của hộp thoại khi rời màn. Số đưa vào `InAppReview.requestIfDue(savedPhotos = ...)` phải bằng số đã ghi trong kho sau lần lưu đó. Luật 3 ảnh, 30 ngày và cờ nằm ở `:core:review`, không đổi |
| KB-10 | Khi quảng cáo toàn màn hình vừa hiện ở cùng lần rời màn xem lại thì app không gọi hộp đánh giá lần đó | Không đổi: `afterReviewAd` vẫn chỉ hỏi khi quảng cáo không hiện |
| KB-10 | Khi cờ `feature_in_app_review_enabled` tắt thì app không gọi hộp đánh giá | Không đổi: cờ do `:core:review` đọc |

Thiết kế tham chiếu: không có thiết kế riêng; không đổi giao diện.

ADR liên quan: ADR-0019 (ghi log), ADR-0031 (mục 3.5).

Phản hồi: `features/camera/feedback/FB-20261113-01-dem-anh-cham-khi-hoi-danh-gia.md` (`triaged`); bằng chứng log ở `docs/test-sessions/TS-20261113-01.md`, mục P-1.

## 3. Giải pháp

### 3.1. Cách làm

Nguyên nhân (đã đo, mục 4): đường lưu từ hộp thoại là `onDiscardSave()` rồi `save()`: ghi ảnh, `repository.recordSaved(uri)`, rồi `leave()` gửi `ReviewEvent.Leave(true)` ngay trong cùng coroutine. `ReviewRoute` nhận sự kiện, gọi `leaveReview` → `showAd` → `afterReviewAd` → `askReview`, và `askReview` đọc `currentSavedCount`. Số này đi qua hai chặng chậm: `savedCount = repository.savedCount.stateIn(...)` chỉ cập nhật khi coroutine thu của `stateIn` được chạy lại, sau sự kiện; và `rememberUpdatedState(savedCount)` chỉ cập nhật khi màn hình vẽ lại. Bấm Lưu rồi mới rời màn thì hai chặng đã kịp, nên số đúng.

Sửa trong `ReviewViewModel`:

- Thêm `private var recordedCount = 0`. Trong `save()`, trong cùng khối `withContext(NonCancellable)` sau `repository.recordSaved(uri)`, đọc `recordedCount = repository.savedCount.first()`. Kho DataStore trả giá trị mới ngay sau `edit` (đã có test `DataStoreSavedPhotosRepositoryTest`, dòng 30). Ghi lỗi (`IOException`) thì không đọc, `recordedCount` giữ nguyên. Kho thật đọc qua `retryWhen`: gặp lỗi đọc thì `first()` trả 0 (bản `emptyPreferences()`); `maxOf` ở dưới giữ số không bị hạ về 0.
- Thêm `internal fun savedCountForReview(): Int = maxOf(savedCount.value, recordedCount)`, kèm KDoc: số ảnh đã lưu đưa cho lời hỏi đánh giá, tính cả ảnh vừa ghi khi `savedCount` chưa kịp cập nhật; `first()` của kho trả 0 khi đọc lỗi, nên lấy số lớn hơn. Lấy số lớn hơn vì luồng đọc của kho phát 0 khi gặp lỗi đọc (`retryWhen`), và vì `savedCount` có thể đã lớn hơn khi mở lại màn sau khi activity được dựng lại.
- Giữ `savedCount` (`StateFlow`), vì test `savedCount_followsRepository` kiểm nó; không còn nơi nào khác đọc nó trong code chính.

Sửa trong `ReviewScreen.kt` (`ReviewRoute`, dòng 75 đến 79): bỏ `val savedCount by viewModel.savedCount.collectAsStateWithLifecycle()` và `val currentSavedCount by rememberUpdatedState(savedCount)`; `askReview` thành `{ currentRequestReview(viewModel.savedCountForReview()) }`, đọc số đúng lúc hỏi. Chú thích cạnh đó ghi vì sao đọc lúc hỏi. Bỏ import không còn dùng nếu có.

Các đường rời màn đã lưu ở lần xem lại đó, và số mà lời hỏi nhận sau khi sửa:

| Đường | Số nhận được |
| --- | --- |
| Bấm Lưu, rồi Chụp lại, Tư thế tiếp hay đóng | `savedCount.value` đã cập nhật, bằng `recordedCount`: số mới |
| Hộp thoại "Bỏ ảnh này?", bấm "Lưu ảnh" (lỗi gốc) | `recordedCount`: số mới |
| Hộp thoại, "Lưu ảnh", máy Android 9 trở xuống phải xin quyền trước, đồng ý | Cùng `save()`: số mới |
| Ghi ảnh xong nhưng ghi số đếm lỗi | `recordedCount` không đổi: số cũ, đúng với kho |
| Mở lại màn sau khi activity được dựng lại, ảnh đã lưu trước đó | `recordedCount` là 0, `savedCount.value` đã đọc từ kho: số trong kho |
| Bấm Lưu rồi rời màn ngay khi việc ghi số đếm chưa xong (màn đã hiện "đã lưu", `save_leaveDuringRecord_recordStillCompletes`) | Lời hỏi chạy trước khi ghi xong: số cũ, như hôm nay. Không sửa ở plan này (mục 8, câu 2) |

Không đổi `leaveReview`, `afterReviewAd` (`ReviewExit.kt`), `ReviewEvent.Leave`, luật của `:core:review`, log hay sự kiện phân tích.

Mẫu có sẵn: `ReviewRouteTest` (T-018, T-019) dựng `ReviewRoute` với `FakeInAppReview` và `FakeSavedPhotosRepository`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewViewModel.kt` | `recordedCount`, đọc lại số đếm sau `recordSaved`, `savedCountForReview()` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreen.kt` | `askReview` đọc `viewModel.savedCountForReview()` lúc hỏi; bỏ `collectAsStateWithLifecycle` và `rememberUpdatedState` của số đếm |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewRouteTest.kt` | Ba test mới ở mục 4 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewViewModelTest.kt` | Hai test mới ở mục 4 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewFakes.kt` | `FakeSavedPhotosRepository.lagUpdates` (mặc định `false`): khi bật, bộ thu đang chạy (`stateIn` của ViewModel) chỉ thấy số đếm lúc bắt đầu thu, bộ thu mới (`first()`) đọc số hiện tại, như DataStore ngay sau khi ghi. Code: `override val savedCount: Flow<Int> get() = if (!lagUpdates) count else flow { emit(count.value); awaitCancellation() }`. Đặt `lagUpdates` trước khi tạo ViewModel. Thuộc commit test của bước 3 |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không thêm; `feature_in_app_review_enabled` vẫn do `:core:review` đọc.
- Quảng cáo: không chạm; thứ tự với `review_interstitial` giữ nguyên.
- Sự kiện phân tích: không chạm.
- File R2: không chạm.
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. `kotlinx.coroutines.flow.first` đã có trên classpath của `:feature:camera` (đã dùng trong test của module).

### 3.4. Phương án dự phòng

Không có. Cách làm chỉ đọc lại một `Flow` đã có test sau khi ghi.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` đã chạy cho plan này; các test ở nhóm "Cần xem trước" và "Nên xem" đã đọc.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Lời hỏi đánh giá nhận `savedCount` qua `rememberUpdatedState` | Lời hỏi nhận `savedCountForReview()`, đọc lúc hỏi | `ReviewRouteTest.savedThenRetake_asksReviewOnceAfterLeaving` (số 3, hỏi một lần, sau khi đã về camera) | Giữ: số trong kho là 3, `savedCountForReview()` trả 3 |
| Không lưu thì không hỏi | Không đổi | `ReviewRouteTest.discardThenLeave_doesNotAskReview` | Giữ |
| `savedCount` theo kho | Không đổi (vẫn là `stateIn`) | `ReviewViewModelTest.savedCount_followsRepository` | Giữ |
| Thứ tự quảng cáo rồi hỏi đánh giá | Không đổi | `ReviewExitTest` | Giữ |
| Sự kiện `Leave(savedThisVisit)` | Không đổi | `ReviewViewModelTest` (các câu `Leave(true)`, `Leave(false)`) | Giữ |
| Rời màn trong lúc ghi số đếm thì việc ghi vẫn xong | Không đổi: đọc lại số đếm nằm trong cùng khối `NonCancellable` sau `recordSaved` | `ReviewViewModelTest.save_leaveDuringRecord_recordStillCompletes` | Giữ |
| Thứ tự rời màn: hỏi quảng cáo trước khi hỏi đánh giá | Không đổi | `ReviewExitTest.saved_asksAdBeforeReview` | Giữ |
| Bỏ ảnh thì rời màn đúng một lần | Không đổi | `ReviewRouteTest.discardThenLeave_leavesOnce` | Giữ |
| "Lưu ảnh" trong hộp thoại lưu rồi rời màn | Không đổi: đọc lại số đếm chạy trước `leave`, sự kiện vẫn là `Leave(true)` | `ReviewViewModelTest.discardSave_savesThenLeaves`, `ReviewViewModelTest.discardSave_permissionGranted_savesThenLeaves` | Giữ |
| Ghi số đếm lỗi thì ảnh vẫn tính là đã lưu | Không đổi: lỗi bắt như cũ, không đọc lại số | `ReviewViewModelTest.save_recordFails_stillSaved` | Giữ |
| Từ chối quyền lưu ảnh hẳn thì hiện thông báo và mở cài đặt | Không đổi (không chạm đường xin quyền) | `ReviewViewModelTest.permissionBlocked_showsBlocked_andOpensSettings` | Giữ |

## 4. Kiểm thử

**Số đo trên code hiện tại** (2026-11-13, commit `8af6a3e`, test tạm `TmpT096Test` không commit, khung `vi-w411dp-h891dp-xxhdpi`, `@GraphicsMode(NATIVE)` như `ReviewRouteTest`): kho giả có 2 ảnh; mở màn với ảnh chưa lưu; bấm "Tư thế tiếp", rồi "Lưu ảnh" trong hộp thoại.

- Lời hỏi nhận `requests=[2]`, trong khi kho đã có `3`; màn được rời đúng một lần. Test thất bại: `expected:<[3]> but was:<[2]>`.
- Lúc hỏi, `viewModel.savedCount.value` cũng là `2`: chỗ chậm đã có ở `stateIn` của ViewModel, không chỉ ở `rememberUpdatedState`. Giả thuyết của FB-20261113-01 đúng một phần; sửa phải đọc lại từ kho, không chỉ bỏ `rememberUpdatedState`.
- Đối chứng: bấm "Lưu vào thư viện" rồi "Tư thế tiếp" thì lời hỏi nhận `[3]` (qua).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-10, tiêu chí 1: lưu từ hộp thoại khi rời màn, số tính cả ảnh vừa lưu | Compose UI test (Robolectric) | `ReviewRouteTest.saveFromDiscardDialog_asksReviewWithNewCount`: `repository.count.value = 2`, `setRouteWithReview(SavedStateHandle(mapOf("review_path" to file.path)))`, bấm "Tư thế tiếp", `waitForIdle`, bấm "Lưu ảnh", `waitForIdle`; khẳng định `leaves == 1`, `inAppReview.requests == listOf(3)`, `inAppReview.leavesAtRequest == listOf(1)`. Trên code hiện tại thất bại với `[2]` (đã đo) |
| KB-10, tiêu chí 1: bấm Lưu rồi rời màn (đối chứng, giữ hành vi) | Compose UI test | `ReviewRouteTest.saveThenNextPose_asksReviewWithNewCount`: số 2 trong kho, bấm "Lưu vào thư viện", rồi "Tư thế tiếp"; khẳng định `requests == listOf(3)`. Đối chứng, qua trên code hiện tại (đã đo) |
| Ghi số đếm lỗi thì lời hỏi nhận số trong kho | Compose UI test | `ReviewRouteTest.saveFromDiscardDialog_recordFails_asksReviewWithStoredCount`: số 2, `repository.failWrites = true`, đường hộp thoại như trên; khẳng định `leaves == 1`, `requests == listOf(2)`. Đối chứng cho nhánh lỗi (qua trên code hiện tại, vì số trong kho không đổi) |
| `savedCountForReview()` tính ảnh vừa ghi khi `savedCount` chưa cập nhật | Unit test | `ReviewViewModelTest.savedCountForReview_afterSaveFromDialog_isNewCount`: `repository.count.value = 2`, `repository.lagUpdates = true`, rồi `viewModel()`; `onNextPoseClick()`, `onDiscardSave()`, `runCurrent()`; khẳng định `events == listOf(Leave(true))` và `viewModel.savedCountForReview() == 3`. Trên khung của bước 3 thất bại `expected:<3> but was:<2>` (đã đo); không bật `lagUpdates` thì qua ngay trên khung, vì `MainDispatcherRule` dùng `UnconfinedTestDispatcher` và `stateIn` đã là 3 lúc nhận `Leave` (đã đo) |
| Ghi lỗi thì `savedCountForReview()` giữ số trong kho | Unit test (đối chứng) | `ReviewViewModelTest.savedCountForReview_whenRecordFails_keepsStoredCount`: số 2, `lagUpdates = true`, `failWrites = true`, cùng đường; khẳng định `2`. Qua cả trên khung lẫn sau khi sửa (đã đo), giữ để chặn bản sửa cộng số khi ghi lỗi |
| Quảng cáo vừa hiện thì không hỏi; cờ tắt thì không hỏi (KB-10, tiêu chí 2, 3) | Test có sẵn | `ReviewExitTest`, test của `:core:review`; không đổi |
| Hộp đánh giá thật hiện ở lần đủ 3 ảnh khi lưu từ hộp thoại | Kiểm tra thủ công (máy thật: Play, In-App Review chỉ chạy với bản cài từ Play hay Internal App Sharing) | Theo FB-20261113-01: đặt `is_show_review_interstitial` = false trên Firebase staging (bản staging bật sẵn vị trí này ở `StagingRemoteValuesModule`, Firebase ghi đè được); cờ `feature_in_app_review_enabled` luôn bật trên staging (`StagingFeatureFlagsModule`), không cần bật. Xóa dữ liệu app, lưu 2 ảnh bằng nút Lưu, ảnh thứ 3 lưu từ hộp thoại khi bấm "Tư thế tiếp"; xem log `VGL.InAppReview`: `Review asked` (`core/review/.../InAppReview.kt:66`), không có `Review skipped: not_due saved=2`. Lần rời màn đó không có quảng cáo `review_interstitial` (khóa đã tắt ở trên), vì quảng cáo hiện thì không hỏi |

Khung của bước 3 (để test biên dịch được và thất bại ở câu khẳng định): `ReviewViewModel` có `internal fun savedCountForReview(): Int = savedCount.value`, chưa có `recordedCount`, chưa sửa `ReviewScreen.kt`. Commit test của bước 3 có năm test trên cùng `lagUpdates` của `ReviewFakes.kt`. Trên khung đó (đã đo bằng test tạm, không commit): `ReviewRouteTest.saveFromDiscardDialog_asksReviewWithNewCount` thất bại vì `[2]`; `ReviewViewModelTest.savedCountForReview_afterSaveFromDialog_isNewCount` thất bại `expected:<3> but was:<2>`; ba test còn lại là đối chứng, qua. Sau bản sửa của mục 3.1 cả năm qua (đã đo với bản sửa tạm, không commit).

**Ngoại lệ viết test trước:** Không có.

### 4.1. Chạy thử

Không cần (E dưới 2). Lệnh đo ở mục 4: `./gradlew :feature:camera:testDebugUnitTest --tests '*TmpT096Test'` và `--tests '*TmpT096UnitTest'` (test tạm, khung và bản sửa tạm đã trả về bằng `git checkout --`, không commit).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không chạm quảng cáo, `:core:*`, dữ liệu gửi đi hay lược đồ DataStore (chỉ đọc khóa đếm có sẵn).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai file code và hai file test trong `:feature:camera`; thêm một hàm `internal` |
| B. Độ rõ của yêu cầu | 0 | KB-10 đã xác nhận, phản hồi có các bước làm lại, lỗi đã tái hiện |
| C. Khả năng kiểm chứng tự động | 1 | Số đưa cho lời hỏi kiểm bằng test; hộp đánh giá thật của Play chỉ kiểm trên máy thật |
| D. Ảnh hưởng chéo | 2 | T-094 (thanh đáy `review_bottom`) cũng sửa `ReviewScreen.kt` |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn (đọc lại `Flow` sau khi ghi) |
| **Tổng** | 5 | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không. `SavedPhotosRepository` chỉ được đọc, không đổi.
- Task khác có thể xung đột: T-094 (thanh đáy native ở màn xem lại) sửa `ReviewScreen.kt` để bọc màn trong `BottomAdBarScaffold`; T-096 sửa `ReviewRoute` (dòng 75 đến 79) trong cùng file. Hai chỗ sửa gần nhau nhưng khác khối. `run-check.py` sẽ báo cùng sửa file.
- Đề xuất thứ tự chạy: T-096 trước T-094 trong cùng danh sách (task nhỏ, độc lập), hoặc ở hai phiên. Nếu T-094 chạy trước thì T-096 dựng trên `ReviewScreen.kt` đã có thanh đáy; phần sửa của T-096 không đổi.

## 7. Điều kiện dừng riêng

- Sửa xong mà `ReviewRouteTest.savedThenRetake_asksReviewOnceAfterLeaving` hay `ReviewExitTest` đỏ: dừng, vì plan không đổi thứ tự quảng cáo rồi hỏi đánh giá.

## 8. Câu hỏi cho người duyệt

1. Plan sửa ở màn hình và ViewModel, không đổi `SavedPhotosRepository.recordSaved` (có thể trả số mới). Đề xuất của AI: giữ giao diện của kho, đọc lại `savedCount.first()`, để không đổi lớp giả và test của kho. Đồng ý?
2. Còn một đường số chậm hiếm gặp: bấm Lưu, màn hiện "đã lưu", rồi rời màn trong vài mili giây khi việc ghi số đếm chưa xong; lời hỏi khi đó vẫn nhận số cũ. Sửa đường này phải cho rời màn chờ việc ghi, hay cộng thêm một khi đang ghi. Đề xuất của AI: không làm ở plan này, vì lỗi đã thấy trên máy là đường hộp thoại (việc ghi luôn xong trước khi rời màn), còn đường này cần bấm nhanh hơn thời gian ghi DataStore. Đồng ý?

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** không bắt buộc (mức trung bình)
- **Ghi chú:** Nhật duyệt trong phiên có người theo dõi ngày 2026-11-13, lời của Nhật: "duyệt", sau khi AI tóm tắt plan và nói câu hỏi mục 8 theo đề xuất của AI. AI ghi thay. Cùng lúc Nhật xác nhận phần sửa spec camera ở PR #335. Trả lời câu hỏi ở mục 8:
  1. Giữ giao diện của `SavedPhotosRepository`; ViewModel đọc lại `savedCount.first()` sau `recordSaved`, không để `recordSaved` trả số mới.
  2. Không sửa ở plan này đường hiếm "bấm Lưu rồi rời màn trong vài mili giây khi việc ghi số đếm chưa xong"; lời hỏi khi đó vẫn nhận số cũ như hôm nay.

## 10. Nhật ký thực hiện

### RUN-20261113-02, phiên bản plan 1

- **Kết quả:** đã merge.
- **Nhánh, PR:** `claude/T-096-dem-anh-hoi-danh-gia`, PR #339, commit merge `2d28b70`.
- **Đã làm:** `ReviewViewModel` đọc lại số ảnh đã lưu bằng `repository.savedCount.first()` ngay sau `recordSaved` (cùng khối `NonCancellable`) vào `recordedCount`; `savedCountForReview() = maxOf(savedCount.value, recordedCount)` có KDoc. `ReviewRoute` hỏi đánh giá bằng `viewModel.savedCountForReview()` lúc hỏi, bỏ `collectAsStateWithLifecycle` và `rememberUpdatedState` của số đếm. Kho giả `FakeSavedPhotosRepository.lagUpdates`. Bước 3 đến 5 do phiên con làm trong worktree riêng.
- **Test viết trước:** 5 test (commit `0498e59`) trên khung `savedCountForReview() = savedCount.value`. Thất bại đúng lý do: `ReviewRouteTest.saveFromDiscardDialog_asksReviewWithNewCount` (`expected:<[3]> but was:<[2]>`), `ReviewViewModelTest.savedCountForReview_afterSaveFromDialog_isNewCount` (`expected:<3> but was:<2>`). Ba test đối chứng qua trên khung như plan ghi. Test JVM mọi module trên khung: chỉ 2 test đó đỏ.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 2 ghi chú (`savedCount` công khai nay chỉ đọc qua `savedCountForReview()`, plan chủ ý giữ; đường hiếm "bấm Lưu rồi rời màn khi việc ghi chưa xong" vẫn nhận số cũ, mục 8 câu 2). Đột biến của người rà (bỏ phép gán `recordedCount`): đúng 2 test trên đỏ.
- **Kết quả CI:** `pr-check` xanh (lần chạy 43440183407), `main-guard` xanh (43442797260). `scripts/check.sh` trên máy qua (3128 test JVM, 0 lỗi).
- **Lệch so với plan:** Không có. Không có file ngoài mục 3.2; file test không đổi sau commit test.
- **Cần người xem:** thử trên máy thật theo mục 4 (log `Review asked` ở lần đủ 3 ảnh khi lưu từ hộp thoại; đặt `is_show_review_interstitial = false` trên Firebase staging lúc thử rồi trả lại).
