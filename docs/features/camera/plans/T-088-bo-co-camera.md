---
ticket: T-088
title: Bỏ cờ `feature_camera_enabled` (không còn tác dụng từ T-081)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.1M         # cỡ S theo số gợi ý của scripts/token-usage.py estimate (2026-10-31, 27 task); không nâng tay
blocking_points: [core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-088: BỎ CỜ `FEATURE_CAMERA_ENABLED` (KHÔNG CÒN TÁC DỤNG TỪ T-081)

## 1. Tóm tắt

Bỏ `FeatureFlag.CAMERA` (`feature_camera_enabled`) khỏi `:core:config`, bỏ hàm `camera()` của `StagingFeatureFlagsModule`, bỏ `FeatureFlag.CAMERA` khỏi dữ liệu của `MainScreenTest`, sinh lại bảng khóa. Từ T-081 không code nào đọc cờ này: sau màn chào, hay khi màn chào không áp dụng, app luôn mở camera (plan T-081, câu 1). Người dùng không thấy gì khác.

Spec camera mục 8 bỏ dòng cờ và thêm một dòng lịch sử. Spec chuyển `changed`, chờ Nhật xác nhận lại (câu 1 ở mục 8).

Kiểm chứng:

- test viết trước `MainScreenTest.noCameraFlag`: đỏ khi còn dòng enum, xanh sau khi bỏ;
- `MainScreenTest` qua với dữ liệu mới, kể cả test viết lại `chooser_everyOtherFlagOn_choosesCamera`;
- `RemoteConfigKeysTest` qua sau khi sinh lại bảng: bảng chỉ mất đúng dòng và khối của `feature_camera_enabled`;
- lệnh `git grep` không còn tên cờ trong code.

Plan viết theo trạng thái nhánh chính **sau T-081 và T-085** (mục 6).

## 2. Hiểu yêu cầu

Ticket không xử lý kịch bản mới (`scenarios: []`). Nó dựa trên:

| Nguồn | Nội dung | Cách hiểu của AI |
| --- | --- | --- |
| `features/camera/spec.md` (confirmed 2026-10-31), mục 8 | `feature_camera_enabled`: "Từ T-081 không còn tác dụng ... Cờ bỏ khỏi code ở T-088" | Bỏ khai báo cờ, dòng staging, bảng khóa. Dòng của mục 8 bỏ theo, vì spec chỉ liệt kê cờ có thật |
| Plan T-081 (approved), câu 1 và mục 3.1 | `MainScreenChooser` không đọc cờ camera nữa; `FeatureFlag.CAMERA` và `camera()` giữ tới T-088; mô tả cờ đổi thành "No longer read: ... removed in T-088." | Sau T-081, `FeatureFlag.CAMERA` chỉ còn ở `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `MainScreenTest.kt`, hai tệp bảng khóa, và chữ `feature_camera_enabled` trong KDoc của `MainScreenChooser.kt` do T-081 viết (không đọc cờ) |
| Ticket T-088, Không bao gồm | Khóa trên Firebase staging, production người xóa tay sau khi bản không đọc cờ đã phát hành | Plan không gọi Firebase, không đổi cấu hình production |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0003 (cờ tính năng khai báo một chỗ), ADR-0023 (bảng khóa sinh từ code), ADR-0029 (dữ liệu của test cũ), ADR-0031 (câu khẳng định cũ trái plan), ADR-1008 (app không có màn mẫu, nên camera là màn chính duy nhất ngoài màn chào).

## 3. Giải pháp

### 3.1. Cách làm

Một PR, nhánh `claude/T-088-bo-co-camera`, tạo từ nhánh chính **sau khi T-081 và T-085 đã merge** (mục 6). Đoạn code dưới đây viết theo trạng thái sau hai plan đó. Phiên tìm theo chuỗi (`git grep -n "FeatureFlag.CAMERA\|feature_camera_enabled"`), không theo số dòng.

**Tìm chỗ dùng (2026-10-31, trên nhánh chính `e0b06a4`, trước T-081).** `git grep -n "FeatureFlag.CAMERA\|feature_camera_enabled\|\.CAMERA\b"` trong code, test, `app/src/androidTest`, `scripts/`, `.github/`, `docs/remote-config-*`:

- Code: `FeatureFlag.kt` (khai báo), `StagingFeatureFlagsModule.kt` (`camera()`), `MainScreenChooser.kt` (đọc cờ; T-081 bỏ chỗ đọc).
- Test: chỉ `MainScreenTest.kt`. Không test nào đếm số cờ: `DefaultFeatureFlagsTest`, `RemoteConfigKeysTest`, `AppRemoteKeys` lặp qua `FeatureFlag.entries`, không so một con số. `app/src/androidTest` không nhắc cờ (chỉ có quyền `Manifest.permission.CAMERA`, không liên quan).
- Script, `.github/`: không nhắc cờ. `scripts/ads-plan.py` đọc mọi dòng của bảng khóa, không có mục riêng cho cờ này.
- Tài liệu sinh từ code: `docs/remote-config-keys.md`, `docs/remote-config-template.json`.
- `MainScreen.CAMERA`, `Manifest.permission.CAMERA` khớp `\.CAMERA\b` nhưng là màn camera và quyền camera: không đổi.

**`:core:config`, `FeatureFlag.kt`.** Bỏ dòng do T-081 để lại:

```kotlin
    CAMERA("feature_camera_enabled", "No longer read: the camera is always the start screen (T-081); removed in T-088."),
```

Dòng có thể bị ktlint ngắt thành nhiều dòng; phiên bỏ cả khối `CAMERA(...)`. Không đổi dòng nào khác (`LOADING`, `NETWORK_DIALOG`, `ONBOARDING`, `POSES`, `SETTINGS`, `IN_APP_REVIEW`, `POSE_MATCHING`, `PREMIUM_POSES` của T-085 giữ nguyên). Sau task, enum có 8 cờ.

**`app/src/staging/.../StagingFeatureFlagsModule.kt`.** Bỏ hàm:

```kotlin
    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun camera(): FeatureFlag = FeatureFlag.CAMERA
```

Còn 8 hàm: `loading`, `networkDialog`, `onboarding`, `poses`, `settings`, `inAppReview`, `poseMatching`, `premiumPoses` (T-085). Tập `@LocallyEnabledFlags` vẫn không rỗng, nên Hilt không cần `@ElementsIntoSet` rỗng.

**`app/src/main/.../navigation/MainScreenChooser.kt`.** Chỉ chú thích. T-081 viết "The camera is always there, so onboarding only needs its own flag; feature_camera_enabled is not read (plan T-081, question 1)." Cờ không còn tồn tại nên vế "feature_camera_enabled is not read" thành lời nhắc về thứ không có. Câu mới:

```kotlin
     * The camera is always there (plan T-081, question 1), so onboarding only needs its own flag. The stored status is
     * read only when onboarding is available; a read error shows onboarding rather than skip it
     * (plan T-012, question 3).
```

Code của `inputs()` không đổi.

**`MainScreenTest.kt`.** Trạng thái sau T-081 có 13 test (plan T-081, mục 3.5). Thay đổi:

- `FeatureFlag.CAMERA` có ở ba chỗ:
  - `BOTH_FLAGS = setOf(FeatureFlag.ONBOARDING, FeatureFlag.CAMERA)`;
  - `chooser_cameraFlagOn_choosesCamera`;
  - `chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus`.
- Cách sửa từng chỗ ở mục 3.5. Tóm tắt: hằng `BOTH_FLAGS` thành `ONBOARDING_ON = setOf(FeatureFlag.ONBOARDING)`, test thứ hai viết lại thành `chooser_everyOtherFlagOn_choosesCamera`, test thứ ba bỏ `CAMERA` khỏi tập cờ (dữ liệu, ADR-0029).
- Ba test có "Camera" trong tên (`chooser_cameraFlagOff_choosesCamera`, `chooser_onboardingAndCameraOn_notDone_choosesOnboarding`, `chooser_onboardingOnCameraOff_choosesOnboarding`) **giữ tên**: hành vi vẫn đúng với tên, và đổi tên làm mất hàm `@Test` của nhánh gốc mà rà soát của phiên coi là test bị xóa (auto-run.md bước 6, L-093; rà soát độc lập).
- Test duy nhất đổi tên là `chooser_cameraFlagOn_choosesCamera` thành `chooser_everyOtherFlagOn_choosesCamera` (viết lại theo ADR-0031). Phiên ghi bảng "tên cũ, tên mới" vào mô tả PR và vào lời giao cho rà soát độc lập.

  Thân test và câu khẳng định của ba test này giữ nguyên. Tên kết thúc bằng `choosesCamera` chỉ màn camera (`MainScreen.CAMERA`), giữ.
- Chú thích `// T-012: onboarding flag, camera flag and "onboarding done".` (nếu T-081 còn giữ) thành `// T-012: onboarding flag and "onboarding done".`
- Thêm test viết trước `noCameraFlag` (mục 4):

```kotlin
    // Spec camera, section 8 (plan T-088): the camera is always the main screen, so no flag gates it.
    @Test
    fun noCameraFlag() {
        assertTrue(FeatureFlag.entries.isNotEmpty())
        assertFalse(
            "feature_camera_enabled must not come back",
            FeatureFlag.entries.any { it.name == "CAMERA" || it.key == "feature_camera_enabled" },
        )
    }
```

  Cần thêm `import org.junit.Assert.assertFalse` nếu file chưa có.

**Bảng khóa.** Chạy `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Kết quả mong đợi:

- `docs/remote-config-keys.md` mất đúng một dòng `| \`feature_camera_enabled\` | ...`;
- `docs/remote-config-template.json` mất đúng một khối `"feature_camera_enabled": { ... },` (5 dòng);
- không đổi gì khác. Phiên so bằng `git diff --stat` và `git diff` của hai tệp. Không sửa tay.

**Spec camera, mục 8 (đề xuất, câu 1 ở mục 8).** Bảng cờ bỏ dòng:

```markdown
| `feature_camera_enabled` | tắt | Từ T-081 không còn tác dụng: sau màn chào (hay khi màn chào không áp dụng) app luôn mở camera. Cờ bỏ khỏi code ở T-088. Trước T-081: bật để dùng camera làm màn chính |
```

Bảng còn `feature_pose_matching_enabled`, `feature_in_app_review_enabled`, `is_show_review_interstitial`. Frontmatter `status: confirmed` thành `status: changed`; dòng `confirmed:` giữ nguyên. Lịch sử thêm ở cuối:

```markdown
| <ngày của phiên> | AI | Mục 8: bỏ dòng `feature_camera_enabled`, cờ đã bỏ khỏi code và bảng khóa ở T-088 (plan T-088). App luôn mở camera từ T-081. Khóa trên Firebase người xóa tay sau khi bản không đọc cờ đã phát hành. Chuyển `changed`, chờ Nhật xác nhận lại |
```

Dòng lịch sử cũ ngày 2026-10-31 nhắc cờ giữ nguyên (lịch sử không sửa).

**Không đổi.**

- `docs/ops/remote-config.md`, mục 1, dùng `feature_camera_enabled` làm ví dụ tên cờ. File lấy nguyên từ dự án mẫu (`docs/CLAUDE.md`, đầu file), và dự án mẫu cũng dùng ví dụ này: lần đồng bộ sau sẽ đưa lại. Ví dụ chỉ minh họa cách đặt tên, không liệt kê khóa của app. Câu 2 ở mục 8.
- Plan, ticket, báo cáo phiên cũ nhắc cờ: là lịch sử.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | Bỏ `CAMERA(...)` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bỏ `camera()` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenChooser.kt` | Chỉ KDoc của `inputs()` (mục 3.1) |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenTest.kt` | Bỏ `FeatureFlag.CAMERA` khỏi dữ liệu; viết lại `chooser_cameraFlagOn_choosesCamera` (tên mới `chooser_everyOtherFlagOn_choosesCamera`); thêm `noCameraFlag` (mục 3.5, 4) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại: mất dòng `feature_camera_enabled` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh: mất khối `feature_camera_enabled` |
| Sửa | `docs/features/camera/spec.md` | Mục 8 bỏ dòng cờ; `status: changed`; một dòng lịch sử (mục 3.1, câu 1) |
| Sửa | `docs/features/camera/tickets/T-088-bo-co-camera.md` | Trạng thái theo phiên (`on-staging` hay `stopped`) |
| Sửa | `docs/features/camera/plans/T-088-bo-co-camera.md` | Nhật ký thực hiện |

Nơi đọc các file bị đổi (`docs/CLAUDE.md`, mục Khi viết plan):

- `FeatureFlag.kt`:
  - `DefaultFeatureFlagsTest` lặp qua `FeatureFlag.entries` (mặc định tắt, khóa khớp tên, khóa đúng mẫu tên); bốn test dùng `LOADING` làm cờ mẫu sau T-081. Bỏ một dòng enum không làm đỏ test nào ở đây.
  - `AppRemoteKeys` lấy `FeatureFlag.entries`; `RemoteConfigKeysTest.everyFeatureFlagAndPlacement_isListed` lặp qua `entries`.
  - Feature không đọc `FeatureFlag.CAMERA`: `git grep` ở mục 3.1.
- `StagingFeatureFlagsModule.kt`: không test nào đọc nó (`git grep -n StagingFeatureFlagsModule -- '*/src/test/*' '*/src/androidTest/*' scripts`: không có kết quả, 2026-10-31).
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`:
  - `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` (`keysFile_matchesCode`, `templateFile_matchesCode`) so hai tệp với code. Hai tệp sinh lại trong commit code nên test qua mà không sửa.
  - `scripts/ads-plan.py` đọc mọi dòng của bảng, không đếm số khóa: không đổi.
  - `scripts/tests/run.sh` nhắc `docs/remote-config-keys.md` ở khối `ads-plan`. Khối đó tự dựng bảng giả trong repo giả, còn với repo thật chỉ kiểm tệp có tồn tại và đếm vị trí quảng cáo, không đếm cờ. Không ảnh hưởng.
  - `app/build.gradle.kts` chỉ nêu đường dẫn hai tệp: không đổi.
- `docs/features/camera/spec.md`: `scripts/tests/run.sh` chỉ dựng `features/demo/spec.md` giả trong repo giả, không đọc spec camera thật: không ảnh hưởng. `scripts/status.py`, `scripts/plan-queue.py` đọc `status` của spec. Spec `changed` thì `plan-queue.py` không soạn plan cho ticket camera tới khi Nhật xác nhận lại (câu 1).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: bỏ `feature_camera_enabled` (`FeatureFlag.CAMERA`). Không thêm cờ. Khóa trên Firebase staging và production giữ nguyên trong task này. Người xóa tay sau khi bản không đọc cờ đã phát hành (ticket, Không bao gồm). Bản trước T-081 còn đọc cờ; bản từ T-081 bỏ qua khóa, nên xóa sớm cũng không làm hỏng bản mới.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Không thêm: `CAMERA` (plan chỉ nhắc `Manifest.permission.CAMERA` khi tìm chỗ dùng; quyền camera có từ T-013, không đổi).
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

Không có. Task chỉ bỏ một dòng enum, một hàm Hilt và dữ liệu test; không có điểm kỹ thuật chưa chắc.

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `FeatureFlag.entries` có `CAMERA`, bảng khóa có `feature_camera_enabled` | Không còn | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`, `everyFeatureFlagAndPlacement_isListed`; `DefaultFeatureFlagsTest.everyFlag_isOffByDefault`, `withoutLocalOrRemoteValues_everyFlagIsOff`, `everyFlag_remoteKeyMatchesKeyAndDefault`, `everyFlagKey_followsNamingRule` | Giữ. Các test lặp qua `entries` hay so với bảng sinh lại trong cùng commit; không câu nào đếm số cờ |
| Bản staging bật cục bộ `CAMERA` | Không còn cờ để bật | Không có test | Không có |
| `chooser_cameraFlagOn_choosesCamera`: chỉ cờ camera bật, chưa qua màn chào thì `CAMERA` (plan T-081 giữ test này: "cờ camera bật vẫn `CAMERA`") | Không còn cờ camera. Ý định viết thành một câu: "một cờ khác cờ màn chào đang bật không đổi màn chính" | `MainScreenTest.chooser_cameraFlagOn_choosesCamera` | Viết lại thành `chooser_everyOtherFlagOn_choosesCamera`. Dữ liệu: `FakeFeatureFlags(FeatureFlag.entries.toSet() - FeatureFlag.ONBOARDING)`, `FakeOnboardingStatus(done = false)`. Câu khẳng định: `choose() == MainScreen.CAMERA`; `status.reads == 0`; các cờ đã đọc đúng bằng `[FeatureFlag.ONBOARDING]` (danh sách mà `FakeFeatureFlags` ghi từ T-081). Thêm câu kiểm trước, không bớt câu nào. Đây là test viết trước của plan, không phải lượt ADR-0031 của phiên. Đối chứng (sửa theo rà soát: thêm `&& ...` không đỏ được, vì cờ màn chào tắt nên `&&` dừng sớm và cờ thêm không được đọc): thêm một dòng đọc riêng `featureFlags.enabled(FeatureFlag.POSES).first()` ở đầu `MainScreenChooser.inputs()` thì câu "cờ đã đọc đúng bằng `[ONBOARDING]`" đỏ (đọc `[POSES, ONBOARDING]`); hoặc `val onboarding = ... ONBOARDING ... || featureFlags.enabled(FeatureFlag.POSES).first()` thì câu `CAMERA` đỏ (POSES bật nên chọn `ONBOARDING`). Phiên ghi lý do đỏ thật |
| `BOTH_FLAGS = setOf(ONBOARDING, CAMERA)` | `ONBOARDING_ON = setOf(ONBOARDING)` | `chooser_onboardingAndCameraOn_notDone_choosesOnboarding`, `chooser_onboardingDone_choosesCamera`, `chooser_statusReadFails_showsOnboarding_andLogsWarning`, `chooser_logsChosenScreen` | Dữ liệu (ADR-0029). Câu khẳng định giữ nguyên. Từ T-081, chooser không đọc `CAMERA`, nên bỏ nó khỏi tập không đổi kết quả |
| `chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus` dùng `setOf(FeatureFlag.CAMERA)` | `emptySet()` | Chính test đó | Dữ liệu (ADR-0029). Câu `CAMERA` và `status.reads == 0` giữ |
| Ba tên test nhắc cờ camera | Giữ tên (mục 3.1) | `chooser_cameraFlagOff_choosesCamera`, `chooser_onboardingAndCameraOn_notDone_choosesOnboarding`, `chooser_onboardingOnCameraOff_choosesOnboarding` | Giữ nguyên tên, thân và câu khẳng định |

`scripts/existing-tests.py` chạy ngày 2026-10-31 trên plan này, bỏ kết quả của `.claude/worktrees/`:

- 282 test dùng tên của 3 file Kotlin plan sửa: 16 test "Cần xem trước", 195 test "Nên xem"; plan đã nhắc 11 test.
- 16 test "Cần xem trước" là test đo kích thước của `:core:ads`, `:feature:camera`, `:feature:poses` (ví dụ `BottomAdBarTest.slotLoading_reservesNothing`, `PosesScreenTest.wide_hasThreeColumns`). Chúng chỉ khớp vì chữ `loading`, `settings`, `poses` trùng tên các cờ mà mục 3.1 liệt kê. Đã đọc: không test nào dùng `FeatureFlag.CAMERA` hay `StagingFeatureFlagsModule`. Giữ.
- Nhóm "Nên xem" đã đọc:
  - `MainScreenTest`: mục trên. Script đọc bản trước T-081; tên test sau T-081 theo plan T-081, mục 3.5.
  - `DefaultFeatureFlagsTest`: bốn test dùng cờ mẫu `SAMPLE`, T-081 đổi sang `LOADING`; không dùng `CAMERA`. Giữ.
  - `RemoteConfigKeysTest.appModuleKeys_areListed`: đọc khóa của `:app`, không đọc cờ camera. Giữ.
  - `NetworkDialogControllerTest`, `InAppReviewTest`, `RequestInAppReviewTest`, `LoadingRouteTest`, `LoadingViewModelTest`: dùng cờ của chính chúng (`NETWORK_DIALOG`, `IN_APP_REVIEW`, `LOADING`). Giữ.
  - `SampleViewModelTest`: bị xóa ở T-081.
- Các test còn lại chỉ khớp chữ chung (`loading`, `settings`, `poses`, `onboarding`), không dùng phần plan đổi.

Sau task `MainScreenTest` có 14 test: 13 test sau T-081, một test viết lại, thêm `noCameraFlag`. Không test nào bị xóa. Không trường hợp đầu vào nào mất câu kiểm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Không còn `FeatureFlag.CAMERA` (ticket, Định nghĩa hoàn thành 1; spec camera mục 8) | Unit test | `MainScreenTest.noCameraFlag` (mục 3.1). Viết trước: đỏ khi dòng `CAMERA(...)` còn trong `FeatureFlag.kt`, lý do mong đợi là `feature_camera_enabled must not come back`. Phiên ghi lý do thật vào nhật ký |
| Màn chính không phụ thuộc cờ nào khác cờ màn chào | Unit test | `MainScreenTest.chooser_everyOtherFlagOn_choosesCamera` (câu ở mục 3.5), kèm đối chứng ở mục 3.5: phiên chạy phép đột biến, ghi lý do đỏ thật, rồi bỏ phép đột biến |
| Luật chọn màn của T-081 giữ nguyên | Unit test có sẵn | 12 test còn lại của `MainScreenTest` qua với dữ liệu mới (mục 3.5) |
| Bảng khóa khớp code, chỉ mất `feature_camera_enabled` | Unit test có sẵn, kiểm tra bằng lệnh | `RemoteConfigKeysTest` qua sau khi sinh lại. `git diff origin/main -- docs/remote-config-keys.md docs/remote-config-template.json` chỉ có dòng xóa: 1 dòng ở tệp `.md`, 5 dòng ở tệp `.json`. Ghi `git diff --stat` vào nhật ký |
| Không còn tên cờ trong code | Kiểm tra bằng lệnh | `git grep -n "FeatureFlag.CAMERA\|feature_camera_enabled" -- app core feature scripts .github docs/remote-config-keys.md docs/remote-config-template.json` chỉ còn hai dòng chuỗi của `MainScreenTest.noCameraFlag` (`"feature_camera_enabled must not come back"` và `it.key == "feature_camera_enabled"`). Ghi kết quả vào nhật ký |
| Bản staging build được, đồ thị Hilt hợp lệ khi bỏ `camera()` | Build | `scripts/check.sh` (biên dịch `:app` staging và chạy `:app:testStagingDebugUnitTest`) |
| Spec camera mục 8 không còn dòng cờ, đã xác nhận lại | Kiểm tra thủ công (người: Nhật xác nhận spec) | Nhật đọc mục 8 và dòng lịch sử mới, chuyển spec lại `confirmed` (câu 1). Phiên ghi một dòng `- [ ]` ở mục Cần người kiểm tra của báo cáo |
| Mở app staging sau loading vẫn vào màn chào hay camera | Unit test có sẵn, emulator test có sẵn | `MainScreenTest`; `app/src/androidTest` (`AppLaunchTest`) không đọc cờ camera, chạy ở `ui-test` của CI như mọi PR |

**Thứ tự commit (L-025 của mẫu):**

- Commit test (bước 3), mọi phần của `MainScreenTest.kt`:
  - bỏ `CAMERA` khỏi dữ liệu;
  - viết lại `chooser_cameraFlagOn_choosesCamera` thành `chooser_everyOtherFlagOn_choosesCamera` (đổi tên duy nhất);
  - thêm `noCameraFlag`.

  Khi `CAMERA` còn trong enum, mọi test biên dịch được. `noCameraFlag` đỏ. `chooser_everyOtherFlagOn_choosesCamera` qua ngay, vì chooser sau T-081 đã không đọc cờ nào khác; giữ làm test chặn, kèm đối chứng ở mục 3.5. Các test còn lại qua.
- Commit code (bước 4):
  - `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, chú thích `MainScreenChooser.kt`;
  - hai tệp bảng khóa sinh lại;
  - spec camera mục 8.

**Ngoại lệ viết test trước:**

- Bỏ `camera()` của `StagingFeatureFlagsModule`: một dòng nối của Hilt, không có hành vi riêng để kiểm; build `:app` staging kiểm nó.
- Chú thích của `MainScreenChooser`: không có hành vi.
- Hai tệp bảng khóa: sinh từ code; `RemoteConfigKeysTest` kiểm.
- Spec camera mục 8: tài liệu, người xác nhận.

### 4.1. Chạy thử

Không cần (E dưới 2). Task bỏ một dòng enum và một hàm Hilt theo đúng cách T-081 bỏ `FeatureFlag.SAMPLE` và `sample()` (plan T-081 đã chạy thử với `scripts/check.sh`). Plan không sửa lỗi đo được. Không bước nào đếm số cờ (mục 3.1, Tìm chỗ dùng).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7, module core dùng chung (`:core:config`, `FeatureFlag.kt`). Không thêm SDK, quyền, thư viện, vị trí quảng cáo hay sự kiện.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai module (`:core:config`, `:app`), đổi enum công khai `FeatureFlag` (bỏ một giá trị); vài dòng mỗi file |
| B. Độ rõ của yêu cầu | 0 | Spec camera mục 8 và ticket nói rõ; Nhật đã quyết (2026-10-31) |
| C. Khả năng kiểm chứng tự động | 0 | Test viết trước, test có sẵn, `RemoteConfigKeysTest`; chỉ việc xác nhận spec là của người |
| D. Ảnh hưởng chéo | 2 | Sửa `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `MainScreenTest.kt`, bảng khóa mà T-081, T-085 (và có thể T-083, T-086) cũng sửa (mục 6) |
| E. Độ mới của kỹ thuật | 0 | Lặp lại đúng cách T-081 bỏ `FeatureFlag.SAMPLE` |
| **Tổng** | 4 | **Mức: Cao (chạm điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `FeatureFlag.kt` (`:core:config`, mọi feature đọc cờ qua đây), `StagingFeatureFlagsModule.kt`, `MainScreenChooser.kt` (chỉ chú thích), `MainScreenTest.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/features/camera/spec.md`.
- Task khác có thể xung đột:
  - **T-081** (approved, RUN-20261031-02, chạy trước): plan này phụ thuộc (`depends_on`). File trùng:
    - `FeatureFlag.kt`: T-081 bỏ `SAMPLE`, đổi mô tả của `CAMERA`; plan này bỏ `CAMERA`.
    - `StagingFeatureFlagsModule.kt`: T-081 bỏ `sample()`.
    - `MainScreenChooser.kt`: T-081 bỏ chỗ đọc cờ, viết KDoc; plan này đổi KDoc đó.
    - `MainScreenTest.kt`: T-081 viết lại, thêm danh sách cờ đã đọc của `FakeFeatureFlags`.
    - Hai tệp bảng khóa.
    - Plan này dùng đúng trạng thái sau T-081 ở mục 3.1 và 3.5. T-081 dừng thì task này bỏ qua: cờ vẫn được đọc, bỏ nó sẽ đổi luồng chọn màn.
  - **T-085** (approved, RUN-20261031-02, chạy trước): thêm `PREMIUM_POSES` vào `FeatureFlag.kt`, `premiumPoses()` vào `StagingFeatureFlagsModule.kt`, sinh lại bảng khóa (thêm `feature_premium_poses_enabled`). Không phụ thuộc về nội dung. Chạy sau để không trùng vùng sửa của cùng file trong một phiên.
  - **T-080, T-084** (approved, chạy trước trong RUN-20261031-02): sinh lại hai tệp bảng khóa. Không sửa file Kotlin chung với plan này.
  - **T-083, T-086** (plan đang soạn): sinh lại bảng khóa. T-086 có thể sửa `StagingFeatureFlagsModule.kt` hay `StagingRemoteValuesModule.kt`. Chạy trước hay sau T-088 đều được, vì mỗi task sinh lại bảng từ code của nhánh mình; điều kiện dừng ở mục 7 so với bảng của nhánh gốc, không so với một danh sách khóa cố định.
  - **T-082** (plan đang soạn): đọc `FeatureFlag.PREMIUM_POSES`, `ONBOARDING`; ghi `MainScreenTest` "không đổi". Không đọc `CAMERA`.
- Đề xuất thứ tự chạy: sau T-081 và T-085, cuối RUN-20261031-02 (thứ tự T-080, T-084, T-081, T-085, T-088), như ticket ghi.

## 7. Điều kiện dừng riêng

- Trên nhánh gốc của task, `git grep -n "FeatureFlag.CAMERA\|feature_camera_enabled" -- app core feature` cho thấy code ngoài test còn đọc cờ (ví dụ `MainScreenChooser` vẫn gọi `enabled(FeatureFlag.CAMERA)`), hay chỗ dùng nằm ngoài `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `MainScreenTest.kt` và KDoc của `MainScreenChooser.kt` (chú thích do T-081 viết, không phải lời gọi): dừng. Chỉ một lời gọi `enabled(FeatureFlag.CAMERA)` ngoài test mới là lý do dừng; chữ trong chú thích thì không. Nghĩa là T-081 chưa merge, hay một task khác đã đọc lại cờ; bỏ cờ khi đó đổi hành vi.
- Bảng khóa sinh lại khác bảng của nhánh gốc ở chỗ nào khác ngoài việc mất dòng và khối `feature_camera_enabled`: dừng. Nhánh gốc là nhánh chính đã có T-081, T-085 và mọi task trước đó của phiên. Thay đổi hợp lệ của các plan trước (T-081 mất `feature_sample_enabled`, `is_show_sample_native_small`; T-085 thêm `feature_premium_poses_enabled`; T-080, T-084, T-083, T-086 thêm khóa của chúng) đã có sẵn trên nhánh gốc, nên không tính là khác. Khác thêm nghĩa là code của nhánh gốc và bảng của nhánh gốc đã lệch nhau từ trước.
- Bất kỳ ảnh chuẩn screenshot nào đổi: dừng. Task không đổi giao diện.

## 8. Câu hỏi cho người duyệt

1. **Sửa spec camera mục 8 lúc nào, ai xác nhận lại?** Ticket yêu cầu "bỏ dòng cờ, ghi lịch sử (spec chuyển `changed`, người xác nhận lại)". Phiên tự chạy không được chuyển spec sang `confirmed`.
   - Đề xuất của AI (plan viết theo): phiên sửa mục 8 trong PR của task như mục 3.1, chuyển spec sang `changed`, ghi dòng lịch sử. Nhật đọc lúc xem báo cáo phiên và chuyển lại `confirmed`, như lần sửa sau T-074 lượt 3 (2026-10-29). Hệ quả: tới khi Nhật xác nhận, `scripts/plan-queue.py` không soạn plan cho ticket camera mới.
   - Cách khác: Nhật sửa spec ngay trong PR plan này (hay một PR tài liệu trước phiên) và giữ `confirmed`. Spec khi đó nói cờ đã bỏ trước khi code bỏ. Phiên không chạm spec, và mục 3.2 bỏ dòng spec.
2. **`docs/ops/remote-config.md` giữ ví dụ `feature_camera_enabled`?** File lấy nguyên từ dự án mẫu, và dự án mẫu cũng dùng ví dụ này, nên sửa ở app thì lần đồng bộ sau đưa lại.
   - Đề xuất của AI: giữ. Ví dụ chỉ minh họa cách đặt tên cờ, không liệt kê khóa của app. Plan không đưa file này vào mục 3.2.
3. **Xóa khóa `feature_camera_enabled` trên Firebase staging lúc nào?** Ticket để việc này cho người, sau khi bản không đọc cờ đã phát hành.
   - Đề xuất của AI: staging xóa được ngay sau khi task lên staging. Bản staging từ T-081 không đọc cờ; bản staging cũ hơn không còn được dùng để thử. Production xóa sau khi bản production đầu tiên có T-081 đã phát hành. Phiên ghi việc này thành một dòng `- [ ]` ở mục Cần người kiểm tra của báo cáo.

**Rà soát độc lập (2026-11-01, agent ngữ cảnh mới, chỉ đọc): 2 chặn, 2 nên sửa, 4 ghi chú.** Đã sửa cả bốn điểm trong bản này:

- **Chặn:** điều kiện dừng 1 bắt cả KDoc của `MainScreenChooser.kt` do T-081 viết, nên phiên sẽ dừng vì một thay đổi hợp lệ. Sửa: KDoc được phép, chỉ lời gọi `enabled(FeatureFlag.CAMERA)` ngoài test là lý do dừng (mục 2, mục 7).
- **Chặn:** phép đối chứng của `chooser_everyOtherFlagOn_choosesCamera` không thể đỏ (`&&` dừng sớm khi cờ màn chào tắt). Sửa: dòng đọc riêng `POSES` ở đầu `inputs()`, hay `||` (mục 3.5).
- **Nên sửa:** KDoc thay mới dài 122 ký tự: đã ngắt dòng (mục 3.1).
- **Nên sửa:** đổi tên ba test làm mất hàm `@Test` của nhánh gốc (L-093): giữ tên; chỉ đổi tên test viết lại, phiên ghi bảng tên cũ, tên mới.
- **Ghi chú:** số cờ 8 và số hàm 8 chỉ đúng khi T-085 đã merge; T-085 dừng thì 7, không phải lệch plan. `noCameraFlag` ở `MainScreenTest` (`:app`): chấp nhận. Danh sách RUN-20261031-02 cần thêm T-088 (dòng 5) khi duyệt. Câu 1 có thêm hướng: giữ dòng cờ ở spec camera (đã ghi "bỏ ở T-088") để spec giữ `confirmed`.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
