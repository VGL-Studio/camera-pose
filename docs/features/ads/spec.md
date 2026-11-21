---
feature: ads
status: changed              # draft | confirmed | changed
owner: Tuấn
product_owner: Nhật
requests:
  - requests/2026-10-31-thanh-day-native-thu-gon.md
  - requests/2026-11-02-bat-combo-native-toan-man-hinh.md
  - requests/2026-11-13-quang-cao-cai-dat-va-xem-lai.md
  - requests/2026-11-17-do-nguon-cai-tenjin.md
  - requests/2026-11-21-dong-bo-quang-cao-mau-7bf8c3b.md
design:                  # không có thiết kế riêng; giao diện quảng cáo theo khung của dự án mẫu
created: 2026-10-28
confirmed: 2026-11-17
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
- Theo yêu cầu 2026-11-13 (câu 19), hai thanh đáy native nhỏ mới, tắt lúc phát hành:
  - `settings_bottom`: đáy màn Cài đặt (spec settings, mục 5);
  - `review_bottom`: đáy màn xem lại ảnh, dưới hàng nút (spec camera, mục 5).
- Phần mới của khung từ đợt đồng bộ dự án mẫu `698c5b3..bc3e80e` (câu 20, ticket T-098): khe đáy native và lớp thu gọn cao theo khung, bên trong thẻ theo thiết kế mới của khung (thẻ bước của luồng giới thiệu và thẻ trong lưới giữ kích thước); lớp thu gọn co lại hay không mở khi cửa sổ thấp; native chưa có quảng cáo thì thử lại giãn dần; interstitial, rewarded, app open của MAX có sẵn nhưng chưa bật.
- Theo yêu cầu 2026-11-17 (câu 21): đo nguồn cài đặt của chiến dịch thu hút người dùng bằng Tenjin, gửi sang Tenjin doanh thu từng lượt quảng cáo hiện và các sự kiện phễu quảng cáo. Tenjin thay chỗ của AppsFlyer; AppsFlyer vẫn tắt. Không đổi gì trên màn hình.
- Theo yêu cầu 2026-11-21 (câu 22): phần mới của khung từ đợt đồng bộ dự án mẫu `bc3e80e..7bf8c3b`. Native nhỏ (khe đáy, nội dung của lớp thu gọn) và native medium vẽ theo thẻ mới của khung, cao theo cỡ chữ thay vì theo cửa sổ; khe đáy chừa đúng chỗ đang chiếm, nhường chỗ khi lớp thu gọn mở; mỗi màn tối đa một quảng cáo neo đáy; màn có quảng cáo trong nội dung đang bật (thư viện khi ô lưới bật) không có lớp thu gọn; chi tiết tư thế không bao giờ có lớp thu gọn. Không thêm vị trí, khóa hay sự kiện.

**Ngoài phạm vi:**

- Vị trí quảng cáo khác các vị trí ở trên. Mỗi vị trí mới cần spec được duyệt và ticket riêng.
- Combo native sau interstitial dùng thay quảng cáo mở app hay quảng cáo khi quay lại app (`open_app_inter`, `resume_inter`; câu 18).
- Lớp native thu gọn nổi trên thanh đáy (vùng ảnh cộng khe đáy D-02, kích thước theo khung, `docs/ops/ads-business-rules.md`): có trong khung nhưng tắt lúc phát hành (câu 10). Từ yêu cầu 2026-10-31, lớp có mã ở production và bật được từ xa (câu 13 tới 15).
- AppsFlyer (câu 3; Tenjin thay chỗ của nó theo câu 21), màn xem thử quảng cáo `AdGalleryActivity` và `SAMPLE_BOTTOM_BANNER` (câu 4).

## 3. Kịch bản sử dụng

Không có kịch bản mới ở spec này. `review_interstitial` giữ kịch bản của spec camera; bốn vị trí của thư viện và chi tiết theo KB-4, KB-6 của spec poses (câu 8, 9); `settings_bottom` theo KB-1 của spec settings, `review_bottom` theo KB-9 của spec camera (câu 19). Tiêu chí kiểm ở mục 9, ở Định nghĩa hoàn thành của T-074 và T-079.

**KB-T1. Nhóm UA đo nguồn cài và doanh thu quảng cáo (câu 21).** Người dùng không thấy gì mới; người hưởng là nhóm thu hút người dùng xem báo cáo trên Tenjin.

- Khi người dùng cài app từ một chiến dịch và mở app lần đầu, với bản phát hành có khóa Tenjin, thì Tenjin ghi một lượt cài gắn với chiến dịch đó.
- Khi một quảng cáo hiện và mạng quảng cáo báo doanh thu của lượt hiện, thì Tenjin nhận doanh thu của đúng lượt đó, một lần, không nhân đôi.
- Khi một sự kiện phễu quảng cáo xảy ra (đủ điều kiện hiện, nạp xong, đã hiện, theo danh sách ở câu 21 ý b), thì Tenjin nhận sự kiện đó, trừ khi công tắc từ xa của sự kiện đang tắt (câu 21 ý c).
- Khi người dùng ở vùng cần hỏi đồng ý từ chối cho phép theo dõi, thì app không gửi gì sang Tenjin (câu 21 ý d).
- Khi người dùng mở app lần đầu ở vùng cần hỏi đồng ý và chưa trả lời hộp thoại đồng ý (kể cả khi hết 10 giây chờ của màn loading), thì app chưa gửi gì sang Tenjin; khi họ đồng ý thì app bắt đầu gửi, khi họ từ chối thì không gửi (câu 21 ý i).
- Khi bản build không có khóa Tenjin (bản staging của CI, máy dev), thì app không gửi gì sang Tenjin và vẫn chạy như hôm nay.
- Khi Tenjin lỗi hay không trả lời, thì quảng cáo, số liệu Firebase và mọi màn của app vẫn chạy như hôm nay.

## 4. Giao diện

Không có thiết kế riêng: thanh đáy native, ô quảng cáo trong lưới và interstitial dùng giao diện của khung. Trạng thái `ad` của thiết kế thư viện và chi tiết theo câu 5 của spec poses; của Cài đặt và màn xem lại theo câu 19, ý b.

## 5. Quảng cáo

Vị trí của màn: theo `features/camera/spec.md`, `features/poses/spec.md`, `features/onboarding/spec.md` và `features/settings/spec.md`, mục 5 (câu 7 tới 10 đã chép vào spec poses; câu 12 ở spec onboarding và poses). Hai vị trí toàn app (`open_app`, `resume`) ghi ở đây theo câu 6, và chép vào mục 5 của `features/loading/spec.md` (quảng cáo mở app hiện sau loading) và `features/camera/spec.md` (quảng cáo khi quay lại app có thể hiện khi người dùng quay lại lúc đang ở camera) trong PR của T-074. Không thêm vị trí khác.

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

Sự kiện quảng cáo `ad_load_success`, `ad_load_fail`, `ad_clicked` có thêm tham số `ad_platform` (hôm nay luôn là `admob`, vì MAX chưa bật). Đây là tham số mới của sự kiện có sẵn, không phải sự kiện mới. Báo nhóm thu hút người dùng (UA) trước khi phát hành bản production đầu tiên có tham số này (`docs/ops/ads-business-rules.md`, HD-07; câu 20, ý e).

Tenjin (câu 21):

- Dịch vụ bên ngoài mới: Tenjin, nhận lượt cài và phiên mở app, doanh thu từng lượt quảng cáo hiện, các sự kiện phễu quảng cáo ở câu 21 ý b. Chỉ bản build có khóa Tenjin mới gửi (câu 21 ý e).
- Dữ liệu SDK Tenjin tự gửi kèm mỗi lượt, theo tài liệu của Tenjin và đọc mã của SDK 2.0.2: mã quảng cáo của máy, địa chỉ IP, nguồn cài đặt (Play Install Referrer, nguồn cài của Huawei, chợ ứng dụng nguồn), mã cài đặt riêng của Tenjin, mã cài đặt của Firebase Analytics trên máy (SDK tự đọc khi app có Firebase), trạng thái đồng ý (chuỗi TCF, cờ cá nhân hóa quảng cáo và dữ liệu người dùng cho quảng cáo theo luật DMA), hãng, đời máy, phiên bản Android, nhà mạng, kiểu kết nối, ngôn ngữ, quốc gia, múi giờ, số phiên và thời lượng phiên, tên gói và phiên bản app. Không gửi chữ người dùng nhập, ảnh hay mã người dùng (app không có tài khoản).
- Người dùng không đồng ý (câu 21 ý d): app không kết nối Tenjin và không gửi lượt nào. Không dùng chế độ opt out của SDK để thay, vì ở chế độ đó SDK vẫn gửi lượt kết nối với mã cài đặt và nguồn cài, chỉ bớt trường (đọc mã của SDK 2.0.2).
- Quyền: manifest gộp hôm nay đã có `com.google.android.gms.permission.AD_ID`, `com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE`, `INTERNET`, `ACCESS_NETWORK_STATE` (do SDK quảng cáo kéo vào), là các quyền Tenjin cần. Dự kiến không có quyền mới; plan kiểm manifest gộp trước và sau khi thêm SDK, có quyền hay khai báo `queries` mới thì quay lại spec này trước khi duyệt. Không bật phần đo nguồn cài của Meta (cần khai báo `queries` cho Facebook và Instagram, câu 21 ý g).
- An toàn dữ liệu trên Play Console và chính sách quyền riêng tư phải khai thêm Tenjin trước bản production đầu tiên có khóa Tenjin (câu 21 ý h).

Sự kiện phân tích mà khung ghi qua Firebase Analytics (nếu có) được plan liệt kê và chép vào mục 6 của spec màn có vị trí trong PR của T-074.

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

Không có cờ mới. Mỗi vị trí có khóa bật tắt Remote Config (câu 2): `is_show_review_interstitial`, `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_inter_pose_detail`, `is_show_poses_in_collection`; thêm `native_every_poses_in_collection` (mặc định 8). Câu 19 thêm `is_show_settings_bottom`, `is_show_review_bottom`, mặc định tắt. Câu 21 ý c: mỗi sự kiện gửi sang Tenjin có một khóa `is_log_tenjin_<tên sự kiện>`, boolean, mặc định bật. Khóa chung của thanh đáy theo câu 8, 10.

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

19. **Quảng cáo ở màn Cài đặt và màn xem lại ảnh** (yêu cầu `requests/2026-11-13-quang-cao-cai-dat-va-xem-lai.md`). Spec settings ghi "Không có" quảng cáo; spec camera ghi màn xem lại không có vị trí nào của màn (bài học 3: app mẫu bị chê vì quảng cáo che camera và ảnh).
    - Định dạng: thanh đáy native nhỏ ở cả hai màn, vị trí `settings_bottom` và `review_bottom`, cùng luật với `poses_bottom` (thay nội dung tại chỗ mỗi 60 giây, theo `is_show_native_bottom`, `is_show_native_collap` của câu 13 tới 15). Chọn "Thanh đáy native small" cho cả hai màn. Nhật, 2026-11-13.
    - Màn xem lại đi ngược một phần bài học 3; thanh nằm dưới hàng nút, không phủ lên ảnh. Nhật chọn phương án này sau khi được nhắc bài học 3. Nhật, 2026-11-13.
    - Lúc phát hành: tắt cả hai, nhóm bật sau bằng `is_show_settings_bottom`, `is_show_review_bottom` trên Firebase. Chọn "Tắt, bật sau trên Remote Config". Nhật, 2026-11-13.
    - **a. Bản staging bật sẵn hai vị trí?**
      - Đề xuất của AI: bật sẵn trong `StagingRemoteValuesModule`, như câu 11, để người thử thấy bố cục có quảng cáo; Firebase staging vẫn tắt được.
      - Trả lời: Chọn "Bật sẵn ở staging". Nhật, 2026-11-13.
    - **b. Thiết kế.** Thiết kế Cài đặt (sáng, tối) và xem lại (tối, mở gập) chưa có thanh quảng cáo. Ticket giao diện chỉ `ready` khi thiết kế đã duyệt.
      - Đề xuất của AI: thêm trạng thái `ad` lên Figma: `SettingsScreen_ad_light`, `SettingsScreen_ad_dark`, `ReviewScreen_ad_dark`, `ReviewScreen_ad_dark_unfolded`, dùng khối thanh đáy native nhỏ có sẵn của trạng thái `ad` ở thư viện; dev duyệt rồi lập lại gói bàn giao. Màn xem lại: ảnh co lại cho vừa phần trên hàng nút, giữ tỷ lệ, không bị cắt.
      - Trả lời: Chọn "Thêm trạng thái ad lên Figma": AI dựng bốn frame, Nhật duyệt rồi lập lại gói bàn giao; ticket giao diện `ready` khi thiết kế đã duyệt. Nhật, 2026-11-13.
    - **c. Màn xem lại ở các trạng thái khác.**
      - Đề xuất của AI: thanh hiện ở mọi trạng thái của màn xem lại (`default`, `saving`, `save-error`), cả khi mở từ ô ảnh vừa chụp ở camera; hộp thoại "Bỏ ảnh này?" và thông báo lỗi lưu phủ lên trên như hôm nay; nút Chia sẻ đi qua lối rời app có chủ đích như hôm nay, nên quảng cáo khi quay lại app không hiện ngay sau chia sẻ.
      - Trả lời: Chọn "Mọi trạng thái", theo đề xuất. Nhật, 2026-11-13.
    - **d. Đo kết quả trước khi bật `review_bottom` cho mọi người.**
      - Đề xuất của AI: bật dần trên Firebase (10% người dùng trước), giữ khi tỷ lệ lưu ảnh (`photo_saved` trên `photo_captured`) và tỷ lệ giữ chân ngày 1 không giảm quá 2 điểm sau một tuần; `settings_bottom` bật cho mọi người luôn khi nhóm muốn. Không thêm sự kiện mới.
      - Trả lời: Chọn "Bật dần 10% rồi xem số", theo đề xuất. Nhật, 2026-11-13.
    - **e. Lớp native thu gọn ở màn xem lại** (rà soát độc lập PR #323). Khóa chung `is_show_native_collap` bật lớp nổi 250 dp ở mọi thanh đáy; lớp này phủ lên nội dung và chặn chạm, nên sẽ che ảnh và hàng nút của màn xem lại khi nhóm bật nó cho màn khác.
      - Phương án: chấp nhận lớp nổi ở màn xem lại; hoặc không cho lớp nổi ở `review_bottom` (sửa khung theo mô hình chung, không thêm cờ mang tên tính năng vào khung, đề xuất về dự án mẫu).
      - Trả lời: chọn "Không cho lớp nổi ở `review_bottom`": màn xem lại luôn chỉ có khe đáy dưới hàng nút, dù `is_show_native_collap` bật. `settings_bottom` theo khóa chung như `poses_bottom`. Nhật, 2026-11-13.
    - Mã quảng cáo thật của hai vị trí trong cấu hình phát hành và khóa trên Firebase production là việc của người (ADR-0007, ADR-0009); vị trí tắt nên bản phát hành chưa cần mã khi chưa bật.

20. **Lấy phần mới của khung quảng cáo trong đợt đồng bộ dự án mẫu `698c5b3..bc3e80e`** (PR #351, ticket T-098). PR #351 giữ `core/ads` và hai tài liệu ads ở mốc cũ; T-098 đưa chúng lên. Phần mới đổi các điều sau ở cả staging và production:
    - **a. Kích thước native.** Khe native nhỏ ở đáy cao theo cạnh ngắn của cửa sổ: 90 dp ở 320 dp trở xuống, 200 dp ở 600 dp trở lên, tăng đều ở giữa. Khung chuẩn 411 x 891 dp ra 126 dp, khung mở gập 673 x 841 dp ra 200 dp; hôm nay khe cố định 80 dp. Bên trong thẻ cũng đổi theo thiết kế mới của khung (icon, nút, cỡ chữ, chỗ nhãn "Ad"). Lớp native thu gọn là vùng ảnh (160 tới 200 dp) cộng khe: 299 dp ở khung chuẩn, thay cho 250 dp; ở khung mở gập đứng dọc lớp cao 400 dp, gần nửa màn hình. Thẻ bước của luồng giới thiệu (250 dp) và thẻ trong lưới giữ kích thước.
      - Chỗ đang ghi số cũ: tiêu chí "thanh đáy native nhỏ (80 dp)" của spec poses (KB-6), camera (KB-9) và settings (KB-1); mục 2 của spec này ("lớp native thu gọn 250 dp"); các bước 80 dp, 250 dp của `docs/ops/manual-test.md`, mục 11.1; ảnh thiết kế trạng thái `ad` của thư viện, chi tiết, cài đặt, màn xem lại (cả `ReviewScreen_ad_dark_unfolded`) vẽ khe 80 dp.
      - Đề xuất của AI: nhận kích thước của khung (ADR-0045 của dự án mẫu là chuẩn nghiệp vụ chung). Ba spec màn và mục 2 bỏ con số, ghi "theo khung (`docs/ops/ads-business-rules.md`, D-02)". Thiết kế trạng thái `ad` vẽ lại theo component thẻ native mới trên Figma rồi lập lại gói bàn giao, trước khi duyệt plan T-098. Screenshot test cùng tên theo thiết kế mới. Plan đo ở khung thấp `w360dp-h640dp` để nút chính không bị che.
      - Trả lời: (Nhật, 2026-11-16) Nhận kích thước của khung. Ba spec màn và mục 2 bỏ con số, ghi theo khung (D-02). Thiết kế trạng thái `ad` vẽ lại theo component thẻ native mới trên Figma và lập lại gói bàn giao trước khi duyệt plan T-098.
    - **b. Lớp thu gọn khi cửa sổ thấp.** Lớp kể cả lề không cao quá nửa chiều cao cửa sổ: cửa sổ thấp thì vùng ảnh co lại, co tới dưới 132 dp thì lớp không mở, chỉ còn khe đáy. Trên app này là chia đôi màn hình, máy gập hay máy tính bảng nằm ngang; điện thoại khóa dọc nên không gặp khi xoay. Máy gập mở ra đứng dọc thì lớp vẫn mở (ý a). Vị trí `review_bottom` vẫn không bao giờ có lớp nổi (câu 19, ý e).
      - Đề xuất của AI: nhận như dự án mẫu.
      - Trả lời: (Nhật, 2026-11-16) Nhận như dự án mẫu.
    - **c. Thử lại native khi chưa có quảng cáo.** Khung giãn dần 5, 10, 20, 40, 60 giây thay cho thử lại đều 5 giây, đặt lại khi app quay lại foreground hay khi mạng về. Luật áp cho thanh đáy, ô trong lưới thư viện, thẻ luồng giới thiệu và combo native toàn màn hình.
      - Đề xuất của AI: nhận như dự án mẫu. Không có khóa Remote Config mới.
      - Trả lời: (Nhật, 2026-11-16) Nhận như dự án mẫu.
    - **d. Interstitial, rewarded, app open của MAX.** Khung thêm ba định dạng toàn màn hình của MAX và doanh thu của MAX. Như câu 16: không có khóa MAX thì MAX không khởi động, mọi vị trí tải từ AdMob như hôm nay. Không thêm thư viện hay quyền: SDK AppLovin đã có trong app.
      - Đề xuất của AI: nhận vào app, chưa bật, như câu 16. Bật MAX là quyết định sau, qua ticket riêng.
      - Trả lời: (Nhật, 2026-11-16) Nhận vào app, chưa bật, như câu 16. Bật MAX qua ticket riêng.
    - **e. Sự kiện quảng cáo có `ad_platform`.** Từ PR #351 (`:core:analytics` đã lấy), `ad_load_success`, `ad_load_fail`, `ad_clicked` gửi thêm `ad_platform=admob` lên Firebase. Đây là một tham số mới của sự kiện có sẵn, không phải sự kiện mới.
      - Đề xuất của AI: giữ. Báo nhóm thu hút người dùng (UA) trước khi phát hành bản production đầu tiên có tham số này (`ads-business-rules.md`, HD-07). Mục 6 ghi tham số khi spec được xác nhận.
      - Trả lời: (Nhật, 2026-11-16) Giữ. Báo nhóm UA trước bản production đầu tiên có tham số này; mục 6 đã ghi.

21. **Đo nguồn cài và doanh thu quảng cáo bằng Tenjin** (yêu cầu `requests/2026-11-17-do-nguon-cai-tenjin.md`). Nhật chọn trong phiên 2026-11-17: Tenjin làm ba việc (nguồn cài, doanh thu từng lượt quảng cáo, phễu quảng cáo); Tenjin thay AppsFlyer, AppsFlyer vẫn tắt (câu 3); quyết định riêng của app, không đề xuất về dự án mẫu. Quy tắc kỹ thuật (SDK chỉ trong `:core:analytics`, khóa không vào repo) ghi ở ADR-1010 của app; ADR-1007 giữ nguyên (AppsFlyer vẫn tắt). Nhật trả lời ý a tới i ngày 2026-11-17, theo đề xuất của AI.
    - **a. Doanh thu quảng cáo.** Tenjin chỉ tính doanh thu quảng cáo (ROAS) từ hàm doanh thu quảng cáo riêng của nó (với AdMob: hàm nhận giá trị lượt hiện mà AdMob báo; với AppLovin MAX: hàm riêng cho MAX), không từ sự kiện thường. Luật của AppsFlyer ở `ads-business-rules.md` mục 8.3 thì ngược lại (gửi bằng sự kiện thường vì công cụ kiểm của đối tác). Khung còn gửi lượt dự phòng giá trị 0 khi mạng không báo doanh thu.
      - Đề xuất của AI: dùng hàm doanh thu quảng cáo của Tenjin, một lần cho mỗi lượt hiện có doanh thu mạng báo; lượt AdMob qua hàm của AdMob, lượt MAX (khi MAX bật sau này) qua hàm của MAX. Không gửi lượt dự phòng giá trị 0, vì không phải doanh thu thật. Không gửi thêm doanh thu dưới dạng sự kiện thường, để Tenjin không cộng hai lần.
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **b. Sự kiện phễu quảng cáo.** Sự kiện thường của Tenjin chỉ có tên, kèm tùy chọn một số nguyên; không có tham số. Danh sách của AppsFlyer có tham số `ad_format`, `ad_unit_id`, `ad_source` sẽ mất khi sang Tenjin.
      - Đề xuất của AI: gửi cùng bộ sự kiện như mục 8.3, bỏ tiền tố `af_` vì đây không phải AppsFlyer: `inters_ad_eligible`, `inters_api_called`, `inters_displayed`, `rewarded_ad_eligible`, `rewarded_api_called`, `rewarded_ad_displayed`, `native_ad_eligible`, `native_api_called`, `native_ad_displayed`, `inters_displayed_{N}_times` (N từ 1 tới 20). Không có tham số. Điều kiện gửi giữ như mục 8.3. Nhóm UA cần tên khác (ví dụ giữ `af_` để so với app khác) thì ghi ở đây.
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **c. Công tắc từ xa cho từng sự kiện.** Khung có khóa `is_log_af_*` cho từng sự kiện AppsFlyer, mặc định bật, để tắt khẩn một sự kiện làm hỏng số liệu.
      - Đề xuất của AI: mỗi sự kiện Tenjin một khóa mới `is_log_tenjin_<tên sự kiện>` (ví dụ `is_log_tenjin_ad_revenue`, `is_log_tenjin_inters_displayed`, `is_log_tenjin_inters_displayed_n_times`), kiểu boolean, mặc định bật, đổi giữa phiên có hiệu lực từ sự kiện kế tiếp. Không dùng lại khóa `is_log_af_*`: một khóa mang tên AppsFlyer mà điều khiển Tenjin dễ gây nhầm. Khóa `is_log_af_*` vẫn giữ vì khung đọc chúng (ADR-1007).
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **d. Đồng ý của người dùng.** App hỏi đồng ý quảng cáo qua hộp thoại của Google (UMP) và ghi chuỗi đồng ý chuẩn (TCF). Tenjin có hàm tự đọc chuỗi này để bật hay tắt việc gửi.
      - Đề xuất của AI: như AppsFlyer ở ADR-0027: chỉ khởi động Tenjin sau khi có kết quả hỏi đồng ý của lượt mở app, hoặc ngay khi đồng ý của phiên trước còn giữ, cùng lúc với SDK quảng cáo; hết 10 giây chờ của màn loading thì khởi động với đồng ý đang có. Gọi hàm đọc chuỗi đồng ý của Tenjin trước khi kết nối; người dùng không đồng ý thì Tenjin không gửi gì. Người dùng ngoài vùng cần hỏi đồng ý thì gửi bình thường.
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **e. Khóa SDK Tenjin của bản phát hành và của staging.** Khóa là cấu hình riêng của app, không vào repo.
      - Đề xuất của AI: production nhận khóa lúc build bằng tham số `-PtenjinSdkKey`, như khóa MAX (M-09): tùy chọn, không có khóa thì bản phát hành không gửi gì sang Tenjin. Staging của CI không có khóa nên không gửi gì; dev muốn thử trên máy thì đặt `tenjin.sdkKey` trong `local.properties` (không vào git), dùng một app Tenjin riêng cho thử nghiệm để không lẫn số liệu thật. Thêm bước đưa khóa vào `scripts/release.sh` là việc của người (AI không chạm script phát hành), ticket riêng `auto_run: false`.
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **f. Quảng cáo có thưởng và mua hàng.** App có `rw_premium_pose` (rewarded) nhưng không bán gì trong app.
      - Đề xuất của AI: không gửi sự kiện mua hàng hay đăng ký sang Tenjin (app không có). Rewarded đi theo ý a và ý b như mọi định dạng khác.
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **g. Đo nguồn cài của Meta.** Tenjin đọc được nguồn cài từ quảng cáo Facebook, Instagram nếu app khai báo thấy hai app đó và mã app Facebook.
      - Đề xuất của AI: không bật lần này (thêm khai báo `queries` và mã app Facebook là dữ liệu chưa có trong yêu cầu). Nhóm UA chạy chiến dịch Meta thì làm ticket riêng.
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **h. Phát hành.** Nhóm UA cần biết từ bản nào có số liệu Tenjin; Data safety và chính sách quyền riêng tư phải có Tenjin trước khi bản đó lên Play.
      - Đề xuất của AI: người phát hành khai Data safety (mã quảng cáo và mã thiết bị hay mã cài đặt, tương tác với app và quảng cáo, chẩn đoán, như mục 6) và cập nhật chính sách quyền riêng tư trước bản production đầu tiên có khóa Tenjin; báo nhóm UA ngày bật, như HD-07. Hai việc này vào mục "Cần người kiểm tra" của ticket.
      - Trả lời: (Nhật, 2026-11-17) Theo đề xuất của AI.
    - **i. Lần mở đầu ở vùng cần hỏi đồng ý, khi hộp thoại đồng ý chưa kịp hiện.** Theo ý d, hết 10 giây chờ của màn loading thì khung khởi động SDK số liệu với đồng ý đang có. Lúc đó chưa có chuỗi đồng ý, và SDK Tenjin coi không có chuỗi là được gửi (như người dùng ngoài vùng cần hỏi đồng ý). Lượt cài của người dùng này gửi đủ trường; nếu họ từ chối sau đó thì từ lượt sau app mới thôi gửi, lượt đã gửi không rút lại được.
      - Đề xuất của AI: hoãn kết nối Tenjin khi chưa có kết quả hỏi đồng ý mà vùng cần hỏi (khung quảng cáo biết điều này qua UMP); kết nối ở lần app quay lại foreground hay lần mở sau, khi đã có câu trả lời. Ngoài vùng cần hỏi thì kết nối ngay như ý d. Cách này cần `:core:ads` báo cho `:core:analytics` "chưa có câu trả lời ở vùng cần hỏi", nên plan T-099 phải sửa thêm `:core:ads`. Phương án khác: nhận như ý d (giống AppsFlyer ở ADR-0027), chấp nhận lượt cài đầu của nhóm người dùng này.
      - Trả lời: (Nhật, 2026-11-17) Hoãn tới khi có câu trả lời. Cách làm: app chỉ kết nối và gửi sang Tenjin khi đồng ý đã rõ, tức vòng hỏi đồng ý của lượt mở này đã có câu trả lời (kể cả từ chối, kể cả "không cần hỏi" ở ngoài vùng), hoặc vòng hỏi lỗi mạng mà máy vẫn giữ đồng ý của phiên trước. Hết 10 giây chờ mà chưa có câu trả lời thì chưa kết nối; khi có câu trả lời thì kết nối nếu được đồng ý. Vòng hỏi lỗi mạng hay bị ngắt mà không có đồng ý cũ (ở bất kỳ vùng nào) chưa tính là rõ: bản phát hành không hỏi lại trong lượt mở đó, nên Tenjin chờ tới lần mở app sau (lượt cài gửi muộn chứ không mất).

22. **Lấy phần mới của khung quảng cáo trong đợt đồng bộ dự án mẫu `bc3e80e..7bf8c3b`** (PR #372, yêu cầu `requests/2026-11-21-dong-bo-quang-cao-mau-7bf8c3b.md`). PR #372 giữ `core/ads` và hai tài liệu ads ở mốc cũ; nhánh `template-sync/take-ads-7bf8c3b` đưa chúng lên (dựng ngày 2026-11-21). Phần mới đổi các điều sau ở cả staging và production (`docs/ops/ads-business-rules.md` của mốc mới):
    - **a. Kích thước native theo thẻ mới của khung** (D-02, mục 5.7). Native nhỏ hai tầng (hàng trên icon và chữ, tầng dưới nút ngang hết thẻ), cao 106 dp ở cỡ chữ mặc định ở mọi bề rộng và mọi hướng, cao thêm khi người dùng đặt chữ lớn; hôm nay khe đáy cao theo cạnh ngắn của cửa sổ (126 dp ở khung chuẩn 411 x 891 dp, 106 dp ở 360 dp, 200 dp ở khung mở gập 673 x 841 dp). Khoảng chừa dưới nội dung: 122 dp ở cỡ chữ mặc định. Lớp thu gọn giữ tổng chiều cao theo cửa sổ (299 dp ở khung chuẩn), phần nội dung giống native nhỏ, vùng ảnh lấy phần còn lại. Native medium cao 250 dp ở mọi khung (hôm nay 320 tới 400 dp theo chiều cao màn hình); app không dùng native medium. Nhãn "Ad" luôn đứng trước tiêu đề ở mọi cỡ chữ, dòng dưới tiêu đề là nhà quảng cáo (không có thì mô tả). Lớp thu gọn tính phần nội dung ở cỡ chữ hiện tại, nên chữ lớn cần cửa sổ cao hơn mới mở (CL-05; ở cỡ chữ mặc định, cửa sổ rộng 411 dp có lớp từ chiều cao 508 dp). Thẻ bước của luồng giới thiệu (250 dp) và thẻ trong lưới thư viện giữ kích thước.
      - Chỗ đang ghi số cũ: ảnh thiết kế trạng thái `ad` của thư viện, chi tiết, cài đặt, màn xem lại (9 ảnh, kể cả `ReviewScreen_ad_dark_unfolded`) vẽ khe 126 dp; các bước 126 dp của `docs/ops/manual-test.md`, mục 11.1; số đo ghim trong test của plan T-098. Spec màn đã ghi "cao theo khung (D-02)", không có con số.
      - Đề xuất của AI: nhận như dự án mẫu (ADR-0045 của mẫu là chuẩn nghiệp vụ chung). Thiết kế trạng thái `ad` vẽ lại theo component thẻ native mới trên Figma rồi lập lại gói bàn giao, trước khi duyệt plan. Screenshot test cùng tên theo thiết kế mới.
      - Trả lời: (Nhật, 2026-11-21) Nhận như mẫu. Vẽ lại thiết kế trạng thái `ad` trên Figma trước khi duyệt plan.
    - **b. Khoảng chừa khi khe đáy đang tải** (D-04, PR #531 của mẫu). Khe native hiện khung chờ ở lượt nạp đầu, và nội dung chừa đúng chỗ khung chờ chiếm, để khung chờ không đè lên thẻ hay dòng cuối. Nạp lỗi thì khe không vẽ gì, chừa 0; các lượt thử lại sau đó không hiện khung chờ, có quảng cáo mới chừa lại. Khe banner (khi `is_show_native_bottom` tắt, câu 13) cũng vậy: nội dung chừa chiều cao khung banner ngay khi thanh được phép, cả lúc đang tải; nạp lỗi trước quảng cáo đầu thì chừa 0 tới khi có quảng cáo; hôm nay chỉ chừa khi khe đã có quảng cáo. Khóa tắt thì không có thanh, chừa 0. Tiêu chí 3 của KB-6 ở spec poses đang ghi "quảng cáo đang tải thì không chừa khoảng trống; nội dung chỉ được chừa chỗ khi khe đã có quảng cáo", trái luật mới.
      - Đề xuất của AI: nhận như dự án mẫu. Tiêu chí 3 của poses KB-6 viết lại: "Khi khóa tắt hay quảng cáo không tải được thì không chừa khoảng trống. Khi khe đang tải lượt đầu thì nội dung chừa đúng chỗ khe chiếm (khung chờ của native, khung của banner); thẻ cuối không bị che."
      - Trả lời: (Nhật, 2026-11-21) Nhận như mẫu. Tiêu chí 3 của poses KB-6 viết lại theo đề xuất.
    - **c. Khe đáy nhường chỗ cho lớp thu gọn** (D-07, MC-14, T-090 của mẫu). Khi lớp thu gọn đang mở, khe đáy bên dưới không giữ quảng cáo, không vẽ khung chờ, không nạp, không làm mới, không ghi số liệu; người dùng thu gọn lớp thì khe hiện lại như một lượt hiện mới. Khoảng chừa giữ đúng như lúc lớp mở. Lớp chưa có quảng cáo, nạp lỗi hay không đủ chỗ (CL-05) thì không tính là mở. Trên app này lớp thu gọn tắt lúc phát hành (câu 10), nên chỉ có hiệu lực khi bật `is_show_native_collap` từ xa.
      - Đề xuất của AI: nhận như dự án mẫu.
      - Trả lời: (Nhật, 2026-11-21) Nhận như mẫu.
    - **d. Mỗi màn tối đa một quảng cáo neo đáy** (MC-12, T-091 của mẫu). Thanh đáy, banner cố định và banner thu gọn là quảng cáo neo đáy; màn đặt hai cái thì cái sau không nạp, không chiếm chỗ. Mỗi màn của app chỉ có một thanh đáy, nên không đổi gì trên màn hình.
      - Đề xuất của AI: nhận như dự án mẫu.
      - Trả lời: (Nhật, 2026-11-21) Nhận như mẫu.
    - **e. Quảng cáo trong nội dung thắng lớp nổi** (MC-13, T-092 của mẫu). Màn có vị trí quảng cáo trong nội dung (native trong lưới hay danh sách, thẻ bước của luồng, native nhỏ hay medium đứng riêng) không có lớp thu gọn nổi, dù `is_show_native_collap` bật, khi ít nhất một vị trí trong nội dung đó đang bật (`is_show_<mã vị trí>`); mọi vị trí đó tắt thì lớp nổi hiện như thường. Trên app: thư viện có `poses_in_collection` (tắt lúc phát hành, bật ở staging, câu 9, 11), nên `poses_bottom` không có lớp nổi khi `is_show_poses_in_collection` bật, và có lớp nổi như thường khi khóa đó tắt; cài đặt giữ lớp nổi khi bật; màn xem lại vốn không có (câu 19, ý e); luồng giới thiệu không có thanh đáy.
      - Đề xuất của AI: nhận theo luật. Thêm tiêu chí vào KB-6 của spec poses: "Khi `is_show_native_collap` và `is_show_poses_in_collection` cùng bật thì thư viện không có lớp quảng cáo nổi; khi `is_show_poses_in_collection` tắt thì lớp nổi hiện như thường."
      - Trả lời: (Nhật, 2026-11-21) Lần hỏi đầu ghi sai luật ("thư viện không bao giờ có lớp nổi"), Nhật chọn "Nhận". Hỏi lại với luật đúng, Nhật chọn "Theo MC-13": không có lớp nổi khi ô lưới bật, ô lưới tắt thì lớp nổi như thường.
    - **g. Chi tiết tư thế khi bật lớp nổi.** Lớp thu gọn nổi cao khoảng 299 dp ở khung chuẩn phủ lên nút chính "Chụp với tư thế này" (người dùng thu gọn lớp thì nút hiện lại), trái tiêu chí 2 của KB-6 ở spec poses ("nút không bị che"). Chưa từng bàn; lớp nổi hôm nay tắt lúc phát hành.
      - Đề xuất của AI: khai báo `pose_detail_bottom` không có lớp nổi (`collapsibleLayer = false`, D-01), như `review_bottom` (câu 19, ý e). Không thêm vị trí hay khóa; đổi khai báo của vị trí có sẵn.
      - Trả lời: (Nhật, 2026-11-21) Không có lớp nổi.
    - **f. Biết trước hành động có thưởng có hiện quảng cáo không** (R-08, PR #526 của mẫu). Khung có thêm hàm cho app biết lần bấm vào hành động có thưởng lúc này sẽ mở hộp thoại hay làm thẳng, để gắn dấu "xem quảng cáo". App không dùng hàm này: tư thế premium luôn có dấu khóa (spec poses, KB-8).
      - Đề xuất của AI: nhận vào khung, app không dùng. Muốn đổi dấu khóa theo hàm này thì làm ticket riêng.
      - Trả lời: (Nhật, 2026-11-21) Nhận vào khung, app không dùng.

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
| 2026-11-13 | AI | Yêu cầu quảng cáo ở màn Cài đặt và màn xem lại (`requests/2026-11-13-quang-cao-cai-dat-va-xem-lai.md`); Nhật trả lời phần đầu câu 19 trong phiên (AI ghi): thanh đáy native nhỏ `settings_bottom`, `review_bottom`, tắt lúc phát hành. Sửa mục 2, 5, 8; câu 19 còn bốn ý mở. Chuyển `changed`, chờ trả lời và xác nhận |
| 2026-11-13 | Nhật | Trả lời câu 19, ý a tới d (AI ghi): staging bật sẵn, thêm trạng thái `ad` trên Figma, thanh ở mọi trạng thái của màn xem lại, production bật dần 10%. Xác nhận spec. Lời xác nhận của Nhật: chọn "Xác nhận, chuyển confirmed" khi được hỏi. Chuyển lại `confirmed` |
| 2026-11-13 | Nhật | Theo rà soát độc lập PR #323: sửa tiêu chí trái mục 5 mới (KB-1 của spec settings; KB-9, KB-11 của spec camera), thêm khóa ở mục 8, trạng thái `ad` ở mục 4; câu 19 ý e: không cho lớp native thu gọn ở `review_bottom` (AI ghi). Lời xác nhận lại của Nhật: chọn "Xác nhận, chuyển confirmed" khi được hỏi. Giữ `confirmed` |
| 2026-11-13 | Nhật | Sửa vòng 2 theo rà soát PR #323 (AI ghi): `is_show_native_bottom` tắt thì khe đáy là banner (câu 13), thêm tiêu chí banner; spec settings thêm tiêu chí lớp nổi thu gọn; ghi thanh 80 dp theo khung hiện tại; spec camera đổi tên KB-9 thành "Quảng cáo ở màn xem lại". Chuyển `changed` rồi lại `confirmed`; lời xác nhận của Nhật: chọn "Xác nhận" khi được hỏi |
| 2026-11-16 | AI | Đồng bộ dự án mẫu PR #351 giữ `core/ads` ở mốc cũ; thêm câu 20 (kích thước native mới, lớp thu gọn ở màn ngang, thử lại giãn dần, MAX toàn màn hình chưa bật, `ad_platform`) cho ticket T-098. Chuyển `changed`, chờ Nhật trả lời và xác nhận |
| 2026-11-16 | Nhật | Trả lời câu 20, ý a tới e, theo đề xuất của AI (AI ghi): nhận kích thước native và lớp thu gọn của khung, thử lại giãn dần, MAX toàn màn hình có sẵn nhưng chưa bật, giữ `ad_platform` và báo UA. Sửa mục 2, mục 6; tiêu chí thanh đáy của spec poses, camera, settings bỏ con số 80 dp. Chờ Nhật xác nhận |
| 2026-11-16 | Nhật | Xác nhận phần đổi theo câu 20 của spec ads (AI ghi). Lời xác nhận của Nhật: chọn "Tôi là Nhật, xác nhận" khi được hỏi xác nhận cả bốn spec theo nội dung vừa sửa. Chuyển lại `confirmed` |
| 2026-11-16 | Nhật | Xác nhận lại sau sửa theo rà soát độc lập PR #353 (AI ghi): `ad_platform` ở mục 6 của spec poses, camera; mục 2 của spec ads ghi thẻ bước và thẻ trong lưới giữ kích thước, lớp thu gọn là vùng ảnh cộng khe đáy D-02. Lời xác nhận của Nhật: chọn "Xác nhận lại" khi được hỏi. Giữ `confirmed` |
| 2026-11-17 | AI | Yêu cầu đo nguồn cài và doanh thu quảng cáo bằng Tenjin (`requests/2026-11-17-do-nguon-cai-tenjin.md`); Nhật chọn trong phiên (AI ghi): Tenjin làm ba việc, thay AppsFlyer, riêng của app. Sửa mục 2, 3 (KB-T1), 6, 8; câu 21 có tám ý mở (a tới h). Chuyển `changed`, chờ Nhật trả lời và xác nhận |
| 2026-11-17 | Nhật | Trả lời câu 21, ý a tới h, theo đề xuất của AI (AI ghi): hàm doanh thu riêng của Tenjin, bỏ lượt 0; phễu bỏ tiền tố `af_`, không tham số; khóa `is_log_tenjin_*` mặc định bật; khởi động sau đồng ý; `-PtenjinSdkKey` tùy chọn; không mua hàng, không Meta; Data safety và báo UA trước production. Lời của Nhật: chọn "Bỏ tiền tố af_", "Hàm riêng, bỏ lượt 0", "-PtenjinSdkKey tùy chọn", "Nhận cả năm đề xuất" khi được hỏi. Chờ Nhật xác nhận |
| 2026-11-17 | AI | Theo rà soát độc lập PR #360: mục 6 ghi đủ dữ liệu SDK Tenjin gửi (đọc mã SDK 2.0.2) và việc không kết nối khi không đồng ý (chế độ opt out của SDK vẫn gửi); câu 21 sửa câu về ADR-1010, ý h thêm mã cài đặt, thêm ý i (lần mở đầu chưa có đồng ý) còn mở. Chờ Nhật trả lời ý i và xác nhận |
| 2026-11-17 | Nhật | Trả lời câu 21 ý i (AI ghi): hoãn kết nối Tenjin tới khi đồng ý đã rõ; thêm tiêu chí của KB-T1. Lời của Nhật: chọn "Hoãn tới khi có trả lời" khi được hỏi. Chờ Nhật xác nhận spec |
| 2026-11-17 | AI | Theo rà soát lại vòng 2 PR #360: câu trả lời ý i ghi đúng cách khung hỏi lại (lỗi mạng hay bị ngắt mà không có đồng ý cũ thì chờ lần mở sau). Không đổi lựa chọn của Nhật. Chờ Nhật xác nhận |
| 2026-11-17 | Nhật | Xác nhận phần đổi theo câu 21 (Tenjin, ý a tới i), mục 2, 3 (KB-T1), 6, 8 của spec ads, theo PR #360 ở commit 5759b65 (AI ghi). Lời xác nhận của Nhật: chọn "Xác nhận spec, nhận ADR" khi được hỏi xác nhận nội dung PR #360 bản mới nhất. Chuyển lại `confirmed` |
| 2026-11-21 | AI | Yêu cầu đồng bộ khung quảng cáo của dự án mẫu lên `7bf8c3b` (`requests/2026-11-21-dong-bo-quang-cao-mau-7bf8c3b.md`); thêm câu 22 và một dòng ở mục 2. Nhật chọn trong phiên (AI ghi): nhận kích thước native mới, nhận thư viện không có lớp nổi, vẽ lại thiết kế trạng thái `ad` trước khi duyệt plan. Chuyển `changed`, chờ Nhật trả lời ý b, c, d, f |
| 2026-11-21 | Nhật | Trả lời câu 22 ý b, c, d, f theo đề xuất của AI (AI ghi): nhận khoảng chừa khi đang tải, khe nhường chỗ lớp thu gọn, một quảng cáo neo đáy mỗi màn; hàm biết trước hành động có thưởng có trong khung, app không dùng. Lời của Nhật: chọn "Nhận như mẫu" (ý b) và "Nhận cả ba" (ý c, d, f) khi được hỏi. Sửa tiêu chí 3 và thêm tiêu chí lớp nổi ở KB-6 của spec poses. Chờ Nhật xác nhận |
| 2026-11-21 | Nhật | Theo rà soát độc lập PR #374 (AI ghi): câu 22 ý e ghi sai luật MC-13 (thư viện chỉ mất lớp nổi khi ô lưới bật); hỏi lại, Nhật chọn "Theo MC-13". Thêm ý g: chi tiết tư thế không có lớp nổi, Nhật chọn "Không có lớp nổi". Sửa ý a (app không dùng medium; nhãn "Ad", CL-05 theo cỡ chữ), ý b (khe banner cũng chừa khi đang tải), ý c (khi nào lớp tính là mở), dòng ở mục 2. Chờ Nhật xác nhận |
