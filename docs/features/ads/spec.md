---
feature: ads
status: confirmed        # draft | confirmed | changed
owner: Tuấn
product_owner: Nhật
requests:
  - requests/2026-10-31-thanh-day-native-thu-gon.md
  - requests/2026-11-02-bat-combo-native-toan-man-hinh.md
design:                  # không có thiết kế riêng; giao diện quảng cáo theo khung của dự án mẫu
created: 2026-10-28
confirmed: 2026-11-02
---

# SPEC: CHUYỂN QUẢNG CÁO CỦA APP SANG KHUNG ADSKIT CỦA DỰ ÁN MẪU

Spec này chốt các quyết định kinh doanh cần trước khi soạn plan T-074 (lấy khung quảng cáo AdsKit của dự án mẫu, chưa bật AppsFlyer) và T-075 (`release.sh` theo khung mới). Vị trí quảng cáo của từng màn vẫn nằm ở mục 5 của spec màn đó (`features/camera/spec.md`, `features/poses/spec.md`). Khi spec này được xác nhận, các câu trả lời được chép vào hai spec đó trong PR của T-074, cùng lúc với code.

Mục 9 đã có đủ câu trả lời; Nhật xác nhận spec ngày 2026-10-28. Câu 7 tới 11 (Nhật, 2026-10-30) đổi bộ vị trí sau T-074 theo file kế hoạch quảng cáo của nhóm (`scripts/ads-plan.py`, `docs/ops/ads-plan.md`): hai banner thành thanh đáy native, thêm hai vị trí tắt lúc phát hành. Ticket T-079.

## 1. Mục tiêu

- **Người dùng được gì:** quảng cáo hiện theo cùng một bộ luật đã kiểm trên máy thật ở dự án mẫu. Không có vị trí quảng cáo nào của màn camera hay loading; luồng giới thiệu chỉ có thẻ native `native_onboarding` từ yêu cầu 2026-10-31 (câu 12), không có quảng cáo toàn màn hình; quảng cáo mở app hiện ở điểm chuyển từ loading sang camera, quảng cáo khi quay lại app hiện ở mọi màn, kể cả camera (câu 6).
- **Vì sao làm bây giờ:** dự án mẫu đã xong khung quảng cáo theo AdsKit PARTNER (`0.1.0-staging.18`). App đang giữ bộ cũ theo ADR-1004, và mọi sửa của dự án mẫu cho quảng cáo không tới được app.
- **Đo kết quả bằng gì:** doanh thu quảng cáo trên một người dùng mỗi ngày và tỷ lệ giữ chân ngày 1. So hai tuần trước và sau bản có khung mới.

## 2. Phạm vi

**Trong phạm vi:**

- Vị trí của màn (câu 7 tới 10):
  - `review_interstitial`: toàn màn hình, khi rời màn xem lại sau khi lưu ảnh; giữ mã và luật đếm ảnh (câu 1, câu 7);
  - `poses_bottom`: thanh đáy native nhỏ của màn thư viện, thay banner `poses_banner` (câu 8);
  - `pose_detail_bottom`: thanh đáy native nhỏ của màn chi tiết tư thế, thay banner `poses_detail_banner` (câu 8);
  - `inter_pose_detail`: toàn màn hình, khi bấm "Chụp với tư thế này" ở chi tiết, trước khi về camera; tắt lúc phát hành (câu 9);
  - `poses_in_collection`: ô quảng cáo xen trong lưới thư viện, sau mỗi 8 tư thế; tắt lúc phát hành (câu 9).
- Luật tần suất, khóa Remote Config, theo câu trả lời ở mục 9. AppsFlyer chưa bật (câu 3).
- Hai vị trí toàn app của khung (câu 6): quảng cáo mở app (`open_app`), sau loading và trước camera; quảng cáo khi quay lại app (`resume`), ở mọi màn, kể cả camera.
- Theo ba yêu cầu ngày 2026-10-31 (câu 12 tới 15):
  - `native_onboarding`: thẻ native ở mọi trang của luồng giới thiệu, bật lúc phát hành (spec onboarding, KB-6, câu 6, 8).
  - `rw_premium_pose`: quảng cáo có thưởng mở khóa tư thế premium; không có quảng cáo thì chặn (spec poses, KB-8, câu 8, 10).
  - Lớp banner và lớp native thu gọn của thanh đáy có mã quảng cáo ở production (khóa chung theo khe `banner`, `native_collap`; câu 14), chọn lớp bằng hai khóa có sẵn `is_show_native_bottom`, `is_show_native_collap` (câu 13 tới 15).
- Combo native toàn màn hình (`native_combo`) sau mỗi interstitial của màn (`review_interstitial`, `inter_pose_detail`), đếm ngược theo `time_close_native`; bật ở staging và production (câu 18, thay phần combo của câu 6 và câu 9). Ticket T-090.

**Ngoài phạm vi:**

- Vị trí quảng cáo khác các vị trí ở trên. Mỗi vị trí mới cần spec được duyệt và ticket riêng.
- Combo native sau interstitial dùng thay quảng cáo mở app hay quảng cáo khi quay lại app (`open_app_inter`, `resume_inter`; câu 18).
- Lớp native thu gọn 250 dp nổi trên thanh đáy: có trong khung nhưng tắt lúc phát hành (câu 10). Từ yêu cầu 2026-10-31, lớp có mã ở production và bật được từ xa (câu 13 tới 15).
- AppsFlyer (câu 3), màn xem thử quảng cáo `AdGalleryActivity` và `SAMPLE_BOTTOM_BANNER` (câu 4).

## 3. Kịch bản sử dụng

Không có kịch bản mới ở spec này. `review_interstitial` giữ kịch bản của spec camera; bốn vị trí của thư viện và chi tiết theo KB-4, KB-6 của spec poses (câu 8, 9). Tiêu chí kiểm ở mục 9, ở Định nghĩa hoàn thành của T-074 và T-079.

## 4. Giao diện

Không có thiết kế riêng: thanh đáy native, ô quảng cáo trong lưới và interstitial dùng giao diện của khung. Trạng thái `ad` của thiết kế thư viện và chi tiết theo câu 5 của spec poses.

## 5. Quảng cáo

Vị trí của màn: theo `features/camera/spec.md`, `features/poses/spec.md` và `features/onboarding/spec.md`, mục 5 (câu 7 tới 10 đã chép vào spec poses; câu 12 ở spec onboarding và poses). Hai vị trí toàn app (`open_app`, `resume`) ghi ở đây theo câu 6, và chép vào mục 5 của `features/loading/spec.md` (quảng cáo mở app hiện sau loading) và `features/camera/spec.md` (quảng cáo khi quay lại app có thể hiện khi người dùng quay lại lúc đang ở camera) trong PR của T-074. Không thêm vị trí khác.

## 6. Dữ liệu và phân tích

AppsFlyer chưa bật (câu 3): không gửi dữ liệu nào tới AppsFlyer, không có quyền mà SDK AppsFlyer kéo vào.

Quyền mà SDK quảng cáo kéo vào manifest gộp: `VIBRATE` (adapter AppLovin rung mà không kiểm quyền; gỡ quyền thì app có thể dừng đột ngột khi hiện quảng cáo AppLovin). Đây là quyền thường, không cần hỏi người dùng, không thuộc mục An toàn dữ liệu (câu 4 của plan T-074). Plan T-074 kiểm danh sách quyền thật của manifest gộp ở mục 4.1; có quyền khác thì quay lại spec này trước khi merge.

Khai báo `queries` (thấy gói khác trên máy) mà SDK mediation kéo vào manifest gộp, theo lần chạy thử ở mục 4.1 của plan T-074 (2026-10-28). Không phải quyền, nhưng Play xét theo chính sách Package visibility; người kiểm và khai khi chuẩn bị Play Console trước production:
- gói `com.facebook.katana` (Meta Audience Network);
- intent `com.attribution.REFERRAL_PROVIDER` và tám gói chợ ứng dụng: `com.android.vending`, `com.google.market`, `com.amazon.venezia`, `com.sec.android.app.samsungapps`, `com.xiaomi.market`, `com.huawei.appmarket`, `com.oppo.market`, `com.bbk.appstore`;
- intent `android.intent.action.MAIN` (thấy mọi app có màn chính) và năm gói dò root: `com.topjohnwu.magisk`, `eu.chainfire.supersu`, `com.noshufou.android.su`, `com.koushikdutta.superuser`, `com.thirdparty.superuser`;
- intent `com.digitalturbine.ignite.cl.IgniteRemoteService` và `android.intent.action.VIEW` (DT Exchange);
- intent `androidx.browser.customtabs.CustomTabsService`, `VIEW` với `http` và `market` (Unity, InMobi).

Nguồn: AppLovin, Meta, DT Exchange, InMobi, Unity. App nhận các mục này như dự án mẫu, không gỡ (Nhật, 2026-10-28). Lượt 1 của T-074 có thêm mục nào khác thì quay lại spec này trước khi merge.

AppLovin MAX (câu 16): đợt đồng bộ dự án mẫu 47782ec..151b4c2 (PR #279) đưa vào `:core:ads` lời gọi thẳng SDK AppLovin (`com.applovin:applovin-sdk` 13.6.4, đã có trong app từ T-074 qua adapter AppLovin của AdMob) và thêm thư viện `com.applovin.mediation:google-adapter` (adapter Google của MAX). MAX chưa bật: chỉ khởi động khi bản build có khóa MAX, mà bản staging của CI và bản phát hành hiện không có. Manifest gộp không có quyền mới (`MergedManifestTest`); SDK AppLovin vốn đã nằm trong danh sách nguồn ở trên.

Sự kiện phân tích mà khung ghi qua Firebase Analytics (nếu có) được plan liệt kê và chép vào mục 6 của spec màn có vị trí trong PR của T-074.

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

Không có cờ mới. Mỗi vị trí có khóa bật tắt Remote Config (câu 2): `is_show_review_interstitial`, `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_inter_pose_detail`, `is_show_poses_in_collection`; thêm `native_every_poses_in_collection` (mặc định 8). Khóa chung của thanh đáy theo câu 8, 10.

## 9. Câu hỏi còn mở

1. **Luật tần suất của `review_interstitial`.**
   - Hiện nay:
     - `AdsSettings.breakCapDefaults` ở `core/ads` cho `review_interstitial` dùng `BreakCap(firstBreak = 2, breaksBetween = 3, minIntervalSeconds = 120)`: không hiện trước ảnh lưu thứ 2, tối đa 1 lần mỗi 3 ảnh, cách lần trước 120 giây;
     - mỗi luật có khóa riêng: `ads_review_interstitial_first_break`, `ads_review_interstitial_breaks_between`, `ads_review_interstitial_min_interval_seconds`;
     - thêm nhịp chung 45 giây của mọi interstitial (`ads_interstitial_min_interval_seconds`).
   - Khung mới có nhịp chung `ads_interval` (mặc định 45 giây). Đây là bản đổi tên của nhịp chung cũ, không phải luật mới. Khung không có ba luật riêng của vị trí.
   - Phương án:
     - (a) giữ cả ba luật riêng, làm điều kiện trước khi gọi quảng cáo toàn màn hình của khung;
     - (b) bỏ ba luật riêng, chỉ dùng `ads_interval`;
     - (c) giữ "ảnh thứ 2" và "1 lần mỗi 3 ảnh", bỏ 120 giây riêng (chỉ còn `ads_interval`).
   - Đề xuất của AI: (c).
   - Trả lời: (c). Nhật, 2026-10-28. Giữ "không hiện trước ảnh lưu thứ 2" và "tối đa 1 lần mỗi 3 ảnh"; bỏ 120 giây riêng, chỉ còn nhịp chung `ads_interval`.
2. **Tên khóa Remote Config.**
   - Khóa bật tắt: `ads_review_interstitial_enabled`, `ads_poses_banner_enabled`, `ads_poses_detail_banner_enabled` đổi sang `is_show_review_interstitial`, `is_show_poses_banner`, `is_show_poses_detail_banner` (quy ước của khung).
   - Ba khóa giới hạn của câu 1: giữ tên cũ cho luật còn giữ, bỏ khóa của luật bị bỏ; `ads_interstitial_min_interval_seconds` thay bằng `ads_interval`.
   - Trên Firebase, người phụ trách tạo khóa mới với giá trị của khóa cũ trước khi bản mới phát; khóa cũ giữ tới khi bản cũ không còn người dùng.
   - Đề xuất của AI: đồng ý như trên.
   - Trả lời: đồng ý như trên. Nhật, 2026-10-28. Theo câu 1: giữ `ads_review_interstitial_first_break`, `ads_review_interstitial_breaks_between`; bỏ `ads_review_interstitial_min_interval_seconds`.
3. **AppsFlyer.**
   - Bật AppsFlyer như dự án mẫu (ADR-0027)? Staging không có khóa nên không gửi gì; production cần khóa riêng (T-075).
   - Bật thì mục 6 của spec màn có vị trí ghi thêm dữ liệu gửi đi, và mục 6 của spec ghi hai quyền đọc nguồn cài đặt mà SDK kéo vào manifest.
   - Đề xuất của AI: bật.
   - Trả lời: chưa bật. Nhật, 2026-10-28. `release.sh` của mẫu luôn đòi khóa dev AppsFlyer: đã đề xuất về mẫu cho khóa này là tùy chọn (VGL-Studio/app-project-template#261); T-075 chờ thay đổi đó. T-074 lấy khung của `core/analytics` nhưng không đưa SDK AppsFlyer vào app: không thêm thư viện AppsFlyer, không có quyền đọc nguồn cài đặt, không gửi dữ liệu nào tới AppsFlyer. Plan kiểm làm được không; không được thì giữ `core/analytics` cũ (dòng `hold=`) và báo lại trong plan trước khi duyệt. Bật AppsFlyer sau cần spec và ticket riêng.
4. **Màn xem thử quảng cáo (staging) và `feature/sample`.**
   - Giữ màn xem thử của khung (`AdGalleryActivity`, chỉ có ở staging) để thử quảng cáo trên máy thật? Bỏ `SAMPLE_BOTTOM_BANNER` của `feature/sample`?
   - Đề xuất của AI: giữ màn xem thử; bỏ `SAMPLE_BOTTOM_BANNER` nếu app không còn dùng `feature/sample`.
   - Trả lời: bỏ cả hai. Nhật, 2026-10-28. Không mang `AdGalleryActivity` vào app; bỏ `SAMPLE_BOTTOM_BANNER`.
5. **Chia lượt.**
   - Một ticket (T-074) chia ba lượt trong plan (lấy code và build qua; nối vỏ app và chuyển vị trí; spec, ảnh chuẩn, tài liệu), mỗi lượt một PR, cùng một danh sách chạy? Hay tách thành ba ticket nối nhau?
   - Đề xuất của AI: một ticket, ba lượt.
   - Trả lời: một ticket, ba lượt. Nhật, 2026-10-28.
6. **Quảng cáo toàn app có sẵn trong khung.**
   - `AdPlacement` của khung có `open_app`, `open_app_inter`, `resume`, `resume_inter`, `native_combo`.
   - Ở production các khóa của chúng (`show_open_ads`, `show_resum_ads`, `is_show_native_after_inter`) mặc định tắt.
   - `StagingRemoteValuesModule.globalAdSwitchesOn()` của dự án mẫu lại bật sẵn ở staging:
     - quảng cáo mở app;
     - quảng cáo khi quay lại app (kể cả lúc đang ở camera);
     - trang native sau interstitial (sẽ nối sau `review_interstitial`);
     - native đáy.
   - Lấy nguyên phần `app/` thì bản staging trái mục 5 của spec camera ("Không vị trí quảng cáo nào ở camera").
   - Đề xuất của AI: giữ tắt mọi vị trí toàn app. `StagingRemoteValuesModule` của app không bật các khóa đó, chỉ giữ hai khóa ghi log (`is_log_ad_fullscreen_events`, `is_log_ad_slot_events`), có test kiểm. Muốn dùng vị trí nào thì thêm vào spec màn đó qua một ticket riêng.
   - Trả lời: bật quảng cáo mở app và quảng cáo khi quay lại app; native sau interstitial và native đáy giữ tắt. Nhật, 2026-10-28.
     - Quảng cáo mở app (`open_app`): hiện một lần khi loading xong, trước khi vào màn chính (thường là camera; onboarding hay màn dự phòng khi camera tắt). Không hiện trong lúc ở màn chào hay loading.
     - Quảng cáo khi quay lại app (`resume`): hiện ở mọi màn, kể cả khi người dùng quay lại app lúc đang ở camera (Nhật điều chỉnh, 2026-10-28; Nhật giữ lựa chọn này sau khi được nhắc nó đi ngược bài học 3 của spec camera). Khung không có cổng theo màn (luật A-11 của `docs/ops/ads-business-rules.md`), nên không cần sửa `core/ads`. Mục 5 của spec camera đổi câu "Không vị trí quảng cáo nào ở camera": camera vẫn không có vị trí của màn, nhưng quảng cáo toàn app khi quay lại app có thể phủ lên camera.
     - Bản staging: `StagingRemoteValuesModule` của app bật `show_open_ads` và `show_resum_ads`; không bật `is_show_native_after_inter`, `is_show_native_bottom`. Production theo Remote Config, mặc định tắt.
     - `show_open_ads_first_open` (lần mở đầu tiên, còn hộp thoại đồng ý và onboarding): tắt, cả ở staging (Nhật, 2026-10-28). Từ lần mở thứ hai mới có quảng cáo mở app (`show_open_ads`).
     - Quảng cáo mở app chỉ có khi màn loading bật (`feature_loading_enabled`), vì màn loading gọi `StartupAds` của khung (`docs/ops/ads-guide.md`).
     - `open_ad_type`, `resume_ad_type` theo mặc định của khung (app open); `open_app_inter`, `resume_inter` chỉ dùng khi Remote Config đổi loại.

7. **Kế hoạch quảng cáo 2026-10-30: lập lại bộ vị trí từ đầu, đổi `review_interstitial` thành `inter_review`, bỏ luật đếm ảnh?**
   - File kế hoạch đề xuất lập lại theo khung AdsKit, đổi mã `review_interstitial` thành `inter_review` và chỉ giữ nhịp chung `ads_interval`, vì "khung không có luật đếm theo tính năng" (MC-10). Câu này không đúng với app: luật đếm ảnh đã có trong code từ T-074 (câu 1), có test.
   - Đề xuất của AI: không lập lại từ đầu; giữ mã `review_interstitial` (cùng định dạng, giữ khóa trên Firebase và chuỗi số liệu `ad_placement`) và giữ luật đếm ảnh của câu 1.
   - Trả lời: đồng ý với đề xuất. Nhật, 2026-10-30.
8. **Thư viện và chi tiết dùng thanh đáy native nhỏ thay banner?**
   - Thanh đáy của khung: khe đáy native nhỏ 80 dp khi `is_show_native_bottom` bật, banner khi tắt. Production hôm nay chỉ có mã đơn vị cho khe native, chưa có mã theo khe cho lớp banner (`docs/ops/ads-guide.md`, mục 5, bước 6): tắt `is_show_native_bottom` thì đáy trống, không có banner dự phòng.
   - Đổi định dạng nên dùng mã mới: `poses_bottom`, `pose_detail_bottom` (khóa `is_show_poses_bottom`, `is_show_pose_detail_bottom`); bỏ `poses_banner`, `poses_detail_banner` khỏi code. Khóa cũ giữ trên Firebase tới khi bản cũ không còn người dùng (như câu 2).
   - Đề xuất của AI: đổi; lúc phát hành `is_show_native_bottom = true`.
   - Trả lời: đồng ý với đề xuất. Nhật, 2026-10-30.
9. **Hai vị trí đợt 2: `inter_pose_detail` và `poses_in_collection`?**
   - `inter_pose_detail`: interstitial khi bấm "Chụp với tư thế này" ở chi tiết, trước khi về camera. Cùng nhóm giãn cách mặc định với `review_interstitial` (`ads_interval`), không theo luật đếm ảnh.
   - `poses_in_collection`: ô native trong lưới thư viện (`native_in_collection`), sau mỗi 8 tư thế, không ở đầu, không ở cuối; pool cạn thì ẩn ô; thẻ quảng cáo khác rõ thẻ tư thế.
   - Đề xuất của AI: khai báo trong spec, code ở một ticket riêng sau khi đợt 1 lên staging.
   - Trả lời: khai báo trong spec và code cùng đợt 1 (T-079); cả hai tắt lúc phát hành, bật dần trên Firebase khi giữ chân ngày 1 không giảm quá 2 điểm. Nhật, 2026-10-30. Combo native sau interstitial giữ tắt (câu 6).
10. **Lớp native thu gọn 250 dp trên thanh đáy (`is_show_native_collap`, mặc định trong code là bật)?**
    - Đề xuất của AI: tắt lúc phát hành: lớp nổi che khoảng một phần ba lưới tư thế và chưa có mã đơn vị riêng. Thử sau khi có số liệu.
    - Trả lời: tắt lúc phát hành, theo đề xuất (Nhật chọn cùng câu 8, phương án nêu rõ lớp thu gọn tắt). Nhật, 2026-10-30.
11. **Bản staging của T-079.**
    - Theo câu 6, `StagingRemoteValuesModule` chỉ bật khóa đã chốt. Đề xuất của AI: bật `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_native_bottom`, và cả `is_show_inter_pose_detail`, `is_show_poses_in_collection` để người thử thấy hai vị trí đợt 2 (production vẫn tắt); không bật `is_show_native_after_inter`; đặt `is_show_native_collap` là `false` (mặc định trong code là bật), để bản staging giống bản phát hành (câu 10). Có test kiểm.
    - Trả lời: đồng ý với đề xuất. Nhật, 2026-10-30.

12. **Ba yêu cầu ngày 2026-10-31.**
    - Ba yêu cầu:
      - Luồng giới thiệu có native step (spec onboarding).
      - Tư thế premium mở khóa bằng quảng cáo có thưởng (spec poses).
      - Native thu gọn ở thanh đáy (yêu cầu `requests/2026-10-31-thanh-day-native-thu-gon.md`).
    - Mỗi vị trí mới ghi ở mục 5 của spec của màn, cùng cách đã làm với câu 7 tới 10.
    - Đề xuất của AI: xác nhận spec này cùng lúc với spec onboarding và spec poses, sau khi các câu hỏi của cả ba spec có trả lời.
    - Trả lời: xác nhận cùng lúc ba spec, theo đề xuất. Nhật, 2026-10-31.
13. **Chọn lớp của thanh đáy bằng Remote Config thế nào?** Khung đã có hai khóa chung cho mọi thanh đáy. `is_show_native_bottom` chọn khe đáy: bật là native nhỏ 80 dp, tắt là banner. `is_show_native_collap` bật thì lớp native thu gọn 250 dp nổi trên khe đó. Thu gọn lớp thì lộ khe bên dưới, và lớp tự mở lại sau `time_expand_collap_ad` giây.
    - Đề xuất của AI: dùng hai khóa có sẵn, không thêm khóa, không sửa khung. Bốn cách hiện:

      | Cách hiện | `is_show_native_bottom` | `is_show_native_collap` |
      | --- | --- | --- |
      | Native nhỏ | bật | tắt |
      | Banner | tắt | tắt |
      | Native thu gọn trên native nhỏ | bật | bật |
      | Banner (lớp thu gọn không hiện trên banner, luật D-01 của khung) | tắt | bật |

    - Phương án khác: một khóa `bottom_ad_type` (`banner`, `native`, `native_collap`). Cách này cần sửa khung và đề xuất về dự án mẫu. Nó cũng có thêm cách "chỉ lớp thu gọn, không có khe", mà khung chưa có.
    - Hai khóa có sẵn áp chung cho mọi thanh đáy. Muốn chọn riêng cho thư viện và chi tiết thì cần khóa theo vị trí, cũng là sửa khung.
    - Trả lời: hai khóa có sẵn, theo đề xuất. Nhật, 2026-10-31.
    - Sửa 2026-10-31 (rà soát plan T-084): bảng trên ghi "native thu gọn trên banner" cho tổ hợp tắt, bật. Khung không có cách hiện này: lớp thu gọn chỉ hiện khi khe là native (luật D-01, `AdsFlags.kt`, `bottomAdBarLayers`). Nhật chọn ghi lại theo khung: có 3 cách hiện thật (native nhỏ; banner; native thu gọn trên native nhỏ), tổ hợp tắt, bật ra banner, không có lớp. Lớp thu gọn trên banner cần đổi luật D-01, ngoài phạm vi.
14. **Mã quảng cáo theo lớp ở production.** Hôm nay production chỉ có mã của định dạng chính của vị trí (khe native nhỏ). Lớp banner và lớp thu gọn không gửi yêu cầu, vì `AdUnitIds.of(placement, format)` trả `null` (`docs/ops/ads-guide.md`, mục 5, bước 6). Vì vậy tắt `is_show_native_bottom` thì đáy trống.
    - Đề xuất của AI:
      - Cấu hình phát hành nhận thêm mục `<khóa vị trí>/<định dạng lớp>=<mã>`, ví dụ `poses_bottom/banner` và `poses_bottom/native_collap`, cùng kiểu với khóa log và bộ đếm của khung. Hai thanh đáy dùng chung một mã banner và một mã native thu gọn.
      - Bản phát hành báo lỗi khi thanh đáy thiếu mã lớp. Phần này nằm trong `scripts/release.sh`, nên T-075 do người làm (`auto_run: false`).
      - Đề xuất về dự án mẫu (`docs/ops/from-app.md`).
    - Trả lời: `<khóa vị trí>/<định dạng lớp>=<mã>`, theo đề xuất. Nhật, 2026-10-31.
    - Trả lời lại (2026-11-01, thay câu trả lời trên): dùng khóa chung theo khe của dự án mẫu (T-056 của mẫu, có trong app từ PR #279): cấu hình phát hành ghi `banner=<mã>` và `native_collap=<mã>`, một mã cho lớp đó của mọi thanh đáy. Không thêm khóa `<khóa vị trí>/<định dạng lớp>`. Plan T-084 dừng. Phần bản phát hành báo thiếu mã lớp đến cùng `release.sh` mới của mẫu ở T-075. Nhật, 2026-11-01.
15. **Lúc phát hành dùng cách hiện nào?** Câu 8 và 10 đã chốt: khe native nhỏ, lớp thu gọn tắt.
    - Đề xuất của AI:
      - Giữ như câu 8 và 10 cho bản phát hành đầu: `is_show_native_bottom = true`, `is_show_native_collap = false`.
      - Có mã lớp rồi thì thử native thu gọn trên Firebase, khi tỷ lệ giữ chân ngày 1 không giảm quá 2 điểm, như câu 9.
      - Bản staging giữ như câu 11. Người thử bật lớp thu gọn trên Firebase staging.
    - Trả lời: native nhỏ, lớp thu gọn tắt, theo đề xuất. Nhật, 2026-10-31.
16. **SDK AppLovin MAX từ dự án mẫu.** Đợt đồng bộ PR #279 mang về MAX (ADR-0037 của mẫu): banner và MREC tải từ MAX và AdMob theo `ads_mediation_order`, thêm thư viện `com.applovin.mediation:google-adapter`. Spec này chưa có MAX.
    - Đề xuất của AI: nhận MAX vào app nhưng chưa bật. Không có khóa MAX thì MAX không khởi động và mọi vị trí tải từ AdMob như hôm nay. Bật MAX ở production là quyết định sau, qua ticket riêng (cần khóa MAX, mã MAX theo khe, `release.sh` mới ở T-075, khai báo An toàn dữ liệu trên Play).
    - Trả lời: nhận, chưa bật, theo đề xuất. Nhật, 2026-11-01.
17. **Đổi hành vi khác của khung quảng cáo trong đợt đồng bộ PR #279** (ADR-0037, ADR-0038 của mẫu, nguồn JBase). Đợt này đổi các điều sau ở cả bản staging và production:
    - khởi động SDK quảng cáo sớm bằng đồng ý thật của lần mở trước (`CACHED_EARLY`) luôn bật, không còn công tắc; `is_enable_consent_shortcut` nay chỉ bật quyền tạm khi UMP lỗi mạng (`ERROR_ALLOWED`), vẫn mặc định tắt;
    - `time_expand_collap_ad` mặc định 30 giây thành 10 giây (lớp native thu gọn tự mở lại; lớp này đang tắt theo câu 15);
    - khóa mới `time_close_native`: đếm ngược 5 giây trước khi đóng được native toàn màn hình;
    - khóa mới `ads_mediation_order` (thứ tự MAX và AdMob, mặc định `max_admob`; không có khóa MAX thì chỉ AdMob, câu 16);
    - banner cố định dùng cỡ thích ứng neo đáy thường thay cho cỡ lớn (B-01);
    - vị trí mới `gallery_banner_mrec` của màn xem thử quảng cáo ở bản staging (app không có màn này); vị trí và khóa `is_show_gallery_banner_mrec` có ở mọi bản, mặc định tắt.
    - Đề xuất của AI: nhận như dự án mẫu. Đây là chuẩn nghiệp vụ chung của khung (ADR-0038); muốn khác thì đặt giá trị trên Remote Config, trừ `CACHED_EARLY` không còn công tắc. Không cần sửa spec màn nào.
    - Trả lời: nhận như dự án mẫu, theo đề xuất. Nhật, 2026-11-01.

18. **Bật combo native toàn màn hình sau interstitial** (yêu cầu `requests/2026-11-02-bat-combo-native-toan-man-hinh.md`, phiên thử TS-20261102-01). Câu 6 và câu 9 giữ combo tắt, nên app không đặt `FullScreenAdHost` (plan T-074, câu 7; ADR-1007). Phiên thử cho thấy khi khóa bị bật trên Firebase, lượt combo vẫn mở mà không hiện gì và không đóng được.
    - Combo hiện sau interstitial nào? Khung chỉ có một khóa chung `is_show_native_after_inter` (K-01); interstitial lúc mở app không bao giờ nối combo, còn `resume_inter` (quảng cáo khi quay lại app dạng interstitial, A-12) có nối.
      - Trả lời: sau mọi interstitial, trừ khi interstitial dùng thay quảng cáo mở app hay quảng cáo khi quay lại app (`open_app_inter`, `resume_inter`). Hôm nay là `review_interstitial` và `inter_pose_detail`. Lời của Nhật: "Khung native fullscreen sẽ hiển thị ở mọi interstitial, ngoại trừ khi inter được sử dụng thay thế cho aoa/resume ads." Nhật, 2026-11-02.
      - Hệ quả: `resume_inter` không nối combo, khác luật A-12 của khung; ticket T-090 sửa `:core:ads` và đề xuất về dự án mẫu.
    - Bản staging: `StagingRemoteValuesModule` bật `is_show_native_after_inter`, thay phần "không bật `is_show_native_after_inter`" của câu 6. Firebase staging vẫn tắt được. Chọn "Bật sẵn ở staging". Nhật, 2026-11-02.
    - Production: bật ngay khi phát hành, không bật dần như câu 9. Chọn "Bật ngay khi phát hành". Nhật, 2026-11-02.
      - Cần mã quảng cáo thật của `native_combo` trong cấu hình phát hành (T-075). Đặt khóa trên Firebase production là việc của người (ADR-0007, ADR-0009).
    - Đếm ngược, nút đóng, nút Back theo khung (K-02 tới K-08, `time_close_native` mặc định 5 giây). Màn camera và thư viện, chi tiết chép câu này vào mục 5 của spec camera và spec poses trong PR của T-090.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-28 | AI | Tạo bản đầu cho T-074, T-075 theo rà soát PR #209: chuyển câu hỏi từ ticket vào spec, thêm câu 6 (quảng cáo toàn app), sửa chỗ đặt luật tần suất và khóa ở câu 1, 2 |
| 2026-10-28 | Nhật | Trả lời sáu câu của mục 9 (AI ghi). Sửa mục 1, 2, 5, 6 theo câu 3, 4, 6: AppsFlyer chưa bật, bỏ màn xem thử, bật quảng cáo mở app (sau loading) và khi quay lại app (trừ camera) |
| 2026-10-28 | Nhật | Chốt thêm câu 6 (AI ghi): `show_open_ads_first_open` tắt; quảng cáo khi quay lại app hiện ở mọi màn, kể cả camera, thay cho "trừ camera" |
| 2026-10-28 | AI | Chép câu trả lời vào mục 5 của spec camera, loading, poses ngay trong PR xác nhận này (không đợi T-074), theo lựa chọn của Nhật |
| 2026-10-28 | AI | Mục 6: quyền `VIBRATE` do adapter AppLovin kéo vào (câu 4 của plan T-074). Chuyển `changed`, chờ xác nhận lại |
| 2026-10-28 | Nhật | Xác nhận spec (`confirmed`) sau khi xem kết quả kiểm: "Chuyển spec ads sang confirmed cho tôi", rồi "Đồng ý, mở PR" |
| 2026-10-28 | Nhật | Xác nhận phần đổi theo câu 3, 4, 6 của plan T-074 (lời của Nhật: "Xác nhận và merge" (PR #222)). Chuyển lại `confirmed` |
| 2026-10-28 | AI | Mục 6: khai báo `queries` mà SDK mediation kéo vào (chạy thử mục 4.1 và câu 14 của plan T-074; Nhật chọn nhận, ghi vào spec). Chuyển `changed`, chờ xác nhận lại |
| 2026-10-29 | Nhật | Xác nhận mục 6 (khai báo `queries`), lời của Nhật: "Xác nhận và merge" (PR #225). Chuyển lại `confirmed` |
| 2026-10-30 | AI | Rà file kế hoạch quảng cáo của nhóm sau T-074; Nhật trả lời câu 7 tới 10 (AI ghi): giữ `review_interstitial` và luật đếm ảnh; thanh đáy native `poses_bottom`, `pose_detail_bottom` thay hai banner; thêm `inter_pose_detail`, `poses_in_collection`, tắt lúc phát hành; lớp thu gọn tắt. Sửa mục 2, 3, 4, 5, 8. Chuyển `changed` |
| 2026-10-30 | Nhật | Trả lời câu 11 (bản staging bật thanh đáy và hai vị trí đợt 2; AI ghi). Xác nhận spec. Lời xác nhận của Nhật: chọn "Chuyển confirmed" khi được hỏi, sau khi trả lời câu cuối. Chuyển lại `confirmed` |
| 2026-10-31 | Nhật | Chia ticket, không đổi yêu cầu (AI ghi): `poses_in_collection` của câu 9 code ở T-080 thay vì T-079. Lý do: hàm lưới của khung để lại ô trống khi pool cạn, trái KB-6 của spec poses; Nhật chọn sửa khung, và phần sửa này phải chạy thử trước. `inter_pose_detail` vẫn ở T-079. Spec giữ `confirmed` |
| 2026-10-31 | AI | Ba yêu cầu của Nhật: luồng giới thiệu có `native_onboarding` (spec onboarding), tư thế premium có `rw_premium_pose` (spec poses), native thu gọn ở thanh đáy chọn bằng Remote Config (`requests/2026-10-31-thanh-day-native-thu-gon.md`). Mục 2, câu 12 tới 15. Chuyển `changed`, chờ trả lời và xác nhận |
| 2026-10-31 | Nhật | Trả lời câu 12 tới 15 (AI ghi, cùng phiên). Mục 1, 5 sửa theo câu 12. Xác nhận spec: chọn "Ghi và chuyển confirmed" khi được hỏi. Chuyển lại `confirmed` |
| 2026-10-31 | Nhật | Sửa câu 13 theo rà soát plan T-084 (AI ghi): tổ hợp `is_show_native_bottom` tắt, `is_show_native_collap` bật ra banner, không có lớp thu gọn (luật D-01 của khung); còn 3 cách hiện. Lời của Nhật: chọn "Sửa spec: 3 cách hiện", phương án ghi rõ Nhật xác nhận spec lại ngay. Chuyển `changed` rồi lại `confirmed` |
| 2026-11-01 | AI | Đồng bộ dự án mẫu PR #279: mục 6 ghi SDK AppLovin MAX (chưa bật); Nhật trả lời lại câu 14 (khóa khe chung `banner`, `native_collap` của mẫu, dừng T-084) và câu 16 mới (nhận MAX, chưa bật). Chuyển `changed`, chờ xác nhận |
| 2026-11-01 | Nhật | Xác nhận phần đổi của PR #279 (mục 2, mục 6, câu 14, câu 16). Lời xác nhận của Nhật: chọn "Xác nhận, chuyển confirmed" khi được hỏi. Chuyển lại `confirmed` |
| 2026-11-01 | AI | Theo rà soát độc lập PR #279: câu 17 mới, liệt kê các đổi hành vi khác của khung trong đợt đồng bộ. Chuyển `changed`, chờ trả lời và xác nhận |
| 2026-11-01 | Nhật | Trả lời câu 17 (AI ghi) và xác nhận spec. Lời của Nhật: chọn "Nhận như mẫu, xác nhận spec" khi được hỏi. Chuyển lại `confirmed` |
| 2026-11-01 | AI | Câu 17, theo rà soát lại PR #279: sửa chữ cho đúng code (`gallery_banner_mrec` có trong danh sách vị trí ở mọi bản, mặc định tắt). Không đổi câu trả lời, spec giữ `confirmed` |
| 2026-11-02 | AI | Yêu cầu bật combo native toàn màn hình (`requests/2026-11-02-bat-combo-native-toan-man-hinh.md`); Nhật trả lời câu 18 trong phiên (AI ghi): sau mọi interstitial trừ `open_app_inter`, `resume_inter`; bật ở staging; production bật khi phát hành. Sửa mục 2. Chuyển `changed`, chờ xác nhận. Ticket T-090 |
| 2026-11-02 | Nhật | Xác nhận câu 18 và phần sửa mục 2. Lời xác nhận của Nhật: chọn "Xác nhận, chuyển confirmed" khi được hỏi. Chuyển lại `confirmed` |
