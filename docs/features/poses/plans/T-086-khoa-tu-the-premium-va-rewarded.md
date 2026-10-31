---
ticket: T-086
title: Khóa tư thế premium ở thư viện, chi tiết, camera; mở bằng quảng cáo có thưởng `rw_premium_pose`
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 18.0M        # hai lượt theo số gợi ý của scripts/token-usage.py estimate (2026-10-31): lượt 1 cỡ M (9.0M), lượt 2 cỡ M (9.0M); không nâng tay
blocking_points: [ads, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-086: KHÓA TƯ THẾ PREMIUM Ở THƯ VIỆN, CHI TIẾT, CAMERA; MỞ BẰNG QUẢNG CÁO CÓ THƯỞNG `RW_PREMIUM_POSE`

## 1. Tóm tắt

Tư thế premium chưa mở khóa có dấu premium trên thẻ thư viện, ở chi tiết và trên ô của dải tư thế camera. Nút chính của chi tiết thành nút mở khóa; chạm ô khóa ở camera không đổi tư thế đang dùng. Cả hai chỗ mở hộp thoại mở khóa của khung (`rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, ...)` và `RewardedAdDialog`). Xem hết quảng cáo, hay khóa `is_show_rw_premium_pose` tắt, hay cổng đồng ý đóng, thì ViewModel ghi mở khóa qua `:core:poses` (T-085), ghi `premium_pose_unlocked`, rồi làm tiếp việc người dùng định làm: về camera với tư thế đó (chi tiết), hay dùng tư thế đó (camera). Vị trí mới không khai báo `noAdPolicy`, nên không có quảng cáo thì chặn (câu 8).

Việc chia hai lượt trong cùng danh sách chạy, mỗi lượt một PR có CI xanh, như T-079:

- **Lượt 1:** vị trí `rw_premium_pose`, sự kiện, bản staging, bảng khóa, dấu premium của hệ thống thiết kế, thư viện và chi tiết, chữ của hộp thoại khung trong 6 ngôn ngữ.
- **Lượt 2:** camera (dấu trên ô, hộp thoại khi chạm ô khóa, tư thế dùng lần trước bị khóa, "Tư thế tiếp" bỏ qua ô khóa).

Kiểm chứng gồm:

- unit test ViewModel của chi tiết và camera với kho giả: được thưởng, đóng sớm, không có quảng cáo, khóa tắt, ghi lỗi, cờ tắt;
- test giao diện Robolectric cho dấu premium, nút mở khóa và đường "không có bộ điều khiển quảng cáo thì chạy ngay";
- screenshot test cùng tên với mỗi ảnh thiết kế mới, ảnh `_small` của chi tiết khi còn khóa;
- test giá trị staging, bảng khóa sinh lại, test chữ của hộp thoại khung trong 7 ngôn ngữ.

**Điểm chặn trước khi duyệt (mục 8, câu 1):** chưa có thiết kế Figma của tư thế premium, mà ticket ghi thiết kế phải có trước plan. Plan soạn đủ phần code và test; tên các frame cần dựng ở mục 8, câu 1. Plan chờ gói bàn giao (`design/screens/`, `design/context/`, bảng chữ) rồi mới duyệt; số đo của dấu, nút và hộp thoại trong plan là tạm, phiên lấy theo `design/context/`.

**Phụ thuộc chưa xong (mục 8, câu 2):** T-085 (dữ liệu premium) chưa có plan. Plan dùng đúng tên ở ticket T-085 (`Pose.premium`, `unlock(poseId)`, `FeatureFlag.PREMIUM_POSES`) và một tên giả định cho kho (`PremiumPoseRepository`, luồng `lockedIds`). Chữ ký phải khớp plan T-085 khi plan đó xong.

## 2. Hiểu yêu cầu

Ticket xử lý KB-8 của `features/poses/spec.md` (confirmed 2026-10-31, câu 7, 8, 10, 11, 12), hai tiêu chí tư thế premium ở KB-1 của `features/camera/spec.md`, và vị trí `rw_premium_pose` ở câu 12 của `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Poses KB-8, tiêu chí 1 | Khi tư thế là premium và chưa mở khóa thì thẻ ở thư viện, ô ở dải tư thế của camera và màn chi tiết có dấu premium, đúng thiết kế | "Khóa" là `id in lockedIds` của T-085: luồng đã gộp cờ, `premium` và danh sách đã mở. Dấu là thành phần mới `PremiumBadge` của `:core:designsystem`, vẽ trên `PoseCard` và `PoseThumb` khi tham số `premium` là `true`, và ở chi tiết cạnh dòng phụ. Thẻ còn khung xương (ảnh chưa tải) không có dấu. Thư viện ở lượt 1, camera ở lượt 2 |
| Poses KB-8, tiêu chí 2 | Khi mở chi tiết của tư thế premium chưa mở khóa thì ảnh, các bước, lỗi hay gặp hiện như tư thế thường; nút chính là nút mở khóa; lưu và bỏ lưu vẫn được | Chạm thẻ khóa vẫn mở chi tiết như cũ (câu 7). `PoseDetailUiState.Ready.locked`; `PoseDetailBottomBar` đổi chữ và hành động của `PrimaryButton` khi khóa ("Xem quảng cáo để dùng", chữ chốt theo thiết kế). `onToggleSave` không đổi |
| Poses KB-8, tiêu chí 3 | Khi bấm nút mở khóa ở chi tiết, hay chạm ô của tư thế đó ở dải camera, thì hộp thoại mở khóa hiện; tư thế đang dùng ở camera không đổi. Hộp thoại có tên tư thế, lời giải thích, nút "Xem quảng cáo", "Để sau" | Nút gọi `unlock.start()` của `rememberRewardedAction`. Khóa bật và cổng đồng ý mở thì khung mở hộp thoại (`RewardedAdDialog`): tiêu đề là chữ của app, nội dung là `PremiumUnlockContent` (tên tư thế, lời giải thích), hai nút của khung. Chữ hai nút là chuỗi `ads_reward_watch`, `ads_reward_no_thanks` của `:core:ads` (tiếng Anh "Watch ad", "No, thanks"); app dịch trong `app/src/main/res/values-<ngôn ngữ>/` (tiếng Việt "Xem quảng cáo", "Để sau"), đã chạy thử (mục 4.1). Ở camera, `onPoseSelect` của ô khóa không chọn tư thế; ô gọi `onLockedPoseSelect`, ViewModel giữ tư thế chờ mở (`SavedStateHandle`), màn hình gọi `start()` |
| Poses KB-8, tiêu chí 4 | Khi người dùng xem hết quảng cáo và nhận thưởng thì tư thế mở khóa ngay: dấu mất ở mọi màn, và app làm tiếp việc người dùng vừa định làm | `onGranted` của khung chạy một lần mỗi lần mở hộp thoại, trên luồng chính, lúc SDK báo thưởng (trước khi quảng cáo đóng). Chi tiết: `onUnlockGranted()` ghi `unlock(poseId)`, ghi sự kiện, rồi phát sự kiện mở camera với tư thế đó; camera mở bên dưới quảng cáo và hiện khi quảng cáo đóng. Camera: `onUnlockGranted()` ghi, ghi sự kiện, rồi chọn tư thế như chạm ô thường (`pose_selected` `strip`, đánh dấu đã dùng). Dấu mất vì `lockedIds` của T-085 phát giá trị mới, mọi màn đang theo dõi luồng đó |
| Poses KB-8, tiêu chí 5 | Khi đóng quảng cáo trước khi nhận thưởng, hay bấm "Để sau", thì tư thế vẫn khóa, app ở lại màn đang mở | Khung không gọi `onGranted` (R-04; `dismiss()`). Feature không làm gì. Phần khung đã có test ở `RewardedActionTest`; feature kiểm "không gọi `onUnlockGranted` thì không ghi, không điều hướng" |
| Poses KB-8, tiêu chí 6 | Khi bấm "Xem quảng cáo" mà không có quảng cáo thì tư thế vẫn khóa; thử lại sau được. Lỗi mạng thì có thông báo kiểm tra kết nối của khung | Vị trí không khai báo `noAdPolicy`, nên `effectiveNoAdPolicy` là `Block`: `NoAd(granted = false)`, không gọi `onGranted`; lỗi mạng thì khung hiện `Toast` `ads_reward_check_connection` (app dịch, như hai nút). `AdPlacementAppTest` kiểm `noAdPolicy == null` và `effectiveNoAdPolicy == Block` |
| Poses KB-8, tiêu chí 7 | Đã mở khóa thì lần mở app sau, kể cả sau khi cập nhật app hay catalog mới, vẫn mở | Phần lưu là của T-085. Plan này chỉ đọc `lockedIds` mỗi lần màn mở; test lượt 1 và 2 dùng kho giả. Kiểm trên máy thật (mục 4) |
| Poses KB-8, tiêu chí 8 | Khi cờ `feature_premium_poses_enabled` tắt thì không có dấu, mọi tư thế dùng được, không ghi gì vào danh sách đã mở khóa | Cờ tắt thì `lockedIds` của T-085 là tập rỗng: không có dấu, nút chính là "Chụp với tư thế này", ô chạm được như cũ, `start()` không bao giờ được gọi. `onUnlockGranted()` với tư thế không khóa không gọi `unlock` và không ghi sự kiện (phòng khi luồng đổi giữa chừng) |
| Poses KB-8, tiêu chí 9 | Khi cờ bật mà `is_show_rw_premium_pose` tắt hay chưa đồng ý quảng cáo thì bấm "Xem quảng cáo" mở khóa ngay, vĩnh viễn, không hiện quảng cáo | Khung: `rewardedSwitch` là `isEnabled(placement) && canRequestAds`; tắt thì `start()` gọi `onGranted` ngay, không hộp thoại. Nên nút mở khóa (chi tiết) và ô khóa (camera) mở thẳng, không có bước "Xem quảng cáo". Plan hiểu chữ "bấm Xem quảng cáo" của tiêu chí là bấm nút mở khóa, vì khi đó không có hộp thoại nào. Test giao diện của feature chạy đúng đường này: không có `LocalAdsController` thì `start()` gọi `onGranted` ngay (đã chạy thử, mục 4.1) |
| Poses KB-8, tiêu chí 10 | Khi tư thế mở khóa thì app ghi `premium_pose_unlocked` | `EVENT_PREMIUM_POSE_UNLOCKED = "premium_pose_unlocked"` trong `PoseDetailViewModel` (`source` `detail`) và `PoseViewModel` của camera (`source` `camera`), tham số `pose_id`, `category`, `source` (spec mục 6). Ghi sau khi `unlock` xong, một lần mỗi lần mở; ghi lỗi thì không ghi sự kiện |
| Poses KB-8, tiêu chí 11 | Khi mở camera lần đầu thì tư thế mặc định và bộ khởi đầu không bao giờ là premium | Do catalog và script kiểm của T-085. Plan này thêm đường dự phòng ở camera: tư thế dùng lần trước bị khóa thì dùng `defaultPose` (tiêu chí camera dưới) |
| Camera KB-1, tiêu chí 8 | Khi cờ bật thì ô của tư thế premium chưa mở khóa có dấu premium; chạm ô thì hộp thoại mở khóa hiện, tư thế đang dùng không đổi; mở khóa xong thì camera dùng tư thế đó | Lượt 2. `PoseThumbItem.locked`; `PoseThumb(premium = item.locked)`; chạm ô khóa gọi `onLockedPoseSelect(id)`. `PoseViewModel` theo dõi `lockedIds` suốt đời màn (thư viện thì chỉ lấy một lần, như cũ), nên mở khóa ở chi tiết rồi quay lại camera thì dấu đã mất |
| Camera KB-1, tiêu chí 9 | Khi tư thế dùng lần trước trở thành premium chưa mở khóa (catalog mới) thì camera dùng tư thế mặc định | Lượt 2. `onLibraryReady` đọc `lockedIds.first()` trước khi chọn: `requested ?: lastUsedPose?.takeUnless { it.id in locked } ?: defaultPose`. Tư thế đang dùng trở thành khóa giữa lúc camera đang mở (cờ bật từ xa) thì giữ nguyên tới lần mở sau; chỉ chọn mới mới kiểm khóa |
| Spec ads, câu 12 | `rw_premium_pose`: rewarded mở khóa tư thế premium; không có quảng cáo thì chặn | Một dòng `RW_PREMIUM_POSE("rw_premium_pose", AdFormat.REWARDED),` có chú thích hành động. `AdsSettings` tự sinh `is_show_rw_premium_pose`, mặc định tắt (ADR-0014). Bản staging bật (spec poses mục 8). Mã production: danh sách của T-075 lấy mọi dòng `AdPlacement` không phải `gallery_` (ticket T-075, Định nghĩa hoàn thành), nên vị trí tự vào danh sách, không sửa file nào |

Thiết kế tham chiếu: chưa có. Gói hiện có `features/poses/design/README.md` (thư viện, chi tiết) và `features/camera/design/README.md` (camera, chỉ bản tối) không có frame premium. Cần dựng trên Figma và lập gói bàn giao trước khi duyệt (mục 8, câu 1).

ADR liên quan: ADR-0001 (phụ thuộc module), ADR-0003 (cờ là `Flow`), ADR-0008 (hệ thống thiết kế), ADR-0013, ADR-0014 (vị trí mặc định tắt), ADR-0019 (log), ADR-0020 (ảnh thiết kế và screenshot cùng tên), ADR-0023 (tên khóa), ADR-0026 (khung AdsKit), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

**Hai lượt.** Danh sách chạy có hai dòng cùng ticket và plan này, ghi chú `lượt 1` và `lượt 2`; lượt 2 phụ thuộc lượt 1. Mỗi lượt đi đủ 10 bước của `docs/auto-run.md`, mục 3, với nhánh và PR riêng. Nhánh chính phải chạy đúng sau từng lượt.

- **Lượt 1, nhánh `claude/T-086-1-thu-vien-chi-tiet`**, từ nhánh chính có T-080 và T-085. Mục tiêu: vị trí, sự kiện ở chi tiết, staging, bảng khóa, `PremiumBadge`, `PremiumUnlockContent`, thư viện và chi tiết, chữ của hộp thoại khung, `manual-test.md` phần chi tiết.
- **Lượt 2, nhánh `claude/T-086-2-camera`**, từ nhánh chính sau lượt 1. Mục tiêu: dải tư thế camera, hộp thoại ở camera, tư thế dùng lần trước bị khóa, "Tư thế tiếp", `manual-test.md` phần camera.

**API của T-085 mà plan dùng (tên giả định, mục 8, câu 2).** Plan cần ba thứ của `:core:poses`:

```kotlin
// :core:poses (T-085). Names to match plan T-085.
data class Pose(/* ... */ val premium: Boolean = false /* ... */)

interface PremiumPoseRepository {
    /** Ids of premium poses not unlocked on this device; empty while FeatureFlag.PREMIUM_POSES is off. */
    val lockedIds: Flow<Set<String>>

    /** Unlocks [poseId] for good. Throws [java.io.IOException] when the list cannot be written. */
    suspend fun unlock(poseId: String)
}
```

Nếu T-085 chỉ cho luồng theo từng tư thế ("tư thế này có dùng được không"), plan T-085 cần thêm dạng tập hợp cho lưới và dải; không có thì đây là câu hỏi cho người duyệt T-085, không phải phần plan này tự làm. Plan này không đọc `FeatureFlag.PREMIUM_POSES` trực tiếp: cờ đã gộp trong `lockedIds`.

#### Lượt 1: vị trí, thư viện, chi tiết

**`AdPlacement.kt`.** Sau `INTER_POSE_DETAIL` (và dòng `POSES_IN_COLLECTION` của T-080), một dòng, chú thích nêu hành động (`docs/ops/ads-guide.md`, mục 5, bước 2):

```kotlin
    /** Holds the unlock of a premium pose, from the detail and the camera strip; no ad blocks (spec poses, KB-8, question 8). */
    RW_PREMIUM_POSE("rw_premium_pose", AdFormat.REWARDED),
```

Không khai báo `noAdPolicy` (câu 8). `scripts/release.sh` và `scripts/ads-plan.py` đọc dòng một-dòng và chú thích ngay trên, nên không sửa script.

**`RewardedAction.kt` (mục 8, câu 4).** Đổi `internal fun RewardedAdDialogCard(...)` thành công khai, thêm KDoc "for previews and screenshot tests; screens use [RewardedAdDialog]". Lý do: feature không dựng được hộp thoại thật trong test (không có `AdsController` thì `start()` chạy hành động ngay, và `RewardedAction` có hàm dựng `internal`), nên ảnh thiết kế của hộp thoại chỉ so được bằng thẻ không trạng thái này. Không đổi hành vi, không đổi ảnh chuẩn của khung.

**`:core:designsystem`.**

- `component/PremiumBadge.kt` (mới): `@Composable fun PremiumBadge(modifier: Modifier = Modifier)`, icon `ic_premium` trong vòng tròn, màu và cỡ theo `design/context/` (tạm: vòng 24 dp, nền `tertiaryContainer`, icon `onTertiaryContainer`, viền `outline` `Stroke.sticker`, như nét Sticker). `contentDescription` là `premium_badge_label`. `const val PREMIUM_BADGE_TAG = "pose_premium_badge"`.
- `PoseCard.kt`: tham số cuối `premium: Boolean = false`; `true` thì vẽ `PremiumBadge` ở góc trên trái của ảnh (nút lưu ở góc trên phải), lề `Spacing.sm`. `PoseCardSkeleton` không đổi. Mặc định `false` giữ mọi ảnh chuẩn cũ.
- `component/PremiumUnlockContent.kt` (mới): `@Composable fun PremiumUnlockContent(poseName: String, modifier: Modifier = Modifier)`: tên tư thế (`titleMedium`, `onSurface`) và `premium_unlock_message` (`bodyMedium`, `onSurfaceVariant`), cách nhau `Spacing.sm`. Dùng làm `content` của `RewardedAdDialog` ở chi tiết (lượt 1) và camera (lượt 2), để chữ chỉ dịch một lần.
- `res/drawable/ic_premium.xml` (mới): vector 24 dp, hình theo thiết kế (tạm: `workspace_premium` của Material Symbols, Apache 2.0 như các icon đã có).
- Chuỗi mới trong `values/strings.xml` và 6 thư mục ngôn ngữ (bảng ở cuối lượt 1).

**`:feature:poses`, dữ liệu.**

- `PosesLibrarySource.kt`: `LibraryContent` thêm `val lockedIds: Set<String> = emptySet()` (tham số cuối, trước `lists`, có mặc định để `testContent()` cũ không đổi); interface thêm `suspend fun unlock(poseId: String)` ("Throws [java.io.IOException]").
- `DefaultPosesLibrarySource.kt`: nhận thêm `PremiumPoseRepository`; `library` thành `combine(libraryRepository.library, premiumRepository.lockedIds)`, `Ready` mang `lockedIds` (giữ những id có trong catalog là đủ, không cần lọc); `unlock` chuyển thẳng xuống kho.

**`:feature:poses`, thư viện.** `PoseCardItem` thêm `val locked: Boolean = false`. `PosesViewModel.publish()` đặt `locked = id in content.lockedIds`. `PoseGridCell` truyền `premium = item.locked` cho `PoseCard`. Chạm thẻ vẫn mở chi tiết (câu 7). `PosesScreen.kt` không đổi (tag của dấu nằm ở `:core:designsystem`), nên lượt 1 không chạm file mà T-080 sửa nhiều nhất.

**`:feature:poses`, chi tiết.**

- `PoseDetailUiState.Ready` thêm `val locked: Boolean = false`, `val unlockFailed: Boolean = false`.
- `PoseDetailViewModel`:
  - `ready()` đặt `locked = pose.id in content.lockedIds`.
  - `val openCamera: Flow<String>` (một `Channel<String>(Channel.BUFFERED)` nhận qua `receiveAsFlow()`): mỗi phần tử là một lần mở camera sau khi mở khóa.
  - `fun onUnlockGranted()`:

    ```kotlin
    /** The rewarded action of the unlock button ran (KB-8): unlocks, logs, then opens the camera with the pose. */
    fun onUnlockGranted() {
        val ready = state.value as? PoseDetailUiState.Ready ?: return
        if (unlocking) return
        val pose = ready.pose
        if (!ready.locked) {
            // Already usable (flag off, or unlocked elsewhere): no write, no event (KB-8, criterion 8).
            openCamera.trySend(pose.id)
            return
        }
        unlocking = true
        viewModelScope.launch {
            try {
                librarySource.unlock(pose.id)
                analytics.log(
                    AnalyticsEvent(
                        EVENT_PREMIUM_POSE_UNLOCKED,
                        mapOf("pose_id" to pose.id, "category" to pose.categoryId, "source" to SOURCE_DETAIL),
                    ),
                )
                logger.info(LOG_TAG, "Premium pose unlocked")
                logSelected(pose)  // pose_selected, source detail (question 6)
                openCamera.send(pose.id)
            } catch (e: IOException) {
                unlockFailed = true
                logger.warn(LOG_TAG, "Premium pose could not be unlocked", e)
                publish()
            } finally {
                unlocking = false
            }
        }
    }
    ```

    `logSelected` là phần ghi `pose_selected` tách ra từ `onShoot()` (một hàm riêng, `onShoot` gọi lại nó; hành vi `onShoot` không đổi). `fun onUnlockFailedShown()` xóa cờ như `onSaveFailedShown`. `EVENT_PREMIUM_POSE_UNLOCKED = "premium_pose_unlocked"`, `SOURCE_DETAIL = "detail"` trong `companion object`. Log không có mã hay tên tư thế (như log hiện có của màn này).
- `PoseDetailScreen.kt`:
  - `PoseDetailRoute`:
    - `val unlock = rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, onGranted = viewModel::onUnlockGranted)`.
    - `LaunchedEffect(viewModel) { viewModel.openCamera.collect { currentOnShootWithPose(it) } }`: điều hướng chạy trong `LaunchedEffect` của composition, trên luồng chính (L-030). Mở khóa xong thì không qua `inter_pose_detail` (mục 8, câu 5).
    - `RewardedAdDialog(unlock, title = stringResource(DesignR.string.premium_unlock_title)) { PremiumUnlockContent(poseName = ...) }`, với tên tư thế lấy từ `uiState` (`Ready.pose.name.resolve(screenLocale())`), không có `Ready` thì chuỗi rỗng.
    - `PoseDetailScreen` nhận thêm `onUnlock = unlock::start`, `onUnlockFailedShown`.
  - `PoseDetailScreen` hiện snackbar `poses_unlock_failed` khi `unlockFailed`, như `saveFailed`.
  - `PoseDetailTestTags.UNLOCK = "pose_detail_unlock"`.
- `PoseDetailComponents.kt`: `PoseDetailBottomBar(locked: Boolean, onShoot, onUnlock)`: khóa thì `PrimaryButton(text = poses_detail_unlock, icon theo thiết kế, onClick = onUnlock, modifier = testTag(UNLOCK))`, không thì nút cũ. `PoseDetailMeta` có `PremiumBadge` đầu dòng phụ khi khóa (chỗ đặt theo thiết kế). `PhoneDetailLayout`, `WideDetailLayout` truyền tiếp `locked`, `onUnlock`.

**Chữ hộp thoại khung (`:app`).** Sáu tệp `app/src/main/res/values-<ngôn ngữ>/strings.xml` dịch ba chuỗi của `:core:ads` (`ads_reward_watch`, `ads_reward_no_thanks`, `ads_reward_check_connection`), theo `ads-guide.md` mục 5, dòng Rewarded: "app dịch trong `values-<ngôn ngữ>` của mình". Tài nguyên của `:app` đè tài nguyên cùng tên của thư viện. Lint chạy với `checkDependencies = true` báo `ExtraTranslation` khi `values/` của `:app` không có chuỗi gốc, và `MissingTranslation` cho `app_name` khi `:app` có thư mục ngôn ngữ (chạy thử lần 1, mục 4.1). Nên `app/src/main/res/values/strings.xml` thêm ba chuỗi với đúng chữ tiếng Anh của khung, và `app_name` thêm `translatable="false"` (tên app không dịch):

```xml
<resources>
    <string name="app_name" translatable="false">Camera Pose: Lines and Guides</string>

    <!--
        The rewarded dialog of :core:ads, the same English text as the framework: the app gives the default here so
        that its values-<language> files translate a string of its own (lint ExtraTranslation; plan T-086).
    -->
    <string name="ads_reward_watch">Watch ad</string>
    <string name="ads_reward_no_thanks">No, thanks</string>
    <string name="ads_reward_check_connection">Check your connection and try again</string>
</resources>
```

Đây là nội dung đúng của chạy thử lần 2 (mục 4.1). Hệ quả: khung đổi chữ tiếng Anh sau này thì app giữ chữ cũ tới khi sửa tệp này; `AdsStringsTranslationTest.english_keepsTheFrameworkText` so với chữ cố định nên sẽ báo. Tiếng Anh giữ "No, thanks" hay đổi "Not now" ở mục 8, câu 3.

| Tệp | `ads_reward_watch` | `ads_reward_no_thanks` | `ads_reward_check_connection` |
| --- | --- | --- | --- |
| `values-vi` | Xem quảng cáo | Để sau | Kiểm tra kết nối rồi thử lại |
| `values-hi` | विज्ञापन देखें | अभी नहीं | अपना कनेक्शन जांचें और फिर से कोशिश करें |
| `values-pt-rBR` | Assistir anúncio | Agora não | Verifique sua conexão e tente novamente |
| `values-b+es+419` | Ver anuncio | Ahora no | Revisa tu conexión y vuelve a intentarlo |
| `values-in` | Tonton iklan | Nanti saja | Periksa koneksi Anda dan coba lagi |
| `values-th` | ดูโฆษณา | ไว้ทีหลัง | ตรวจสอบการเชื่อมต่อแล้วลองอีกครั้ง |

Mỗi tệp có dòng chú thích `<!-- Buttons and message of the rewarded dialog of :core:ads (spec poses, KB-8; plan T-086). -->`.

**Chuỗi mới của app (chữ tạm, chốt theo bảng chữ của thiết kế; mục 8, câu 1).** Không chuỗi nào đi cùng con số.

| Module, tên | en (`values/`) | vi |
| --- | --- | --- |
| designsystem `premium_badge_label` | Premium pose | Tư thế cao cấp |
| designsystem `premium_unlock_title` | Unlock this pose | Mở khóa tư thế |
| designsystem `premium_unlock_message` | Watch a short ad to unlock this pose. It stays unlocked on this device. | Xem một quảng cáo ngắn để mở khóa tư thế này. Tư thế sẽ mở mãi trên máy này. |
| poses `poses_detail_unlock` | Watch an ad to use | Xem quảng cáo để dùng |
| poses `poses_unlock_failed` | Couldn\'t unlock the pose. Try again. | Chưa mở khóa được tư thế. Thử lại. |
| camera `camera_cd_thumb_premium` (lượt 2) | %1$s, premium, tap to unlock | %1$s, cao cấp, chạm để mở khóa |

Năm ngôn ngữ còn lại:

| Tên | hi | pt-BR | es-419 | in | th |
| --- | --- | --- | --- | --- | --- |
| `premium_badge_label` | प्रीमियम पोज़ | Pose premium | Pose premium | Pose premium | ท่าพรีเมียม |
| `premium_unlock_title` | यह पोज़ अनलॉक करें | Desbloquear esta pose | Desbloquear esta pose | Buka pose ini | ปลดล็อกท่านี้ |
| `premium_unlock_message` | इस पोज़ को अनलॉक करने के लिए एक छोटा विज्ञापन देखें। यह इस डिवाइस पर अनलॉक रहेगा। | Assista a um anúncio curto para desbloquear esta pose. Ela continua desbloqueada neste aparelho. | Mira un anuncio corto para desbloquear esta pose. Seguirá desbloqueada en este dispositivo. | Tonton iklan singkat untuk membuka pose ini. Pose tetap terbuka di perangkat ini. | ดูโฆษณาสั้นๆ เพื่อปลดล็อกท่านี้ ท่านี้จะปลดล็อกอยู่ในอุปกรณ์นี้ |
| `poses_detail_unlock` | इस्तेमाल के लिए विज्ञापन देखें | Assista a um anúncio para usar | Mira un anuncio para usar | Tonton iklan untuk memakai | ดูโฆษณาเพื่อใช้ |
| `poses_unlock_failed` | पोज़ अनलॉक नहीं हो सका। फिर से कोशिश करें। | Não foi possível desbloquear a pose. Tente novamente. | No se pudo desbloquear la pose. Vuelve a intentarlo. | Pose belum bisa dibuka. Coba lagi. | ปลดล็อกท่าไม่ได้ ลองอีกครั้ง |
| `camera_cd_thumb_premium` | %1$s, प्रीमियम, अनलॉक करने के लिए टैप करें | %1$s, premium, toque para desbloquear | %1$s, premium, toca para desbloquear | %1$s, premium, ketuk untuk membuka | %1$s, พรีเมียม, แตะเพื่อปลดล็อก |

**Bản staging.** `StagingRemoteValuesModule.appPlacementsShown()` thêm `AdPlacement.RW_PREMIUM_POSE`. `FeatureFlag.PREMIUM_POSES` ở `StagingFeatureFlagsModule` là việc của T-085.

**Bảng khóa.** `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Bảng chỉ thêm `is_show_rw_premium_pose` (`boolean`, `false`, `:core:ads`).

**Sau khi merge lượt 1 (việc của người, không nằm trong PR).** Chạy `python3 scripts/ads-plan.py --keep <file trên Drive>`; dòng `rw_premium_pose` có trạng thái `Đã code`.

#### Lượt 2: camera

- `PoseUiState.kt`: `PoseThumbItem` thêm `val locked: Boolean = false`; `PoseUiState.Ready` thêm `val unlockPrompt: UnlockPrompt? = null`, `data class UnlockPrompt(val id: String, val name: String)`: tư thế đang chờ mở, để màn hình vẽ tên trong hộp thoại.
- `PoseViewModel.kt`:
  - Nhận thêm `PremiumPoseRepository`.
  - `init` theo dõi `premiumRepository.lockedIds` (`collect { lockedIds = it; publish() }`).
  - `onLibraryReady` đọc `lockedIds = premiumRepository.lockedIds.first()` cùng chỗ đọc cài đặt nét viền, rồi chọn `requested?.takeUnless { it.id in lockedIds } ?: ready.lastUsedPose?.takeUnless { it.id in lockedIds } ?: ready.defaultPose`. Tư thế được yêu cầu (từ chi tiết) chỉ tới sau khi đã mở khóa, nên nhánh khóa của nó chỉ là phòng hờ: ghi `logger.warn(LOG_TAG, "Requested pose is locked")` và rơi về các lựa chọn sau. `onRequestedPose` (khóa được đặt sau khi mở) làm tương tự: khóa thì giữ tư thế hiện tại.
  - `onPoseSelect(id)`: tư thế khóa thì không chọn, trả về (phòng hờ; ô khóa gọi `onLockedPoseSelect`).
  - `fun onLockedPoseSelect(id: String)`: tư thế có trong catalog và đang khóa thì `savedStateHandle[KEY_UNLOCK_POSE_ID] = id`, `publish()` (để `unlockPrompt` có tên), log `"Locked pose tapped"`. Không chọn, không ghi `pose_selected`.
  - `fun onUnlockGranted()`: đọc `KEY_UNLOCK_POSE_ID`; không có thì thôi. Không khóa thì chọn như ô thường (`select(pose, Source.STRIP)`), không ghi, không sự kiện. Khóa thì `launch { unlock(id); log premium_pose_unlocked (source camera); xóa khóa chờ; select(pose, Source.STRIP) }`; `IOException` thì log warn, giữ tư thế cũ, không sự kiện (mục 8, câu 7). Một lượt ghi mỗi lúc (`unlocking`).
  - `onNextPose()`: bỏ qua tư thế khóa: tư thế không khóa đầu tiên sau tư thế đang chọn, vòng về đầu; không còn tư thế không khóa thì không làm gì (mục 8, câu 8).
  - `publish()`: `PoseThumbItem(..., locked = it.id in lockedIds)`; `unlockPrompt` từ `KEY_UNLOCK_POSE_ID` và tên tư thế.
  - `EVENT_PREMIUM_POSE_UNLOCKED = "premium_pose_unlocked"`, `SOURCE_CAMERA = "camera"`, `KEY_UNLOCK_POSE_ID = "unlockPoseId"`.
- `PosePicker.kt`: `PoseActions` thêm `val onLockedPoseSelect: (String) -> Unit = {}`. Trong `PoseStrip`, ô khóa có `onClick = { actions.onLockedPoseSelect(item.id) }`, `premium = true`, `contentDescription = camera_cd_thumb_premium` (ô lỗi tải ảnh vẫn dùng chuỗi lỗi; ô khóa mà ảnh lỗi: chạm vẫn mở hộp thoại, và tải lại ảnh như cũ).
- `PoseThumb.kt` (`:core:designsystem`): tham số cuối `premium: Boolean = false`; `true` thì vẽ `PremiumBadge` cỡ nhỏ ở góc trên trái ô (cỡ theo thiết kế; ô 64 dp, dấu chọn ✓ ở góc trên phải).
- `CameraScreen.kt`, `CameraRoute`:
  - `val unlock = rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, onGranted = poseViewModel::onUnlockGranted)`.
  - `PoseActions(onLockedPoseSelect = { id -> poseViewModel.onLockedPoseSelect(id); unlock.start() }, ...)`: `start()` sau khi ViewModel đã giữ tư thế, nên khi khóa tắt `onGranted` (chạy ngay) đọc đúng tư thế.
  - Trong `AppTheme(darkTheme = true)`: `RewardedAdDialog(unlock, title = premium_unlock_title) { PremiumUnlockContent(prompt?.name.orEmpty()) }`, với `prompt = (poseState as? PoseUiState.Ready)?.unlockPrompt`.
  - Đang đếm ngược thì dải bị khóa (`enabled = false`), nên không mở được hộp thoại lúc đếm (KB-6 giữ nguyên).
- Hộp thoại của khung giữ trạng thái `ASKING` qua dựng lại activity; tư thế chờ nằm trong `SavedStateHandle`, nên sau xoay màn hình `onGranted` vẫn mở đúng tư thế.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Lượt 1: `RW_PREMIUM_POSE` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/RewardedAction.kt` | Lượt 1: `RewardedAdDialogCard` công khai, KDoc (mục 8, câu 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Lượt 1: dòng `rw_premium_pose` trong tập vị trí của app; test `rwPremiumPose_blocksWithoutAd` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Lượt 1: thêm `rw_premium_pose=unit/13` vào hai chuỗi cấu hình (dữ liệu, ADR-0029; mục 3.5) |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PremiumBadge.kt` | Lượt 1: `PremiumBadge`, `PREMIUM_BADGE_TAG` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PremiumUnlockContent.kt` | Lượt 1: nội dung hộp thoại mở khóa |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseCard.kt` | Lượt 1: tham số `premium` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseThumb.kt` | Lượt 2: tham số `premium` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_premium.xml` | Lượt 1: icon của dấu |
| Sửa | `core/designsystem/src/main/res/values/strings.xml` | Lượt 1: `premium_badge_label`, `premium_unlock_title`, `premium_unlock_message` |
| Sửa | `core/designsystem/src/main/res/values-vi/strings.xml` | Lượt 1: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-hi/strings.xml` | Lượt 1: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-pt-rBR/strings.xml` | Lượt 1: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-b+es+419/strings.xml` | Lượt 1: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-in/strings.xml` | Lượt 1: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-th/strings.xml` | Lượt 1: ba chuỗi trên |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseCardTest.kt` | Lượt 1: test dấu trên thẻ |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseComponentsTest.kt` | Lượt 2: test dấu trên ô |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemStringsTest.kt` | Lượt 1: ba chuỗi mới vào danh sách dịch (dữ liệu, ADR-0029) |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/PosesLibrarySource.kt` | Lượt 1: `LibraryContent.lockedIds`, `unlock` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPosesLibrarySource.kt` | Lượt 1: gộp `lockedIds`, `unlock` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesUiState.kt` | Lượt 1: `PoseCardItem.locked` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesViewModel.kt` | Lượt 1: `locked` của thẻ |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesComponents.kt` | Lượt 1: `premium = item.locked` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailUiState.kt` | Lượt 1: `locked`, `unlockFailed` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailViewModel.kt` | Lượt 1: `onUnlockGranted`, `openCamera`, `onUnlockFailedShown`, `logSelected`, hằng sự kiện |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreen.kt` | Lượt 1: `rememberRewardedAction`, `RewardedAdDialog`, `openCamera`, snackbar, `UNLOCK` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailComponents.kt` | Lượt 1: nút mở khóa, dấu ở dòng phụ |
| Sửa | `feature/poses/src/main/res/values/strings.xml` | Lượt 1: `poses_detail_unlock`, `poses_unlock_failed` |
| Sửa | `feature/poses/src/main/res/values-vi/strings.xml` | Lượt 1: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-hi/strings.xml` | Lượt 1: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-pt-rBR/strings.xml` | Lượt 1: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-b+es+419/strings.xml` | Lượt 1: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-in/strings.xml` | Lượt 1: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-th/strings.xml` | Lượt 1: hai chuỗi trên |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesFakes.kt` | Lượt 1: `FakePosesLibrarySource.unlock` (ghi lại lời gọi, `failUnlock`, phát nội dung mới không còn id trong `lockedIds`); `testContent(lockedIds = ...)` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesViewModelTest.kt` | Lượt 1: test `locked` của thẻ |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Lượt 1: test dấu trên thẻ, chạm thẻ khóa mở chi tiết |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | Lượt 1: ảnh `premium` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesStringsTest.kt` | Lượt 1: hai chuỗi mới vào `TRANSLATED` (dữ liệu, ADR-0029) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailViewModelTest.kt` | Lượt 1: test mở khóa (mục 4) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Lượt 1: test nút mở khóa, đường khóa tắt, snackbar |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | Lượt 1: ảnh `premium` sáng, tối, `_small`; ảnh hộp thoại |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_premium_light.png` | Lượt 1: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_premium_dark.png` | Lượt 1: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_premium_light.png` | Lượt 1: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_premium_dark.png` | Lượt 1: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_premium_small.png` | Lượt 1: ảnh chuẩn mới, khung `w360dp-h640dp-xxhdpi` |
| Tạo | `feature/poses/src/test/screenshots/PoseUnlockDialog_default_light.png` | Lượt 1: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseUnlockDialog_default_dark.png` | Lượt 1: ảnh chuẩn mới |
| Tạo | `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt` | Lượt 1: kịch bản emulator (mục 4) |
| Tạo | `docs/features/poses/design/review/PosesScreen_premium_light_compare.png` | Lượt 1: ảnh so sánh của `scripts/design-compare.sh poses` |
| Tạo | `docs/features/poses/design/review/PosesScreen_premium_dark_compare.png` | Lượt 1: như trên |
| Tạo | `docs/features/poses/design/review/PoseDetailScreen_premium_light_compare.png` | Lượt 1: như trên |
| Tạo | `docs/features/poses/design/review/PoseDetailScreen_premium_dark_compare.png` | Lượt 1: như trên |
| Tạo | `docs/features/poses/design/review/PoseUnlockDialog_default_light_compare.png` | Lượt 1: như trên |
| Tạo | `docs/features/poses/design/review/PoseUnlockDialog_default_dark_compare.png` | Lượt 1: như trên |
| Sửa | `docs/features/poses/design/review/summary.md` | Lượt 1: sinh lại cùng lệnh |
| Sửa | `app/src/main/res/values/strings.xml` | Lượt 1: ba chuỗi gốc tiếng Anh của hộp thoại khung, `app_name` `translatable="false"` (3.1) |
| Tạo | `app/src/main/res/values-vi/strings.xml` | Lượt 1: ba chuỗi của hộp thoại khung (bảng ở 3.1) |
| Tạo | `app/src/main/res/values-hi/strings.xml` | Lượt 1: như trên |
| Tạo | `app/src/main/res/values-pt-rBR/strings.xml` | Lượt 1: như trên |
| Tạo | `app/src/main/res/values-b+es+419/strings.xml` | Lượt 1: như trên |
| Tạo | `app/src/main/res/values-in/strings.xml` | Lượt 1: như trên |
| Tạo | `app/src/main/res/values-th/strings.xml` | Lượt 1: như trên |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ads/AdsStringsTranslationTest.kt` | Lượt 1: test chữ hộp thoại khung (mục 4; bản đã chạy thử) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Lượt 1: `appPlacementsShown()` thêm `RW_PREMIUM_POSE` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Lượt 1: tập mong đợi thêm `is_show_rw_premium_pose=true` (mục 3.5) |
| Sửa | `docs/remote-config-keys.md` | Lượt 1: sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Lượt 1: sinh lại cùng lệnh |
| Sửa | `docs/ops/ads-guide.md` | Lượt 1: dòng Rewarded của bảng định dạng ghi vị trí của app `rw_premium_pose` (T-086); `RewardedAdDialogCard` cho ảnh chuẩn; chữ hộp thoại dịch ở `:app` |
| Sửa | `docs/ops/manual-test.md` | Lượt 1: mục 11 thêm `is_show_rw_premium_pose` vào danh sách khóa không được `false`, mục 11.5 mở khóa ở chi tiết. Lượt 2: bước mở khóa ở camera |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseUiState.kt` | Lượt 2: `PoseThumbItem.locked`, `UnlockPrompt` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModel.kt` | Lượt 2: `lockedIds`, `onLockedPoseSelect`, `onUnlockGranted`, tư thế đầu khi khóa, `onNextPose` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PosePicker.kt` | Lượt 2: `onLockedPoseSelect`, `premium` của ô |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Lượt 2: `rememberRewardedAction`, `RewardedAdDialog` trong `CameraRoute` |
| Sửa | `feature/camera/src/main/res/values/strings.xml` | Lượt 2: `camera_cd_thumb_premium` |
| Sửa | `feature/camera/src/main/res/values-vi/strings.xml` | Lượt 2: như trên |
| Sửa | `feature/camera/src/main/res/values-hi/strings.xml` | Lượt 2: như trên |
| Sửa | `feature/camera/src/main/res/values-pt-rBR/strings.xml` | Lượt 2: như trên |
| Sửa | `feature/camera/src/main/res/values-b+es+419/strings.xml` | Lượt 2: như trên |
| Sửa | `feature/camera/src/main/res/values-in/strings.xml` | Lượt 2: như trên |
| Sửa | `feature/camera/src/main/res/values-th/strings.xml` | Lượt 2: như trên |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseFakes.kt` | Lượt 2: `FakePremiumPoseRepository` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModelTest.kt` | Lượt 2: hàm dựng `viewModel()` nhận kho giả (dựng test, mục 3.5); test mới (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenPoseTest.kt` | Lượt 2: test ô khóa |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | Lượt 2: ảnh `premium` |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_premium_dark.png` | Lượt 2: ảnh chuẩn mới |
| Tạo | `docs/features/camera/design/review/CameraScreen_premium_dark_compare.png` | Lượt 2: ảnh so sánh của `scripts/design-compare.sh camera` |
| Sửa | `docs/features/camera/design/review/summary.md` | Lượt 2: sinh lại cùng lệnh |

Nơi khác đọc các file bị đổi (L-020 của mẫu):

- `scripts/ads-plan.py` đọc `AdPlacement.kt` (dòng một-dòng và chú thích ngay trên), `docs/remote-config-keys.md` và các hằng `EVENT_`: dòng mới và `EVENT_PREMIUM_POSE_UNLOCKED` tự vào file Excel, không sửa script. Test của script đọc code thật, không đếm số vị trí cố định.
- `scripts/release.sh` đọc dòng một-dòng của `AdPlacement`: dòng mới là một dòng, không có tiền tố `gallery_`, nên production cần mã `rw_premium_pose` (đúng ý ticket, T-075). `AdPlacementSourceTest` kiểm luật một-dòng: qua.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so `docs/remote-config-keys.md` và `docs/remote-config-template.json` với khóa của code; tập khóa lấy từ `AdsSettings.all()` và `AdPlacement.entries`, không ghi tên vị trí, nên không sửa test; hai tệp được sinh lại trong cùng commit code.
- `app/build.gradle.kts` chỉ nêu đường dẫn của `docs/remote-config-keys.md` và `docs/remote-config-template.json` cho tham số `-PremoteConfigKeys.update`; đường dẫn không đổi, nên không sửa.
- `.github/workflows/device-session.yml` đọc `summary.md` của thư mục phiên thử, không đọc `docs/features/camera/design/review/summary.md` hay `docs/features/poses/design/review/summary.md` của gói thiết kế, nên không sửa.
- `RewardedActionTest` gọi `RewardedAdDialogCard` trong cùng module: đổi `internal` thành công khai không ảnh hưởng.
- `PoseCard`, `PoseThumb` có tham số mới cuối, mặc định `false`: mọi chỗ gọi cũ và ảnh chuẩn của `:core:designsystem`, `:feature:poses`, `:feature:camera` không đổi.
- `LibraryContent` có tham số mới có mặc định: `testContent()` và mọi chỗ dựng cũ biên dịch được.
- Tài nguyên của `:app` đè ba chuỗi của `:core:ads` cho cả bản build: không module nào khác đọc tên đó ngoài `:core:ads`. `RewardedAdDialogScreenshotTest` của `:core:ads` dựng trong module của nó, không thấy tài nguyên của `:app`, nên ảnh chuẩn của khung giữ tiếng Anh.
- `docs/features/poses/design/review/summary.md`, `docs/features/camera/design/review/summary.md`: `scripts/design-compare/DesignCompare.java` ghi các tệp này; plan không sửa script, chỉ chạy nó. `scripts/device-session.sh` và `scripts/tests/run.sh` (khối `device-session`) đọc một `summary.md` khác, của thư mục phiên thử `test-sessions/`; khối `design-compare` của `scripts/tests/run.sh` đọc `summary.md` trong thư mục tạm của chính test. Không tệp nào đọc `summary.md` của gói thiết kế, nên không sửa.
- `docs/remote-config-keys.md`: `scripts/tests/run.sh` (khối `ads-plan`) chạy `scripts/ads-plan.py` trên repo giả do test dựng, không đọc bảng khóa thật của app, nên dòng mới không ảnh hưởng.
- `scripts/check.sh`, `scripts/design-compare.sh` đọc `src/test/screenshots/` và `design/screens/`: sau lượt 1 mọi ảnh thiết kế premium của poses có screenshot cùng tên; sau lượt 2 của camera.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: `FeatureFlag.PREMIUM_POSES` (`feature_premium_poses_enabled`, mặc định tắt) do T-085 khai báo và gộp vào `lockedIds`. Plan này không thêm cờ, không đọc cờ trực tiếp.
- Quảng cáo: thêm `rw_premium_pose` (rewarded; spec ads câu 12; spec poses mục 5, KB-8), mặc định tắt (ADR-0014), bật ở staging. Khóa `is_show_rw_premium_pose`, sinh từ `AdPlacement`. Không khai báo `noAdPolicy`: chặn (câu 8). Không qua `ads_interval`, không đẩy mốc (khung, R-05).
- Sự kiện phân tích: thêm `premium_pose_unlocked` (`pose_id`, `category`, `source`: `detail`, `camera`), đúng spec poses mục 6, câu 11. Sự kiện của khung (`reward_dialog`, `ad_reward_earned`, `ad_opportunity`...) tự ghi cho vị trí mới.
- File R2: Không chạm. Catalog có `premium` là của T-085 và người làm nội dung.
- **Quyền truy cập:** Không thêm. Không thêm: `CAMERA` (màn camera đã xin quyền này từ trước, plan không đổi gì ở phần quyền).
- **Thư viện và SDK:** Không thêm. `rememberRewardedAction`, `RewardedAdDialog` thuộc `:core:ads`, đã trên classpath biên dịch của `:feature:poses` (chạy thử biên dịch và chạy qua, mục 4.1) và `:feature:camera` (`ReviewScreen.kt` đã dùng `:core:ads`). `Channel` thuộc `kotlinx-coroutines-core`, đã có.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** tài nguyên `values-<ngôn ngữ>` của `:app` chỉ có ba chuỗi của `:core:ads` (không có `app_name`), mà lint chạy với `checkDependencies = true`: `MissingTranslation` hay `ExtraTranslation` có thể báo lỗi.
  Chạy thử lần 1 đã gặp đúng hai lỗi này; cách chính của mục 3.1 (chuỗi gốc ở `values/` của `:app`, `app_name` không dịch) là cách sửa đã chạy thử lần 2.
- **Điều kiện chuyển:** trên nhánh có code, `:app:lintStagingDebug` vẫn báo `ExtraTranslation` hay `MissingTranslation` ở các tệp này sau cách của mục 3.1.
- **Cách làm thay:** thêm `tools:ignore="ExtraTranslation"` vào thẻ `<resources>` của sáu tệp ngôn ngữ (dòng chú thích nêu lý do: chuỗi của thư viện), không tắt quy tắc lint ở cấu hình chung. Test `AdsStringsTranslationTest` giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` chạy ngày 2026-10-31 trên plan này (bỏ kết quả trong `.claude/worktrees/`). Mọi hành vi cũ giữ khi tư thế không khóa (kho giả trả tập rỗng, như cờ tắt), nên câu khẳng định cũ giữ nguyên; các dòng dưới chỉ đổi phần dựng test hay dữ liệu.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Tập vị trí của app có bốn dòng (năm sau T-080) | Thêm `rw_premium_pose` | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` | Giữ câu khẳng định; thêm `"rw_premium_pose" to AdFormat.REWARDED` vào bản đồ mong đợi (dữ liệu, ADR-0029) |
| Mọi vị trí không phải `gallery_` có mã đơn vị trong chuỗi cấu hình thì `missing` rỗng | Vị trí mới cần mã | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` | Giữ; thêm `rw_premium_pose=unit/13` vào hai chuỗi (dữ liệu) |
| Bản staging bật bốn (năm sau T-080) vị trí của app | Thêm `is_show_rw_premium_pose` | `StagingRemoteValuesModuleTest` (tập `expected`) | Giữ; thêm `"is_show_rw_premium_pose" to true` (dữ liệu) |
| Danh sách chuỗi đã dịch của poses, designsystem | Thêm chuỗi mới | `PosesStringsTest.everyReleaseLanguage_isTranslated` (`TRANSLATED`), `DesignSystemStringsTest` (danh sách) | Giữ; thêm tên chuỗi mới (dữ liệu). `CameraStringsTest` đọc mọi chuỗi bằng phản chiếu, không sửa |
| Chi tiết: nút chính luôn "Chụp với tư thế này" | Khóa thì nút mở khóa | `PoseDetailScreenTest` (nút chụp, `onShoot`), ảnh `PoseDetailScreen_*` | Giữ: dữ liệu cũ không khóa (`locked = false` mặc định) |
| `onShoot()` ghi `pose_selected` | `onShoot()` gọi `logSelected`, cùng sự kiện | `PoseDetailViewModelTest` (sự kiện `pose_selected`) | Giữ nguyên câu khẳng định |
| Camera: mở lần sau dùng tư thế dùng lần trước | Tư thế đó khóa thì dùng tư thế mặc định | `PoseViewModelTest.lastUsedPose_isSelectedOnOpen_withOutline`, `lastUsedMissingFromCatalog_fallsBackToDefault` | Giữ: kho giả mặc định không khóa gì |
| Camera: chạm ô thì chọn tư thế | Ô khóa gọi `onLockedPoseSelect`, không chọn | `PoseViewModelTest.selectFromStrip_logsEvent_marksUsed_resetsStep`, `CameraScreenPoseTest` (chạm ô) | Giữ: ô không khóa |
| Camera: "Tư thế tiếp" theo dải, vòng về đầu | Bỏ qua tư thế khóa | `PoseViewModelTest.nextPose_followsStrip_andWraps` | Giữ: không tư thế nào khóa |
| Camera: `PoseViewModel` có 10 tham số | Thêm `PremiumPoseRepository` | Hàm `viewModel()` của `PoseViewModelTest` | Dựng test: truyền `FakePremiumPoseRepository()` (ADR-0033/L11, không đổi câu khẳng định) |
| `PosesLibrarySource` có ba thành viên | Thêm `unlock` | `FakePosesLibrarySource` (`PosesFakes.kt`) | Dựng test: cài `unlock` (dữ liệu giả), không đổi câu khẳng định |
| Cỡ thẻ, ô 64 dp, vị trí nút chính và dải (nhóm "Cần xem trước" của `existing-tests.py`) | Dấu vẽ bên trong vùng ảnh của thẻ và ô, không đổi cỡ; tham số mặc định `false` | `PoseCardTest.skeleton_sameSizeAsOneLineCard`, `PoseCardTest.saved_buttonFillIsSecondaryContainer_unsavedIsSurface`, `PoseComponentsTest.poseThumb_is64dp`, `PoseComponentsTest.selectedBadge_is22dpYellow`, `PoseDetailScreenTest.realBottomBar_withoutAds_takesNoSpace`, `PoseDetailScreenTest.wide_imageFailed_retryInLeftColumn`, `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace`, `CameraScreenPoseTest.small_noPose_hintInsidePreview`, `CameraScreenOverlayTest.outline_largestAndMovedDown_notDrawnOverPickerOrCaptureRow`, `CameraScreenMatchTest.small_matchHintInsidePreview_clearOfStepCardAndZoom` | Giữ nguyên: dữ liệu không khóa. Test mới `PoseDetailScreenTest.locked_small_unlockButtonFullyVisible` kiểm nút mở khóa ở khung thấp |
| `RewardedAdDialogCard` là `internal`; test của khung đo nút, màu chữ | Công khai, không đổi bố cục | `RewardedActionTest.watchButton_is56dpHigh_fullCardWidth`, `noThanksText_usesOnSurfaceVariant_light_and_dark`, `longContent_small_buttonsInsideWindow` | Giữ nguyên |

## 4. Kiểm thử

Test viết trước code (bước 3). Khung rỗng của bước 3: `RW_PREMIUM_POSE` có dòng enum (test dữ liệu của `AdPlacementAppTest`, `AdUnitIdsTest` nhắc hằng enum nên đi cùng commit có dòng enum, như T-080), tham số `locked`, `premium`, `onUnlockGranted`, `onLockedPoseSelect`, `openCamera` có mà chưa làm gì (`premium` không vẽ gì, `onUnlockGranted` rỗng, `lockedIds` không được đọc), để test biên dịch được rồi đỏ ở câu khẳng định.

**Lượt 1**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Spec ads câu 12; KB-8 tiêu chí 6 (chặn) | Unit test | `AdPlacementAppTest.rwPremiumPose_blocksWithoutAd`: `RW_PREMIUM_POSE.key == "rw_premium_pose"`, `format == REWARDED`, `noAdPolicy == null`, `declaration.effectiveNoAdPolicy == RewardedNoAdPolicy.Block`, không `stagingOnly`, không `appWide`, `placementEnabled(...).name == "is_show_rw_premium_pose"`, mặc định `false`. Khung rỗng đã có dòng enum nên test này qua ngay: phép đột biến là thêm `noAdPolicy = RewardedNoAdPolicy.Allow` vào dòng thì test đỏ |
| KB-8 tiêu chí 1, thư viện | Unit test | `PosesViewModelTest.lockedPose_cardIsLocked_othersNot`: `testContent(lockedIds = setOf("w03"))` thì thẻ `w03` có `locked = true`, thẻ khác `false`. Đối chứng cùng test: phát lại nội dung với `lockedIds` rỗng thì `w03` thành `false` |
| KB-8 tiêu chí 1, thư viện | Compose UI test | `PosesScreenTest.lockedCard_showsPremiumBadge_andOpensDetail`: thẻ khóa có node `PREMIUM_BADGE_TAG` trong cây con của `poses_card_w03`, thẻ khác không; chạm thẻ khóa gọi `onOpenPose("w03")` |
| KB-8 tiêu chí 1, thành phần | Compose UI test | `PoseCardTest.premium_drawsBadge_withDescription`: `premium = true` có node `PREMIUM_BADGE_TAG` với `contentDescription` `premium_badge_label`; `premium = false` không có node |
| KB-8 tiêu chí 1, 2, chi tiết | Unit test | `PoseDetailViewModelTest.lockedPose_isLocked_andSaveStillWorks`: `lockedIds = setOf(poseId)` thì `Ready.locked`; `onToggleSave()` vẫn ghi `setSaved` và `pose_saved` như tư thế thường |
| KB-8 tiêu chí 2 | Compose UI test | `PoseDetailScreenTest.locked_mainButtonIsUnlock_callsOnUnlock`: `locked = true` thì có node `pose_detail_unlock` với chữ `poses_detail_unlock`, không có chữ `poses_detail_shoot`; chạm gọi `onUnlock`, không gọi `onShoot`; có node `PREMIUM_BADGE_TAG`. Đối chứng: `locked = false` thì nút chụp như cũ, không có dấu |
| KB-8 tiêu chí 4, chi tiết | Unit test | `PoseDetailViewModelTest.unlockGranted_unlocks_logsEvent_thenOpensCamera`: tư thế khóa, `onUnlockGranted()` thì `FakePosesLibrarySource.unlocks == listOf(poseId)`, sự kiện `premium_pose_unlocked` với `pose_id`, `category`, `source = "detail"`, rồi `pose_selected` `source = "detail"`, rồi `openCamera` phát đúng `poseId` một lần; sự kiện mở khóa ghi sau khi `unlock` xong (cổng `CompletableDeferred` của kho giả: trước khi mở cổng chưa có sự kiện, chưa có `openCamera`) |
| KB-8 tiêu chí 4 (một lần) | Unit test | `PoseDetailViewModelTest.unlockGranted_twiceWhileWriting_writesOnce`: hai lần gọi khi lượt ghi đầu đang chờ cổng thì một lần `unlock`, một sự kiện, một lần `openCamera` |
| KB-8 tiêu chí 4, sau khi mở | Unit test | `PoseDetailViewModelTest.afterUnlock_libraryUpdate_clearsLocked`: kho giả phát nội dung không còn id trong `lockedIds` sau khi ghi thì `Ready.locked` thành `false` |
| KB-8 tiêu chí 5 (không thưởng thì không làm gì) | Unit test | `PoseDetailViewModelTest.noGrant_nothingWritten_noNavigation`: mở chi tiết tư thế khóa, không gọi `onUnlockGranted`: không `unlock`, không sự kiện `premium_pose_unlocked`, `openCamera` không phát (đọc bằng `backgroundScope` với `toList`). Khung giữ phần "đóng sớm, Để sau, không có quảng cáo thì không gọi `onGranted`" (`RewardedActionTest`, đã có) |
| KB-8 tiêu chí 8 (cờ tắt) | Unit test | `PoseDetailViewModelTest.notLocked_grant_opensCamera_withoutWriteOrEvent`: `lockedIds` rỗng, `onUnlockGranted()` thì `openCamera` phát `poseId`, không `unlock`, không `premium_pose_unlocked`. Đối chứng là test được thưởng ở trên (tư thế khóa thì có ghi) |
| KB-8 tiêu chí 9 (khóa tắt hay chưa đồng ý: mở ngay) | Compose UI test | `PoseDetailRouteTest.noAdsController_unlockButton_unlocksAtOnce_withoutDialog` (lớp mới trong `PoseDetailScreenTest.kt`): dựng `PoseDetailRoute` với ViewModel thật và kho giả (tư thế khóa), không có `LocalAdsController` (như khóa tắt hay cổng đóng: `start()` gọi `onGranted` ngay, đã chạy thử ở mục 4.1); chạm `pose_detail_unlock` thì `unlocks == listOf(poseId)`, `onShootWithPose(poseId)` được gọi một lần, không có node `RewardedAdDialogTestTags.CARD` |
| Ghi lỗi (CLAUDE.md, bước 4) | Unit test | `PoseDetailViewModelTest.unlockWriteFails_staysLocked_showsMessage_noEvent`: `failUnlock` thì `Ready.unlockFailed`, `locked` vẫn `true`, không sự kiện, `openCamera` không phát, `logger` có một dòng `warn`; `onUnlockFailedShown()` xóa cờ; gọi lại `onUnlockGranted()` sau khi hết lỗi thì ghi được (người dùng thử lại) |
| Ghi lỗi, màn hình | Compose UI test | `PoseDetailScreenTest.unlockFailed_showsSnackbar_once`: `unlockFailed = true` thì snackbar `poses_unlock_failed` hiện, rồi `onUnlockFailedShown` được gọi |
| KB-8 tiêu chí 3, nội dung hộp thoại | Compose UI test | `PoseDetailScreenTest.unlockContent_showsNameAndMessage`: `PremiumUnlockContent("Tựa lan can")` có chữ tên và chữ `premium_unlock_message` |
| Chữ hai nút và thông báo của khung (KB-8 tiêu chí 3, 6) | Unit test (Robolectric) | `AdsStringsTranslationTest` (`:app`): `vi` cho "Xem quảng cáo", "Để sau"; `en` giữ "Watch ad", "No, thanks"; sáu ngôn ngữ phát hành khác tiếng Anh cho cả ba chuỗi. Bản đã chạy thử (mục 4.1): bỏ `values-vi` thì hai test đỏ đúng lý do |
| Bản staging | Unit test | `StagingRemoteValuesModuleTest`: tập mong đợi có `is_show_rw_premium_pose=true` (đỏ trước khi thêm vào `appPlacementsShown()`) |
| Bảng khóa | Unit test | `RemoteConfigKeysTest` (có sẵn): đỏ khi thêm dòng enum mà chưa sinh lại; qua sau khi sinh lại |
| Giao diện thư viện có tư thế khóa | Screenshot test | `PosesScreenScreenshotTest`: `PosesScreen_premium_light.png`, `PosesScreen_premium_dark.png`, khung `w411dp-h891dp-xxhdpi`, dữ liệu theo `design/context/` |
| Giao diện chi tiết khi khóa | Screenshot test | `PoseDetailScreenScreenshotTest`: `PoseDetailScreen_premium_light.png`, `PoseDetailScreen_premium_dark.png` |
| Màn hình thấp (L-028; ticket: ảnh `_small` có nút chính khi còn khóa) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `PoseDetailScreen_premium_small.png`: tư thế khóa có 5 bước và khối lỗi hay gặp; kèm test `PoseDetailScreenTest.locked_small_unlockButtonFullyVisible` (`@Config(qualifiers = "w360dp-h640dp-xxhdpi")`): `boundsInRoot` của `pose_detail_unlock` nằm trọn trong cửa sổ, nội dung cuộn được |
| Hộp thoại mở khóa | Screenshot test | `PoseDetailScreenScreenshotTest`: `PoseUnlockDialog_default_light.png`, `PoseUnlockDialog_default_dark.png`: `RewardedAdDialogCard(title = premium_unlock_title, onWatch = {}, onNoThanks = {}) { PremiumUnlockContent(...) }` (mục 8, câu 4), khung `w411dp-h891dp-xxhdpi` |
| Đúng thiết kế (khi có `design/screens/`) | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh poses`, rà soát độc lập theo mục 7 của mẫu thiết kế | `PosesScreen_premium_light`, `PosesScreen_premium_dark`, `PoseDetailScreen_premium_light`, `PoseDetailScreen_premium_dark`, `PoseUnlockDialog_default_light`, `PoseUnlockDialog_default_dark` |
| Ảnh thiết kế đã có, không đổi | Screenshot test có sẵn | Ảnh thiết kế của ticket khác (T-021, T-022, T-024, T-079, T-080): `PoseDetailScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_default_dark`, `PoseDetailScreen_default_dark_unfolded`, `PoseDetailScreen_default_light`, `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_error_dark`, `PoseDetailScreen_error_light`, `PoseDetailScreen_loading_dark`, `PoseDetailScreen_loading_light`, `PosesScreen_ad-collection_light`, `PosesScreen_ad_dark`, `PosesScreen_ad_light`, `PosesScreen_default_dark`, `PosesScreen_default_dark_unfolded`, `PosesScreen_default_light`, `PosesScreen_default_light_unfolded`, `PosesScreen_error_dark`, `PosesScreen_error_light`, `PosesScreen_loading_dark`, `PosesScreen_loading_light`, `PosesScreen_saved-empty_dark`, `PosesScreen_saved-empty_light` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `PoseDetailScreenTest.locked_mainButtonIsUnlock_callsOnUnlock` và `unlockContent_showsNameAndMessage` đọc chữ qua `stringResource`; giá trị tiếng Việt của `strings.xml` so với bảng chữ của gói bàn giao khi rà soát |
| KB-8 tiêu chí 2, 3, 5, 9 trên máy | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt`) | Tệp kịch bản dưới; `device-session` cuối phiên chạy tệp này. Catalog staging chưa có tư thế premium (nội dung sau T-085) thì tệp `bỏ qua (môi trường)` ở bước `need` |
| KB-8 tiêu chí 4, 5, 6 với quảng cáo thử thật | Kiểm tra thủ công (máy thật: quảng cáo thật) | `manual-test.md` mục 11.5: bản staging, đã đồng ý; mở chi tiết tư thế premium, bấm nút mở khóa, "Xem quảng cáo", xem quảng cáo thử có thưởng tới hết (không bấm vào quảng cáo): camera mở với tư thế đó, dấu mất ở thư viện; tư thế khác: đóng quảng cáo sớm thì vẫn khóa; "Để sau" thì vẫn khóa; chế độ máy bay rồi "Xem quảng cáo" thì vẫn khóa, có thông báo kiểm tra kết nối (tiếng Việt khi máy để tiếng Việt) |
| KB-8 tiêu chí 7 (giữ qua lần mở sau) | Kiểm tra thủ công (máy thật: khác, cần đóng hẳn app và mở lại trên bản staging thật cùng catalog R2 của T-085) | Sau bước trên, vuốt app khỏi danh sách gần đây, mở lại: tư thế vẫn không có dấu ở thư viện, chi tiết, camera |
| Sự kiện `premium_pose_unlocked` tới Firebase | Kiểm tra thủ công (máy thật: Remote Config) | Bản staging không gửi số liệu (ADR-0012); kiểm bằng log `VGL.Analytics` hay DebugView khi có bản dùng Firebase thật; ghi kết quả vào nhật ký |
| Gói thiết kế đủ frame (mục 8, câu 1) | Kiểm tra thủ công (người: duyệt thiết kế) | Nhật duyệt các frame và bảng chữ trước khi duyệt plan |

Tệp kịch bản `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt` (lượt 1; `scripts/device-scenario.py check` chạy ở bước 3 của task, vì `pose_detail_unlock` và `pose_premium_badge` chưa có trong code lúc viết plan):

```text
ticket: T-086
criterion: Tư thế premium chưa mở khóa mở được chi tiết, nút chính là nút mở khóa; bấm nút thì hộp thoại mở khóa hiện, hay tư thế mở ngay khi cổng đồng ý đóng; "Để sau" giữ khóa (spec poses KB-8, tiêu chí 2, 3, 5, 9)
timeout: 240
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
need id=camera_poses 15
tap id=camera_poses
wait id=poses_grid 20
need id=pose_premium_badge in=poses_grid 20
tap id=pose_premium_badge in=poses_grid index=1
wait id=pose_detail_screen 15
expect id=pose_detail_unlock
shot locked
tap id=pose_detail_unlock
wait str=ads_reward_watch | id=camera_screen 20
shot after_unlock_tap
tap_if str=ads_reward_no_thanks
wait id=pose_detail_unlock | id=camera_screen 15
```

Chạm vào dấu (không chạm được) rơi vào thẻ bọc nó, nên mở chi tiết. `fresh` đặt ngôn ngữ app là tiếng Anh, nên `str=ads_reward_watch` khớp "Watch ad". Bộ chọn dùng chữ, không dùng `RewardedAdDialogTestTags.CARD`, vì hộp thoại là cửa sổ riêng và tag của nó có thể không thành `resource-id`.

**Lượt 2**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Camera KB-1 tiêu chí 8, dấu trên ô | Unit test | `PoseViewModelTest.lockedPose_cellIsLocked_andUpdatesWhenUnlockedElsewhere`: kho giả `lockedIds = setOf("c01")` thì ô `c01` `locked`; kho phát tập rỗng (mở ở chi tiết) thì ô thành không khóa, dải giữ thứ tự |
| Camera KB-1 tiêu chí 8, chạm ô khóa | Unit test | `PoseViewModelTest.lockedPoseTap_keepsSelection_noEvent_setsPrompt`: tư thế đang chọn `w01`, `onLockedPoseSelect("c01")` thì `selected.id == "w01"`, không `pose_selected`, không `markUsed`, `unlockPrompt == UnlockPrompt("c01", <tên>)`; `onPoseSelect("c01")` cũng không đổi tư thế (phòng hờ) |
| Camera KB-1 tiêu chí 8, mở khóa xong | Unit test | `PoseViewModelTest.unlockGranted_unlocks_logsCameraEvent_thenSelects`: sau `onLockedPoseSelect("c01")`, `onUnlockGranted()` thì `unlocks == listOf("c01")`, `premium_pose_unlocked` với `source = "camera"`, rồi `selected.id == "c01"`, `pose_selected` `source = "strip"`, `markUsed("c01")`, `unlockPrompt == null` |
| Camera KB-1 tiêu chí 8, ghi lỗi | Unit test | `PoseViewModelTest.unlockWriteFails_keepsPose_noEvent`: `failUnlock` thì tư thế cũ giữ, không sự kiện, một dòng `warn`; ô vẫn khóa |
| Camera, cờ tắt | Unit test | `PoseViewModelTest.notLocked_grant_selectsWithoutWriteOrEvent`: `lockedIds` rỗng, `onLockedPoseSelect` rồi `onUnlockGranted()` thì chọn tư thế, không `unlock`, không `premium_pose_unlocked` |
| Camera, dựng lại | Unit test | `PoseViewModelTest.pendingUnlock_survivesRecreation`: `onLockedPoseSelect("c01")` rồi dựng ViewModel mới với cùng `SavedStateHandle`: `onUnlockGranted()` mở đúng `c01` |
| Camera KB-1 tiêu chí 9 | Unit test | `PoseViewModelTest.lastUsedLocked_fallsBackToDefault`: lịch sử dùng gần nhất `c01`, `lockedIds = setOf("c01")` thì mở với `defaultPose` (`w01`), không sự kiện. Đối chứng cùng lớp: `lastUsedPose_isSelectedOnOpen_withOutline` (có sẵn) với `c01` không khóa thì mở `c01` |
| Camera, tư thế được yêu cầu bị khóa (phòng hờ) | Unit test | `PoseViewModelTest.requestedLocked_isIgnored_andLogsWarning`: `KEY_POSE_ID = "c01"` khóa thì không chọn `c01`, có một dòng `warn` |
| "Tư thế tiếp" (mục 8, câu 8) | Unit test | `PoseViewModelTest.nextPose_skipsLockedPoses`: dải `w01, w02, w03, ...`, `w02` khóa, đang ở `w01` thì `onNextPose()` chọn `w03`; mọi tư thế khác khóa thì không đổi |
| Camera KB-1 tiêu chí 8, màn hình | Compose UI test | `CameraScreenPoseTest.lockedCell_showsBadge_tapCallsLockedSelect`: ô khóa có `PREMIUM_BADGE_TAG` trong cây con của `camera_pose_c01`, `contentDescription` là `camera_cd_thumb_premium`; chạm gọi `onLockedPoseSelect("c01")`, không gọi `onPoseSelect` |
| Thành phần ô | Compose UI test | `PoseComponentsTest.thumb_premium_drawsBadge`: `PoseThumb(premium = true)` có node dấu; `false` không có |
| Camera KB-1 tiêu chí 8, khóa tắt hay chưa đồng ý | Compose UI test | `CameraScreenPoseTest.noAdsController_lockedCellTap_unlocksAndSelectsAtOnce`: dựng `CameraRoute` với các ViewModel giả như `CameraScreenPoseTest` đang làm (hay `PoseViewModel` thật và kho giả nếu lớp test đã dựng như vậy; phiên chọn theo cách lớp đang dựng): chạm ô khóa thì `unlocks == listOf("c01")` và ô `c01` thành ô đang chọn, không có node `RewardedAdDialogTestTags.CARD` |
| Giao diện dải có ô khóa | Screenshot test | `CameraScreenScreenshotTest`: `CameraScreen_premium_dark.png` |
| Đúng thiết kế (khi có `design/screens/`) | Screenshot test cùng tên, `scripts/design-compare.sh camera` | `CameraScreen_premium_dark` |
| Camera KB-1 tiêu chí 8 với quảng cáo thử | Kiểm tra thủ công (máy thật: quảng cáo thật) | `manual-test.md` mục 11.5, bước camera: chạm ô có dấu ở dải, hộp thoại hiện, tư thế đang dùng không đổi; "Xem quảng cáo", xem hết: camera dùng tư thế đó; tư thế khác: "Để sau" thì không đổi |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085); `scripts/plan-check.py` báo lỗi dòng thiếu khi plan còn `draft` (quy tắc 11).

**Ngoại lệ viết test trước:**

- Ba tệp bảng khóa sinh lại, ảnh chuẩn và ảnh so sánh mới: sinh ra từ code, không viết trước được.
- Tệp kịch bản `T-086-mo-khoa-chi-tiet.txt`: chạy ở `device-session` cuối phiên, không "thất bại trước" được.
- Phần nối ở `CameraRoute`: `onLockedPoseSelect` gọi `start()` sau ViewModel, và `RewardedAdDialog` dùng `unlockPrompt`. Đường "không có bộ điều khiển" có test ở lượt 2; đường hộp thoại thật (khóa bật, đã đồng ý) không dựng được trong test của feature (`RewardedAction` có hàm dựng `internal`), nên kiểm bằng tay ở mục 11.5 và bằng test của khung.
- Tương tự ở `PoseDetailRoute`: đường hộp thoại thật chỉ kiểm bằng tay.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-31, hai điểm lần đầu có trong dự án: (1) `:app` dịch chuỗi của `:core:ads` bằng tài nguyên cùng tên trong `values-<ngôn ngữ>` (đúng sáu tệp và bảng chữ ở mục 3.1); (2) một module feature gọi `rememberRewardedAction` và `RewardedAdDialog` (chưa feature nào gọi; chỉ có test của `:core:ads`). Làm ngay trong worktree của nhánh plan, tệp chưa theo dõi, không commit, xóa sau khi chạy (phiên soạn plan bị chặn tạo worktree ngoài thư mục của nó, nên không dùng `../plan-T-086-try`). Commit gốc `4a5ee7e` (nhánh chính, trước T-080 và T-085).
  - Test tạm `app/src/test/.../ads/AdsStringsTranslationTest.kt` (3 test): `vietnamese_usesTheAppTranslation`, `english_keepsTheFrameworkText`, `everyReleaseLanguage_isTranslated`. Nội dung đúng như tệp sẽ tạo ở lượt 1.
  - Test tạm `feature/poses/src/test/.../trial/RewardedTrialTest.kt` (1 test): `noController_start_grantsAtOnce_noDialog`: trong `:feature:poses`, `rememberRewardedAction(AdPlacement.GALLERY_RW_ACTION, onGranted = { granted++ })` và `RewardedAdDialog(current, title = "Unlock")`; `start()` thì `granted == 1`, không có node `RewardedAdDialogTestTags.CARD`. Vị trí `gallery_rw_action` thay cho `rw_premium_pose` chưa có.
- **Lệnh:**
  - `./gradlew -q :app:testStagingDebugUnitTest --tests com.vglstudio.camera.pose.line.guide.ads.AdsStringsTranslationTest :feature:poses:testDebugUnitTest --tests com.vglstudio.camera.pose.line.guide.feature.poses.trial.RewardedTrialTest` (17:38 tới 17:40 UTC).
  - Đối chứng: tạm bỏ `app/src/main/res/values-vi/strings.xml`, chạy lại test của `:app`.
  - `scripts/check.sh` qua khóa chung của các phiên soạn plan (`check-locked.sh`), hai lần.
- **Kết quả:**
  - Lệnh Gradle: `AdsStringsTranslationTest` 3/3 qua; `RewardedTrialTest` 1/1 qua. `:feature:poses` biên dịch được với `rememberRewardedAction`, `RewardedAdDialog`, `RewardedAdDialogTestTags` (không cần thêm phụ thuộc).
  - Đối chứng: 2 trong 3 test đỏ đúng lý do: `expected:<[Xem quảng cáo]> but was:<[Watch ad]>` và `vi. Actual: Watch ad`; `english_keepsTheFrameworkText` vẫn xanh. Đã trả tệp lại.
  - `scripts/check.sh` lần 1 (17:43 tới 17:50 UTC, chỉ có sáu tệp ngôn ngữ): thất bại ở `:app:lintStagingDebug`, 19 lỗi: 18 `ExtraTranslation` ("`ads_reward_watch` is translated here but not found in default locale", ba chuỗi trong sáu tệp) và 1 `MissingTranslation` (`app_name` của `app/src/main/res/values/strings.xml` không có bản dịch ở hi, vi, pt, th, in, es). Test JVM và script không đỏ. Đúng điểm chưa chắc của mục 3.4.
  - Sửa: `app/src/main/res/values/strings.xml` thêm ba chuỗi gốc tiếng Anh của khung và `translatable="false"` cho `app_name` (khối XML ở mục 3.1 là nội dung đã chạy).
  - `scripts/check.sh` lần 2 (17:51 tới 17:58 UTC): `Cổng chất lượng: QUA`, exit 0. Test JVM 2428, 0 lỗi (`./app` 110, gồm 3 test tạm; `./feature/poses` 171, gồm 1 test tạm; `./core/ads` 1100). 138 ảnh screenshot, không ảnh chuẩn nào đổi (ảnh `RewardedAdDialog*` của `:core:ads` giữ tiếng Anh). Lint và build staging qua. `english_keepsTheFrameworkText` vẫn qua sau khi `:app` có chuỗi gốc.
  - Tệp tạm đã xóa sau khi chạy; nhánh plan chỉ có plan và ticket.
- **Chưa chạy thử:** phần dữ liệu của T-085 (chưa có code), giao diện dấu và hộp thoại (chưa có thiết kế), đường hộp thoại thật với `AdsController` (không dựng được trong feature; khung đã có test). Phần còn lại là mẫu đã có: `combine` luồng, `Channel` cho sự kiện một lần, `SavedStateHandle`, `PrimaryButton`, snackbar như `saveFailed`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (thêm vị trí `rw_premium_pose`, điểm 1); thu thập dữ liệu (thêm sự kiện `premium_pose_unlocked`, điểm 4); module core dùng chung (`:core:ads` đổi `RewardedAdDialogCard` sang công khai, `:core:designsystem` thêm thành phần, điểm 7). Đổi hành vi chọn tư thế ở camera, màn chính. Rủi ro Cao, cần duyệt chéo (Tuấn và Nhật). Điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads`, `:core:designsystem`, `:feature:poses`, `:feature:camera`, `:app` (tài nguyên, staging), tài liệu; đổi giao diện công khai (`PoseCard`, `PoseThumb`, `PosesLibrarySource`, `PoseActions`) |
| B. Độ rõ của yêu cầu | 2 | Spec đã chốt và đủ tiêu chí, nhưng chưa có thiết kế (ticket đòi có trước plan) và API của T-085 chưa chốt; vài lựa chọn còn ở mục 8 (interstitial sau mở khóa, "Tư thế tiếp", chữ tiếng Anh) |
| C. Khả năng kiểm chứng tự động | 1 | ViewModel và màn hình có test với kho giả; đường hộp thoại thật và quảng cáo thử kiểm bằng tay (khung đã có test) |
| D. Ảnh hưởng chéo | 2 | Sửa `AdPlacement.kt`, `PoseCard.kt`, bảng khóa, `StagingRemoteValuesModule.kt`, test của `:feature:poses` mà T-080 cũng sửa; dùng API của T-085 |
| E. Độ mới của kỹ thuật | 2 | Lần đầu feature dùng rewarded của khung, lần đầu `:app` dịch chuỗi của thư viện; đã chạy thử cả hai (mục 4.1) |
| **Tổng** | 10 (tham khảo) | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt`, `RewardedAction.kt` (`:core:ads`); `PoseCard.kt`, `PoseThumb.kt`, chuỗi của `:core:designsystem`; bảng khóa; `StagingRemoteValuesModule.kt`; tài nguyên đè của `:app`; `docs/ops/ads-guide.md`, `docs/ops/manual-test.md`.
- Task khác trong sprint có thể xung đột:
  - **T-080** (approved, chạy ở RUN-20261031-02 trước ticket này): cùng sửa `AdPlacement.kt` (dòng `POSES_IN_COLLECTION`, plan này thêm dòng sau), `AdPlacementAppTest.kt`, `AdUnitIdsTest.kt` (T-080 dùng `unit/12`, plan này `unit/13`), `StagingRemoteValuesModule.kt`, `StagingRemoteValuesModuleTest.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `PoseCard.kt` (`PoseCardDefaults.oneLineHeight`), `PosesScreenTest.kt`, `PosesScreenScreenshotTest.kt`, `docs/ops/ads-guide.md`, `docs/ops/manual-test.md` (mục 11), `docs/features/poses/design/review/summary.md`. Không sửa `PosesScreen.kt` (tag của dấu ở `:core:designsystem`), tránh phần lưới mà T-080 viết lại.
  - **T-085** (plan đang soạn): plan này dùng `Pose.premium`, `PremiumPoseRepository.lockedIds`, `unlock(poseId)`; T-085 sửa `PosesModule.kt`, `StagingFeatureFlagsModule`, bảng khóa (`feature_premium_poses_enabled`). Hai plan cùng sinh lại bảng khóa: chạy nối tiếp thì không xung đột.
  - **T-082** (luồng giới thiệu, trang tư thế premium): có thể dùng `PremiumBadge`; nếu T-082 cũng tạo thành phần dấu thì thống nhất một plan tạo, plan kia dùng lại (L-033).
  - **T-084** (lớp thanh đáy): có thể sửa bảng khóa và `StagingRemoteValuesModule.kt`; chạy nối tiếp.
- Đề xuất thứ tự chạy: sau T-080 và sau T-085 (phụ thuộc). Lượt 1 rồi lượt 2, liền nhau. Mỗi lượt từ nhánh chính mới nhất.
- Phần có ích cho mọi app (đổi `RewardedAdDialogCard` sang công khai để app có ảnh chuẩn của hộp thoại; cách app dịch chuỗi của khung): ghi vào mô tả PR lượt 1 để đề xuất về dự án mẫu (`docs/ops/from-app.md`).

## 7. Điều kiện dừng riêng

- Chưa có gói bàn giao thiết kế premium (`design/screens/` có các frame ở mục 8, câu 1) khi phiên tới task: dừng, không tự đoán giao diện.
- Chữ ký `:core:poses` của T-085 trên nhánh chính khác mục 3.1 mà plan này chưa được cập nhật (phiên bản mới): dừng.
- Bảng khóa sinh lại có thay đổi ngoài `is_show_rw_premium_pose` (lượt 1) hay có thay đổi ở lượt 2: dừng.
- `git status --short -- '*/src/test/screenshots/'` có ảnh chuẩn nào đổi ngoài các ảnh mới của mục 3.2: dừng, không ghi lại ảnh cũ (tham số mới mặc định `false` phải giữ mọi ảnh cũ, kể cả ảnh `RewardedAdDialog*` của `:core:ads`).
- `RewardedActionTest` hay test nào khác của `:core:ads` đỏ: dừng; lượt 1 không đổi hành vi của khung.

## 8. Câu hỏi cho người duyệt

1. **Điểm chặn: thiết kế Figma chưa có.** Ticket ghi thiết kế phải có trước plan; `docs/features/poses/design/` và `docs/features/camera/design/` không có frame premium. Plan chờ gói bàn giao (ảnh vào `design/screens/`, số liệu vào `design/context/`, bảng chữ trong `README.md` của mỗi gói) rồi mới duyệt. Một phiên Figma dựng các frame sau, theo quy ước `<Screen>_<trạng thái>_<theme>` của gói hiện có:
   - `PosesScreen_premium_light`, `PosesScreen_premium_dark`: lưới chip "Tất cả", ít nhất hai thẻ có dấu premium, một thẻ đã lưu có dấu (dấu và nút lưu cùng lúc).
   - `PoseDetailScreen_premium_light`, `PoseDetailScreen_premium_dark`: chi tiết tư thế khóa, dấu ở dòng phụ (hay chỗ khác do thiết kế chọn), nút chính mở khóa (chữ, icon).
   - `PoseUnlockDialog_default_light`, `PoseUnlockDialog_default_dark`: thẻ hộp thoại của khung (bề ngang tối đa 312, nền `surfaceContainerHigh`, nút "Xem quảng cáo" 56 dp bo 28, nút chữ "Để sau"; theo `RewardedAdDialogCard`), tiêu đề và nội dung của app (tên tư thế, lời giải thích). Chỉ vẽ thẻ, không vẽ màn phía sau.
   - `CameraScreen_premium_dark`: camera với dải tư thế có ô khóa (dấu nhỏ trên ô 64), một ô đang chọn không khóa.
   - Thành phần `PremiumBadge` ở trang Design system (cỡ trên thẻ và trên ô, màu, icon).
   - Không cần frame `_unfolded` mới (dấu và nút dùng lại bố cục đã có); nếu thiết kế vẽ thì plan thêm screenshot cùng tên.
   - Đề xuất của AI: dựng đủ 7 frame và thành phần trên, kèm bảng chữ (chữ nút chính, tiêu đề, lời giải thích, nhãn trợ năng của dấu); plan cập nhật chữ tạm ở mục 3.1 theo bảng chữ đó.
2. **Chữ ký của T-085.** Plan dùng `Pose.premium` (mặc định `false`), `PremiumPoseRepository` với `lockedIds: Flow<Set<String>>` (rỗng khi cờ tắt) và `suspend fun unlock(poseId: String)` (ném `IOException`). Plan T-085 chưa có. Đề xuất: khi plan T-085 xong, AI so chữ ký và sửa mục 3.1, 3.2, 4 của plan này (một phiên bản mới nếu đã duyệt). T-085 cần cho dạng tập hợp (`Set<String>` của tư thế khóa) vì lưới và dải đọc nhiều tư thế một lúc.
3. **Chữ tiếng Anh của hai nút khung.** Spec ghi "Xem quảng cáo", "Để sau". Tiếng Việt và năm ngôn ngữ khác dịch ở `:app` (mục 3.1). Tiếng Anh giữ chữ của khung "Watch ad", "No, thanks", hay đè ở `app/src/main/res/values/` thành "Not now" cho khớp nghĩa "Để sau"? Đề xuất của AI: giữ chữ của khung (không đè `values/`), để ảnh chuẩn của khung và của app cùng một chữ tiếng Anh.
4. **Ảnh chuẩn của hộp thoại.** Feature không dựng được hộp thoại thật trong test, và thẻ hộp thoại `RewardedAdDialogCard` là `internal` của `:core:ads`. Đề xuất của AI: đổi nó sang công khai (một từ, KDoc "for previews and screenshot tests"), đề xuất về dự án mẫu ở mô tả PR. Cách khác: frame `PoseUnlockDialog_*` chỉ vẽ phần nội dung của app (`PremiumUnlockContent`), không có tiêu đề và hai nút; cách này không kiểm được tiêu đề và chữ hai nút đã dịch.
5. **Interstitial `inter_pose_detail` sau khi mở khóa ở chi tiết.** Mở khóa xong, app về camera với tư thế đó (KB-8 tiêu chí 4). Đề xuất của AI: không qua `inter_pose_detail` (người dùng vừa xem xong một quảng cáo có thưởng; hai quảng cáo liền nhau dễ bị coi là gây khó chịu). Cách khác: qua `rememberShootWithAd` như nút chụp; nhịp `ads_interval` không tính rewarded nên inter có thể hiện ngay sau.
6. **`pose_selected` khi mở khóa.** Spec ghi `pose_selected` "khi bấm Chụp với tư thế này" (`source` `detail`) và khi chọn ở dải (`strip`). Đề xuất của AI: mở khóa xong mà app về camera hay dùng tư thế đó cũng ghi `pose_selected` (`detail` ở chi tiết, `strip` ở camera), sau `premium_pose_unlocked`, để tỷ lệ "mở chi tiết rồi chụp" của spec mục 1 vẫn đếm tư thế premium. Cách khác: chỉ ghi `premium_pose_unlocked`.
7. **Ghi mở khóa lỗi sau khi người dùng đã xem quảng cáo.** `unlock` ném `IOException` (bộ nhớ đầy, hiếm). Đề xuất của AI: tư thế vẫn khóa, không ghi sự kiện, không điều hướng; chi tiết hiện snackbar "Chưa mở khóa được tư thế. Thử lại."; camera chỉ ghi log `warn` (camera không có snackbar), ô vẫn khóa. Người dùng phải xem lại quảng cáo. Cách khác: lần này vẫn cho dùng tư thế (về camera), chỉ báo lỗi; cần camera nhận tư thế khóa từ chi tiết, trái tiêu chí "chọn tư thế khóa thì hộp thoại hiện".
8. **"Tư thế tiếp" của màn xem lại khi tư thế kế là premium chưa mở.** Spec không nói. Đề xuất của AI: bỏ qua tư thế khóa, chọn tư thế không khóa kế tiếp (vòng về đầu); không mở hộp thoại từ màn xem lại. Cách khác: dừng ở tư thế khóa và mở hộp thoại ở camera.
9. **Đề xuất về dự án mẫu.** Đổi `RewardedAdDialogCard` sang công khai (câu 4) và cách app dịch chuỗi của khung trong `values-<ngôn ngữ>` của `:app` (đã chạy thử) có ích cho mọi app. Đề xuất: ghi trong mô tả PR lượt 1 theo `docs/ops/from-app.md`, người mở đề xuất ở repo mẫu sau khi merge. Đúng không?

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
