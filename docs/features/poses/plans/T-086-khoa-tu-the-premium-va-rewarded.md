---
ticket: T-086
title: Khóa tư thế premium ở thư viện, chi tiết, camera; mở bằng quảng cáo có thưởng `rw_premium_pose`
status: stopped              # draft | approved | stopped | done
version: 3                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 39.4M        # một lượt cỡ L theo số gợi ý của scripts/token-usage.py estimate (2026-11-01, 8 task cỡ L); không nâng tay; task cỡ L thật có thể lớn hơn nhiều (T-074 dùng 101M)
blocking_points: [ads, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-086: KHÓA TƯ THẾ PREMIUM Ở THƯ VIỆN, CHI TIẾT, CAMERA; MỞ BẰNG QUẢNG CÁO CÓ THƯỞNG `RW_PREMIUM_POSE`

## 1. Tóm tắt

Tư thế premium chưa mở khóa có dấu premium trên thẻ thư viện, ở chi tiết và trên ô của dải tư thế camera. Nút chính của chi tiết thành nút mở khóa; chạm ô khóa ở camera không đổi tư thế đang dùng. Cả hai chỗ mở hộp thoại mở khóa của khung (`rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, ...)` và `RewardedAdDialog`). Xem hết quảng cáo, hay khóa `is_show_rw_premium_pose` tắt, hay cổng đồng ý đóng, thì ViewModel gọi `PremiumPoseRepository.unlock` của `:core:poses` (T-085). Kho trả `true` thì app ghi `premium_pose_unlocked`; rồi app làm tiếp việc người dùng định làm: về camera với tư thế đó (chi tiết), hay dùng tư thế đó (camera). Vị trí mới không khai báo `noAdPolicy`, nên không có quảng cáo thì chặn (câu 8).

Một lượt, một PR có CI xanh (ticket cỡ L). Commit đi theo hai phần:

- **Phần A:** vị trí `rw_premium_pose`, sự kiện, bản staging, bảng khóa, dấu premium của hệ thống thiết kế, thư viện và chi tiết, chữ của hộp thoại khung trong 6 ngôn ngữ.
- **Phần B:** camera (dấu trên ô, hộp thoại khi chạm ô khóa, tư thế dùng lần trước bị khóa, "Tư thế tiếp" bỏ qua ô khóa).

Kiểm chứng gồm:

- unit test cho `DefaultPosesLibrarySource` (cờ tắt, cờ bật chưa mở, đã mở một) và ViewModel của thư viện, chi tiết, camera với kho giả: được thưởng, kho trả `false`, không thưởng, ghi lỗi, cờ tắt;
- test giao diện Robolectric cho dấu premium, nút mở khóa và đường "không có bộ điều khiển quảng cáo thì chạy ngay";
- screenshot test cùng tên với 7 ảnh thiết kế premium (PR #278), ảnh `_small` của chi tiết khi còn khóa;
- test giá trị staging, bảng khóa sinh lại, test chữ của hộp thoại khung trong 7 ngôn ngữ.

**Phiên bản 3 (2026-11-01, `draft`, soạn sau lần dừng ở RUN-20261031-02).** Phiên bản 2 chưa đủ tốt ở một điểm: mục 4 lấy chiều cao thẻ hộp thoại 288 dp từ frame `PoseUnlockDialogScreen_*`, frame vẽ nút "Để sau" cao 40 dp. Code thật dùng `RewardedAdDialogCard` của khung `:core:ads` (không đổi, theo mục 3.1), nút chữ của Material giữ vùng chạm tối thiểu 48 dp, nên thẻ cao 296 dp và góc trên ở 297,5 dp. Test `PoseDetailScreenTest.unlockDialogCard_matchesDesign` đỏ: `expected:<288.0> but was:<296.00003>`. Phiên không được đổi con số test viết trước kiểm (điều kiện dừng 8 của `risk-scoring.md` mục 4), nên dừng. Thay đổi so với phiên bản 2: chỉ dòng "Kích thước thẻ hộp thoại" và mô tả khung của dòng "Hộp thoại mở khóa" ở mục 4 (thẻ cao 296, góc trên 297,5, theo vùng chạm 48 dp của khung); mục 3.1 thêm cách tiếp tục; câu 13 ở mục 8. Cách làm, phạm vi, file và mọi test khác giữ như phiên bản 2.

**Phiên bản 2 (2026-11-01).** Phiên bản 1 (`draft`, PR #273) viết trên nhánh chính trước ba việc, nên chưa đủ:

1. **Gói thiết kế đã có (PR #278, Nhật duyệt 2026-10-31).** Bỏ điểm chặn "chưa có thiết kế". Số đo của mục 3.1 lấy từ `design/context/`:
   - `PremiumBadge` có ba cỡ: thẻ 32 (icon 20), ô 22 (icon 14), dòng phụ 20 (icon 14);
   - chỗ đặt trên thẻ, trên ô và ở dòng phụ;
   - nút mở khóa có icon `smart_display`;
   - frame hộp thoại 411 x 891, thẻ ở giữa.

   Chữ theo bảng chữ: "premium" thay "cao cấp"; lời giải thích mới. Ảnh chuẩn hộp thoại đổi tên thành `PoseUnlockDialogScreen_default_<theme>`, như frame. Câu 1 của mục 8 đã trả lời.
2. **API của T-085 (plan approved 2026-10-31).** Bỏ giả định `lockedIds`. Kho cho:
   - `PremiumPoseRepository.access: Flow<PremiumAccess>`, với `PremiumAccess.isLocked(pose)`, `isUsable(pose)`;
   - `isUsable(pose): Flow<Boolean>`;
   - `suspend fun unlock(poseId): Boolean`.

   `:feature:poses` tự tính `LibraryContent.lockedIds` trong `DefaultPosesLibrarySource`, nên ViewModel và kho giả của thư viện ít đổi; có test riêng cho `DefaultPosesLibrarySource`. `PoseViewModel` của camera theo `access`. `unlock` trả `false` (cờ vừa tắt) thì không ghi sự kiện, vẫn làm tiếp việc người dùng định làm; có hai test mới. Câu 2 của mục 8 đã trả lời.
3. **Đồng bộ dự án mẫu đợt 5 (PR #279).**
   - Quy tắc mới của `docs/CLAUDE.md` (L-142 của mẫu): không chia một plan thành nhiều lượt trong danh sách chạy. Phiên bản 1 có hai lượt; phiên bản 2 gộp thành một lượt, ticket giữ nguyên. Tách camera thành ticket riêng là câu 10 của mục 8.
   - Rà API rewarded: `RewardedAction.kt` và chuỗi `ads_reward_*` không đổi. `start()` vẫn chạy hành động ngay khi không có `LocalAdsController`, khi khóa tắt hay cổng đồng ý đóng. Hai điều mới:
     - cổng gói bỏ quảng cáo (`AdFreeStatus`, `AdFreeModule` của `:app` gắn `AdFreeStatus.NONE`): người dùng có gói thì `rewardedSwitch` tắt, `start()` mở khóa ngay (câu 11);
     - F-08 (T-055 của mẫu): activity không dùng được lúc hiện thì lượt kết thúc như không có quảng cáo, chặn với vị trí này.
   - Lệnh xem ảnh chuẩn đổi thành `git status --short -- '*/src/test/screenshots/*'` (mục 7). Bộ build mới (AGP 9) không đổi gì cho plan: `:feature:poses` vẫn biên dịch được với `:core:ads` không thêm phụ thuộc (chạy thử lại, mục 4.1).
   - Chạy thử lại trên `origin/main` `9af5928` (mục 4.1). `scripts/existing-tests.py` chạy lại (mục 3.5).
4. **Rà soát độc lập phiên bản 2 (2026-11-01): 1 chặn, 9 nên sửa, 8 ghi chú, đã sửa trong phiên bản này** (kết quả ở cuối mục 8):
   - Phần nối của camera tách thành `LockedPoseUnlock.kt`, có test với `PoseViewModel` thật; `PoseDetailRoute` dựng được trong test (chạy thử).
   - `PremiumBadge` có tham số `contentDescription`; ô camera truyền `null` để TalkBack không đọc trùng (chạy thử trên cây ngữ nghĩa và `AccessibilityNodeInfo`).
   - Kịch bản emulator đi qua luồng giới thiệu của T-082 (kiểm bằng `device-scenario.py check`).
   - `ads-guide.md` chỉ giữ phần chung; đổi `RewardedAdDialogCard` ghi vào `docs/template-sync.txt`.
   - Thêm hai test; ticket bỏ câu "chia hai lượt" (sửa trong PR của plan); `unit/14` thống nhất.

## 2. Hiểu yêu cầu

Ticket xử lý KB-8 của `features/poses/spec.md` (confirmed 2026-10-31, câu 7, 8, 10, 11, 12), hai tiêu chí tư thế premium ở KB-1 của `features/camera/spec.md`, và vị trí `rw_premium_pose` ở câu 12 của `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Poses KB-8, tiêu chí 1 | Khi tư thế là premium và chưa mở khóa thì thẻ ở thư viện, ô ở dải tư thế của camera và màn chi tiết có dấu premium, đúng thiết kế | "Khóa" là `PremiumAccess.isLocked(pose)` của T-085: cờ bật, `pose.premium`, chưa mở. Dấu là thành phần mới `PremiumBadge(size)` của `:core:designsystem`, ba cỡ theo thiết kế: trên `PoseCard` (32) và `PoseThumb` (22) khi tham số `premium` là `true`, và ở đầu dòng phụ của chi tiết (20). Thẻ còn khung xương (ảnh chưa tải) không có dấu. Thư viện và chi tiết ở phần A, camera ở phần B |
| Poses KB-8, tiêu chí 2 | Khi mở chi tiết của tư thế premium chưa mở khóa thì ảnh, các bước, lỗi hay gặp hiện như tư thế thường; nút chính là nút mở khóa; lưu và bỏ lưu vẫn được | Chạm thẻ khóa vẫn mở chi tiết như cũ (câu 7). `PoseDetailUiState.Ready.locked`; `PoseDetailBottomBar` đổi chữ, icon và hành động của `PrimaryButton` khi khóa ("Xem quảng cáo để dùng", icon `smart_display`, theo bảng chữ). `onToggleSave` không đổi |
| Poses KB-8, tiêu chí 3 | Khi bấm nút mở khóa ở chi tiết, hay chạm ô của tư thế đó ở dải camera, thì hộp thoại mở khóa hiện; tư thế đang dùng ở camera không đổi. Hộp thoại có tên tư thế, lời giải thích, nút "Xem quảng cáo", "Để sau" | Nút gọi `unlock.start()` của `rememberRewardedAction`. Khóa bật và cổng đồng ý mở thì khung mở hộp thoại (`RewardedAdDialog`): tiêu đề là chữ của app, nội dung là `PremiumUnlockContent` (tên tư thế, lời giải thích), hai nút của khung. Chữ hai nút là chuỗi `ads_reward_watch`, `ads_reward_no_thanks` của `:core:ads` (tiếng Anh "Watch ad", "No, thanks"); app dịch trong `app/src/main/res/values-<ngôn ngữ>/` (tiếng Việt "Xem quảng cáo", "Để sau"), đã chạy thử (mục 4.1). Ở camera, `onPoseSelect` của ô khóa không chọn tư thế; ô gọi `onLockedPoseSelect`, ViewModel giữ tư thế chờ mở (`SavedStateHandle`), màn hình gọi `start()` |
| Poses KB-8, tiêu chí 4 | Khi người dùng xem hết quảng cáo và nhận thưởng thì tư thế mở khóa ngay: dấu mất ở mọi màn, và app làm tiếp việc người dùng vừa định làm | `onGranted` của khung chạy một lần mỗi lần mở hộp thoại, trên luồng chính, lúc SDK báo thưởng (trước khi quảng cáo đóng). Chi tiết: `onUnlockGranted()` gọi `unlock(poseId)`; kho trả `true` thì ghi sự kiện; rồi phát sự kiện mở camera với tư thế đó; camera mở bên dưới quảng cáo và hiện khi quảng cáo đóng. Camera: `onUnlockGranted()` gọi `unlock`, ghi sự kiện khi `true`, rồi chọn tư thế như chạm ô thường (`pose_selected` `strip`, đánh dấu đã dùng). Dấu mất vì `access` của T-085 phát giá trị mới; thư viện, chi tiết (qua `DefaultPosesLibrarySource`) và camera đều theo dõi luồng đó |
| Poses KB-8, tiêu chí 5 | Khi đóng quảng cáo trước khi nhận thưởng, hay bấm "Để sau", thì tư thế vẫn khóa, app ở lại màn đang mở | Khung không gọi `onGranted` (R-04; `dismiss()`). Feature không làm gì. Phần khung đã có test ở `RewardedActionTest`; feature kiểm "không gọi `onUnlockGranted` thì không ghi, không điều hướng" |
| Poses KB-8, tiêu chí 6 | Khi bấm "Xem quảng cáo" mà không có quảng cáo thì tư thế vẫn khóa; thử lại sau được. Lỗi mạng thì có thông báo kiểm tra kết nối của khung | Vị trí không khai báo `noAdPolicy`, nên `effectiveNoAdPolicy` là `Block`: `NoAd(granted = false)`, không gọi `onGranted`; lỗi mạng thì khung hiện `Toast` `ads_reward_check_connection` (app dịch, như hai nút). Activity không dùng được lúc hiện (F-08, mới ở #279) cũng kết thúc như không có quảng cáo, nên cũng chặn. `AdPlacementAppTest` kiểm `noAdPolicy == null` và `effectiveNoAdPolicy == Block` |
| Poses KB-8, tiêu chí 7 | Đã mở khóa thì lần mở app sau, kể cả sau khi cập nhật app hay catalog mới, vẫn mở | Phần lưu là của T-085 (khóa DataStore `poses_unlocked`, theo mã). Plan này đọc `access` mỗi lần màn mở; test dùng kho giả. Kiểm trên máy thật (mục 4) |
| Poses KB-8, tiêu chí 8 | Khi cờ `feature_premium_poses_enabled` tắt thì không có dấu, mọi tư thế dùng được, không ghi gì vào danh sách đã mở khóa | Cờ tắt thì `PremiumAccess.enabled = false`, `isLocked` luôn `false`: `lockedIds` rỗng, không có dấu, nút chính là "Chụp với tư thế này", ô chạm được như cũ, `start()` không được gọi. `onUnlockGranted()` với tư thế không khóa không gọi `unlock` và không ghi sự kiện. Cờ vừa tắt giữa lúc màn còn hiện tư thế khóa: `unlock` của T-085 đọc cờ một lần, không ghi gì và trả `false`; app không ghi `premium_pose_unlocked`, vẫn làm tiếp việc người dùng định làm (tư thế đã dùng được vì cờ tắt) |
| Poses KB-8, tiêu chí 9 | Khi cờ bật mà `is_show_rw_premium_pose` tắt hay chưa đồng ý quảng cáo thì bấm "Xem quảng cáo" mở khóa ngay, vĩnh viễn, không hiện quảng cáo | Khung: `rewardedSwitch` là `isEnabled(placement) && canRequestAds`; `isEnabled` là `is_show_ads && !adFree && is_show_rw_premium_pose` (#279 thêm `adFree`). Tắt thì `start()` gọi `onGranted` ngay, không hộp thoại. Nên nút mở khóa (chi tiết) và ô khóa (camera) mở thẳng, không có bước "Xem quảng cáo". Plan hiểu chữ "bấm Xem quảng cáo" của tiêu chí là bấm nút mở khóa, vì khi đó không có hộp thoại nào. Test giao diện của feature chạy đúng đường này: không có `LocalAdsController` thì `start()` gọi `onGranted` ngay (đã chạy thử lại sau #279, mục 4.1) |
| Poses KB-8, tiêu chí 10 | Khi tư thế mở khóa thì app ghi `premium_pose_unlocked` | `EVENT_PREMIUM_POSE_UNLOCKED = "premium_pose_unlocked"` trong `PoseDetailViewModel` (`source` `detail`) và `PoseViewModel` của camera (`source` `camera`), tham số `pose_id`, `category`, `source` (spec mục 6). Ghi sau khi `unlock` trả `true`, một lần mỗi lần mở; `false` hay ghi lỗi thì không ghi sự kiện |
| Poses KB-8, tiêu chí 11 | Khi mở camera lần đầu thì tư thế mặc định và bộ khởi đầu không bao giờ là premium | Do catalog, parser (`defaultPose` là tư thế miễn phí đầu tiên) và script kiểm của T-085. Plan này thêm đường dự phòng ở camera: tư thế dùng lần trước bị khóa thì dùng `defaultPose` (tiêu chí camera dưới) |
| Camera KB-1, tiêu chí 8 | Khi cờ bật thì ô của tư thế premium chưa mở khóa có dấu premium; chạm ô thì hộp thoại mở khóa hiện, tư thế đang dùng không đổi; mở khóa xong thì camera dùng tư thế đó | Phần B. `PoseThumbItem.locked = access.isLocked(pose)`; `PoseThumb(premium = item.locked)`; chạm ô khóa gọi `onLockedPoseSelect(id)`. `PoseViewModel` theo dõi `access` suốt đời màn (thư viện thì chỉ lấy một lần, như cũ), nên mở khóa ở chi tiết rồi quay lại camera thì dấu đã mất |
| Camera KB-1, tiêu chí 9 | Khi tư thế dùng lần trước trở thành premium chưa mở khóa (catalog mới) thì camera dùng tư thế mặc định | Phần B. `onLibraryReady` đọc `access = premiumRepository.access.first()` trước khi chọn: `requested?.takeUnless(access::isLocked) ?: ready.lastUsedPose?.takeUnless(access::isLocked) ?: ready.defaultPose`. Tư thế đang dùng trở thành khóa giữa lúc camera đang mở (cờ bật từ xa) thì giữ nguyên tới lần mở sau; chỉ chọn mới mới kiểm khóa |
| Spec ads, câu 12 | `rw_premium_pose`: rewarded mở khóa tư thế premium; không có quảng cáo thì chặn | Một dòng `RW_PREMIUM_POSE("rw_premium_pose", AdFormat.REWARDED),` có chú thích hành động. `AdsSettings` tự sinh `is_show_rw_premium_pose`, mặc định tắt (ADR-0014). Bản staging bật (spec poses mục 8). Mã production: danh sách của T-075 lấy mọi dòng `AdPlacement` không phải `gallery_` (ticket T-075, Định nghĩa hoàn thành), nên vị trí tự vào danh sách, không sửa file nào |

Thiết kế tham chiếu (PR #278, `approved_by: Nhật`, `approved_at: 2026-10-31`):

- `features/poses/design/README.md`: mục 1 (frame `PosesScreen_premium_<theme>`, `PoseDetailScreen_premium_<theme>`, `PoseUnlockDialogScreen_default_<theme>`), mục 2 (bảng chữ, dòng premium), mục 4 (`PremiumBadge`, thẻ premium, hộp thoại), luồng ở mục 5.
- `features/camera/design/README.md`: frame `CameraScreen_premium_dark`, bảng chữ `camera_cd_thumb_premium`, dòng ô premium.
- `design/context/` cùng tên của 7 frame trên.

Không có frame `_unfolded` mới.

ADR liên quan: ADR-0001 (phụ thuộc module), ADR-0003 (cờ là `Flow`), ADR-0008 (hệ thống thiết kế), ADR-0013, ADR-0014 (vị trí mặc định tắt), ADR-0019 (log), ADR-0020 (ảnh thiết kế và screenshot cùng tên), ADR-0023 (tên khóa), ADR-0026 (khung AdsKit), ADR-0029, ADR-0031, ADR-0043 (test emulator chạy sau merge).

## 3. Giải pháp

### 3.1. Cách làm

**Tiếp tục (phiên bản 3).** Tiếp tục trên nhánh `claude/T-086-khoa-tu-the-premium` (commit test `b356d3e`, commit code `a52568f`, chưa có PR). Phiên cập nhật nhánh theo nhánh chính (sau `git fetch origin main`), sửa con số của `unlockDialogCard_matchesDesign` theo mục 4 phiên bản 3 (thay đổi điều test kiểm đã được người duyệt ở phiên bản này, không phải lệch), chạy `scripts/check.sh`, rồi đi tiếp từ bước 5. Bước 3 chỉ áp cho test đổi: test mới phải qua trên code đã có, và đỏ khi tạm đặt lại 288 (đối chứng). Ảnh chuẩn `PoseUnlockDialogScreen_default_<light|dark>.png` đã ghi với thẻ 296 trên nhánh. Câu 13 ở mục 8 chọn (b) thì không cần phiên bản này: người sửa frame rồi ghi lại `context/` và phiên đổi lại 288 thành số mới của frame.

**Một lượt.** Danh sách chạy có một dòng cho ticket và plan này. Nhánh `claude/T-086-khoa-tu-the-premium`, từ nhánh chính có T-080 và T-085. Task đi đủ 10 bước của `docs/auto-run.md`, mục 3. Commit test (bước 3) và commit code (bước 4) mỗi loại đi theo thứ tự phần A rồi phần B, để rà soát đọc từng phần; `scripts/check.sh` chạy một lần cho cả nhánh.

**API của T-085 mà plan dùng** (plan T-085, mục 3.1, phần "API công khai"; chưa có trên nhánh chính khi soạn plan này):

```kotlin
// :core:poses (T-085), model/Pose.kt: one new field, default false.
data class Pose(/* ... */ val premium: Boolean = false)

// :core:poses (T-085), model/PremiumAccess.kt
data class PremiumAccess(val enabled: Boolean, val unlockedIds: Set<String>) {
    fun isLocked(pose: Pose): Boolean = enabled && pose.premium && pose.id !in unlockedIds
    fun isUsable(pose: Pose): Boolean = !isLocked(pose)
}

// :core:poses (T-085), PremiumPoseRepository.kt; Hilt @Singleton in PosesModule.
interface PremiumPoseRepository {
    val access: Flow<PremiumAccess>
    fun isUsable(pose: Pose): Flow<Boolean>
    /** False when FeatureFlag.PREMIUM_POSES is off (nothing written). Throws IOException when the list cannot be written. */
    suspend fun unlock(poseId: String): Boolean
}
```

Gói: `com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository`, `com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess`. Plan này không đọc `FeatureFlag.PREMIUM_POSES` trực tiếp: cờ đã gộp trong `access`. Kho không biết catalog, nên chỉ gọi `unlock` cho tư thế đang khóa (plan T-085, mục 3.1). Plan không dùng `isUsable(pose)`: chi tiết đọc `lockedIds` qua `PosesLibrarySource` như thư viện, để một luồng phục vụ cả hai màn.

#### Phần A: vị trí, hệ thống thiết kế, thư viện, chi tiết

**`AdPlacement.kt`.** Sau `INTER_POSE_DETAIL` (và dòng `POSES_IN_COLLECTION` của T-080), một dòng, chú thích nêu hành động (`docs/ops/ads-guide.md`, mục 5, bước 2):

```kotlin
    /** Holds the unlock of a premium pose, from the detail and the camera strip; no ad blocks (spec poses, KB-8, question 8). */
    RW_PREMIUM_POSE("rw_premium_pose", AdFormat.REWARDED),
```

Không khai báo `noAdPolicy` (câu 8). `scripts/release.sh` và `scripts/ads-plan.py` đọc dòng một-dòng và chú thích ngay trên, nên không sửa script.

**`RewardedAction.kt` (mục 8, câu 4).** Đổi `internal fun RewardedAdDialogCard(...)` thành công khai, thêm vào KDoc "Public for previews and screenshot tests; screens use [RewardedAdDialog]". Lý do: feature không dựng được hộp thoại thật trong test (không có `AdsController` thì `start()` chạy hành động ngay, và `RewardedAction` có hàm dựng `internal`), nên ảnh thiết kế của hộp thoại chỉ so được bằng thẻ không trạng thái này. Không đổi hành vi, không đổi ảnh chuẩn của khung. Tệp không đổi ở #279 (đã so `git diff 4a5ee7e 9af5928`). `RewardedAction.kt` là tệp của dự án mẫu (`docs/ops/to-app.md` của mẫu, mục 2): đợt đồng bộ sau phải giữ chữ `fun RewardedAdDialogCard` không có `internal`. Plan ghi điều này vào đoạn chú thích "App values kept inside template files after each sync" của `docs/template-sync.txt` (chỉ thêm một dòng chú thích `#`, không chạm dòng `base=` hay `hold=`, ADR-0030):

```text
# core/ads RewardedAction.kt: RewardedAdDialogCard is public (plan T-086, screenshot of the unlock dialog); keep it.
```

**`docs/ops/ads-guide.md`** (tệp của mẫu): chỉ thêm phần chung, để đề xuất về mẫu: dòng Rewarded của mục 5 thêm "thẻ `RewardedAdDialogCard` công khai cho ảnh chuẩn của hộp thoại" và "app dịch ba chuỗi trong `values-<ngôn ngữ>` của `:app`, kèm chuỗi gốc tiếng Anh ở `values/` và `app_name` `translatable="false"` (lint `ExtraTranslation`, `MissingTranslation`)". Không ghi tên vị trí hay ticket của app vào tệp này.

**`:core:designsystem`** (số đo: `features/poses/design/README.md` mục 4; `context/PosesScreen_premium_light.md`, `PoseDetailScreen_premium_light.md`, `features/camera/design/context/CameraScreen_premium_dark.md`):

- `component/PremiumBadge.kt` (mới):

  ```kotlin
  /** Sizes of [PremiumBadge] (design: component PremiumBadge, Size=Card, Thumb, Inline). */
  enum class PremiumBadgeSize(internal val ring: Dp, internal val icon: Dp) {
      /** Top left of the image of a PoseCard. */
      Card(32.dp, 20.dp),
      /** Over the top left corner of a 64 dp PoseThumb, mirroring SelectedBadge. */
      Thumb(22.dp, 14.dp),
      /** Before the meta line of the pose detail. */
      Inline(20.dp, 14.dp),
  }

  /**
   * The premium mark of a pose not unlocked yet (spec poses, KB-8): never blurs or covers the pose image.
   * [contentDescription] is null where the parent already says "premium" (a camera cell), so TalkBack does not
   * read it twice.
   */
  @Composable
  fun PremiumBadge(size: PremiumBadgeSize, contentDescription: String?, modifier: Modifier = Modifier)

  const val PREMIUM_BADGE_TAG = "pose_premium_badge"
  ```

  Vòng `CircleShape`, nền `tertiaryContainer`, viền `Stroke.sticker` màu `outline`; icon `ic_premium` màu `onTertiaryContainer`, cỡ `size.icon`, ở giữa, `contentDescription = null`. `testTag(PREMIUM_BADGE_TAG)` trên vòng; `contentDescription` khác `null` thì thêm `Modifier.semantics { this.contentDescription = it }`. Đây là code đã chạy thử (mục 4.1). Màu theo theme: chế độ tối của camera cho nền `#6B1D35`, viền trắng, icon `#FFD6E2` đúng `context/` (không viết cứng mã màu).
  Ai truyền gì (trợ năng, L-004; đo ở mục 4.1):
  - `PoseCard` truyền `premium_badge_label`: thẻ là node gộp (`clickable`), chữ tên và dòng phụ là `text`, nên TalkBack đọc tên, dòng phụ rồi "Tư thế premium" một lần.
  - `PoseThumb` truyền `null`: ô đã có `contentDescription` riêng `camera_cd_thumb_premium` ("…, premium, chạm để mở khóa"); truyền nhãn thì node gộp có hai mô tả và TalkBack đọc "premium" hai lần (đối chứng của chạy thử).
  - Dòng phụ ở chi tiết truyền `premium_badge_label` (dòng phụ không phải node gộp).
- `PoseCard.kt`: tham số cuối `premium: Boolean = false`. `true` thì vẽ `PremiumBadge(PremiumBadgeSize.Card)` trong `Box` của ảnh, `Modifier.align(Alignment.TopStart).offset(x = PremiumOffsetX, y = PremiumOffsetY)` với `PremiumOffsetX = 6.dp`, `PremiumOffsetY = 10.dp`. Chú thích: "Design: the 32 dp badge sits at (6, 10) in the image, its centre mirroring the 36 dp save circle (48 dp area 2 dp outside the top right corner)". `contentDescription = stringResource(R.string.premium_badge_label)`. Dấu không nhận chạm, nên chạm vào dấu rơi vào thẻ. `PoseCardSkeleton` không đổi. Mặc định `false` giữ mọi ảnh chuẩn cũ.
- `component/PremiumUnlockContent.kt` (mới): `@Composable fun PremiumUnlockContent(poseName: String, modifier: Modifier = Modifier)`: `Column(verticalArrangement = spacedBy(Spacing.sm))` gồm tên tư thế (`titleMedium`, `onSurface`) và `premium_unlock_message` (`bodyMedium`, `onSurfaceVariant`). Đúng `context/PoseUnlockDialogScreen_default_light.md`: tên 24 cao, lời 40 cao, cách 8. Dùng làm `content` của `RewardedAdDialog` ở chi tiết và camera, để chữ chỉ dịch một lần.
- `res/drawable/ic_premium.xml` (mới): vector 24 dp `workspace_premium` của Material Symbols (Apache 2.0, như các icon đã có), vẽ ở cỡ `size.icon`.
- Chuỗi mới trong `values/strings.xml` và 6 thư mục ngôn ngữ (bảng ở cuối phần A).

**`:feature:poses`, dữ liệu.**

- `PosesLibrarySource.kt`: `LibraryContent` thêm `val lockedIds: Set<String> = emptySet()` (KDoc "Premium poses not unlocked yet; empty while premium poses are off"), đặt trước `lists`, có mặc định để `testContent()` cũ không đổi. Interface thêm:

  ```kotlin
  /**
   * Unlocks [poseId] for good (spec poses, KB-8). False when premium poses are off, so nothing was written and no
   * unlock is logged. Throws [java.io.IOException] when the list cannot be written.
   */
  suspend fun unlock(poseId: String): Boolean
  ```
- `DefaultPosesLibrarySource.kt`:

  ```kotlin
  internal class DefaultPosesLibrarySource @Inject constructor(
      private val libraryRepository: PoseLibraryRepository,
      private val historyRepository: PoseHistoryRepository,
      private val premiumRepository: PremiumPoseRepository,
  ) : PosesLibrarySource {
      // The locks follow the flag and the unlocked list of :core:poses (T-085): a new access gives a new content.
      override val library: Flow<LibraryResult> =
          combine(libraryRepository.library, premiumRepository.access) { state, access ->
              when (state) {
                  LibraryState.Loading -> LibraryResult.Loading
                  is LibraryState.Failed -> LibraryResult.Failed(state.failure)
                  is LibraryState.Ready -> LibraryResult.Ready(state.library.toContent(access))
              }
          }

      override suspend fun unlock(poseId: String): Boolean = premiumRepository.unlock(poseId)

      private fun PoseLibrary.toContent(access: PremiumAccess): LibraryContent {
          val filters = listOf(PoseFilter.All, PoseFilter.Saved) + categories.map { PoseFilter.Category(it.id) }
          return LibraryContent(
              categories = categories,
              totalPoses = totalPoses,
              savedIds = savedIds,
              lockedIds = posesFor(PoseFilter.All).filter(access::isLocked).mapTo(mutableSetOf()) { it.id },
              lists = filters.associateWith { posesFor(it) },
          )
      }
  }
  ```

  `combine` chờ giá trị đầu của cả hai luồng: `access` của T-085 đọc DataStore và cờ trên máy, có giá trị gần như ngay; luồng đọc DataStore tự thử lại khi gặp `IOException` (T-085), nên thư viện không kẹt ở `Loading`. `di/` của `:feature:poses` không đổi: Hilt tiêm `PremiumPoseRepository` từ `PosesModule` của `:core:poses` (T-085).

**`:feature:poses`, thư viện.** `PoseCardItem` thêm `val locked: Boolean = false`. `PosesViewModel.publish()` đặt `locked = id in content.lockedIds`. `PoseGridCell` truyền `premium = item.locked` cho `PoseCard`. Chạm thẻ vẫn mở chi tiết (câu 7). `PosesScreen.kt` không đổi (tag của dấu nằm ở `:core:designsystem`), nên plan không chạm phần lưới mà T-080 viết lại.

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
            logSelected(pose)
            openCamera.trySend(pose.id)
            return
        }
        unlocking = true
        viewModelScope.launch {
            try {
                if (librarySource.unlock(pose.id)) {
                    analytics.log(
                        AnalyticsEvent(
                            EVENT_PREMIUM_POSE_UNLOCKED,
                            mapOf("pose_id" to pose.id, "category" to pose.categoryId, "source" to SOURCE_DETAIL),
                        ),
                    )
                    logger.info(LOG_TAG, "Premium pose unlocked")
                } else {
                    // Premium poses were switched off meanwhile: nothing written, the pose is usable (T-085).
                    logger.info(LOG_TAG, "Unlock skipped: premium off")
                }
                logSelected(pose) // pose_selected, source detail (question 6)
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
    - `PoseDetailScreen` nhận thêm `onUnlock: () -> Unit = {}`, `onUnlockFailedShown: () -> Unit = {}` (có mặc định, nên các test và ảnh chuẩn đang gọi `PoseDetailScreen` không đổi); route truyền `onUnlock = unlock::start`, `onUnlockFailedShown = viewModel::onUnlockFailedShown`.
  - `PoseDetailScreen` hiện snackbar `poses_unlock_failed` khi `unlockFailed`, như `saveFailed`.
  - `PoseDetailTestTags.UNLOCK = "pose_detail_unlock"`.
- `PoseDetailComponents.kt`:
  - `PoseDetailBottomBar(locked: Boolean, onShoot, onUnlock)`: khóa thì `PrimaryButton(text = poses_detail_unlock, icon = painterResource(R.drawable.ic_poses_smart_display), onClick = onUnlock, modifier = ...testTag(UNLOCK))`; không khóa thì nút cũ. Cùng chỗ, cùng cỡ (`context/PoseDetailScreen_premium_light.md`: nút 379 x 60 ở y 811, icon 20, chữ `titleMedium`, như nút chụp).
  - `PoseDetailMeta(pose, locked)`: khóa thì `Row(verticalAlignment = CenterVertically)` gồm `PremiumBadge(PremiumBadgeSize.Inline, contentDescription = stringResource(DesignR.string.premium_badge_label))`, `Spacer(Modifier.width(Spacing.sm))`, rồi chữ dòng phụ như cũ (`context/`: dấu 20 ở x 16, chữ ở x 44).
  - `PhoneDetailLayout`, `WideDetailLayout` truyền tiếp `locked`, `onUnlock`.
- `res/drawable/ic_poses_smart_display.xml` (mới): vector 24 dp `smart_display` của Material Symbols, như `ic_poses_photo_camera.xml`.

**Chữ hộp thoại khung (`:app`).** Sáu tệp `app/src/main/res/values-<ngôn ngữ>/strings.xml` dịch ba chuỗi của `:core:ads` (`ads_reward_watch`, `ads_reward_no_thanks`, `ads_reward_check_connection`), theo `ads-guide.md` mục 5, dòng Rewarded: "app dịch trong `values-<ngôn ngữ>` của mình". Tài nguyên của `:app` đè tài nguyên cùng tên của thư viện. Lint chạy với `checkDependencies = true` báo `ExtraTranslation` khi `values/` của `:app` không có chuỗi gốc, và `MissingTranslation` cho `app_name` khi `:app` có thư mục ngôn ngữ (chạy thử phiên bản 1, lần 1, mục 4.1). Nên `app/src/main/res/values/strings.xml` thêm ba chuỗi với đúng chữ tiếng Anh của khung, và `app_name` thêm `translatable="false"` (tên app không dịch):

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

Đây là nội dung đã chạy thử (phiên bản 1 lần 2, và lại sau #279, mục 4.1). Hệ quả: khung đổi chữ tiếng Anh sau này thì app giữ chữ cũ tới khi sửa tệp này; `AdsStringsTranslationTest.english_keepsTheFrameworkText` so với chữ cố định nên sẽ báo. Tiếng Anh giữ "No, thanks" hay đổi "Not now" ở mục 8, câu 3.

| Tệp | `ads_reward_watch` | `ads_reward_no_thanks` | `ads_reward_check_connection` |
| --- | --- | --- | --- |
| `values-vi` | Xem quảng cáo | Để sau | Kiểm tra kết nối rồi thử lại |
| `values-hi` | विज्ञापन देखें | अभी नहीं | अपना कनेक्शन जांचें और फिर से कोशिश करें |
| `values-pt-rBR` | Assistir anúncio | Agora não | Verifique sua conexão e tente novamente |
| `values-b+es+419` | Ver anuncio | Ahora no | Revisa tu conexión y vuelve a intentarlo |
| `values-in` | Tonton iklan | Nanti saja | Periksa koneksi Anda dan coba lagi |
| `values-th` | ดูโฆษณา | ไว้ทีหลัง | ตรวจสอบการเชื่อมต่อแล้วลองอีกครั้ง |

Mỗi tệp có dòng chú thích `<!-- Buttons and message of the rewarded dialog of :core:ads (spec poses, KB-8; plan T-086). -->`.

**Chuỗi mới của app.** Tiếng Việt đúng bảng chữ của thiết kế (`features/poses/design/README.md` mục 2, `features/camera/design/README.md` mục 2; "premium" như trang 3 của luồng giới thiệu, Nhật chốt 2026-10-31). `poses_unlock_failed` không có frame; README ghi chữ trong `Snackbar`. Không chuỗi nào đi cùng con số.

| Module, tên | en (`values/`) | vi |
| --- | --- | --- |
| designsystem `premium_badge_label` | Premium pose | Tư thế premium |
| designsystem `premium_unlock_title` | Unlock this pose | Mở khóa tư thế |
| designsystem `premium_unlock_message` | Watch a short ad to unlock this pose, then use it for good on this device. | Xem một quảng cáo ngắn để mở tư thế này, rồi dùng mãi về sau trên máy này. |
| poses `poses_detail_unlock` | Watch an ad to use | Xem quảng cáo để dùng |
| poses `poses_unlock_failed` | Couldn\'t unlock the pose. Try again. | Chưa mở khóa được tư thế. Thử lại. |
| camera `camera_cd_thumb_premium` (phần B) | %1$s, premium, tap to unlock | %1$s, premium, chạm để mở khóa |

Năm ngôn ngữ còn lại:

| Tên | hi | pt-BR | es-419 | in | th |
| --- | --- | --- | --- | --- | --- |
| `premium_badge_label` | प्रीमियम पोज़ | Pose premium | Pose premium | Pose premium | ท่าพรีเมียม |
| `premium_unlock_title` | यह पोज़ अनलॉक करें | Desbloquear esta pose | Desbloquear esta pose | Buka pose ini | ปลดล็อกท่านี้ |
| `premium_unlock_message` | इस पोज़ को अनलॉक करने के लिए एक छोटा विज्ञापन देखें, फिर इसे इस डिवाइस पर हमेशा इस्तेमाल करें। | Assista a um anúncio curto para desbloquear esta pose e use-a para sempre neste aparelho. | Mira un anuncio corto para desbloquear esta pose y úsala siempre en este dispositivo. | Tonton iklan singkat untuk membuka pose ini, lalu pakai selamanya di perangkat ini. | ดูโฆษณาสั้นๆ เพื่อปลดล็อกท่านี้ แล้วใช้ได้ตลอดในอุปกรณ์นี้ |
| `poses_detail_unlock` | इस्तेमाल के लिए विज्ञापन देखें | Assista a um anúncio para usar | Mira un anuncio para usar | Tonton iklan untuk memakai | ดูโฆษณาเพื่อใช้ |
| `poses_unlock_failed` | पोज़ अनलॉक नहीं हो सका। फिर से कोशिश करें। | Não foi possível desbloquear a pose. Tente novamente. | No se pudo desbloquear la pose. Vuelve a intentarlo. | Pose belum bisa dibuka. Coba lagi. | ปลดล็อกท่าไม่ได้ ลองอีกครั้ง |
| `camera_cd_thumb_premium` | %1$s, प्रीमियम, अनलॉक करने के लिए टैप करें | %1$s, premium, toque para desbloquear | %1$s, premium, toca para desbloquear | %1$s, premium, ketuk untuk membuka | %1$s, พรีเมียม, แตะเพื่อปลดล็อก |

**Bản staging.** `StagingRemoteValuesModule.appPlacementsShown()` thêm `AdPlacement.RW_PREMIUM_POSE`. `FeatureFlag.PREMIUM_POSES` ở `StagingFeatureFlagsModule` là việc của T-085.

**Bảng khóa.** `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Bảng chỉ thêm `is_show_rw_premium_pose` (`boolean`, `false`, `:core:ads`).

**Sau khi merge (việc của người, không nằm trong PR).** Chạy `python3 scripts/ads-plan.py --keep <file trên Drive>`; dòng `rw_premium_pose` có trạng thái `Đã code`.

#### Phần B: camera

- `PoseUiState.kt`: `PoseThumbItem` thêm `val locked: Boolean = false`; `PoseUiState.Ready` thêm `val unlockPrompt: UnlockPrompt? = null`, `data class UnlockPrompt(val id: String, val name: String)`: tư thế đang chờ mở, để màn hình vẽ tên trong hộp thoại.
- `PoseViewModel.kt`:
  - Nhận thêm `PremiumPoseRepository` (tham số sau `historyRepository`).
  - `private var access = PremiumAccess(enabled = false, unlockedIds = emptySet())`: chưa có giá trị thì không khóa gì; giá trị đầu đọc trước khi chọn tư thế (dưới).
  - `init` thêm `viewModelScope.launch { premiumRepository.access.collect { access = it; publish() } }`.
  - `onLibraryReady` đọc `access = premiumRepository.access.first()` cùng chỗ đọc cài đặt nét viền, rồi chọn:

    ```kotlin
    val requested = savedStateHandle.get<String>(KEY_POSE_ID)?.let { posesById[it] }
    if (requested != null && access.isLocked(requested)) logger.warn(LOG_TAG, "Requested pose is locked")
    val first = requested?.takeUnless(access::isLocked)
        ?: ready.lastUsedPose?.takeUnless(access::isLocked)
        ?: ready.defaultPose
    ```

    Tư thế được yêu cầu (từ chi tiết) chỉ tới sau khi đã mở khóa, nên nhánh khóa của nó chỉ là phòng hờ. `onRequestedPose` (khóa được đặt sau khi mở) làm tương tự: `access.isLocked(pose)` thì log warn, giữ tư thế hiện tại. Camera là đích mới mở từ chi tiết, nên `PoseViewModel` mới đọc `access.first()` trong `onLibraryReady` trước `onRequestedPose`: giá trị đó đã có tư thế vừa mở (DataStore ghi xong trước khi chi tiết điều hướng), nên tư thế được yêu cầu không bị coi là khóa.
  - `onPoseSelect(id)`: `access.isLocked(pose)` thì không chọn, trả về (phòng hờ; ô khóa gọi `onLockedPoseSelect`).
  - `fun onLockedPoseSelect(id: String)`: tư thế có trong catalog và `access.isLocked(pose)` thì `savedStateHandle[KEY_UNLOCK_POSE_ID] = id`, `publish()` (để `unlockPrompt` có tên), log `"Locked pose tapped"`. Không chọn, không ghi `pose_selected`. Tư thế không khóa (vừa mở ở nơi khác) thì như `onPoseSelect`.
  - `fun onUnlockGranted()`:

    ```kotlin
    /** The rewarded action of a locked cell ran (KB-8): unlocks, logs, then selects the pose as a tap on a cell. */
    fun onUnlockGranted() {
        val id = savedStateHandle.get<String>(KEY_UNLOCK_POSE_ID) ?: return
        val pose = posesById[id] ?: return
        if (unlocking) return
        if (!access.isLocked(pose)) {
            // Usable already (flag off, unlocked elsewhere): no write, no event (KB-8, criterion 8).
            savedStateHandle[KEY_UNLOCK_POSE_ID] = null
            select(pose, Source.STRIP)
            return
        }
        unlocking = true
        viewModelScope.launch {
            try {
                if (premiumRepository.unlock(id)) {
                    analytics.log(
                        AnalyticsEvent(
                            EVENT_PREMIUM_POSE_UNLOCKED,
                            mapOf("pose_id" to pose.id, "category" to pose.categoryId, "source" to SOURCE_CAMERA),
                        ),
                    )
                    logger.info(LOG_TAG, "Premium pose unlocked")
                } else {
                    logger.info(LOG_TAG, "Unlock skipped: premium off")
                }
                savedStateHandle[KEY_UNLOCK_POSE_ID] = null
                select(pose, Source.STRIP)
            } catch (e: IOException) {
                // Question 7: the pose stays locked and the current pose stays; the cell keeps its badge.
                savedStateHandle[KEY_UNLOCK_POSE_ID] = null
                logger.warn(LOG_TAG, "Premium pose could not be unlocked", e)
                publish()
            } finally {
                unlocking = false
            }
        }
    }
    ```

    `select` chạy trước khi `access` mới tới (kho giả hay DataStore phát sau): `select` không kiểm khóa, nên tư thế vừa mở được chọn ngay; dấu của ô mất khi `access` phát giá trị mới.
  - `onNextPose()`: bỏ qua tư thế khóa: tư thế không khóa đầu tiên sau tư thế đang chọn, vòng về đầu; không còn tư thế không khóa nào khác thì không làm gì (mục 8, câu 8).
  - `publish()`: `PoseThumbItem(..., locked = access.isLocked(it))`; `unlockPrompt` từ `KEY_UNLOCK_POSE_ID` và tên tư thế (`name.resolve()`).
  - `EVENT_PREMIUM_POSE_UNLOCKED = "premium_pose_unlocked"`, `SOURCE_CAMERA = "camera"`, `KEY_UNLOCK_POSE_ID = "unlockPoseId"`.
- `PosePicker.kt`: `PoseActions` thêm `val onLockedPoseSelect: (String) -> Unit = {}`. Trong `PoseStrip`, ô khóa có `onClick = { actions.onLockedPoseSelect(item.id) }`, `premium = true`, `contentDescription = camera_cd_thumb_premium` (ô lỗi tải ảnh vẫn dùng chuỗi lỗi; ô khóa mà ảnh lỗi: chạm vẫn mở hộp thoại, và tải lại ảnh như cũ).
- `PoseThumb.kt` (`:core:designsystem`): tham số cuối `premium: Boolean = false`; `true` thì vẽ `PremiumBadge(PremiumBadgeSize.Thumb, contentDescription = null, Modifier.align(Alignment.TopStart).offset(x = -BadgeOffset, y = -BadgeOffset))`, đối xứng `SelectedBadge` (`BadgeOffset = 7.dp` có sẵn; `context/CameraScreen_premium_dark.md`: dấu 22 ở (-7, -7) của ô 64). Ô khóa thường không phải ô đang chọn; tư thế đang dùng trở thành khóa giữa phiên (cờ bật từ xa) thì ô có cả hai dấu, ở hai góc, không chồng nhau.
- `pose/LockedPoseUnlock.kt` (mới, rà soát độc lập, điểm chặn): phần nối của ô khóa, tách khỏi `CameraRoute` để test được (không test nào dựng `CameraRoute`: cần bốn ViewModel Hilt, `CameraController`, `PoseFrameSource`):

  ```kotlin
  /** The unlock of a locked cell (spec poses, KB-8; camera KB-1, criterion 8): the rewarded action and the tap. */
  @Stable
  internal class LockedPoseUnlock(private val action: State<RewardedAction>, private val hold: (String) -> Unit) {
      val rewarded: RewardedAction get() = action.value

      /** Holds [id] in the view model first, so an action that runs at once (key off, consent closed) reads it. */
      fun select(id: String) {
          hold(id)
          action.value.start()
      }
  }

  @Composable
  internal fun rememberLockedPoseUnlock(viewModel: PoseViewModel): LockedPoseUnlock {
      val action = rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, onGranted = viewModel::onUnlockGranted)
      val current = rememberUpdatedState(action)
      return remember(viewModel) { LockedPoseUnlock(current, viewModel::onLockedPoseSelect) }
  }

  /** The dialog of [unlock]; inside the dark theme of the camera. */
  @Composable
  internal fun LockedPoseUnlockDialog(unlock: LockedPoseUnlock, poseName: String?) {
      RewardedAdDialog(unlock.rewarded, title = stringResource(DesignR.string.premium_unlock_title)) {
          PremiumUnlockContent(poseName = poseName.orEmpty())
      }
  }
  ```

  `rememberUpdatedState` giữ `RewardedAction` mới nhất cho `PoseActions` đã `remember` (ghi chú của rà soát). Cách nối này (ViewModel giữ tư thế rồi `start()`, không có `LocalAdsController` thì chạy ngay) đã chạy thử với `PoseViewModel` thật và kho giả (mục 4.1).
- `CameraScreen.kt`, `CameraRoute`:
  - `val lockedUnlock = rememberLockedPoseUnlock(poseViewModel)` trước `PoseActions`.
  - `PoseActions(onLockedPoseSelect = lockedUnlock::select, ...)`.
  - Trong `AppTheme(darkTheme = true)`: `LockedPoseUnlockDialog(lockedUnlock, poseName = prompt?.name)`, với `prompt = (poseState as? PoseUiState.Ready)?.unlockPrompt`.
  - Đang đếm ngược thì dải bị khóa (`enabled = false`), nên không mở được hộp thoại lúc đếm (KB-6 giữ nguyên).
- Hộp thoại của khung giữ trạng thái `ASKING` qua dựng lại activity; tư thế chờ nằm trong `SavedStateHandle`, nên sau xoay màn hình `onGranted` vẫn mở đúng tư thế.
- Sau "Để sau" hay đóng quảng cáo sớm, `KEY_UNLOCK_POSE_ID` còn giữ tư thế: vô hại, vì `onUnlockGranted` chỉ chạy khi khung cho thưởng, và lần chạm ô khóa sau ghi đè giá trị. `unlockPrompt` còn nhưng hộp thoại không hiện (khung chỉ vẽ khi `dialogVisible`).
- **Lúc `access` phát `enabled = false` trước rồi `true`** (plan T-085, mục 3.1: trước khi Remote Config tải xong cờ là mặc định tắt, trừ staging; T-085 giao T-086 quyết cách hiện): plan hiện đúng theo giá trị đang có, không giấu hay chờ. Lý do: màn loading chờ Remote Config (tối đa 15 giây, ADR-1001) trước khi vào app, nên lúc thư viện mở thường đã có giá trị thật; bản staging bật cờ cục bộ nên không bao giờ thấy. Trong khoảng ngắn còn lại, tư thế premium hiện như mở; chạm "Chụp" hay ô lúc đó dùng được tư thế (đúng với cờ lúc đó), không ghi gì; khi `true` tới thì dấu hiện. Camera đã chọn tư thế premium lúc cờ tắt thì giữ tới lần mở sau (như tiêu chí 9 của KB-1).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Phần A: `RW_PREMIUM_POSE` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/RewardedAction.kt` | Phần A: `RewardedAdDialogCard` công khai, KDoc (mục 8, câu 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Phần A: dòng `rw_premium_pose` trong tập vị trí của app; test `rwPremiumPose_blocksWithoutAd` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Phần A: thêm `rw_premium_pose=unit/14` vào ba chuỗi cấu hình của `configuredUnits_areParsedPerPlacementKey` và vào `placementUnits` của `missingUnitsWarning_listsMissingKeys` (dữ liệu, ADR-0029; mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerUnitIdsTest.kt` | Phần A: thêm `rw_premium_pose=unit/14` vào `PLACEMENT_UNITS` (dữ liệu, ADR-0029; mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPreloadPlanTest.kt` | Phần A: phép đối chứng của `fromPlacements_hasRewardedInSecondGroup` bỏ cả `RW_PREMIUM_POSE` (dữ liệu, ADR-0029; mục 3.5) |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PremiumBadge.kt` | Phần A: `PremiumBadge`, `PremiumBadgeSize`, `PREMIUM_BADGE_TAG` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PremiumUnlockContent.kt` | Phần A: nội dung hộp thoại mở khóa |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseCard.kt` | Phần A: tham số `premium` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseThumb.kt` | Phần B: tham số `premium` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_premium.xml` | Phần A: icon của dấu |
| Sửa | `core/designsystem/src/main/res/values/strings.xml` | Phần A: `premium_badge_label`, `premium_unlock_title`, `premium_unlock_message` |
| Sửa | `core/designsystem/src/main/res/values-vi/strings.xml` | Phần A: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-hi/strings.xml` | Phần A: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-pt-rBR/strings.xml` | Phần A: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-b+es+419/strings.xml` | Phần A: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-in/strings.xml` | Phần A: ba chuỗi trên |
| Sửa | `core/designsystem/src/main/res/values-th/strings.xml` | Phần A: ba chuỗi trên |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseCardTest.kt` | Phần A: test dấu trên thẻ (cỡ, chỗ đặt, nhãn) |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseComponentsTest.kt` | Phần B: test dấu trên ô |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemStringsTest.kt` | Phần A: ba chuỗi mới vào danh sách dịch (dữ liệu, ADR-0029) |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/PosesLibrarySource.kt` | Phần A: `LibraryContent.lockedIds`, `suspend fun unlock(poseId): Boolean` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPosesLibrarySource.kt` | Phần A: nhận `PremiumPoseRepository`; `combine` với `access`, tính `lockedIds` bằng `access::isLocked`; `unlock` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesUiState.kt` | Phần A: `PoseCardItem.locked` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesViewModel.kt` | Phần A: `locked` của thẻ |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesComponents.kt` | Phần A: `premium = item.locked` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailUiState.kt` | Phần A: `locked`, `unlockFailed` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailViewModel.kt` | Phần A: `onUnlockGranted`, `openCamera`, `onUnlockFailedShown`, `logSelected`, hằng sự kiện |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreen.kt` | Phần A: `rememberRewardedAction`, `RewardedAdDialog`, `openCamera`, snackbar, `UNLOCK` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailComponents.kt` | Phần A: nút mở khóa, dấu ở dòng phụ |
| Tạo | `feature/poses/src/main/res/drawable/ic_poses_smart_display.xml` | Phần A: icon của nút mở khóa (Material Symbols `smart_display`) |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PremiumUnlock.kt` | (dự phòng) Phần A: phần nối của nút mở khóa khi `PoseDetailRoute` không dựng được trong test (mục 3.4) |
| Sửa | `feature/poses/src/main/res/values/strings.xml` | Phần A: `poses_detail_unlock`, `poses_unlock_failed` |
| Sửa | `feature/poses/src/main/res/values-vi/strings.xml` | Phần A: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-hi/strings.xml` | Phần A: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-pt-rBR/strings.xml` | Phần A: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-b+es+419/strings.xml` | Phần A: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-in/strings.xml` | Phần A: hai chuỗi trên |
| Sửa | `feature/poses/src/main/res/values-th/strings.xml` | Phần A: hai chuỗi trên |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesFakes.kt` | Phần A: `FakePosesLibrarySource.unlock` (ghi lại lời gọi, `unlockResult`, `failUnlock`, cổng `CompletableDeferred`, phát nội dung mới không còn id trong `lockedIds`); `testContent(lockedIds = ...)`; `FakePremiumPoseRepository` (`MutableStateFlow<PremiumAccess>`, `unlocks`, `unlockResult`) cho `DefaultPosesLibrarySourceTest` |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPosesLibrarySourceTest.kt` | Phần A: test `lockedIds` (cờ tắt, cờ bật chưa mở, đã mở một, theo `access` mới) và `unlock` chuyển xuống kho (mục 4) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesViewModelTest.kt` | Phần A: test `locked` của thẻ |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Phần A: test dấu trên thẻ, chạm thẻ khóa mở chi tiết |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | Phần A: ảnh `premium` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesStringsTest.kt` | Phần A: hai chuỗi mới vào `TRANSLATED` (dữ liệu, ADR-0029) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailViewModelTest.kt` | Phần A: test mở khóa (mục 4) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Phần A: test nút mở khóa, đường khóa tắt, snackbar |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | Phần A: ảnh `premium` sáng, tối, `_small`; ảnh hộp thoại |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_premium_light.png` | Phần A: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_premium_dark.png` | Phần A: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_premium_light.png` | Phần A: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_premium_dark.png` | Phần A: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_premium_small.png` | Phần A: ảnh chuẩn mới, khung `w360dp-h640dp-xxhdpi` |
| Tạo | `feature/poses/src/test/screenshots/PoseUnlockDialogScreen_default_light.png` | Phần A: ảnh chuẩn mới |
| Tạo | `feature/poses/src/test/screenshots/PoseUnlockDialogScreen_default_dark.png` | Phần A: ảnh chuẩn mới |
| Tạo | `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt` | Phần A: kịch bản emulator (mục 4) |
| Tạo | `docs/features/poses/design/review/PosesScreen_premium_light_compare.png` | Phần A: ảnh so sánh của `scripts/design-compare.sh poses` |
| Tạo | `docs/features/poses/design/review/PosesScreen_premium_dark_compare.png` | Phần A: như trên |
| Tạo | `docs/features/poses/design/review/PoseDetailScreen_premium_light_compare.png` | Phần A: như trên |
| Tạo | `docs/features/poses/design/review/PoseDetailScreen_premium_dark_compare.png` | Phần A: như trên |
| Tạo | `docs/features/poses/design/review/PoseUnlockDialogScreen_default_light_compare.png` | Phần A: như trên |
| Tạo | `docs/features/poses/design/review/PoseUnlockDialogScreen_default_dark_compare.png` | Phần A: như trên |
| Sửa | `docs/features/poses/design/review/summary.md` | Phần A: sinh lại cùng lệnh |
| Sửa | `app/src/main/res/values/strings.xml` | Phần A: ba chuỗi gốc tiếng Anh của hộp thoại khung, `app_name` `translatable="false"` (3.1) |
| Tạo | `app/src/main/res/values-vi/strings.xml` | Phần A: ba chuỗi của hộp thoại khung (bảng ở 3.1) |
| Tạo | `app/src/main/res/values-hi/strings.xml` | Phần A: như trên |
| Tạo | `app/src/main/res/values-pt-rBR/strings.xml` | Phần A: như trên |
| Tạo | `app/src/main/res/values-b+es+419/strings.xml` | Phần A: như trên |
| Tạo | `app/src/main/res/values-in/strings.xml` | Phần A: như trên |
| Tạo | `app/src/main/res/values-th/strings.xml` | Phần A: như trên |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ads/AdsStringsTranslationTest.kt` | Phần A: test chữ hộp thoại khung (mục 4; bản đã chạy thử) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Phần A: `appPlacementsShown()` thêm `RW_PREMIUM_POSE` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Phần A: tập mong đợi thêm `is_show_rw_premium_pose=true` (mục 3.5) |
| Sửa | `docs/remote-config-keys.md` | Phần A: sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Phần A: sinh lại cùng lệnh |
| Sửa | `docs/ops/ads-guide.md` | Phần A: chỉ phần chung của dòng Rewarded, mục 5 (`RewardedAdDialogCard` công khai cho ảnh chuẩn; cách app dịch chữ hộp thoại); không ghi tên vị trí của app (3.1) |
| Sửa | `docs/template-sync.txt` | Phần A: một dòng chú thích trong đoạn "App values kept inside template files": giữ `RewardedAdDialogCard` công khai; không chạm `base=`, `hold=` (ADR-0030) |
| Sửa | `docs/ops/manual-test.md` | Phần A: mục 11 thêm `is_show_rw_premium_pose` vào danh sách khóa không được `false`; mục con kế tiếp còn trống của mục 11 (T-083 cũng thêm mục con, nên không định trước số) cho mở khóa ở chi tiết. Phần B: bước mở khóa ở camera trong cùng mục con |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseUiState.kt` | Phần B: `PoseThumbItem.locked`, `UnlockPrompt` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModel.kt` | Phần B: nhận `PremiumPoseRepository`, theo `access`; `onLockedPoseSelect`, `onUnlockGranted`, tư thế đầu khi khóa, `onNextPose` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PosePicker.kt` | Phần B: `onLockedPoseSelect`, `premium` của ô |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/LockedPoseUnlock.kt` | Phần B: `LockedPoseUnlock`, `rememberLockedPoseUnlock`, `LockedPoseUnlockDialog` (3.1) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Phần B: `rememberLockedPoseUnlock`, `LockedPoseUnlockDialog`, `PoseActions.onLockedPoseSelect` trong `CameraRoute` |
| Sửa | `feature/camera/src/main/res/values/strings.xml` | Phần B: `camera_cd_thumb_premium` |
| Sửa | `feature/camera/src/main/res/values-vi/strings.xml` | Phần B: như trên |
| Sửa | `feature/camera/src/main/res/values-hi/strings.xml` | Phần B: như trên |
| Sửa | `feature/camera/src/main/res/values-pt-rBR/strings.xml` | Phần B: như trên |
| Sửa | `feature/camera/src/main/res/values-b+es+419/strings.xml` | Phần B: như trên |
| Sửa | `feature/camera/src/main/res/values-in/strings.xml` | Phần B: như trên |
| Sửa | `feature/camera/src/main/res/values-th/strings.xml` | Phần B: như trên |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseFakes.kt` | Phần B: `FakePremiumPoseRepository` (`MutableStateFlow<PremiumAccess>`, `unlocks`, `unlockResult`, `failUnlock`, cổng `CompletableDeferred`); hàm dựng tư thế premium (`copy(premium = true)`) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModelTest.kt` | Phần B: hàm dựng `viewModel()` nhận kho giả (dựng test, mục 3.5); test mới (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenPoseTest.kt` | Phần B: test ô khóa trên `CameraScreen` (dấu, mô tả, `onLockedPoseSelect`) |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/LockedPoseUnlockTest.kt` | Phần B: `noAdsController_lockedCellTap_unlocksAndSelectsAtOnce` với `PoseViewModel` thật (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | Phần B: ảnh `premium` |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_premium_dark.png` | Phần B: ảnh chuẩn mới |
| Tạo | `docs/features/camera/design/review/CameraScreen_premium_dark_compare.png` | Phần B: ảnh so sánh của `scripts/design-compare.sh camera` |
| Sửa | `docs/features/camera/design/review/summary.md` | Phần B: sinh lại cùng lệnh |

Nơi khác đọc các file bị đổi (L-020 của mẫu; rà lại trên `9af5928`):

- `scripts/ads-plan.py` đọc `AdPlacement.kt` (dòng một-dòng và chú thích ngay trên), `docs/remote-config-keys.md` và các hằng `EVENT_`: dòng mới và `EVENT_PREMIUM_POSE_UNLOCKED` tự vào file Excel, không sửa script. Test của script đọc code thật, không đếm số vị trí cố định.
- `scripts/release.sh` đọc dòng một-dòng của `AdPlacement`: dòng mới là một dòng, không có tiền tố `gallery_`, nên production cần mã `rw_premium_pose` (đúng ý ticket, T-075). `AdPlacementSourceTest` kiểm luật một-dòng: qua.
- `AdUnitIds.kt` (#279: `ConfiguredAdUnitIds.missing` là `List<String>`, có `missingUnitKeys`): vị trí rewarded không có khe của thanh đáy (`layerFormats` chỉ cho thanh đáy), nên khóa thiếu là `rw_premium_pose`; `AdUnitIdsTest` và `AdsControllerUnitIdsTest` chỉ thêm `rw_premium_pose=unit/14` vào cuối các chuỗi cấu hình (dữ liệu, mục 3.5).
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so `docs/remote-config-keys.md` và `docs/remote-config-template.json` với khóa của code; tập khóa lấy từ `AdsSettings.all()` và `AdPlacement.entries`, không ghi tên vị trí, nên không sửa test; hai tệp được sinh lại trong cùng commit code.
- `scripts/main-guard-verified.sh` (mới ở #279, ADR-0039) có `docs/remote-config-keys.md` và `docs/remote-config-template.json` trong `GRADLE_READ_DOCS`: commit merge khác lần kiểm của PR ở hai tệp này thì `main-guard` kiểm đầy đủ. Script chỉ đọc tên tệp, không đọc nội dung; PR của task có cả code nên dù sao cũng kiểm đầy đủ. Không sửa.
- `docs/template-sync.txt`: `scripts/setup-check-app.sh` chỉ nhắc tên tệp trong một thông báo (`note`), không đọc tệp. `scripts/tests/run.sh` chỉ đọc `template-sync.txt` của repo giả do test dựng (dòng `base=`, `hold=`), không đọc tệp thật của app. Plan chỉ thêm một dòng chú thích `#`, không chạm `base=`, `hold=` (ADR-0030). Không sửa script nào.
- `app/build.gradle.kts` chỉ nêu đường dẫn của `docs/remote-config-keys.md` và `docs/remote-config-template.json` cho tham số `-PremoteConfigKeys.update`; đường dẫn không đổi, nên không sửa.
- `.github/workflows/device-session.yml` đọc `summary.md` của thư mục phiên thử, không đọc `docs/features/camera/design/review/summary.md` hay `docs/features/poses/design/review/summary.md` của gói thiết kế, nên không sửa.
- `RewardedActionTest` gọi `RewardedAdDialogCard` trong cùng module: đổi `internal` thành công khai không ảnh hưởng.
- `PoseCard`, `PoseThumb` có tham số mới cuối, mặc định `false`: mọi chỗ gọi cũ và ảnh chuẩn của `:core:designsystem`, `:feature:poses`, `:feature:camera` không đổi.
- `LibraryContent` có tham số mới có mặc định: `testContent()` và mọi chỗ dựng cũ biên dịch được. `PosesLibrarySource` chỉ có hai bản cài: `DefaultPosesLibrarySource` và `FakePosesLibrarySource` (cả hai trong mục 3.2). `PoseViewModel` chỉ được dựng trong `PoseViewModelTest` (Hilt dựng ở app).
- Tài nguyên của `:app` đè ba chuỗi của `:core:ads` cho cả bản build: không module nào khác đọc tên đó ngoài `:core:ads`. `RewardedAdDialogScreenshotTest` của `:core:ads` dựng trong module của nó, không thấy tài nguyên của `:app`, nên ảnh chuẩn của khung giữ tiếng Anh (chạy thử lại: không ảnh chuẩn nào đổi, mục 4.1).
- `docs/features/poses/design/review/summary.md`, `docs/features/camera/design/review/summary.md`: `scripts/design-compare/DesignCompare.java` ghi các tệp này (PR #278 đã thêm các dòng "thiếu ảnh chuẩn screenshot" của frame premium); plan không sửa script, chỉ chạy nó. `scripts/device-session.sh` và `scripts/tests/run.sh` (khối `device-session`) đọc một `summary.md` khác, của thư mục phiên thử `test-sessions/`; khối `design-compare` của `scripts/tests/run.sh` đọc `summary.md` trong thư mục tạm của chính test. Không tệp nào đọc `summary.md` của gói thiết kế, nên không sửa.
- `docs/remote-config-keys.md`: `scripts/tests/run.sh` (khối `ads-plan`) chạy `scripts/ads-plan.py` trên repo giả do test dựng, không đọc bảng khóa thật của app, nên dòng mới không ảnh hưởng.
- `scripts/check.sh`, `scripts/design-compare.sh` đọc `src/test/screenshots/` và `design/screens/`: sau task mọi ảnh thiết kế premium của poses và camera có screenshot cùng tên.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: `FeatureFlag.PREMIUM_POSES` (`feature_premium_poses_enabled`, mặc định tắt) do T-085 khai báo, gộp vào `PremiumAccess.enabled`. Plan này không thêm cờ, không đọc cờ trực tiếp.
- Quảng cáo: thêm `rw_premium_pose` (rewarded; spec ads câu 12; spec poses mục 5, KB-8), mặc định tắt (ADR-0014), bật ở staging. Khóa `is_show_rw_premium_pose`, sinh từ `AdPlacement`. Không khai báo `noAdPolicy`: chặn (câu 8). Không qua `ads_interval`, không đẩy mốc (khung, R-05). Gói bỏ quảng cáo (`AdFreeStatus`, #279): app gắn `AdFreeStatus.NONE`, nên chưa ai có gói; khi có, `start()` mở khóa ngay (câu 11).
- Sự kiện phân tích: thêm `premium_pose_unlocked` (`pose_id`, `category`, `source`: `detail`, `camera`), đúng spec poses mục 6, câu 11; chỉ ghi khi `unlock` trả `true`. Sự kiện của khung (`reward_dialog`, `ad_reward_earned`, `ad_opportunity`...) tự ghi cho vị trí mới.
- File R2: Không chạm. Catalog có `premium` là của T-085 và người làm nội dung.
- **Quyền truy cập:** Không thêm. Không thêm: `CAMERA` (màn camera đã xin quyền này từ trước, plan không đổi gì ở phần quyền).
- **Thư viện và SDK:** Không thêm. `rememberRewardedAction`, `RewardedAdDialog`, `RewardedAdDialogTestTags` thuộc `:core:ads`, đã trên classpath biên dịch của `:feature:poses` (chạy thử lại sau #279 với AGP 9: biên dịch và chạy qua, mục 4.1) và `:feature:camera` (`ReviewScreen.kt` đã dùng `:core:ads`). `PremiumPoseRepository`, `PremiumAccess` thuộc `:core:poses`, cả hai feature đã phụ thuộc. `Channel` thuộc `kotlinx-coroutines-core`, đã có. `LocalActivity` không dùng trong feature (khung đọc nó bên trong `:core:ads`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** tài nguyên `values-<ngôn ngữ>` của `:app` chỉ có ba chuỗi của `:core:ads` (không có `app_name`), mà lint chạy với `checkDependencies = true`: `MissingTranslation` hay `ExtraTranslation` có thể báo lỗi.
  Chạy thử phiên bản 1, lần 1, đã gặp đúng hai lỗi này; cách chính của mục 3.1 (chuỗi gốc ở `values/` của `:app`, `app_name` không dịch) là cách sửa đã chạy thử lần 2, và chạy lại qua sau #279 (mục 4.1).
- **Điều kiện chuyển:** trên nhánh có code, `:app:lintStagingDebug` vẫn báo `ExtraTranslation` hay `MissingTranslation` ở các tệp này sau cách của mục 3.1.
- **Cách làm thay:** thêm `tools:ignore="ExtraTranslation"` vào thẻ `<resources>` của sáu tệp ngôn ngữ (dòng chú thích nêu lý do: chuỗi của thư viện), không tắt quy tắc lint ở cấu hình chung. Test `AdsStringsTranslationTest` giữ nguyên.
- **Điểm chưa chắc thứ hai:** `PoseDetailRouteTest` dựng `PoseDetailRoute` với ViewModel thật (đã chạy thử trên route hiện có, mục 4.1); route mới thêm `rememberRewardedAction`, `RewardedAdDialog`, `LaunchedEffect` theo `openCamera`.
  - **Điều kiện chuyển:** sau khi thêm các phần đó, test dựng route không chạy được trên Robolectric (lỗi dựng, không phải câu khẳng định đỏ).
  - **Cách làm thay:** tách phần nối thành `detail/PremiumUnlock.kt` (`rememberPremiumUnlock(viewModel, onShootWithPose)`: giữ `rememberRewardedAction`, thu `openCamera`, trả hàm `start`; `PremiumUnlockDialog(...)` vẽ hộp thoại), như `LockedPoseUnlock.kt` của camera; test dựng composable đó với ViewModel thật. Câu khẳng định của test giữ nguyên. Tệp ghi "(dự phòng)" ở mục 3.2.
- **Điểm chưa chắc thứ ba:** T-085 merge với chữ ký khác mục 3.1 (tên, kiểu trả của `unlock`). Không có phương án dự phòng: dừng (mục 7).

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` chạy lại ngày 2026-11-01 trên phiên bản 2, nhánh chính `9af5928` (bỏ kết quả trong `.claude/worktrees/`): 1100 test dùng tên hay tag của các file plan; 31 "Cần xem trước", 473 "Nên xem". Đã đọc các test của nhóm "Cần xem trước" và các test của "Nên xem" nhắc vị trí của app hay luồng rewarded. Phần lớn chỉ khớp vì tên chung (`AdPlacement`, `Loading`, `Ready`) và không đọc vị trí mới hay tham số mới. Mọi hành vi cũ giữ khi tư thế không khóa (kho giả không khóa gì, như cờ tắt), nên câu khẳng định cũ giữ nguyên; các dòng dưới chỉ đổi phần dựng test hay dữ liệu. Ba dòng mới do đợt đồng bộ #279: `AdUnitIdsTest` (nay có ba chuỗi và `missingUnitsWarning_listsMissingKeys`), `AdsControllerUnitIdsTest`, `AdPreloadPlanTest`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Tập vị trí của app có bốn dòng (năm sau T-080) | Thêm `rw_premium_pose` | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` | Giữ câu khẳng định; thêm `"rw_premium_pose" to AdFormat.REWARDED` vào bản đồ mong đợi (dữ liệu, ADR-0029) |
| Mọi vị trí không phải `gallery_` có mã đơn vị trong chuỗi cấu hình thì `missing` rỗng; thiếu hai khóa khe thì `missing == [banner, native_collap]`; thiếu vị trí cả app thì đúng danh sách bảy khóa | Vị trí mới cần mã | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (ba chuỗi), `AdUnitIdsTest.missingUnitsWarning_listsMissingKeys` (`placementUnits`; phép đối chứng "đủ hai khóa khe thì không cảnh báo") | Giữ; thêm `rw_premium_pose=unit/14` vào cuối ba chuỗi và vào cuối `placementUnits` (dữ liệu). Tìm chỗ sửa theo chuỗi (`inter_pose_detail=unit/13`, `PLACEMENT_UNITS`), không theo số dòng: T-080, T-081, T-083 cũng thêm dòng vào các chuỗi này. Thiếu thì `missing` có thêm `rw_premium_pose` và phép đối chứng có cảnh báo |
| Đủ mã của mọi vị trí và hai khóa khe thì lúc khởi động không cảnh báo | Vị trí mới cần mã | `AdsControllerUnitIdsTest.start_withAllUnits_noWarning` (`PLACEMENT_UNITS`) | Giữ; thêm `rw_premium_pose=unit/14` vào `PLACEMENT_UNITS` (dữ liệu). `start_withMissingUnits_warnsOnce` vẫn qua: cảnh báo vẫn chứa `banner, native_collap` |
| Bỏ vị trí rewarded của trang thử thì không nhóm nạp sẵn nào có `REWARDED` | Vị trí rewarded thứ hai của app | `AdPreloadPlanTest.fromPlacements_hasRewardedInSecondGroup` (phép đối chứng `AdPlacement.entries - AdPlacement.GALLERY_RW_ACTION`) | Giữ ý định của phép đối chứng (không vị trí rewarded nào thì không nạp sẵn rewarded); dữ liệu thành `AdPlacement.entries - AdPlacement.GALLERY_RW_ACTION - AdPlacement.RW_PREMIUM_POSE` (dòng enum mới, ADR-0029). Câu khẳng định `REWARDED in groups[1]` giữ |
| Bản staging bật bốn (năm sau T-080) vị trí của app | Thêm `is_show_rw_premium_pose` | `StagingRemoteValuesModuleTest` (tập `expected`) | Giữ; thêm `"is_show_rw_premium_pose" to true` (dữ liệu). `noGalleryNorSamplePlacement_isShown`, `firstOpen_choosesNoStartupAd` không đổi |
| Danh sách chuỗi đã dịch của poses, designsystem | Thêm chuỗi mới | `PosesStringsTest.everyReleaseLanguage_isTranslated` (`TRANSLATED`), `DesignSystemStringsTest` (danh sách) | Giữ; thêm tên chuỗi mới (dữ liệu). `CameraStringsTest` đọc mọi chuỗi bằng phản chiếu, không sửa |
| `PoseDetailScreen` có các callback hiện có | Thêm `onUnlock`, `onUnlockFailedShown`, cả hai mặc định `{}` | `PoseDetailScreenTest`, `PoseDetailScreenScreenshotTest` (mọi lời gọi `PoseDetailScreen(...)`) | Giữ: tham số có mặc định nên lời gọi cũ biên dịch, không đổi dòng dựng test nào |
| Chi tiết: nút chính luôn "Chụp với tư thế này" | Khóa thì nút mở khóa | `PoseDetailScreenTest` (nút chụp, `onShoot`), ảnh `PoseDetailScreen_*` | Giữ: dữ liệu cũ không khóa (`locked = false` mặc định) |
| `onShoot()` ghi `pose_selected` | `onShoot()` gọi `logSelected`, cùng sự kiện | `PoseDetailViewModelTest` (sự kiện `pose_selected`) | Giữ nguyên câu khẳng định |
| `DefaultPosesLibrarySource` có hai tham số, chỉ `map` thư viện | Thêm `PremiumPoseRepository`, `combine` với `access` | Không có test có sẵn (lớp chưa có test) | Test mới `DefaultPosesLibrarySourceTest` (mục 4) |
| Camera: mở lần sau dùng tư thế dùng lần trước | Tư thế đó khóa thì dùng tư thế mặc định | `PoseViewModelTest.lastUsedPose_isSelectedOnOpen_withOutline`, `lastUsedMissingFromCatalog_fallsBackToDefault` | Giữ: kho giả mặc định `PremiumAccess(enabled = false, ...)`, không khóa gì |
| Camera: chạm ô thì chọn tư thế | Ô khóa gọi `onLockedPoseSelect`, không chọn | `PoseViewModelTest.selectFromStrip_logsEvent_marksUsed_resetsStep`, `selectSamePose_doesNothing`, `selectingPose_doesNotReorderStrip`, `CameraScreenPoseTest` (chạm ô) | Giữ: ô không khóa |
| Camera: "Tư thế tiếp" theo dải, vòng về đầu | Bỏ qua tư thế khóa | `PoseViewModelTest.nextPose_followsStrip_andWraps`, `adjustment_isKeptWhenPoseChanges` | Giữ: không tư thế nào khóa |
| Camera: `PoseViewModel` có 10 tham số | Thêm `PremiumPoseRepository` | Hàm `viewModel()` của `PoseViewModelTest` | Dựng test: truyền `FakePremiumPoseRepository()` (ADR-0033/L11, không đổi câu khẳng định) |
| `PosesLibrarySource` có ba thành viên | Thêm `unlock(): Boolean` | `FakePosesLibrarySource` (`PosesFakes.kt`) | Dựng test: cài `unlock` (dữ liệu giả), không đổi câu khẳng định |
| Cỡ thẻ, ô 64 dp, vị trí nút chính và dải (nhóm "Cần xem trước") | Dấu vẽ bên trong vùng ảnh của thẻ, lấn góc ô như `SelectedBadge`, không đổi cỡ; tham số mặc định `false` | `PoseCardTest.skeleton_withOnClick_callsIt`, `skeleton_withoutOnClick_notClickable`, `PoseCardTest.skeleton_sameSizeAsOneLineCard`, `PoseCardTest.imageArea_hasDesignAspectRatio`, `PoseCardTest.saved_buttonFillIsSecondaryContainer_unsavedIsSurface`, `CameraScreenMatchTest.small_matchHintInsidePreview_clearOfStepCardAndZoom`, `PoseComponentsTest.poseThumb_is64dp`, `selectedBadge_is22dpYellow`, `poseThumb_selectedBorder_isYellow3dp`, `PosesScreenTest.skeletonThenImage_keepsCardBounds`, `wideLoading_hasThreeColumns`, `PoseDetailScreenTest.realBottomBar_withoutAds_takesNoSpace`, `PoseDetailScreenTest.wide_imageFailed_retryInLeftColumn`, `CameraScreenPoseTest.small_noPose_hintInsidePreview`, `CameraScreenOverlayTest.outline_largestAndMovedDown_notDrawnOverPickerOrCaptureRow` | Giữ nguyên: dữ liệu không khóa. Test mới `PoseDetailScreenTest.locked_small_unlockButtonFullyVisible` kiểm nút mở khóa ở khung thấp |
| `RewardedAdDialogCard` là `internal`; test của khung đo nút, màu chữ; gói bỏ quảng cáo bật lúc hộp thoại mở thì "Watch ad" chạy hành động (#279) | Công khai, không đổi bố cục hay hành vi | `RewardedActionTest.watchButton_is56dpHigh_fullCardWidth`, `noThanksText_usesOnSurfaceVariant_light_and_dark`, `longContent_small_buttonsInsideWindow`, `realController_adFreeWhileDialogOpen_watchRunsAction` | Giữ nguyên |
| Test khác của `:core:ads` thuộc nhóm "Cần xem trước" (`AdSlotTest`, `BannerAdSizeTest`, `InListAdsTest`, `NativeAdCellTest`, `NativeAdSlotTest`, `NativeEligibleReportTest`, `NativeFlowStepTest`, `NativeFullScreenOverlayTest`), của camera và settings (`CameraScreenTest`, `ReviewScreen*Test`, `Settings*Test`) | Không đổi | Các test đó | Giữ: chỉ khớp vì tên chung; không đọc vị trí rewarded hay tham số mới |

## 4. Kiểm thử

Test viết trước code (bước 3). Khung rỗng của bước 3: `RW_PREMIUM_POSE` có dòng enum (test dữ liệu của `AdPlacementAppTest`, `AdUnitIdsTest`, `AdsControllerUnitIdsTest`, `AdPreloadPlanTest` nhắc hằng enum nên đi cùng commit có dòng enum, như T-080; hai tệp bảng khóa sinh lại trong commit đó để `RemoteConfigKeysTest` không đỏ vì lý do khác), `PremiumBadge`, `PremiumBadgeSize`, tham số `locked`, `premium`, `onUnlockGranted`, `onLockedPoseSelect`, `openCamera`, `unlock` có mà chưa làm gì (`premium` không vẽ gì, `onUnlockGranted` rỗng, `DefaultPosesLibrarySource` nhận kho mà `lockedIds` luôn rỗng, `PoseViewModel` nhận kho mà không đọc `access`), để test biên dịch được rồi đỏ ở câu khẳng định.

Kho giả của hai feature (`FakePremiumPoseRepository`) giữ `access = MutableStateFlow(PremiumAccess(enabled = false, unlockedIds = emptySet()))`, ghi `unlocks`, có `unlockResult = true`, `failUnlock`, cổng `CompletableDeferred` cho test "ghi một lần". Mặc định kho giả trả `unlock` như kho thật khi cờ bật: thêm id vào `unlockedIds` của `access` rồi trả `unlockResult`. Tư thế premium dựng bằng `testPose(...).copy(premium = true)` (plan T-085, mục 3.1).

**Phần A**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Spec ads câu 12; KB-8 tiêu chí 6 (chặn) | Unit test | `AdPlacementAppTest.rwPremiumPose_blocksWithoutAd`: `RW_PREMIUM_POSE.key == "rw_premium_pose"`, `format == REWARDED`, `noAdPolicy == null`, `declaration.effectiveNoAdPolicy == RewardedNoAdPolicy.Block`, không `stagingOnly`, không `appWide`, `placementEnabled(...).name == "is_show_rw_premium_pose"`, mặc định `false`. Khung rỗng đã có dòng enum nên test này qua ngay: phép đột biến là thêm `noAdPolicy = RewardedNoAdPolicy.Allow` vào dòng thì test đỏ |
| KB-8 tiêu chí 1, 8, nguồn thư viện (cờ tắt) | Unit test | `DefaultPosesLibrarySourceTest.flagOff_nothingLocked`: kho thư viện giả phát `LibraryState.Ready(PoseLibrary.from(PoseCatalog(...)))` với `w02`, `c01` premium; kho premium `PremiumAccess(enabled = false, unlockedIds = emptySet())`: `content.lockedIds` rỗng. Cách dựng (`PoseLibrary.from`, `DefaultPosesLibrarySource` trong test của `:feature:poses`) đã chạy thử (mục 4.1) |
| KB-8 tiêu chí 1, nguồn thư viện (cờ bật, chưa mở) | Unit test | `DefaultPosesLibrarySourceTest.flagOn_premiumNotUnlocked_isLocked`: `PremiumAccess(true, emptySet())`: `lockedIds == setOf("w02", "c01")`, tư thế miễn phí không có trong tập. Đối chứng của test cờ tắt ở trên: cùng catalog, chỉ khác `enabled` |
| KB-8 tiêu chí 1, 4, nguồn thư viện (đã mở một) | Unit test | `DefaultPosesLibrarySourceTest.oneUnlocked_isNotLocked_andFollowsAccess`: `PremiumAccess(true, setOf("w02"))`: `lockedIds == setOf("c01")`; kho phát `PremiumAccess(true, setOf("w02", "c01"))`: giá trị kế tiếp của `library` có `lockedIds` rỗng (luồng theo `access`, Turbine hay `take(2).toList()`) |
| `unlock` chuyển xuống kho | Unit test | `DefaultPosesLibrarySourceTest.unlock_returnsRepositoryResult`: `unlockResult = true` thì `unlock("w02") == true`, `unlocks == listOf("w02")`; `unlockResult = false` thì `false` |
| KB-8 tiêu chí 1, thư viện | Unit test | `PosesViewModelTest.lockedPose_cardIsLocked_othersNot`: `testContent(lockedIds = setOf("w03"))` thì thẻ `w03` có `locked = true`, thẻ khác `false`. Đối chứng cùng test: phát lại nội dung với `lockedIds` rỗng thì `w03` thành `false` |
| KB-8 tiêu chí 1, thư viện | Compose UI test | `PosesScreenTest.lockedCard_showsPremiumBadge_andOpensDetail`: thẻ khóa có node `PREMIUM_BADGE_TAG` trong cây con của `poses_card_w03`, thẻ khác không; chạm thẻ khóa gọi `onOpenPose("w03")` |
| KB-8 tiêu chí 1, thành phần thẻ | Compose UI test (`@GraphicsMode(NATIVE)`) | `PoseCardTest.premium_drawsBadge_32dpAtTopLeft_withDescription`: `premium = true` có node `PREMIUM_BADGE_TAG`, `contentDescription` là `premium_badge_label`, cỡ 32 x 32 dp, góc trên trái ở (6, 10) dp so với `POSE_CARD_IMAGE_TAG` (dung sai 0,5 dp), tâm cách mép trái ảnh đúng bằng tâm vòng lưu cách mép phải; `premium = false` không có node |
| Trợ năng của thẻ (L-004) | Compose UI test | `PoseCardTest.premium_mergedNode_readsBadgeOnce`: thẻ có `onClick`, `premium = true`: node gộp của thẻ `assertContentDescriptionEquals("Premium pose")` (một mô tả; tên và dòng phụ là `text`); dấu cũng là một node riêng có tag `pose_premium_badge` (đo trên `AccessibilityNodeInfo` ở mục 4.1). Đã chạy thử |
| KB-8 tiêu chí 1, màu dấu | Compose UI test (`@GraphicsMode(NATIVE)`) | `PoseCardTest.premiumBadge_colors_light_and_dark`: vẽ `decorView` ra `Bitmap` (L-006), điểm giữa vành nền của dấu (lệch 4 dp từ mép vòng, xa icon) là `tertiaryContainer` của theme, sáng và tối |
| KB-8 tiêu chí 1, 2, chi tiết | Unit test | `PoseDetailViewModelTest.lockedPose_isLocked_andSaveStillWorks`: `lockedIds = setOf(poseId)` thì `Ready.locked`; `onToggleSave()` vẫn ghi `setSaved` và `pose_saved` như tư thế thường |
| KB-8 tiêu chí 2 | Compose UI test | `PoseDetailScreenTest.locked_mainButtonIsUnlock_callsOnUnlock`: `locked = true` thì có node `pose_detail_unlock` với chữ `poses_detail_unlock`, không có chữ `poses_detail_shoot`; chạm gọi `onUnlock`, không gọi `onShoot`; có node `PREMIUM_BADGE_TAG` cỡ 20 dp đứng trước chữ dòng phụ (`left` của chữ cách `right` của dấu `Spacing.sm`). Đối chứng: `locked = false` thì nút chụp như cũ, không có dấu |
| KB-8 tiêu chí 2, màn mở gập (L-072: đường `WideDetailLayout`) | Compose UI test (`@Config(qualifiers = "w673dp-h841dp-xxhdpi")`) | `PoseDetailScreenTest.wide_locked_showsUnlockAndBadge`: `locked = true` ở khung mở gập: có node `pose_detail_unlock` (chạm gọi `onUnlock`) và node `PREMIUM_BADGE_TAG`, không có chữ `poses_detail_shoot` |
| KB-8 tiêu chí 4, chi tiết | Unit test | `PoseDetailViewModelTest.unlockGranted_unlocks_logsEvent_thenOpensCamera`: tư thế khóa, `onUnlockGranted()` thì `FakePosesLibrarySource.unlocks == listOf(poseId)`, sự kiện `premium_pose_unlocked` với `pose_id`, `category`, `source = "detail"`, rồi `pose_selected` `source = "detail"`, rồi `openCamera` phát đúng `poseId` một lần; sự kiện mở khóa ghi sau khi `unlock` xong (cổng `CompletableDeferred` của kho giả: trước khi mở cổng chưa có sự kiện, chưa có `openCamera`) |
| KB-8 tiêu chí 8, 10 (cờ vừa tắt: kho trả `false`) | Unit test | `PoseDetailViewModelTest.unlockReturnsFalse_noEvent_stillOpensCamera`: tư thế khóa, `unlockResult = false`: `unlocks == listOf(poseId)`, không `premium_pose_unlocked`, có `pose_selected` `detail`, `openCamera` phát `poseId` một lần, có dòng log `INFO` "Unlock skipped: premium off", không `warn`. Đối chứng là test được thưởng ở trên (`true` thì có sự kiện) |
| KB-8 tiêu chí 4 (một lần) | Unit test | `PoseDetailViewModelTest.unlockGranted_twiceWhileWriting_writesOnce`: hai lần gọi khi lượt ghi đầu đang chờ cổng thì một lần `unlock`, một sự kiện, một lần `openCamera` |
| KB-8 tiêu chí 4, sau khi mở | Unit test | `PoseDetailViewModelTest.afterUnlock_libraryUpdate_clearsLocked`: kho giả phát nội dung không còn id trong `lockedIds` sau khi ghi thì `Ready.locked` thành `false` |
| KB-8 tiêu chí 5 (không thưởng thì không làm gì) | Unit test | `PoseDetailViewModelTest.noGrant_nothingWritten_noNavigation`: mở chi tiết tư thế khóa, không gọi `onUnlockGranted`: không `unlock`, không sự kiện `premium_pose_unlocked`, `openCamera` không phát (đọc bằng `backgroundScope` với `toList`). Khung giữ phần "đóng sớm, Để sau, không có quảng cáo thì không gọi `onGranted`" (`RewardedActionTest`, đã có) |
| KB-8 tiêu chí 8 (cờ tắt) | Unit test | `PoseDetailViewModelTest.notLocked_grant_opensCamera_withoutWriteOrEvent`: `lockedIds` rỗng, `onUnlockGranted()` thì `openCamera` phát `poseId`, không `unlock`, không `premium_pose_unlocked`. Đối chứng là test được thưởng ở trên (tư thế khóa thì có ghi) |
| KB-8 tiêu chí 9 (khóa tắt hay chưa đồng ý: mở ngay) | Compose UI test | `PoseDetailRouteTest.noAdsController_unlockButton_unlocksAtOnce_withoutDialog` (lớp mới trong `PoseDetailScreenTest.kt`, `@GraphicsMode(NATIVE)`): dựng `PoseDetailRoute(onBack, onShootWithPose, viewModel)` với `PoseDetailViewModel` thật và kho giả (tư thế khóa); repo chưa có test nào dựng route, nhưng route hiện có dựng được như vậy (chạy thử, mục 4.1; không được thì phương án dự phòng ở 3.4), không có `LocalAdsController` (như khóa tắt hay cổng đóng: `start()` gọi `onGranted` ngay, đã chạy thử lại sau #279, mục 4.1); chạm `pose_detail_unlock` thì `unlocks == listOf(poseId)`, `onShootWithPose(poseId)` được gọi một lần, không có node `RewardedAdDialogTestTags.CARD` |
| Ghi lỗi (CLAUDE.md, bước 4) | Unit test | `PoseDetailViewModelTest.unlockWriteFails_staysLocked_showsMessage_noEvent`: `failUnlock` thì `Ready.unlockFailed`, `locked` vẫn `true`, không sự kiện, `openCamera` không phát, `logger` có một dòng `warn`; `onUnlockFailedShown()` xóa cờ; gọi lại `onUnlockGranted()` sau khi hết lỗi thì ghi được (người dùng thử lại) |
| Ghi lỗi, màn hình | Compose UI test | `PoseDetailScreenTest.unlockFailed_showsSnackbar_once`: `unlockFailed = true` thì snackbar `poses_unlock_failed` hiện, rồi `onUnlockFailedShown` được gọi |
| KB-8 tiêu chí 3, nội dung hộp thoại | Compose UI test | `PoseDetailScreenTest.unlockContent_showsNameAndMessage`: `PremiumUnlockContent("Tựa lan can")` có chữ tên và chữ `premium_unlock_message`, chữ lời nằm dưới chữ tên, cách `Spacing.sm` |
| Chữ hai nút và thông báo của khung (KB-8 tiêu chí 3, 6) | Unit test (Robolectric) | `AdsStringsTranslationTest` (`:app`): `vi` cho "Xem quảng cáo", "Để sau"; `en` giữ "Watch ad", "No, thanks", "Check your connection and try again"; sáu ngôn ngữ phát hành khác tiếng Anh cho cả ba chuỗi. Bản đã chạy thử lại sau #279 (mục 4.1): bỏ `values-vi` thì hai test đỏ đúng lý do |
| Bản staging | Unit test | `StagingRemoteValuesModuleTest`: tập mong đợi có `is_show_rw_premium_pose=true` (đỏ trước khi thêm vào `appPlacementsShown()`) |
| Bảng khóa | Unit test | `RemoteConfigKeysTest` (có sẵn): đỏ khi thêm dòng enum mà chưa sinh lại; qua sau khi sinh lại |
| Giao diện thư viện có tư thế khóa | Screenshot test | `PosesScreenScreenshotTest`: `PosesScreen_premium_light.png`, `PosesScreen_premium_dark.png`, khung `w411dp-h891dp-xxhdpi`, dữ liệu theo `context/PosesScreen_premium_light.md`: chip "Tất cả", "Tựa lan can", "Nhìn qua vai" (đã lưu), "Ngồi bậc thềm" khóa |
| Giao diện chi tiết khi khóa | Screenshot test | `PoseDetailScreenScreenshotTest`: `PoseDetailScreen_premium_light.png`, `PoseDetailScreen_premium_dark.png`, tư thế theo `context/PoseDetailScreen_premium_light.md` (dòng phụ "Một người · Nữ · Toàn thân · 4 bước") |
| Màn hình thấp (L-028; ticket: ảnh `_small` có nút chính khi còn khóa) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `PoseDetailScreen_premium_small.png`: tư thế khóa có 5 bước và khối lỗi hay gặp; kèm test `PoseDetailScreenTest.locked_small_unlockButtonFullyVisible` (`@Config(qualifiers = "w360dp-h640dp-xxhdpi")`): `boundsInRoot` của `pose_detail_unlock` nằm trọn trong cửa sổ, nội dung cuộn được |
| Hộp thoại mở khóa | Screenshot test | `PoseDetailScreenScreenshotTest`: `PoseUnlockDialogScreen_default_light.png`, `PoseUnlockDialogScreen_default_dark.png`: khung `w411dp-h891dp-xxhdpi`, `Box(fillMaxSize, background, contentAlignment = Center)` chứa `RewardedAdDialogCard(title = premium_unlock_title, onWatch = {}, onNoThanks = {}) { PremiumUnlockContent("Tựa lan can") }` (mục 8, câu 4), đúng frame trừ chiều cao thẻ (thẻ 312 x 296 ở (49,5; 297,5) vì nút "Để sau" của khung giữ vùng chạm 48 dp, frame vẽ 312 x 288 ở (50, 302); nền trơn `background`). Ảnh chạy tiếng Việt như các ảnh khác của lớp (`vi-w411dp-h891dp-xxhdpi`): tiêu đề và nội dung tiếng Việt; hai nút giữ tiếng Anh của khung ("Watch ad", "No, thanks"), vì test của `:feature:poses` không thấy chữ dịch ở `:app`. Ảnh so sánh khác frame đúng ở chữ hai nút; chữ tiếng Việt của hai nút kiểm bằng `AdsStringsTranslationTest` (mục 8, câu 4) |
| Kích thước thẻ hộp thoại | Compose UI test (`@GraphicsMode(NATIVE)`) | `PoseDetailScreenTest.unlockDialogCard_matchesDesign`: cùng dựng như ảnh trên: node `RewardedAdDialogTestTags.CARD` rộng 312 dp, cao 296 dp (dung sai 1 dp), góc trên trái (49,5; 297,5) dp (dung sai 1 dp); phiên bản 3: 296 thay 288 của frame, vì nút "Để sau" của `RewardedAdDialogCard` giữ vùng chạm tối thiểu 48 dp của Material (frame vẽ 40 dp); đo thật ở RUN-20261031-02: cao 296,00003, nội dung 72, nút "Watch ad" 56. Đối chứng: tạm đặt lại 288 thì đỏ |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh poses`, rà soát độc lập theo mục 7 của mẫu thiết kế | `PosesScreen_premium_light`, `PosesScreen_premium_dark`, `PoseDetailScreen_premium_light`, `PoseDetailScreen_premium_dark`, `PoseUnlockDialogScreen_default_light`, `PoseUnlockDialogScreen_default_dark` |
| Ảnh thiết kế đã có, không đổi | Screenshot test có sẵn | Ảnh thiết kế của ticket khác (T-021, T-022, T-024, T-079, T-080): `PoseDetailScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_default_dark`, `PoseDetailScreen_default_dark_unfolded`, `PoseDetailScreen_default_light`, `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_error_dark`, `PoseDetailScreen_error_light`, `PoseDetailScreen_loading_dark`, `PoseDetailScreen_loading_light`, `PosesScreen_ad-collection_light`, `PosesScreen_ad_dark`, `PosesScreen_ad_light`, `PosesScreen_default_dark`, `PosesScreen_default_dark_unfolded`, `PosesScreen_default_light`, `PosesScreen_default_light_unfolded`, `PosesScreen_error_dark`, `PosesScreen_error_light`, `PosesScreen_loading_dark`, `PosesScreen_loading_light`, `PosesScreen_saved-empty_dark`, `PosesScreen_saved-empty_light` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `PoseDetailScreenTest.locked_mainButtonIsUnlock_callsOnUnlock` và `unlockContent_showsNameAndMessage` đọc chữ qua `stringResource`; `PosesStringsTest`, `DesignSystemStringsTest` thêm một câu: giá trị tiếng Việt của `poses_detail_unlock`, `premium_badge_label`, `premium_unlock_title`, `premium_unlock_message` đúng chữ của bảng chữ (README mục 2) |
| KB-8 tiêu chí 2, 3, 5, 9 trên máy | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt`) | Tệp kịch bản dưới; `device-session` cuối phiên chạy tệp này. Catalog staging chưa có tư thế premium (nội dung sau T-085) thì tệp `bỏ qua (môi trường)` ở bước `need` |
| KB-8 tiêu chí 4, 5, 6 với quảng cáo thử thật | Kiểm tra thủ công (máy thật: quảng cáo thật) | `manual-test.md`, mục con mới của mục 11: bản staging, đã đồng ý; mở chi tiết tư thế premium, bấm nút mở khóa, "Xem quảng cáo", xem quảng cáo thử có thưởng tới hết (không bấm vào quảng cáo): camera mở với tư thế đó, dấu mất ở thư viện; tư thế khác: đóng quảng cáo sớm thì vẫn khóa; "Để sau" thì vẫn khóa; chế độ máy bay rồi "Xem quảng cáo" thì vẫn khóa, có thông báo kiểm tra kết nối (tiếng Việt khi máy để tiếng Việt). `is_show_rw_premium_pose` nằm ở `StagingRemoteValuesModule`, nên tắt được trên Firebase staging cho bước "khóa tắt thì mở ngay"; cờ `feature_premium_poses_enabled` bật sẵn ở `StagingFeatureFlagsModule` (T-085), Firebase không tắt được, nên nhánh cờ tắt kiểm bằng test, không bằng tay |
| KB-8 tiêu chí 7 (giữ qua lần mở sau) | Kiểm tra thủ công (máy thật: khác, cần đóng hẳn app và mở lại trên bản staging thật cùng catalog R2 của T-085) | Sau bước trên, vuốt app khỏi danh sách gần đây, mở lại: tư thế vẫn không có dấu ở thư viện, chi tiết, camera |
| Sự kiện `premium_pose_unlocked` tới Firebase | Kiểm tra thủ công (máy thật: Remote Config) | Bản staging không gửi số liệu (ADR-0012); kiểm bằng log `VGL.Analytics` hay DebugView khi có bản dùng Firebase thật; ghi kết quả vào nhật ký |

Tệp kịch bản `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt` (chạy `scripts/device-scenario.py check` ở bước 3 của task; đã kiểm ở mục 4.1):

```text
ticket: T-086
criterion: Tư thế premium chưa mở khóa mở được chi tiết, nút chính là nút mở khóa; bấm nút thì hộp thoại mở khóa hiện, hay tư thế mở ngay khi cổng đồng ý đóng; "Để sau" giữ khóa (spec poses KB-8, tiêu chí 2, 3, 5, 9)
timeout: 240
fresh
consent decline 30
# Onboarding of T-082 (3 or 4 pages): Next on each page, then Enable camera; a camera already granted skips it.
wait id=onboarding_page_intro | id=camera_screen 60
tap_if id=onboarding_next
wait id=onboarding_page_poses | id=camera_screen 10
tap_if id=onboarding_next
wait id=onboarding_page_premium | id=onboarding_page_camera | id=camera_screen 10
tap_if id=onboarding_next
wait id=onboarding_page_camera | id=camera_screen 10
tap_if id=onboarding_enable_camera
wait id=camera_screen 15
need id=camera_poses 15
tap id=camera_poses
wait id=poses_grid 20
# The badge is a node of its own (resource-id pose_premium_badge, plan T-086 section 4.1); it takes no tap, so a tap
# on it lands on its card.
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

- Luồng giới thiệu là của T-082 (plan đã duyệt, chạy trước task này): "Tiếp" ở mỗi trang, trang premium chỉ có khi cờ bật, rồi "Bật camera". Tag `onboarding_page_*`, `onboarding_next` lấy từ kịch bản của plan T-082 (mục 4). `fresh` cấp quyền camera, nên app có thể vào thẳng camera; mọi bước giới thiệu là `tap_if` hay `wait` có nhánh `camera_screen`.
- Chọn thẻ khóa: dấu có `contentDescription` trên thẻ nên là một node riêng trong cây trợ năng, `resource-id` `pose_premium_badge` (đo trên `AccessibilityNodeInfo`, mục 4.1). Dấu không nhận chạm, nên chạm vào tâm của nó rơi vào thẻ bọc nó và mở chi tiết.
- `fresh` đặt ngôn ngữ app là tiếng Anh, nên `str=ads_reward_watch` khớp "Watch ad". Bộ chọn dùng chữ, không dùng `RewardedAdDialogTestTags.CARD`, vì hộp thoại là cửa sổ riêng và tag của nó có thể không thành `resource-id`.
- Catalog staging chưa có tư thế premium thì tệp `bỏ qua (môi trường)` ở bước `need`.

**Phần B**

Dữ liệu camera là `PoseFixtures` của `:feature:camera` (`w01, m01, w02, m02, w03`; mặc định `w01`); tư thế khóa trong các test dưới là `m02` (`PoseFixtures.pose("m02").copy(premium = true)`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Camera KB-1 tiêu chí 8, dấu trên ô | Unit test | `PoseViewModelTest.lockedPose_cellIsLocked_andUpdatesWhenUnlockedElsewhere`: catalog có `m02` premium, kho giả `PremiumAccess(true, emptySet())` thì ô `m02` `locked`, ô khác không; kho phát `PremiumAccess(true, setOf("m02"))` (mở ở chi tiết) thì ô thành không khóa, dải giữ thứ tự |
| Camera KB-1 tiêu chí 8, chạm ô khóa | Unit test | `PoseViewModelTest.lockedPoseTap_keepsSelection_noEvent_setsPrompt`: tư thế đang chọn `w01`, `onLockedPoseSelect("m02")` thì `selected.id == "w01"`, không `pose_selected`, không `markUsed`, `unlockPrompt == UnlockPrompt("m02", <tên>)`; `onPoseSelect("m02")` cũng không đổi tư thế (phòng hờ) |
| Camera KB-1 tiêu chí 8, mở khóa xong | Unit test | `PoseViewModelTest.unlockGranted_unlocks_logsCameraEvent_thenSelects`: sau `onLockedPoseSelect("m02")`, `onUnlockGranted()` thì `unlocks == listOf("m02")`, `premium_pose_unlocked` với `pose_id`, `category`, `source = "camera"`, rồi `selected.id == "m02"`, `pose_selected` `source = "strip"`, `markUsed("m02")`, `unlockPrompt == null` |
| Camera, cờ vừa tắt: kho trả `false` | Unit test | `PoseViewModelTest.unlockReturnsFalse_noEvent_stillSelects`: tư thế khóa, `unlockResult = false`: `unlocks == listOf("m02")`, không `premium_pose_unlocked`, `selected.id == "m02"`, `pose_selected` `strip`, `unlockPrompt == null`, có dòng `INFO` "Unlock skipped: premium off". Đối chứng là test trên |
| Camera KB-1 tiêu chí 8, ghi lỗi | Unit test | `PoseViewModelTest.unlockWriteFails_keepsPose_noEvent`: `failUnlock` thì tư thế cũ giữ, không sự kiện, một dòng `warn`; ô vẫn khóa; `unlockPrompt == null` |
| Camera, cờ tắt | Unit test | `PoseViewModelTest.notLocked_grant_selectsWithoutWriteOrEvent`: `PremiumAccess(false, emptySet())`, `onLockedPoseSelect("m02")` rồi `onUnlockGranted()` thì chọn `m02`, không `unlock`, không `premium_pose_unlocked` |
| Camera, dựng lại | Unit test | `PoseViewModelTest.pendingUnlock_survivesRecreation`: `onLockedPoseSelect("m02")` rồi dựng ViewModel mới với cùng `SavedStateHandle`: `onUnlockGranted()` mở đúng `m02` |
| Camera KB-1 tiêu chí 9 | Unit test | `PoseViewModelTest.lastUsedLocked_fallsBackToDefault`: lịch sử dùng gần nhất `m02`, `m02` premium, `PremiumAccess(true, emptySet())` thì mở với `defaultPose` (`w01`), không sự kiện. Đối chứng cùng test: `PremiumAccess(true, setOf("m02"))` thì mở `m02` |
| Camera, tư thế được yêu cầu bị khóa (phòng hờ) | Unit test | `PoseViewModelTest.requestedLocked_isIgnored_andLogsWarning`: `KEY_POSE_ID = "m02"`, `m02` khóa thì không chọn `m02`, có một dòng `warn` "Requested pose is locked" |
| "Tư thế tiếp" (mục 8, câu 8) | Unit test | `PoseViewModelTest.nextPose_skipsLockedPoses`: dải `w01, m01, w02, m02, w03`, `m01` khóa, đang ở `w01` thì `onNextPose()` chọn `w02`; mọi tư thế khác khóa thì không đổi |
| Camera KB-1 tiêu chí 8, màn hình | Compose UI test | `CameraScreenPoseTest.lockedCell_showsBadge_tapCallsLockedSelect`: ô khóa có `PREMIUM_BADGE_TAG` trong cây con của `camera_pose_m02`, `contentDescription` là `camera_cd_thumb_premium`; chạm gọi `onLockedPoseSelect("m02")`, không gọi `onPoseSelect` |
| Thành phần ô | Compose UI test (`@GraphicsMode(NATIVE)`) | `PoseComponentsTest.thumb_premium_drawsBadge_22dpOverTopLeft`: `PoseThumb(premium = true)` có node dấu 22 x 22 dp, góc trên trái ở (-7, -7) dp so với ô (đối xứng `SelectedBadge`, `context/CameraScreen_premium_dark.md`); `false` không có |
| Trợ năng của ô (L-004) | Compose UI test | `PoseComponentsTest.thumb_premium_readsDescriptionOnce`: `PoseThumb(premium = true, contentDescription = "Lean, premium, tap to unlock")`: node gộp của ô `assertContentDescriptionEquals("Lean, premium, tap to unlock")` (một mô tả, dấu không thêm). Đã chạy thử; đối chứng cùng lớp: một ô `selectable` chứa `PremiumBadge` có mô tả thì node gộp có hai mô tả |
| Camera KB-1 tiêu chí 8, khóa tắt hay chưa đồng ý (phần nối) | Compose UI test (`@GraphicsMode(NATIVE)`) | `LockedPoseUnlockTest.noAdsController_lockedCellTap_unlocksAndSelectsAtOnce` (lớp mới): `setContent` dựng `rememberLockedPoseUnlock(viewModel)`, một nút gọi `select("m02")` và `LockedPoseUnlockDialog(unlock, null)`; `PoseViewModel` thật với các kho giả như `PoseViewModelTest` và `FakePremiumPoseRepository` (`m02` khóa), không có `LocalAdsController`. Chạm nút thì `unlocks == listOf("m02")`, tư thế đang chọn thành `m02`, không có node `RewardedAdDialogTestTags.CARD`. Cách dựng đã chạy thử (mục 4.1): bỏ `start()` thì đỏ `expected:<[m02]> but was:<[w01]>` |
| Camera KB-8 tiêu chí 4 (một lần) | Unit test | `PoseViewModelTest.unlockGranted_twiceWhileWriting_writesOnce`: `onLockedPoseSelect("m02")`, rồi hai lần `onUnlockGranted()` khi kho giả đang chờ cổng `CompletableDeferred`: một lần `unlock`, một `premium_pose_unlocked`, một `pose_selected` sau khi mở cổng |
| Giao diện dải có ô khóa | Screenshot test | `CameraScreenScreenshotTest`: `CameraScreen_premium_dark.png`, dữ liệu theo `context/CameraScreen_premium_dark.md` (ô 4 và 6 khóa, ô 2 đang chọn) |
| Đúng thiết kế | Screenshot test cùng tên, `scripts/design-compare.sh camera` | `CameraScreen_premium_dark` |
| Camera KB-1 tiêu chí 8 với quảng cáo thử | Kiểm tra thủ công (máy thật: quảng cáo thật) | `manual-test.md`, mục con mới của mục 11, bước camera: chạm ô có dấu ở dải, hộp thoại hiện, tư thế đang dùng không đổi; "Xem quảng cáo", xem hết: camera dùng tư thế đó; tư thế khác: "Để sau" thì không đổi |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085); `scripts/plan-check.py` báo lỗi dòng thiếu khi plan còn `draft` (quy tắc 11).

**Ngoại lệ viết test trước:**

- Ba tệp bảng khóa sinh lại, ảnh chuẩn và ảnh so sánh mới: sinh ra từ code, không viết trước được.
- Tệp kịch bản `T-086-mo-khoa-chi-tiet.txt`: chạy ở `device-session` cuối phiên, không "thất bại trước" được.
- Phần nối ở `CameraRoute`: chỉ còn hai dòng gọi `rememberLockedPoseUnlock` và `LockedPoseUnlockDialog`; phần nối có test ở `LockedPoseUnlockTest`. Đường hộp thoại thật (khóa bật, đã đồng ý) không dựng được trong test của feature (`RewardedAction` có hàm dựng `internal`), nên kiểm bằng tay ở `manual-test.md` và bằng test của khung.
- Tương tự ở `PoseDetailRoute`: đường hộp thoại thật chỉ kiểm bằng tay.

### 4.1. Chạy thử

**Lần chạy thử sau đồng bộ #279 (phiên bản 2, 2026-11-01).** Dựng lại đúng code chạy thử của phiên bản 1 trên nhánh tạm `tmp/T-086-try` trong worktree của phiên soạn plan, từ nhánh plan đã merge `origin/main` `9af5928`; code tạm không commit, nhánh tạm xóa sau khi chạy. Worktree tạm riêng (`../plan-T-086-try`) không dùng vì phiên bị chặn tạo worktree ngoài thư mục của nó.

- **Đã chạy thử:** hai điểm lần đầu có trong dự án:
  1. `:app` dịch chuỗi của `:core:ads` bằng tài nguyên cùng tên trong `values-<ngôn ngữ>`. Code: đúng sáu tệp, bảng chữ và khối `values/strings.xml` ở mục 3.1.
  2. Một module feature gọi `rememberRewardedAction` và `RewardedAdDialog`; chưa feature nào gọi, chỉ có test của `:core:ads`. Lần này kiểm thêm trên AGP 9, compileSdk 37 của #279.

  Test tạm:
  - `app/src/test/.../ads/AdsStringsTranslationTest.kt` (3 test): `vietnamese_usesTheAppTranslation`, `english_keepsTheFrameworkText`, `everyReleaseLanguage_isTranslated`. Nội dung đúng như tệp sẽ tạo.
  - `feature/poses/src/test/.../trial/RewardedTrialTest.kt` (3 test):
    - `noController_start_grantsAtOnce_noDialog`: trong `:feature:poses`, `rememberRewardedAction(AdPlacement.GALLERY_RW_ACTION, onGranted = { granted++ })` và `RewardedAdDialog(current, title = "Unlock")`; chạm nút gọi `start()` thì `granted == 1`, không có node `RewardedAdDialogTestTags.CARD`. Vị trí `gallery_rw_action` thay cho `rw_premium_pose` chưa có.
    - `featureTest_vi_seesFrameworkEnglishButtons` (mới, cho mục 8 câu 4): ở `vi-w411dp-h891dp-xxhdpi`, `ads_reward_watch` đọc trong test của `:feature:poses` là "Watch ad".
    - `librarySource_readyFromPublicLibraryFactory` (mới, cho `DefaultPosesLibrarySourceTest`): dựng `DefaultPosesLibrarySource` với kho thư viện giả phát `LibraryState.Ready(PoseLibrary.from(PoseCatalog("test", ..., PosesFixtures.POSES)))` và kho lịch sử giả; `library.first()` là `Ready` có `totalPoses == 7`. Chưa có tham số `PremiumPoseRepository` (T-085 chưa có code).
- **Lệnh:**
  - `./gradlew -q :app:testStagingDebugUnitTest --tests com.vglstudio.camera.pose.line.guide.ads.AdsStringsTranslationTest :feature:poses:testDebugUnitTest --tests 'com.vglstudio.camera.pose.line.guide.feature.poses.trial.RewardedTrialTest'` (05:05 tới 05:20 UTC; lần đầu 04:58 dừng vì máy chưa có SDK Platform 37, Gradle tự cài rồi báo `Failed to find target with hash string 'android-37'`, chạy lại thì qua).
  - Đối chứng: tạm chuyển `app/src/main/res/values-vi/` ra ngoài repo, chạy lại test của `:app` (05:20 tới 05:22 UTC), rồi trả lại.
  - `scripts/check.sh` qua khóa chung của các phiên soạn plan (`check-locked.sh`), một lần, với đủ code tạm ở trên (05:23 tới 05:37 UTC, kể cả lúc chờ khóa).
- **Kết quả:**
  - Lệnh Gradle: `AdsStringsTranslationTest` 3/3 qua; `RewardedTrialTest` 3/3 qua. `:feature:poses` biên dịch được với `rememberRewardedAction`, `RewardedAdDialog`, `RewardedAdDialogTestTags`, `PoseLibrary.from`, `PoseCatalog` trên bộ build mới, không cần thêm phụ thuộc.
  - Đối chứng: 2 trong 3 test đỏ đúng lý do: `vietnamese_usesTheAppTranslation` (`expected:<[Xem quảng cáo]> but was:<[Watch ad]>`) và `everyReleaseLanguage_isTranslated` (`vi ads_reward_watch is not translated. Actual: Watch ad`); `english_keepsTheFrameworkText` vẫn xanh. Giống hệt phiên bản 1.
  - `scripts/check.sh`: `Cổng chất lượng: QUA`, `check.sh exit=0`. Test script qua; test JVM 2708, 0 lỗi (`./app` 114, gồm 3 test tạm; `./feature/poses` 173, gồm 3 test tạm; `./core/ads` 1374). 140 ảnh screenshot; `git status --short -- '*/src/test/screenshots/*'` rỗng, không ảnh chuẩn nào đổi (ảnh `RewardedAdDialog*` của `:core:ads` giữ tiếng Anh). ktlint, `:app:lintStagingDebug` (không `ExtraTranslation`, không `MissingTranslation` với khối `values/strings.xml` của mục 3.1) và build staging qua. Cách chính của mục 3.1 vẫn đúng sau #279; phương án dự phòng của mục 3.4 chưa cần.
  - Tệp tạm đã xóa sau khi chạy; nhánh plan chỉ có plan.

**Lần chạy thử theo rà soát độc lập (2026-11-01, phiên bản 2).** Nhánh tạm `tmp/T-086-try2` từ nhánh plan (`9af5928` cộng plan), trong worktree của phiên soạn plan; code tạm không commit, nhánh tạm xóa sau khi chạy.

- **Đã chạy thử:**
  - Code thật của plan ở `:core:designsystem`: `PremiumBadge.kt` đúng mục 3.1 (có `contentDescription`), `PoseCard(premium)` với dấu ở `offset(6.dp, 10.dp)` và nhãn `premium_badge_label`, `PoseThumb(premium)` với dấu ở `(-7, -7)` và `contentDescription = null`, chuỗi `premium_badge_label` ở 7 thư mục. `ic_premium.xml` tạm dùng hình của `ic_check.xml`; task lấy hình `workspace_premium`.
  - `PremiumTrialTest` (`:core:designsystem`, `@GraphicsMode(NATIVE)`, 3 test):
    - `card_mergedDescription_andBadgeNode`: node gộp của thẻ có đúng một mô tả "Premium pose"; dấu ở (6, 10) dp trong ảnh, cỡ 32 dp; trên `AccessibilityNodeInfo` (gốc có `testTagsAsResourceId`), dấu là một node riêng có `viewIdResourceName` `pose_premium_badge` và mô tả "Premium pose".
    - `thumb_nullBadgeDescription_readsOnce`: node gộp của ô có đúng một mô tả (của ô).
    - `control_badgeWithDescription_insideSelectable_readsTwice` (đối chứng): ô `selectable` chứa `PremiumBadge` có mô tả thì node gộp có hai mô tả. Đây là lỗi đọc trùng mà `contentDescription = null` tránh.
  - `LockedPoseUnlockTrialTest` (`:feature:camera`, 1 test): bản thay tạm của `rememberLockedPoseUnlock` (giữ tư thế rồi `start()` của `rememberRewardedAction(GALLERY_RW_ACTION)`, `RewardedAdDialog`, `rememberUpdatedState`) với `PoseViewModel` thật và kho giả của `PoseViewModelTest`, không `LocalAdsController`. Chạm nút thì tư thế đang chọn đổi từ `w01` sang `m02`, không có node `RewardedAdDialogTestTags.CARD`. `onPoseSelect` thay cho `onLockedPoseSelect`, `onUnlockGranted` chưa có.
  - `PoseDetailRouteTrialTest` (`:feature:poses`, 1 test): `PoseDetailRoute(onBack, onShootWithPose, viewModel)` với `PoseDetailViewModel` thật và kho giả, không `LocalAdsController`; chạm "Shoot with this pose" thì `onShootWithPose("w01")` chạy một lần.
  - Tệp kịch bản `T-086-mo-khoa-chi-tiet.txt` (mục 4) qua `scripts/device-scenario.py check`.
- **Lệnh:**
  - `./gradlew -q :core:designsystem:testDebugUnitTest --tests '*PremiumTrialTest' :feature:camera:testDebugUnitTest --tests '*LockedPoseUnlockTrialTest' :feature:poses:testDebugUnitTest --tests '*PoseDetailRouteTrialTest' -i` (05:59 tới 06:00 UTC); lần đó hai test feature đỏ vì dựng ảnh ở chế độ đồ họa mặc định (`Bitmap.setHasAlpha` trên `null`), thêm `@GraphicsMode(NATIVE)` rồi chạy lại hai lớp đó (06:00 tới 06:01 UTC). Test thật của plan ghi `@GraphicsMode(NATIVE)` (mục 4).
  - Đối chứng camera: bỏ `current.start()` trong bản thay tạm, chạy lại `LockedPoseUnlockTrialTest`.
  - `python3 scripts/device-scenario.py check feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt`, lần đầu với một tệp tạm khai các tag của T-082 và `pose_detail_unlock` (chưa có trong code), lần sau không có tệp tạm (đối chứng).
  - `scripts/check.sh` qua khóa chung (`check-locked.sh`), với mọi code tạm của lần này, trừ tệp kịch bản và tệp tag tạm (06:03 tới 06:24 UTC, kể cả lúc chờ khóa).
- **Kết quả:**
  - `PremiumTrialTest` 3/3, `LockedPoseUnlockTrialTest` 1/1, `PoseDetailRouteTrialTest` 1/1 qua. In ra: `card badge offset=(6.0, 10.0) size=32.0`, `badge a11y=(Premium pose, pose_premium_badge)`. `AccessibilityNodeInfo.contentDescription` của chính node thẻ và node ô là `null` trên Robolectric (mô tả nằm ở cây ngữ nghĩa gộp); vì vậy test thật khẳng định trên node gộp (`assertContentDescriptionEquals`), và kịch bản chọn bằng node dấu, không bằng `desc=` của thẻ.
  - Đối chứng camera: đỏ đúng lý do, `expected:<[m02]> but was:<[w01]>`.
  - `device-scenario.py check`: exit 0 khi có tag; không có tệp tag tạm thì exit 1, chỉ báo các tag chưa có (`onboarding_page_*`, `onboarding_next` của T-082, `pose_detail_unlock`); `pose_premium_badge` (đã có trong code tạm) không bị báo.
  - `scripts/check.sh`: `Cổng chất lượng: QUA`, `check.sh exit=0`. Test JVM 2707, 0 lỗi (`./core/designsystem` 119, gồm 3 test tạm; `./feature/camera` 464, gồm 1; `./feature/poses` 171, gồm 1). 140 ảnh screenshot; `git status --short -- '*/src/test/screenshots/*'` rỗng: tham số `premium` mặc định `false` giữ mọi ảnh chuẩn cũ của `PoseCard`, `PoseThumb`. ktlint, lint (chuỗi `premium_badge_label` đủ 7 ngôn ngữ) và build staging qua.

**Lần chạy thử của phiên bản 1 (2026-10-31, commit gốc `4a5ee7e`, trước #279; giữ để đối chiếu).** Cùng hai điểm, chạy trong worktree của nhánh plan.

- `./gradlew -q :app:testStagingDebugUnitTest --tests ...AdsStringsTranslationTest :feature:poses:testDebugUnitTest --tests ...RewardedTrialTest` (17:38 tới 17:40 UTC): 3/3 và 1/1 qua. Đối chứng bỏ `values-vi`: 2 trong 3 đỏ đúng lý do.
- `scripts/check.sh` lần 1 (17:43 tới 17:50 UTC, chỉ có sáu tệp ngôn ngữ): thất bại ở `:app:lintStagingDebug`, 19 lỗi: 18 `ExtraTranslation` ("`ads_reward_watch` is translated here but not found in default locale", ba chuỗi trong sáu tệp) và 1 `MissingTranslation` (`app_name` không có bản dịch). Đúng điểm chưa chắc của mục 3.4.
- Sửa: `app/src/main/res/values/strings.xml` thêm ba chuỗi gốc tiếng Anh và `translatable="false"` cho `app_name` (khối XML ở mục 3.1).
- `scripts/check.sh` lần 2 (17:51 tới 17:58 UTC): `Cổng chất lượng: QUA`, exit 0. Test JVM 2428, 0 lỗi; 138 ảnh screenshot, không ảnh chuẩn nào đổi.

**Chưa chạy thử:** phần dữ liệu của T-085 (chưa có code trên nhánh chính: `PremiumAccess`, `PremiumPoseRepository`, `Pose.premium`); màu dấu, ảnh chuẩn và hộp thoại có `PremiumUnlockContent` (mẫu `Box`, `CircleShape`, `PrimaryButton` có icon đã có trong hệ thống thiết kế; cỡ, chỗ đặt và trợ năng của dấu đã chạy thử); đường hộp thoại thật với `AdsController` (không dựng được trong feature; khung đã có test). Phần còn lại là mẫu đã có: `combine` luồng, `Channel` cho sự kiện một lần, `SavedStateHandle`, snackbar như `saveFailed`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (thêm vị trí `rw_premium_pose`, điểm 1); thu thập dữ liệu (thêm sự kiện `premium_pose_unlocked`, điểm 4); module core dùng chung (`:core:ads` đổi `RewardedAdDialogCard` sang công khai, `:core:designsystem` thêm thành phần, điểm 7). Đổi hành vi chọn tư thế ở camera, màn chính. Rủi ro Cao, cần duyệt chéo (Tuấn và Nhật). Điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads`, `:core:designsystem`, `:feature:poses`, `:feature:camera`, `:app` (tài nguyên, staging), tài liệu; đổi giao diện công khai (`PoseCard`, `PoseThumb`, `PosesLibrarySource`, `PoseActions`); một lượt cỡ L |
| B. Độ rõ của yêu cầu | 1 | Spec đã chốt, thiết kế đã duyệt (PR #278), API của T-085 đã duyệt; còn vài lựa chọn ở mục 8 (interstitial sau mở khóa, "Tư thế tiếp", chữ tiếng Anh, gói bỏ quảng cáo, tách ticket) |
| C. Khả năng kiểm chứng tự động | 1 | ViewModel, nguồn thư viện và màn hình có test với kho giả; đường hộp thoại thật và quảng cáo thử kiểm bằng tay (khung đã có test) |
| D. Ảnh hưởng chéo | 2 | Sửa `AdPlacement.kt`, `PoseCard.kt`, bảng khóa, `StagingRemoteValuesModule.kt`, test của `:core:ads` và `:feature:poses` mà T-080, T-081, T-083 cũng sửa; dùng API của T-085 chưa merge |
| E. Độ mới của kỹ thuật | 2 | Lần đầu feature dùng rewarded của khung, lần đầu `:app` dịch chuỗi của thư viện; đã chạy thử cả hai, và chạy lại sau #279 (mục 4.1) |
| **Tổng** | 9 (tham khảo) | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt`, `RewardedAction.kt` (`:core:ads`); `PoseCard.kt`, `PoseThumb.kt`, chuỗi của `:core:designsystem`; bảng khóa; `StagingRemoteValuesModule.kt`; tài nguyên đè của `:app`; `docs/ops/ads-guide.md`, `docs/ops/manual-test.md`.
- Task khác có thể xung đột:
  - **T-080** (approved phiên bản 2 trước #279; đang soạn phiên bản 4 song song sau #279): cùng sửa `AdPlacement.kt` (dòng `POSES_IN_COLLECTION`, plan này thêm dòng sau), `AdPlacementAppTest.kt`, `AdUnitIdsTest.kt` (T-080 phiên bản 2 ghi `unit/12`, đã trùng với `review_interstitial`; plan này dùng `unit/14`, mã trong test chỉ là chữ, trùng không làm test sai), `StagingRemoteValuesModule.kt`, `StagingRemoteValuesModuleTest.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `PoseCard.kt` (`PoseCardDefaults.oneLineHeight`), `PosesScreenTest.kt`, `PosesScreenScreenshotTest.kt`, `docs/ops/ads-guide.md`, `docs/ops/manual-test.md` (mục 11), `docs/features/poses/design/review/summary.md`. Có thể cùng sửa `AdsControllerUnitIdsTest.kt` nếu bản soạn lại của T-080 theo #279. Không sửa `PosesScreen.kt` (tag của dấu ở `:core:designsystem`), tránh phần lưới mà T-080 viết lại.
  - **T-081, T-083** (đang soạn lại song song sau #279: T-081 phiên bản 2, T-083 phiên bản 3): cùng sửa `AdPlacement.kt` và bảng khóa (`docs/remote-config-keys.md`, `docs/remote-config-template.json`), có thể cả `StagingRemoteValuesModule.kt` và test dữ liệu của `:core:ads` (`AdPlacementAppTest`, `AdUnitIdsTest`, `AdsControllerUnitIdsTest`). Mỗi task sinh lại bảng khóa từ code của nhánh mình; điều kiện dừng ở mục 7 so với bảng của nhánh gốc.
  - **T-085** (approved): plan này dùng `Pose.premium`, `PremiumAccess`, `PremiumPoseRepository` (`access`, `unlock(): Boolean`); T-085 sửa `PosesModule.kt`, `StagingFeatureFlagsModule`, bảng khóa (`feature_premium_poses_enabled`). Hai plan cùng sinh lại bảng khóa: chạy nối tiếp thì không xung đột.
  - **T-082** (plan phiên bản 2 đã duyệt và merge vào nhánh chính; chạy trước task này): trang 3 của luồng giới thiệu dùng icon `workspace_premium` (thiết kế ghi dấu premium giống trang này). Plan T-082 không tạo `PremiumBadge` hay `ic_premium.xml` (đã rà `docs/features/onboarding/plans/T-082-*.md`), nên không trùng file (L-033). Tệp kịch bản của plan này đi qua màn giới thiệu (mục 4, ghi chú dưới tệp kịch bản).
  - **T-088** (draft): sinh lại bảng khóa (bỏ `feature_camera_enabled`); chạy trước hay sau đều được.
  - **T-084**: `stopped`, không chạy.
- Dữ liệu test của `:core:ads` sau #279 (`AdUnitIdsTest` ba chuỗi và `missingUnitsWarning_listsMissingKeys`, `AdsControllerUnitIdsTest.PLACEMENT_UNITS`): T-080, T-081, T-083 cũng thêm khóa vào các chuỗi này. T-086 chỉ thêm `rw_premium_pose=unit/14` vào cuối mỗi chuỗi, tìm chỗ theo chuỗi, không theo số dòng; chạy sau các plan đó thì chuỗi dài hơn nhưng cách sửa không đổi.
- Đề xuất thứ tự chạy: sau T-080 và sau T-085 (phụ thuộc trong ticket); sau T-081, T-083 nếu chúng cùng danh sách, vì cùng sửa `AdPlacement.kt` và bảng khóa. Một lượt, từ nhánh chính mới nhất.
- Phần có ích cho mọi app (đổi `RewardedAdDialogCard` sang công khai để app có ảnh chuẩn của hộp thoại; cách app dịch chuỗi của khung): ghi vào mô tả PR để đề xuất về dự án mẫu (`docs/ops/from-app.md`).

## 7. Điều kiện dừng riêng

- Chữ ký `:core:poses` của T-085 trên nhánh chính khác mục 3.1 (`PremiumAccess`, `PremiumPoseRepository.access`, `unlock(poseId): Boolean`) mà plan này chưa được cập nhật (phiên bản mới): dừng.
- Gói thiết kế premium (`docs/features/poses/design/screens/` và `docs/features/camera/design/screens/` có 7 frame của mục 2) không còn trên nhánh chính, hay `context/` đổi so với số đo của mục 3.1: dừng, không tự đoán giao diện.
- Bảng khóa sinh lại có thay đổi ngoài `is_show_rw_premium_pose`: dừng.
- `git status --short -- '*/src/test/screenshots/*'` có ảnh chuẩn nào đổi ngoài các ảnh mới của mục 3.2: dừng, không ghi lại ảnh cũ (tham số mới mặc định `false` phải giữ mọi ảnh cũ, kể cả ảnh `RewardedAdDialog*` của `:core:ads`).
- `RewardedActionTest` hay test nào khác của `:core:ads` đỏ ngoài bốn test dữ liệu của mục 3.5: dừng; plan không đổi hành vi của khung.

## 8. Câu hỏi cho người duyệt

1. **Đã trả lời (2026-10-31, PR #278): thiết kế Figma.** Nhật duyệt gói bàn giao premium: 6 frame của `poses`, 1 frame của `camera`, component `PremiumBadge` ba cỡ, bảng chữ ("premium" thay "cao cấp"). Khác phiên bản 1: frame hộp thoại tên `PoseUnlockDialogScreen_default_<theme>`, khung 411 x 891 thẻ ở giữa. Mục 2, 3.1, 3.2, 4 đã theo gói này.
2. **Đã trả lời (plan T-085 approved 2026-10-31): chữ ký của T-085.** `PremiumAccess(enabled, unlockedIds)` với `isLocked`, `isUsable`; `PremiumPoseRepository { val access: Flow<PremiumAccess>; fun isUsable(pose): Flow<Boolean>; suspend fun unlock(poseId): Boolean }`. Không có tập mã đang khóa: `DefaultPosesLibrarySource` tự tính `lockedIds` cho thư viện và chi tiết; camera hỏi `access.isLocked` (mục 3.1).
3. **Chữ tiếng Anh của hai nút khung.** Spec ghi "Xem quảng cáo", "Để sau". Tiếng Việt và năm ngôn ngữ khác dịch ở `:app` (mục 3.1). Tiếng Anh giữ chữ của khung "Watch ad", "No, thanks", hay đè ở `app/src/main/res/values/` thành "Not now" cho khớp nghĩa "Để sau"? Đề xuất của AI: giữ chữ của khung, để ảnh chuẩn của khung và của app cùng một chữ tiếng Anh.
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
4. **Ảnh chuẩn của hộp thoại.** Feature không dựng được hộp thoại thật trong test, và thẻ hộp thoại `RewardedAdDialogCard` là `internal` của `:core:ads`. Đề xuất của AI: đổi nó sang công khai (một từ, KDoc "for previews and screenshot tests"), đề xuất về dự án mẫu ở mô tả PR. Hệ quả mới thấy khi đối chiếu frame của PR #278: ảnh chuẩn dựng trong `:feature:poses` không thấy chữ dịch của `:app`, nên hai nút trên ảnh là "Watch ad", "No, thanks", còn frame vẽ "Xem quảng cáo", "Để sau"; ảnh so sánh khác đúng ở hai chữ đó. Đề xuất: chấp nhận khác biệt này khi rà ảnh so sánh, vì chữ tiếng Việt đã có test riêng. Cách khác: frame `PoseUnlockDialogScreen_*` chỉ vẽ phần nội dung của app (`PremiumUnlockContent`); cách này không kiểm được tiêu đề và bố cục hai nút.
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
5. **Interstitial `inter_pose_detail` sau khi mở khóa ở chi tiết.** Mở khóa xong, app về camera với tư thế đó (KB-8 tiêu chí 4). Đề xuất của AI: không qua `inter_pose_detail` (người dùng vừa xem xong một quảng cáo có thưởng; hai quảng cáo liền nhau dễ bị coi là gây khó chịu). Cách khác: qua `rememberShootWithAd` như nút chụp; nhịp `ads_interval` không tính rewarded nên inter có thể hiện ngay sau.
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
6. **`pose_selected` khi mở khóa.** Spec ghi `pose_selected` "khi bấm Chụp với tư thế này" (`source` `detail`) và khi chọn ở dải (`strip`). Đề xuất của AI: mở khóa xong mà app về camera hay dùng tư thế đó cũng ghi `pose_selected` (`detail` ở chi tiết, `strip` ở camera), sau `premium_pose_unlocked`, để tỷ lệ "mở chi tiết rồi chụp" của spec mục 1 vẫn đếm tư thế premium. Cách khác: chỉ ghi `premium_pose_unlocked`.
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
7. **Ghi mở khóa lỗi sau khi người dùng đã xem quảng cáo.** `unlock` ném `IOException` (bộ nhớ đầy, hiếm). Đề xuất của AI: tư thế vẫn khóa, không ghi sự kiện, không điều hướng; chi tiết hiện snackbar "Chưa mở khóa được tư thế. Thử lại."; camera chỉ ghi log `warn` (camera không có snackbar), ô vẫn khóa. Người dùng phải xem lại quảng cáo. Cách khác: lần này vẫn cho dùng tư thế (về camera), chỉ báo lỗi; cần camera nhận tư thế khóa từ chi tiết, trái tiêu chí "chọn tư thế khóa thì hộp thoại hiện".
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
8. **"Tư thế tiếp" của màn xem lại khi tư thế kế là premium chưa mở.** Spec không nói. Đề xuất của AI: bỏ qua tư thế khóa, chọn tư thế không khóa kế tiếp (vòng về đầu); không mở hộp thoại từ màn xem lại. Cách khác: dừng ở tư thế khóa và mở hộp thoại ở camera.
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
9. **Đề xuất về dự án mẫu.** Đổi `RewardedAdDialogCard` sang công khai (câu 4) và cách app dịch chuỗi của khung trong `values-<ngôn ngữ>` của `:app` (đã chạy thử) có ích cho mọi app. `ads-guide.md` chỉ thêm phần chung của hai điều này, và `docs/template-sync.txt` có dòng chú thích giữ `RewardedAdDialogCard` công khai qua các đợt đồng bộ (mục 3.1). Đề xuất: ghi trong mô tả PR theo `docs/ops/from-app.md`, người mở đề xuất ở repo mẫu sau khi merge. Đúng không?
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
10. **Một lượt hay tách ticket (mới, theo #279).** Quy tắc mới của `docs/CLAUDE.md` (L-142 của mẫu) không cho một plan chạy nhiều lượt; ticket T-086 (mục Rủi ro sơ bộ) đã ghi lại theo quy tắc này trong PR của plan (rà soát độc lập, nên sửa 8). Phiên bản 2 gộp thành một lượt, một PR cỡ L (`token_estimate` 39,4M theo số gợi ý cỡ L). Cách khác: tách phần camera thành ticket mới (`depends_on: [T-086]`), plan riêng; T-086 còn thư viện, chi tiết, khung (cỡ M). Đề xuất của AI: giữ một lượt; phần camera dùng lại mọi thứ của phần A, và ticket ghi một kịch bản (KB-8) xuyên ba màn. Nếu nhóm muốn PR nhỏ hơn để rà, người tách ticket rồi AI soạn lại hai plan.
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
11. **Người dùng có gói bỏ quảng cáo (mới, theo #279).** Khung có cổng `AdFreeStatus`: tín hiệu bật thì `rewardedSwitch` tắt, nên `start()` chạy hành động ngay, như khóa vị trí tắt (ads-guide mục 6; `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction`). Với plan này: người có gói chạm nút mở khóa thì tư thế mở ngay và mãi (vẫn ghi `premium_pose_unlocked`), từng tư thế một; dấu premium vẫn hiện trên tư thế chưa chạm. App hiện gắn `AdFreeStatus.NONE` (`AdFreeModule`), chưa có gói nào, nên hôm nay không ai gặp. Spec poses chưa nói gì về gói này. Đề xuất của AI: giữ theo khung trong plan này, không thêm code; khi app có gói bỏ quảng cáo thì spec quyết có mở hết tư thế premium (ẩn dấu) cho người có gói hay không, ở ticket của gói đó.
   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
12. **Quảng cáo nạp sẵn (thông tin, theo #279).** Khi `is_show_rw_premium_pose` bật (staging bật), khung nạp sẵn một quảng cáo rewarded cho vị trí này ở nhóm nạp sẵn thứ hai (`AdPreloadPlan`, `AdPreloadPlanTest.fromPlacements_hasRewardedInSecondGroup`), như vị trí rewarded của trang thử. Không đổi gì ở plan; ghi để người duyệt biết staging có thêm một lượt nạp rewarded lúc mở app.

   - Trả lời: theo đề xuất của AI. Nhật ủy quyền ngày 2026-11-01 ("Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi").
13. **Chiều cao thẻ hộp thoại (mới, phiên bản 3).** Frame `PoseUnlockDialogScreen_*` vẽ nút "Để sau" cao 40 dp nên thẻ 288 dp; khung `:core:ads` giữ vùng chạm tối thiểu 48 dp của Material nên thẻ thật 296 dp. Hai hướng: (a) nhận thẻ 296, góc trên 297,5 (phiên bản 3 viết theo hướng này); (b) sửa frame cho nút "Để sau" cao 48, ghi lại `design/context/PoseUnlockDialogScreen_*`, rồi test lấy số mới của frame. Đổi khung `:core:ads` để nút cao 40 làm giảm vùng chạm (trợ năng) và đổi ảnh chuẩn `RewardedAdDialog*`, nên không đề xuất. Đề xuất của AI: (a), và người thiết kế sửa frame cho khớp khi rảnh.
   - Trả lời:

**Kết quả rà soát độc lập phiên bản 2 (2026-11-01, agent ngữ cảnh mới, chỉ đọc): 1 chặn, 9 nên sửa, 8 ghi chú. Đã sửa trong phiên bản 2:**

- **Chặn:** test của camera nói dựng `CameraRoute` "như `CameraScreenPoseTest`", mà lớp đó chỉ dựng `CameraScreen` không trạng thái; không test nào dựng `CameraRoute`. Sửa:
  - phần nối tách thành `pose/LockedPoseUnlock.kt`, test `LockedPoseUnlockTest` với `PoseViewModel` thật, đã chạy thử (mục 3.1, 4, 4.1);
  - `PoseDetailRouteTest` dựng `PoseDetailRoute` với ViewModel thật: đã chạy thử trên route hiện có; phương án dự phòng tách phần nối ở mục 3.4.
- **Nên sửa:**
  1. Dấu nằm trong node gộp, TalkBack có thể đọc trùng: `PremiumBadge` có tham số `contentDescription`, ô camera truyền `null`; thêm test `assertContentDescriptionEquals` cho ô và thẻ; đã chạy thử trên cây ngữ nghĩa và `AccessibilityNodeInfo` (L-004).
  2. Kịch bản emulator: đi qua các trang của T-082; cách chọn thẻ khóa dựa trên số đo (dấu là node riêng có `resource-id`); đã kiểm bằng `device-scenario.py check`.
  3. `manual-test.md`: ghi "mục con mới của mục 11", không định trước số 11.5.
  4. `ads-guide.md` chỉ giữ phần chung; ghi `RewardedAdDialogCard` công khai vào chú thích của `docs/template-sync.txt`.
  5. Thêm `PoseViewModelTest.unlockGranted_twiceWhileWriting_writesOnce`.
  6. Thêm `PoseDetailScreenTest.wide_locked_showsUnlockAndBadge` (`WideDetailLayout`, L-072).
  7. Gộp vào điểm chặn (`PoseDetailRouteTest`).
  8. Ticket T-086 (mục Rủi ro sơ bộ) bỏ câu "chia hai lượt", ghi một lượt theo L-142; sửa ngay trong PR của plan này (#273).
  9. `rw_premium_pose=unit/14` ở mọi chỗ; tìm chỗ sửa theo chuỗi.
- **Ghi chú đã áp:**
  - mục 6 cập nhật phiên bản đang soạn của T-080, T-081, T-083, T-082 đã merge, dữ liệu test sau #279;
  - `PoseDetailScreen` thêm callback có mặc định (3.5);
  - ba test "Cần xem trước" được nêu tên ở dòng "Giữ nguyên" (3.5);
  - `rememberUpdatedState` cho `RewardedAction` trong `PoseActions` (3.1);
  - hành vi sau "Để sau" (3.1);
  - `onRequestedPose` trên ViewModel mới (3.1);
  - cách hiện khi `access` phát `enabled = false` rồi `true` (3.1);
  - ghi chú token cỡ L (frontmatter).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt (phiên bản 2)
- **Người duyệt:** Tuấn, **duyệt chéo:** Nhật
- **Ghi chú:** Nhật ủy quyền ngày 2026-11-01: "Hãy hoàn thành tất cả những việc lập kế hoạch, sau đó merge tự động, không cần hỏi ý kiến tôi". AI ghi duyệt theo ủy quyền đó. Câu 3 tới 12 ở mục 8 theo đề xuất của AI; rà soát độc lập phiên bản 2: 1 chặn, 9 nên sửa, 8 ghi chú, đã sửa hết. Phần dùng API của T-085 chưa chạy thử vì T-085 chưa có code: test viết trước bắt lỗi.

Phiên bản 3:

- **Quyết định:**
- **Người duyệt:**, **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261031-02, phiên bản plan 2

- **Kết quả:** dừng ở bước 4 (2026-11-01 14:09 UTC), điều kiện dừng 8 (và 3: frame mâu thuẫn với khung đã có) của `risk-scoring.md` mục 4.
- **Nhánh, PR:** `claude/T-086-khoa-tu-the-premium` (commit test `b356d3e`, commit code `a52568f`, đã đẩy), không có PR.
- **Đã làm:** toàn bộ code của mục 3.1 (phần A và B): vị trí `RW_PREMIUM_POSE`, sự kiện `premium_pose_unlocked`, `PremiumBadge`, khóa ở thư viện, chi tiết và camera, `LockedPoseUnlock`, chữ của hộp thoại khung trong 6 ngôn ngữ, bảng khóa, staging, kịch bản emulator, `manual-test.md` mục 11.6, 8 ảnh chuẩn mới, ảnh so sánh của `poses` và `camera`. Bước 3 đến 5 do phiên con làm trong worktree `../task-T-086`.
- **Test viết trước:** khoảng 50 test mới hay mở rộng (commit `b356d3e`), đều đỏ đúng lý do trên khung rỗng (ví dụ `expected:<[w02, c01]> but was:<[]>`, "could not find any node … pose_premium_badge"); hai test qua ngay như plan dự kiến, đột biến hay đối chứng đỏ; 2 test viết chặt bằng đối chứng (L-055).
- **Lý do dừng:** `PoseDetailScreenTest.unlockDialogCard_matchesDesign` đỏ: `expected:<288.0> but was:<296.00003>`, góc trên 297,7 thay 302. Nội dung đúng 72 dp, nút "Watch ad" đúng 56 dp; phần lệch là nút "No, thanks" của `RewardedAdDialogCard`: Material giữ vùng chạm tối thiểu 48 dp, frame Figma vẽ 40 dp. Mục 3.1 không cho đổi khung `:core:ads` và ảnh chuẩn `RewardedAdDialog*`; đổi con số test kiểm là điều kiện dừng 8.
- **Kiểm tra cục bộ:** `scripts/check.sh` không chạy tới cuối vì test trên. Chạy tay tương đương: `testDebugUnitTest testStagingDebugUnitTest :app:lintStagingDebug :app:assembleStagingDebug ktlintCheck checkModuleDependencies --continue`: 2817 test, 1 đỏ (test trên); lint, build staging, ktlint, quy tắc phụ thuộc qua. `verifyRoborazziDebug --rerun` của `:feature:poses`, `:feature:camera`, `:core:designsystem`, `:core:ads`: mọi ảnh chuẩn cũ khớp. `device-scenario.py check` của kịch bản mới qua. Phương án dự phòng 3.4 không dùng.
- **Rà soát độc lập:** chưa rà soát (task dừng trước bước 6).
- **Lệch so với plan:** không file nào ngoài mục 3.2; không câu khẳng định cũ nào viết lại; ba chỗ code khác chữ của plan, giữ ý định: log "Requested pose is locked" đặt ở `onRequestedPose` thay `onLibraryReady` để ghi một lần; `onNextPose` giữ hành vi cũ khi dải chỉ có một tư thế và không có tư thế khóa; mục con của `manual-test.md` là 11.6.
- **Việc còn lại:** người chọn hướng ở câu 13 mục 8 và duyệt phiên bản 3; phiên sau tiếp tục trên nhánh (mục 3.1, Tiếp tục).
- **Cần người xem:** câu 13 mục 8; ảnh chuẩn mới trên nhánh (`PosesScreen_premium_*`, `PoseDetailScreen_premium_*`, `PoseUnlockDialogScreen_default_*`, `CameraScreen_premium_dark`); ảnh chi tiết premium có bóng của `PrimaryButton`, có thể lệch trên CI Linux (L-145).
