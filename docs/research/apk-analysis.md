# Phân tích tĩnh APK: 8 app pose camera

Ngày: 2026-10-19. Chỉ giải nén và đọc. Không chạy code trong APK, không tải nội dung từ mạng.

- APK gốc: `apks/<package>/`
- Kết quả giải nén: `decoded/<package>/jadx/` (mã và resource từ jadx), `decoded/<package>/raw/assets/` (asset giải nén bằng unzip)
- Id đơn vị quảng cáo, khóa API, token: chỉ ghi có hay không, không chép.

## Bảng so sánh

| App | Công nghệ | minSdk / target | Quảng cáo | Mediation | Firebase | ML | Nguồn tư thế | Tư thế đóng gói |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `com.camera.filter.pose.guide.ideas` (Pose Camera) | View + ViewBinding, Kotlin | 32 / 36 | AdMob: app open, inter, native, banner, banner thu gọn | AdMob + AppLovin, Meta, Mintegral, Pangle | Analytics, Remote Config, Crashlytics, Messaging | Không | Drawable trong APK | 32 (nét vẽ trắng) |
| `com.camera.filter.pose.guide.ideas.photo` (Pose Camera: Aesthetic Guides) | View + ViewBinding, Kotlin | 32 / 36 | Như trên, thêm màn "welcome back" | AdMob + AppLovin, Meta, Pangle, Liftoff (Vungle) | Như trên | Không | Drawable trong APK | 38 |
| `com.camera.guide.ideas.photo.pose` (Pose Camera) | View + ViewBinding, Kotlin | 32 / 36 | Như trên, thêm quảng cáo ở màn gỡ app | AdMob + AppLovin, Meta, Mintegral, Pangle | Như trên | Không | Drawable trong APK | 24 cặp ảnh thật + nét viền |
| `com.posecamera.poseideas` (Pose Camera – Photo Pose Ideas) | View, Kotlin | 32 / 37 | AdMob: inter, native, native toàn màn hình, app open | AdMob + AppLovin, Meta, Mintegral, Pangle, Unity, IronSource, Liftoff | Như trên | Không | Drawable trong APK | 38 (trùng ảnh với `.photo`) |
| `com.aiposing.pose` (AI Posing) | Compose, Kotlin, một Activity | 32 / 36 | AdMob (cả GMA next-gen): app open, inter, reward, native, native toàn màn hình | AdMob + AppLovin, Meta, InMobi, Mintegral, Pangle, Unity | Như trên | ML Kit Pose (3 model), Selfie Segmentation, Object Detection, Image Labeling | API riêng `apiposephoto.aifastwork.com` | 0 |
| `com.cam.simplepose.pose.camera.photo.guide` (SimplePose) | View + Fragment, Kotlin, Koin | 28 / 36 | AdMob: app open, inter, reward, native, native toàn màn hình, native thu gọn | AdMob + AppLovin, Meta, Mintegral, Pangle, Unity/LevelPlay, Liftoff, DT Fyber, Chartboost | Analytics, Remote Config, Crashlytics | Không có bộ nhận dạng tư thế; dữ liệu đã có 33 điểm khớp | Asset + catalog từ xa (GitHub raw, kho `storage.bralyvn.com`) | 24 |
| `com.gws.posemate` (PoseMate) | Compose, Kotlin, Hilt, Room | 26 / 36 | AdMob: banner thích ứng, inter, reward | Không (chỉ AdMob) | Analytics, Crashlytics | ML Kit Pose (base) | Asset + Play Asset Delivery (gói `poses`) | 1.629 tư thế (3.258 ảnh) |
| `com.vector123.posecamera` (PoseCamera) | Flutter (Dart), camera dùng CameraX | 26 / 36 | Không | Không | Không | Không | Asset Flutter | 120 (nét vẽ) |

Ghi chú chung:
- Mọi app dùng CameraX. Không app nào dùng Camera2 trực tiếp, MediaPipe hay TFLite riêng ngoài model của ML Kit.
- Có Play Billing: cả 8 app. App có màn trả phí rõ ràng: AI Posing, SimplePose, PoseMate (gói gỡ quảng cáo), PoseCamera Flutter (Pro). Nhóm Pose Camera có khóa `monthly_pro`, `yearly_pro`, `trial_yearly_pro` trong strings nhưng không thấy màn trả phí trong layout (chỉ `bg_iap.png`).
- 7/8 app có Pairip (chống sửa APK của Google Play), trừ `.photo`. App dùng nhiều mạng quảng cáo đều có thêm Adjust hoặc AppsFlyer để đo nguồn cài đặt, và APM Insight của ByteDance (đi kèm Pangle).

---

## 1. com.camera.filter.pose.guide.ideas (Pose Camera, 1.0.2)

**Công nghệ.** View, ViewBinding, Kotlin. minSdk 32, targetSdk 36. Ngôn ngữ app: en, ar, bn, de, es, fr, hi, in, it, ja, ko, pt, ru, tr, vi.

**SDK.**
- Thư viện quảng cáo nội bộ `com.cyber.ads`. AdMob là mạng chính. Có adapter AdMob cho AppLovin, Meta, Mintegral, Pangle.
- Loại quảng cáo: app open lúc mở, interstitial, native (nhiều mẫu `native_template_big1..5`, `small1..5`, `tiny1..2`, `full`, `collap`), banner, banner thu gọn. Có id đơn vị trong remote_config_defaults.xml: có.
- Firebase: Analytics, Remote Config, Crashlytics, Messaging. UMP. Play Review, Play App Update. Adjust, Facebook SDK, TikTok.
- Lottie, Glide, Room. Thư viện `sdp/ssp` (kích thước theo màn hình).

**Nội dung tư thế.** Đóng gói sẵn. `res/drawable-xxhdpi/im_<nhóm>_<n>.webp`. 32 ảnh: selfie 9, couple 5, family 4, friend 5, kid 4, sport 5. WebP RGBA, phần lớn 1164 x 1242, nét vẽ trắng trên nền trong. Danh sách nhóm viết cứng trong code. Không tải từ mạng.

**Remote Config.** Một khóa `ad_config`, giá trị là JSON:
- `settings`: `ads_enable`, `language_delay_millis`, `language_setup`, `intro1_setup..intro3_setup` (dạng `next=1,ds=1,delay=0`), `intro_swipe`, `on_resume`.
- Từng vị trí: `ads_splash`, `native_language`, `native_small_language`, `inter_language`, `native_intro`, `native_intro_full`, `inter_intro`, `inter_home`, `inter_back`, `inter_view_image`, `banner_home`, `banner_camera`, `banner_gallery`, `native_custom_home`.
- Mỗi vị trí có `enable` (số chọn loại: inter, native thay inter, cả hai), `step_count` (tần suất inter: mỗi N lần bấm), `native_template`, với banner có `refresh_rate` 20 và `wait_time`.

**Giao diện.** Nền đen. Chữ trắng, chữ phụ `#dadada`. Màu nhấn vàng `#e5b43f`, đỏ `#ec2045`. Font Nunito SemiBold. Cỡ chữ chính 12, 14, 16, 20 sp.
Màn hình: Splash, Language, Intro (3 trang), Main (home), Camera, Detail, Gallery, Setting, Uninstall và Uninstall Survey. Hộp thoại: rating, feedback, save, delete, info, no internet, loading trước inter.

**Chuỗi chữ.** "POSE GUIDE", "Pose Inspiration", "Take Photo with Guide", "Get Pose Inspiration", "Save and Share Photos", "Shutter sound", "Your Gallery". Khảo sát gỡ app: "Too many ads", "I didn't understand how to use it", "Feature is not working". Chính sách trên `tokiiapps.top` (Tokii Apps).

## 2. com.camera.filter.pose.guide.ideas.photo (Pose Camera: Aesthetic Guides, 1.0.6)

**Công nghệ.** Như app 1. Thêm màn chính dạng fragment (`fragment_home`, `fragment_collection`, `fragment_setting`, `layout_bottom` là thanh tab). Ngôn ngữ: en, ar, bn, de, es, fr, hi, in, it, ja, ko, pt, ru, tr.

**SDK.** Thư viện quảng cáo đổi sang `com.dino.ads` (bản mới hơn của cùng kiểu thư viện). Adapter: AppLovin, Meta, Pangle, Liftoff. Thêm OkHttp. Còn lại như app 1.

**Nội dung tư thế.** 38 ảnh trong drawable: couple 6, family 6, friend 8, kid 5, selfie 6, sport 7. Ảnh cỡ 1170 x 1557 đến 2568 x 2568.

**Remote Config.** `ad_config` như app 1, thêm `native_language_first`, `native_language_small_first`, `native_intro_first`, `inter_collection`, `inter_tab`, `inter_welcome_back`, `native_welcome_back`, `native_splash`. Mặc định `ads_enable` = 0, có `release_log`.

**Giao diện.** Nền đen, màu nhấn tím `#a68df9`, chữ tím nhạt `#a88ef9`, `#b5adfc`. Font Inter 18pt (Regular, Medium, SemiBold, Bold), Baloo 2, Titan One, Roboto. Cỡ chữ 12, 14, 20, 24 sp.
Màn hình: Splash, Language (có nhóm ngôn ngữ cha con), Intro 4 trang, Main (Home, Collection, Setting), Camera, Detail, Gallery, Welcome Back.

**Chuỗi chữ.** Nhóm: Selfie, Couple, Family, Friend, Kid, Sport. "Strike Your Perfect Pose", "START POSING", "Discover how to pose for every moment", "Flash", "Timer", "Welcome back". Chính sách trên `oceanstore.top` (Ocean Limousine Limited).

## 3. com.camera.guide.ideas.photo.pose (Pose Camera, 1.0.0)

**Công nghệ.** Như app 1 (thư viện `com.cyber.ads`). Ngôn ngữ: en, ar, bn, de, es, fr, hi, in, it, ko, pt, ru.

**SDK.** Như app 1.

**Nội dung tư thế.** 4 nhóm: selfie 8, couple 5, friend 6, family 5. Mỗi tư thế là một cặp: ảnh thật `im_<nhóm>_<n>.webp` (1500 x 1500) và nét viền `im_<nhóm>_<n>_overlay.webp`. Cấu trúc trong code: `PoseCategory(tên, ảnh nhóm, List<PoseItem(realImageResId, overlayImageResId)>)`. Có thêm màn Template (bắt đầu từ tư thế đầu của nhóm).

**Remote Config.** `ad_config` như app 1, thêm `native_uninstall`, `native_uninstall_survey`, `inter_uninstall_survey`, `inter_uninstall`, `banner_template`, `inter_template`, `inter_gallery`, `inter_detail`, `banner_detail`, `banner_information`. Ví dụ mặc định: `inter_home` và `inter_back` có `step_count` 2; banner home và camera làm mới 20 giây.

**Giao diện.** Nền đen, màu nhấn hồng tím `#d862f5`, chữ hồng `#ff81d5`, `#c646ce`. Font Inter 18pt, Baloo 2, Nunito. Màn hình: Splash, Language, Intro, Home, Collection (album), Camera, Detail, Template, Information, Setting, Confirm Uninstall, Uninstall Survey.

**Chuỗi chữ.** Như app 2, thêm "Template", "Album", "Information". Chính sách trên `smartexperiences.top` (Smart Experiences).

## 4. com.posecamera.poseideas (Pose Camera – Photo Pose Ideas, 1.2)

**Công nghệ.** View, Kotlin. minSdk 32, targetSdk 37. Ngôn ngữ phát qua split APK (có split `vi` chứa chuỗi app). Có màn chọn nhóm ngôn ngữ và màn "đang áp dụng ngôn ngữ".

**SDK.** AdMob + adapter AppLovin, Meta, IronSource, Mintegral, Pangle, Unity, Liftoff. Adjust, AppsFlyer. Firebase Analytics, Remote Config, Crashlytics, Messaging. Lottie, Glide, Room, OkHttp. Mã bị làm rối mạnh (gói `defpackage`).

**Nội dung tư thế.** 38 ảnh drawable, trùng từng byte với 39 file `im_*` của app 2. Ảnh chính 1500 x 1500. Thêm 6 ảnh `im_home_*.png` cho trang chủ.

**Remote Config.** Không có file mặc định. Tên khóa thấy trong code: `inter_splash`, `inter_onb`, `inter_home`, `inter_resume`, `native_splash`, `native_language_first`, `native_language_click`, `native_on_boarding_first`, `native_on_boarding_four`, `native_main`, `native_full_one`, `native_full_two`, `native_exit_dialog`.

**Giao diện.** Sáng. Nền `#f4f1fe`, `#f8f7fc`. Màu chính tím `#9368fa`, `#9e80f9`. Chữ đậm `#1e1e2e`, `#0d1120`, chữ phụ `#7d8597`. Dải vàng `#ffb62d` → `#ffce14`. Font Inter, Baloo 2. Cỡ chữ 11, 12, 14, 22, 26 sp.
Màn hình: Splash, LanguageGroup, LanguageApplying, OnBoarding (4 trang), Main (Home, Collection, Setting), Camera, Detail, Exit (hộp thoại thoát có native).

**Chuỗi chữ.** Như app 2, thêm "Your everyday pose guide for picture-perfect moments", "Think, Tap & Escape", "Confirm Exit?", "This action might contain Ad".

## 5. com.aiposing.pose (AI Posing: Pose Ideas, Guide, 1.4.0)

**Công nghệ.** Jetpack Compose, Kotlin, một `MainActivity`, Navigation Compose với route kiểu lớp. minSdk 32, targetSdk 36. Ngôn ngữ: en, ar, br, cs, de, es, fr, hi, in, it, ja, ko, ms, phi, pl, pt, ru, th, tr, tw, vi, zh.

**SDK.**
- AdMob và GMA next-gen SDK. Adapter: AppLovin, Meta, InMobi, Mintegral, Pangle, Unity.
- Loại: app open (2 tầng id cao, trung bình), inter splash, inter trong app, rewarded, rewarded interstitial, native nhỏ, native toàn màn hình (onboarding, tutorial, sau trả phí).
- Firebase Analytics, Remote Config, Crashlytics, Messaging. AppsFlyer (deep link), Adjust. WorkManager cho thông báo quay lại.
- ML Kit: Pose Detection (3 model tflite, 12 MB, có `SmoothedLandmark` để làm mượt điểm khớp), Selfie Segmentation, Object Detection, Image Labeling (để "AI quét xung quanh" rồi gợi ý tư thế).
- Play Billing. Kiểm tra giao dịch qua `checkiap.betasoftmobile.com`.

**Nội dung tư thế.** Không đóng gói. Tải từ API `https://apiposephoto.aifastwork.com`:
- `/api/v1/category/getAll`, `/api/v1/style/getAll`, `/api/v1/photo/getByCategoryID`, `/api/v1/photo/getDetail`, ảnh ở `/api/v1/image/`.
- Danh mục: `{id, name, style: [{id, name}], createdAt, updatedAt}`.
- Ảnh tư thế: `{id, name, content, path, outline, videoPath, width, height, createdAt, updatedAt}`. Mỗi tư thế có ảnh thật, ảnh nét viền và có thể có video.
- Drawable chỉ có ảnh minh họa nhóm (indoor, outdoor, posture) và ảnh onboarding, trả phí.

**Remote Config.** Rất nhiều cờ bật tắt màn hình và quảng cáo: `inter_splash`, `inter_inapp`, `open_app`, `open_high_unit_ads`, `open_medium_unit_ads`, `rewarded_interstitial`, `native_ob1..3`, `native_language1/2`, `fsn_onboard`, `native_small_json`, `native_explore_json`, `time_show_ads`, `time_show_open_ads`, `time_out_inter_splash`, `time_out_open_ads`, `time_out_reward`, `time_refresh_native`, `show_iap`, `show_iap_from`, `is_show_iap_onboard`, `is_show_inter_after_iap_onboard`, `show_dialog_rewarded_ads`, `show_dialog_more_pose`, `show_rate`, `show_offline_mode_dialog`, `show_outline_pose`, `is_show_overlay_pose`, `show_camera_with_suggestion`, `show_tutorial_1..4`, `iap_sub_week`, `iap_sub_year`, `iap_sub_monthly`, `sub_week_aipose`, `sub_year_aipose`, `domain_app`.

**Giao diện.** Màu trong code Compose (colors.xml chỉ có `color_button` xanh `#27c488`). Font: Arimo, Inter (Regular, SemiBold, Black), Roboto Medium. Có video splash và GIF minh họa nét viền.
Màn hình (route): Splash, StartupTwo, Language, Onboard, Main (tab Home, Explore, Trending, Album), ExploreNow, ExploreDetails, Trending, Camera, OptionTakePhoto, CreatedImageDetails, ViewCreatedImage, AlbumDetails, Premium, IapJan, NativeFullIAP, ProblemRateUs, RemoveApp.

**Chuỗi chữ.** Nhóm: Selfie, Couple, Family, Maternity, Newborn, Portrait, Beach, Coffee, Forest, Ocean, Sunset, Indoor, Outdoor. Tab: Home, Explore, Trending, Personal library. Công cụ: Overlay, Outline, "AI Recommend", "Smart Suggestions", "Let's our AI scan your surroundings", "Perfect! Snap Your Photo!", bộ lọc (Bright, Cool, Dreamy, Moody, Sepia, Vintage, Warm). Trả phí: "Unlimited Poses", "No more Ads", "Unlimited new album creation", "Unlimited image save formats", "Try 3 Days For Free", "Save up to %d%%", gói tuần, tháng, năm. Mở khóa bằng reward: "Just watch a short video to unlock". Thông báo theo giờ trong ngày (morning, afternoon, evening) và theo thời gian vắng (1h, 3h, 7h).

## 6. com.cam.simplepose.pose.camera.photo.guide (SimplePose, 1.0.1)

**Công nghệ.** View, Fragment, một `MainActivity`, Kotlin, Koin, Room. minSdk 28, targetSdk 36. Ngôn ngữ: en, de, es, fr, in, ja, nl, pl, pt-BR, vi. Nhà làm: Braly (Đà Nẵng, Việt Nam), log viết bằng tiếng Việt.

**SDK.**
- Thư viện quảng cáo `com.bralydn.ads`. AdMob, GMA next-gen. Adapter: AppLovin, Chartboost, Meta, DT Fyber, IronSource, Mintegral, Pangle, Unity, Liftoff.
- Loại: app open, inter (splash, category, back, camera), reward (mở thêm tư thế), native trong danh sách, native chi tiết, native toàn màn hình, native thu gọn.
- Firebase Analytics, Remote Config, Crashlytics. AppsFlyer, Facebook SDK. Play Billing, Play Review. WebRTC (`libjingle`) cho chế độ Mirror. CustomActivityOnCrash, PermissionX.

**Nội dung tư thế.** Kết hợp.
- Asset `assets/pose-data/`: 24 tư thế (Woman 12, Man 12). Mỗi thư mục mã băm có `pose.json`, `preview_1200.webp`, `preview_540.webp`, `ink_outline@2x.png` (nét viền, cao 2048).
- Danh mục `pose-dataset.json`: `datasetVersion`, `minAppSchema`, `categories` (Women, Men, Couples, Besties, Friends), `facets` (framing), `vocabNames` (56 nhãn: airport, áo dài, cafe…), `searchAliases`, `poses`.
- Mỗi tư thế: `id`, `categoryId`, `subjectType`, `cameraOrientation`, `soloFriendly`, `tags`, `framing`, `poseFamily`, `previewImage {url, width, height, thumbnail}`, `overlayInk {image, detailed}`, `targetKeypoints` (33 điểm khớp chuẩn MediaPipe, tọa độ 0..1 theo khung nét viền).
- Catalog mới tải từ GitHub raw (`Braly-Danang-3/SimplePose_Data/main/PoseData_V1/`) có token trong app (có: có), dự phòng ở `storage.bralyvn.com`. Dùng ETag, ghi file tạm rồi áp dụng ở lần mở sau, thử 3 lần.
- Điểm khớp chưa dùng để nhận dạng trên máy: không có ML Kit hay MediaPipe trong bản này.

**Remote Config.** Cấu hình quảng cáo mặc định `assets/ads_config.json`: `configs {enable, interval: 30, app_id}`, `placements` (splash, full_backup, native_backup, native_view_list, native_onboard2, native_onboard3, native_category, item_reward, native_list, native_icon_reload, full_category, native_detail, native_detail_icon, native_open_camera, full_back, full_camera), `ad_item` (đơn vị, bật tắt, `native_config {layout_res, should_reload, blur_background}`), `units` (danh sách mạng và id theo thứ tự). Khóa khác trong code: `ads_config_unit`, `ads_config_placement`, `ads_config_item`, `banner_interval`, `collapsible_native`, `native_duration`, `reward_config`, `inter_config`, `show_cmp`, `show_update_app_`, `config_enable_features`.

**Giao diện.** Tối. Bộ màu `ds_*`: nền `#17161f`, bề mặt `#1c1a26`, `#262433`, viền `#343143`, chữ `#f5f5f7`, chữ phụ `#9a9aa6`, màu nhấn tím `#6d5ef7`, khớp gần đúng vàng `#ffb800`, lỗi `#ff4d4d`, nét viền trắng có quầng đen. Font chính Be Vietnam Pro.
Màn hình: Splash, Language, Onboarding (3 bước), Discover, Search, Pose Detail, Collections, Collection Detail, Camera Flow, Paywall, Setting, Notification Settings, Mirror (QR, nhập mã, điều khiển, camera, lỗi, mất kết nối).

**Chuỗi chữ.** Tab: Discover, Collections, Camera. "Watch an ad to show %d more poses", "Unlock all poses with Pro", "Daily limit reached — %d unlocks per category". Camera: Pose outline, Opacity, Size, Lock overlay, Mirror the pose outline, Grid, Timer, Flash, Next pose. Onboarding: "Find a pose by category", "Colour tells you how close" (xanh là khớp, vàng là gần), "Plan your shoot". Trả phí: "Shoot every Pro pose", "Written guide: hand, weight and light notes", "No ads, anywhere", gói tuần, tháng, năm, có dùng thử. Mirror Mode: hai điện thoại cùng Wi-Fi, một máy chụp, một máy điều khiển.

## 7. com.gws.posemate (PoseMate, 1.0.8)

**Công nghệ.** Compose, Kotlin, Hilt, Room, Coil 3, một `MainActivity`. minSdk 26, targetSdk 36. Chỉ tiếng Anh (chuỗi viết cứng trong Compose, split `vi` chỉ có chuỗi của thư viện). Đây là app gần nhất với công nghệ của dự án mẫu.

**SDK.** Chỉ AdMob, không mediation. Banner thích ứng neo, interstitial, rewarded. UMP. Firebase Analytics và Crashlytics, chỉ bật khi người dùng đồng ý (`telemetry_opt_in`). Play Billing (sản phẩm `premium_ad_free`). Play App Update. Play Asset Delivery. CameraX. ML Kit Pose Detection (2 model, 5.8 MB) để tính điểm khớp.

**Nội dung tư thế.** Đóng gói nhiều nhất.
- `assets/poses_data/<nhóm>.json`: 27 nhóm, tổng 1.629 tư thế. Nhóm lớn: seasonal 160, selfies 150, weddings 120, couples 100, bold_sensuous 80, nature 70.
- Mỗi tư thế: `id`, `name`, `description`, `poseGuide`, `difficulty`, `steps[]`, `angle`, `framing`, `lens`, `lighting`, `commonMistakes[]`, `bestTime`, `mood`, `tags[]`, `illustrationUrl`, `outlineImageUrl`.
- `assets/pose_thumbs/<nhóm>/`: 3.258 ảnh WebP nhỏ (ảnh thật khoảng 360 px, nét viền khoảng 180 px). Ảnh cỡ lớn nằm trong asset pack `poses` của Play, đọc theo đường dẫn tương đối.
- File `*_crop_manifest.json` cho thấy cách làm: tạo lưới ảnh bằng AI (4 x 5 ô), nâng phân giải bằng Nero AI Upscaler, cắt từng ô, vẽ nét viền theo lưới tương ứng.

**Remote Config.** Không dùng.

**Giao diện.** Theme gốc `Theme.Material.Light`, màu trong code Compose, có đổi giao diện sáng tối (ThemeViewModel). Màn hình (theo gói): Onboarding, Poses (danh sách, chi tiết), Camera, Tips (danh sách, chi tiết), Saved, My Photos, Settings.

**Chuỗi chữ.** Tab: Poses, Tips, Saved, Settings. "Saved poses & shooting score", "Capture photos to build your score", "My shooting list", "Display rule-of-thirds grid on camera", "Auto Save Photos", "Browse category poses from the camera", "Avoid these mistakes", "Enjoy PoseMate without interruptions".

## 8. com.vector123.posecamera (PoseCamera, 1.0.4)

**Công nghệ.** Flutter (Dart). Gói Dart `photo_poses`, kiến trúc data, domain, presentation, Provider. Plugin: camera (CameraX), permission_handler, shared_preferences, image, image_gallery_saver, url_launcher, hydro_glass_nav_bar. minSdk 26, targetSdk 36. Nội dung có en, pl, pt.

**SDK.** Không quảng cáo, không Firebase. Play Billing để mở "Pose Camera Pro" (chụp có tư thế không giới hạn, kế hoạch chụp không giới hạn). Nhà làm vector123 có nhiều app tiện ích khác.

**Nội dung tư thế.** Đóng gói trong `flutter_assets/assets/`:
- `images/`: 120 ảnh WebP 1024 x 1536, nét vẽ người kiểu ma-nơ-canh. Female Solo 33, Couple 49, Solo 18, Group 20.
- `poses/<lang>/poses_<nhóm>.json`: `id`, `name`, `description`, `tags`, `difficulty`, `steps[]`, `camera_tips {angle, framing, lens, lighting}`, `common_mistakes[]`.
- `photo_techniques/<lang>/`: 200 mẹo chụp ảnh trong 5 nhóm (bắt đầu bằng Exposure Basics).

**Remote Config.** Không.

**Giao diện.** Theme sáng, tối trong Dart. Font Material Icons, Cupertino Icons. Màn hình (theo file Dart): Privacy Gate, Pose Home, Pose List, Pose Search, Pose Detail, Camera Overlay, Photo Preview, Favorites, Technique Category, Technique Detail, Technique Search, Settings.

---

## Nhận định

### App cùng nhà làm hoặc cùng mã nguồn

- **Nhóm Pose Camera (3 app): cùng một mã nguồn.** `com.camera.filter.pose.guide.ideas`, `com.camera.filter.pose.guide.ideas.photo`, `com.camera.guide.ideas.photo.pose`. Giống nhau: gói `screens/opening/SplashActivity`, theme `Theme.PoseCamera`, tên layout (`item_pose`, `item_inspiration`, `dialog_loading_interstitial`, `layout_ob_center`), tên ảnh `im_<nhóm>_<n>`, khóa `ad_config` cùng cấu trúc, cùng font Inter 18pt và Baloo 2, cùng `bg_iap.png`. Ba tài khoản khác nhau (Tokii Apps, Ocean Limousine Limited, Smart Experiences), trang chính sách cùng mẫu `policy.php` trên tên miền `.top`. Đây là cách làm nhiều app từ một khung, đổi màu nhấn (vàng, tím, hồng).
- **`com.posecamera.poseideas`: cùng gốc nội dung với nhóm trên.** 39 ảnh tư thế trùng từng byte với `.photo`, chuỗi chữ giống ("Strike Your Perfect Pose", "START POSING"). Nhưng khung code và thư viện quảng cáo khác. Có thể là bản đổi giao diện từ mã nguồn mua lại.
- **4 app còn lại độc lập:** AI Posing (Betasoft, API `aifastwork`), SimplePose (Braly Đà Nẵng), PoseMate (gws), PoseCamera Flutter (vector123).

### Cách làm nội dung tư thế phổ biến

- Cả 8 app dùng cùng một ý: **ảnh mẫu và nét viền (outline) phủ lên khung camera**. App tốt hơn có cặp ảnh thật và nét viền riêng (guide.ideas, PoseMate, SimplePose, AI Posing).
- Phổ biến nhất là **đóng gói trong APK** (6/8). Chỉ AI Posing tải hoàn toàn từ API. SimplePose đóng gói một ít và cập nhật catalog từ xa. PoseMate dùng Play Asset Delivery cho ảnh lớn.
- App làm kỹ có **dữ liệu có cấu trúc**: bước tạo dáng, góc máy, khung hình, ống kính, ánh sáng, lỗi hay gặp, độ khó, thẻ. PoseMate và PoseCamera Flutter có nội dung gần như cùng khuôn. SimplePose thêm 33 điểm khớp để so dáng.
- Ảnh tư thế nhiều khả năng do AI tạo (PoseMate ghi rõ quy trình lưới ảnh AI, nâng phân giải, cắt ô).
- So dáng trực tiếp bằng ML Kit Pose: AI Posing và PoseMate. SimplePose đã có dữ liệu điểm khớp nhưng chưa có bộ nhận dạng.

### Chiến lược quảng cáo thấy được

- App kiểu "nhà máy" (nhóm Pose Camera, poseideas, AI Posing, SimplePose) dồn quảng cáo vào luồng mở đầu: app open hoặc inter ở splash, native ở chọn ngôn ngữ và từng trang intro, native toàn màn hình giữa các trang, inter sau ngôn ngữ và intro. Thêm inter khi vào home, khi back, khi xem ảnh; banner hoặc native thu gọn ở home và camera; reward để mở thêm tư thế.
- Tần suất điều khiển từ xa: `step_count` (inter mỗi N lần bấm), `interval` 30 giây giữa hai inter (SimplePose), `refresh_rate` 20 giây cho banner, thời gian chờ tối đa khi tải.
- Mỗi vị trí có quảng cáo dự phòng (native thay inter, banner thay native thu gọn) và bật tắt riêng.
- PoseMate làm nhẹ: chỉ banner, inter, reward của AdMob, gói gỡ quảng cáo một lần.

### Gợi ý kỹ thuật cho app clone (Kotlin, Compose, Hilt, Firebase, AdMob)

1. **Lấy PoseMate làm mẫu kiến trúc.** Cùng công nghệ với dự án mẫu: Compose, Hilt, Room, CameraX, ML Kit Pose, chỉ AdMob, minSdk 26.
2. **Mô hình dữ liệu tư thế** gộp từ PoseMate, PoseCamera Flutter và SimplePose: `id`, `category`, `subjectType` (một người, cặp đôi, nhóm), `framing`, `tags`, `difficulty`, `steps`, `cameraTips` (góc, khung, ống kính, ánh sáng), `commonMistakes`, `preview` (ảnh thật, nhiều cỡ), `outline` (PNG trong suốt), tùy chọn `keypoints` (33 điểm). Có `datasetVersion` và `minAppSchema` để cập nhật an toàn.
3. **Nguồn nội dung:** đóng gói một bộ nhỏ (khoảng 50 đến 100 tư thế, WebP) để dùng khi mất mạng. Phần còn lại đặt trên R2 qua `RemoteAssets.load(AssetPath(...))` của `:core:assets`, một file catalog JSON có phiên bản, đường dẫn có mã băm (ADR-0015). Cách này giống SimplePose nhưng không cần token trong app.
4. **Camera:** CameraX `PreviewView` trong `AndroidView`, lớp phủ nét viền vẽ bằng Compose, chỉnh độ mờ, cỡ, lật, khóa vị trí; lưới ba phần, hẹn giờ, đèn flash, đổi camera. Giai đoạn 2: ML Kit Pose (bản base) so với `keypoints`, tô màu xanh, vàng theo mức khớp như SimplePose.
5. **Quảng cáo:** vị trí khai báo trong `AdPlacement`, mặc định tắt, bật bằng `ads_<vị trí>_enabled` (ADR-0013, ADR-0014). Gợi ý ít vị trí: banner ở danh sách tư thế, inter ở điểm dừng tự nhiên (sau khi lưu ảnh), reward để mở thêm tư thế. Thêm tham số kinh doanh trong `RemoteSettings` cho tần suất inter (số lần, khoảng cách giây) theo kiểu `step_count` và `interval` của đối thủ. Không cần mediation lúc đầu.
6. **Firebase:** Analytics, Remote Config, Crashlytics qua `:core:analytics`, `:core:config`, `:core:logging`. Không cần Messaging lúc đầu.
7. **Trả phí (sau):** một gói gỡ quảng cáo như PoseMate, hoặc gói tuần, năm có dùng thử như AI Posing và SimplePose. Cần spec riêng vì thêm SDK Billing.
8. **Tránh:** minSdk 32 như nhóm Pose Camera (mất nhiều máy cũ), quảng cáo dày ở luồng mở đầu, khảo sát gỡ app, nhiều mạng quảng cáo làm APK nặng 50 đến 90 MB.
