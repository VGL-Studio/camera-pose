---
title: "Camera Pose: Lines and Guides"
package: com.vglstudio.camera.pose.line.guide
date: 2026-10-21
author: AI, theo yêu cầu của anh Nhật
reference_apps: [com.camera.filter.pose.guide.ideas, com.vector123.posecamera, com.cam.simplepose.pose.camera.photo.guide, com.aiposing.pose, com.gws.posemate, com.camera.filter.pose.guide.ideas.photo, com.camera.guide.ideas.photo.pose, com.posecamera.poseideas]
status: approved                  # draft | approved. Chỉ dev ghi approved
approved_by: anh Nhật
approved_at: 2026-10-21
design_direction: Sticker             # tên hướng thiết kế dev đã chọn (mục 10), điền sau bước chọn hướng
---

# ĐỀ XUẤT APP: "CAMERA POSE: LINES AND GUIDES"

<!--
Bản đề xuất app clone (ADR-0021, docs/ops/clone-study.md). AI viết từ báo cáo khảo sát; dev duyệt trước khi sang Figma.
Mỗi cải tiến dẫn về bài học có bằng chứng. Quảng cáo và dữ liệu thu thập chỉ ghi thành câu hỏi.
-->

Nguồn: các báo cáo nằm cạnh file này. Tên ảnh `NN-ten` là ảnh trong `captures/<package>/` của thư mục làm việc, không có trong repo. Viết tắt trong bài:

| Viết tắt | App | Báo cáo |
| --- | --- | --- |
| Pose Camera | `com.camera.filter.pose.guide.ideas` | [com.camera.filter.pose.guide.ideas.md](com.camera.filter.pose.guide.ideas.md) |
| PoseCamera | `com.vector123.posecamera` | [com.vector123.posecamera.md](com.vector123.posecamera.md) |
| SimplePose | `com.cam.simplepose.pose.camera.photo.guide` | [com.cam.simplepose.pose.camera.photo.guide.md](com.cam.simplepose.pose.camera.photo.guide.md) |
| AI Posing | `com.aiposing.pose` | [com.aiposing.pose.md](com.aiposing.pose.md) |
| PoseMate | `com.gws.posemate` | [com.gws.posemate.md](com.gws.posemate.md) |
| (APK) | 8 app | [apk-analysis.md](apk-analysis.md) |
| (Play) | 8 app và đối thủ | [web-play.md](web-play.md) |

## 1. Thông tin dev cung cấp

- **Ý tưởng:** app gợi ý tư thế chụp ảnh ("pose camera"): thư viện tư thế, khung camera có nét viền tư thế phủ lên để người dùng đứng theo rồi chụp.
- **Cách làm:** app clone, bám theo các app đã thành công trên Play. App mới phải tốt hơn các app mẫu và có nhận diện riêng (màu, icon, chữ khác).
- **Cách kiếm tiền:** quảng cáo AdMob.
- **Kiến trúc:** Kotlin, Jetpack Compose, Hilt, Firebase (Remote Config, Analytics, Crashlytics), AdMob, file tĩnh trên Cloudflare R2, không có backend, minSdk 26.
- **App mẫu:** 8 app trong bảng mục 2. Năm app đã khảo sát trên máy.
- **Trả lời ngày 2026-10-21 (mục 12):**
  - Tên app: **Camera Pose: Lines and Guides** (29 ký tự, vừa giới hạn 30 ký tự của Play). Tên gói: **`com.vglstudio.camera.pose.line.guide`**.
  - Thị trường: **Ấn Độ, Brazil, Mexico, Philippines, Indonesia, Thái Lan, Việt Nam**.
  - Quảng cáo: **mức vừa** (mức B, mục 9.1). Không có gói gỡ quảng cáo.
  - Màn bắt đầu: **camera**. Camera và màn xem lại chỉ có giao diện tối.
  - Nội dung tư thế: **AI tự tạo, người duyệt**; mượn tạm từ APK đã giải nén trong lúc chờ.
  - CameraX và quyền `CAMERA`: **thêm ngay khi khởi tạo app**, trong bước dựng repo do dev làm, không qua phiên tự chạy.
  - **Có hỏi đánh giá trong app.** Không lưu thêm bản ảnh kèm nét viền.
  - **Không hỗ trợ máy tính bảng; có hỗ trợ điện thoại màn hình gập.**
  - **Nhận dạng dáng và chấm điểm khớp (ML Kit Pose): làm ngay trong bản đầu.**
  - Sự kiện phân tích: đúng 9 sự kiện ở mục 9.2. Dữ liệu: ảnh chỉ lưu trên máy, không cần đăng nhập.

## 2. Tóm tắt thị trường

### App mẫu

| App (package) | Lượt cài (số chính xác) | Điểm (số đánh giá) | Kiếm tiền | Điểm mạnh nổi bật | Khảo sát trên máy |
| --- | --- | --- | --- | --- | --- |
| Pose Camera (`com.camera.filter.pose.guide.ideas`) | 500,000+ (572,801) | 3,3 ở Mỹ (3.686) | Quảng cáo rất dày, 5 mạng | Một màn camera làm đủ mọi thứ, nét vẽ rõ | Có |
| AI Posing (`com.aiposing.pose`) | 100,000+ (103,754) | 4,2 ở VN (465) | Quảng cáo dày + thuê bao 99.000 ₫/tuần | Ảnh người thật nhiều chủ đề, ML Kit chấm khớp, màn xem lại | Có |
| Pose Camera: Aesthetic Guides (`...pose.guide.ideas.photo`) | 50,000+ (60,514) | 3,6 ở Ấn Độ (65) | Quảng cáo dày | Cùng mã nguồn với Pose Camera | Không |
| Pose Camera - Photos Guide (`com.camera.guide.ideas.photo.pose`) | 10,000+ (49,811) | 2,8 ở VN (395) | Quảng cáo dày | Cùng mã nguồn với Pose Camera, cặp ảnh thật và nét viền | Không |
| PoseCamera (`com.vector123.posecamera`) | 10,000+ (13,056) | 1,8 ở Ấn Độ (16) | Không quảng cáo, Pro mua một lần 39.000 ₫ | Hướng dẫn sâu (bước, góc, ống kính, lỗi), 200 mẹo, offline | Có |
| Pose Camera - Photos Idea (`com.posecamera.poseideas`) | 1,000+ (1,251) | Chưa có | Quảng cáo dày | 39 ảnh tư thế trùng với nhóm Pose Camera | Không |
| PoseMate (`com.gws.posemate`) | 100+ (301) | Chưa có | Một banner + gỡ quảng cáo $0.99 | 1.629 tư thế, hướng dẫn dày, màn "Nailed it!" so sánh trước sau | Có |
| SimplePose (`com.cam.simplepose.pose.camera.photo.guide`) | 100+ (261) | Chưa có | Quảng cáo vừa + thuê bao | Lớp phủ chỉnh đủ (độ mờ, cỡ, kéo, khóa, lật), bộ sưu tập, chế độ hai máy | Có |

Nguồn: Play, mục 1; APK, bảng so sánh.

**Ba app không khảo sát trên máy.** `com.camera.filter.pose.guide.ideas.photo` và `com.camera.guide.ideas.photo.pose` cùng mã nguồn với Pose Camera (cùng gói `screens/opening/SplashActivity`, cùng tên layout, cùng khóa `ad_config`, chỉ đổi màu nhấn). `com.posecamera.poseideas` có 39 ảnh tư thế trùng từng byte với `.photo` nhưng khung code khác. Hành vi của ba app này suy từ Pose Camera và từ APK, chưa xác nhận trên máy (APK, mục Nhận định).

### Người dùng, quốc gia, xu hướng

- **Người dùng chính:** nữ trẻ, chụp selfie, OOTD, du lịch, cà phê, cặp đôi. Nhóm thứ hai: cặp đôi, nhóm bạn, gia đình. **Nam bị bỏ quên:** có phàn nàn trực tiếp, trong khi "Photo pose for men" đạt hơn 3,3 triệu lượt cài (Play, 5.1).
- **Quốc gia:** Ấn Độ là nguồn đánh giá lớn nhất; Mexico, Brazil, Indonesia, Việt Nam, Philippines, Thái Lan có đánh giá đáng kể. Mexico và Việt Nam khắt khe nhất với quảng cáo (Pose Camera: 4,1 ở Ấn Độ, 1,9 ở Mexico). Đây là dấu hiệu các app mẫu mua lượt cài ở thị trường giá rẻ (Play, 5.2; suy luận, chưa có số liệu SensorTower).
- **Xu hướng:** từ thư viện ảnh sang camera có lớp phủ; AI chấm khớp tư thế (chưa app Android nào làm tốt); AI tạo tư thế theo bối cảnh (Superpose iOS gọi vốn 2,2 triệu USD); chép tư thế từ ảnh bất kỳ (poby, TwinCam); clone hàng loạt vòng đời ngắn (Play, 5.4).
- **Đánh giá:** khoảng 700 đánh giá đã đọc. Pose Camera có 504 đánh giá tiếng Anh, 293 nhắc quảng cáo, 286 trong số đó là 1 sao. 51 đánh giá dùng từ "fake", "scam", "clickbait" (Play, mục 3).

### Khoảng trống

1. Ít quảng cáo, mở app là tới camera. Phàn nàn số một áp đảo.
2. Thư viện đủ lớn ngay bản đầu, có tư thế cho nam.
3. Lớp phủ dễ nhìn và chỉnh được.
4. Chạy được khi mất mạng. Không theo khoảng trống này: mọi app của nhóm luôn cần mạng (mục 12, câu 13).
5. Ảnh store trung thực.
6. Bản địa hóa (tiếng Việt, Tây Ban Nha, Bồ Đào Nha, Indonesia, Hindi).

## 3. Tên và tên gói đề nghị

Tránh: "Pose Camera", "PoseCamera", "PoseMate", "SimplePose", "AI Posing", và các tên đã có trên thị trường: Posely, PoseSnap, SnapPose, Posica, poby, Superpose (Play, mục 4). Cụm "Pose Camera" có ít nhất 6 app dùng nguyên văn, nên đặt nó vào phần mô tả tiêu đề trên Play (ví dụ "PoseLine: Pose Camera Guide") thay vì làm tên chính.

| Tên | Tên gói | Lý do | Kiểm tra trùng trên Play |
| --- | --- | --- | --- |
| PoseLine | `com.vglstudio.poseline` | Ngắn, nói đúng tính năng chính (nét viền tư thế), dễ đọc ở mọi thị trường | Tìm web 2026-10-21: không thấy app tên "PoseLine" trên Play; gần nhất là Posely (iOS) và PoseCam (iOS). Chưa tra trực tiếp trên Play |
| Outline Cam | `com.vglstudio.outlinecam` | Mô tả cách dùng: đứng theo nét viền rồi chụp | Tìm web 2026-10-21: không thấy app Android cùng tên; có app iOS cùng ý (Ditto Cam, CoolPoseCam). Chưa tra trực tiếp trên Play |
| StrikeCam | `com.vglstudio.strikecam` | Gợi "strike a pose", vui, hợp người dùng trẻ | Tìm web 2026-10-21: không thấy "StrikeCam"; có app iOS "Strike a pose" (gần tên, cũ). Chưa tra trực tiếp trên Play |
| Dáng | `com.vglstudio.dang` | Nếu nhắm thị trường Việt Nam: từ khóa "tạo dáng chụp ảnh" còn ít cạnh tranh (Play, 5.3) | Chưa kiểm tra |

Nguồn tìm web: tìm kiếm 2026-10-21 với các truy vấn "PoseLine app pose camera", "Outline Cam pose guide app android", "StrikeCam pose app" (kết quả gồm apps.apple.com/app/id6742747216, apps.apple.com/app/id6780748437, mwm.ai/apps/ditto-cam/6760381571, apps.apple.com/app/id859331589).

Dev chốt tên và tên gói trước khi chạy `scripts/new-app.sh`. Tên gói không đổi được sau khi phát hành.

**Đã chốt (2026-10-21):** tên **Camera Pose: Lines and Guides**, tên gói **`com.vglstudio.camera.pose.line.guide`**. Tên này dùng cụm "Camera Pose" mà nhiều app mẫu dùng ("Pose Camera"); nhận diện riêng dựa vào phần "Lines and Guides", icon và ảnh store (ADR-0021: icon và ảnh store không giống app mẫu).

## 4. Bài học từ app mẫu và cải tiến

| # | Bài học | Bằng chứng | Cải tiến | Cách đo |
| --- | --- | --- | --- | --- |
| 1 | Quảng cáo ở luồng mở đầu là lý do bị 1 sao nhiều nhất | Pose Camera: 3 interstitial và 3 native toàn màn hình trước home, khoảng 13 chạm tới camera (`02-splash` đến `14-home-collapsible-ad`). AI Posing: 1 interstitial, 2 native toàn màn hình, 4 native đáy, màn trả phí, hộp thoại đánh giá trước trang chủ, khoảng 13 chạm tới camera (`01` đến `13`). Đánh giá: "5 ads before even reaching the main UI of the app." (Play, 3.1); 286/504 đánh giá tiếng Anh của Pose Camera là 1 sao có nhắc quảng cáo | Mở lần đầu tới camera có tư thế: tối đa 2 chạm (nút bật camera, hộp quyền hệ thống). Không có quảng cáo toàn màn hình nào trước ảnh đầu tiên | Đếm trên bản staging, ghi trong spec: số chạm và số quảng cáo toàn màn hình từ lúc mở app tới camera có tư thế (đích: 2 chạm, 0 quảng cáo; Pose Camera 13 và 6, PoseMate 8 và 0) |
| 2 | Lặp ngôn ngữ và giới thiệu mỗi lần mở làm người dùng bực | Pose Camera: mỗi lần mở nguội lại qua chọn ngôn ngữ (không chọn sẵn) và 4 trang giới thiệu (`40-relaunch-splash-ad`, `41-relaunch-language`). PoseCamera, PoseMate vào thẳng (PoseCamera mục 2, PoseMate `37`) | Không có màn chọn ngôn ngữ lúc mở: theo ngôn ngữ máy, đổi trong Cài đặt (ngôn ngữ theo app của Android 13+). Giới thiệu một màn, chỉ lần đầu | Mở lại nguội: số màn trước camera bằng 0. Test trên emulator: lần mở thứ hai, đích đầu tiên là `CameraScreen` |
| 3 | Quảng cáo che điều khiển camera và nằm đúng chỗ nút | Pose Camera: banner thu gọn tự bung lại khoảng 20 giây sau khi thu, che hàng nhóm và hàng tư thế (mục 6); native toàn màn hình phủ đúng chỗ nút Back, công cụ khảo sát chạm nhầm 2 lần (mục 9, sự cố 2); nút CTA quảng cáo cùng màu vàng với nút app. AI Posing: native 76dp ngay trên camera (`44`) | Không có vị trí quảng cáo nào trên màn camera và màn xem lại. Nút của app không dùng màu hay hình dạng của nút CTA quảng cáo | `AdPlacement` không có vị trí nào thuộc `:feature:camera`. Screenshot test `CameraScreen` không có `AdSlot` |
| 4 | Quá ít tư thế, danh mục rỗng | Pose Camera 32 tư thế (APK mục 1); đánh giá "Too many ads and only 6 poses available." (Play, 3.1). SimplePose khi mất mạng chỉ 24 tư thế, 3/6 danh mục rỗng vẫn hiện (`06-category-empty`). PoseMate 1.629, PoseCamera 120 | Ra mắt với ít nhất 200 tư thế; bộ khởi đầu 60 tư thế tải về ở lần mở đầu tiên và giữ trên máy, để các lần sau hiện ngay; mỗi nhóm hiển thị có ít nhất 15 tư thế; danh mục chưa có tư thế thì ẩn | Đếm catalog bằng script kiểm catalog trước khi đẩy lên R2. Unit test: repository không trả nhóm rỗng |
| 5 | Lớp phủ khó nhìn hoặc không chỉnh được | Pose Camera: nét vẽ cỡ cố định, kéo không di chuyển (`22-couple-pose-drag`). AI Posing: lớp phủ là ảnh mờ của người khác, thanh độ mờ kéo không đổi gì thấy được (`46`). Đánh giá: "outline is too thin to see well" (Play, 3.1). SimplePose làm tốt: nét trắng có quầng đen, độ mờ 20 đến 100, cỡ 40 đến 160, kéo, khóa, lật, ẩn (`15` đến `22`) | Nét viền trắng có quầng tối, nhìn rõ trên nền sáng và tối. Chỉnh được: độ mờ, cỡ (chụm hai ngón và thanh trượt), kéo, lật, khóa, ẩn, đặt lại. Giữ lựa chọn khi đổi tư thế | UI test JVM: đủ 7 thao tác, mỗi thao tác đổi trạng thái lớp phủ. Đo tương phản nét viền trên ảnh nền trắng và đen trong screenshot test |
| 6 | Không có màn xem lại sau khi chụp | Pose Camera lưu thẳng, không xem lại (`28-after-capture`). SimplePose không xem lại, không thông báo đã lưu (`23-after-capture`). PoseCamera: nút lưu trong xem lại chỉ tạo bản sao trùng md5 (mục 5). AI Posing có xem lại nhưng chen màn trả phí và interstitial khi lưu (`32` đến `36`) và mất tư thế sau khi lưu (`37`) | Sau khi chụp mở màn xem lại: Chụp lại, Lưu, Chia sẻ, "Tư thế tiếp". Quay về camera vẫn giữ tư thế đang dùng | UI test: sau nút chụp có `ReviewScreen`; quay lại camera thì tư thế đang chọn không đổi |
| 7 | Lỗi tải nội dung làm màn hình trống mãi | AI Posing: API trả HTTP 429 khoảng 11 phút, danh mục và Khám phá trống hoặc quay vòng mãi, không báo lỗi, không có thử lại (`17`, `18`, `20`, `22`) | Bộ khởi đầu đã tải ở lần mở đầu tiên nên luôn hiện ngay. Tải thêm qua `RemoteAssets.load`; kết quả `Unavailable` đưa vào `UiState` với thông báo và nút thử lại; chờ có giới hạn. Mất mạng thì hiện hộp thoại lỗi mạng (mục 12, câu 13) | Unit test ViewModel: `Unavailable` cho trạng thái lỗi có nút thử lại; chờ tối đa 15 giây rồi báo lỗi. Screenshot `PosesScreen_error_light`, `PosesScreen_error_dark` |
| 8 | Hứa tính năng không có | SimplePose giới thiệu "Màu sắc cho biết bạn đã gần đúng chưa" (`03-onboarding-2`) nhưng camera không so khớp (mục 4). PoseMate nói "Tap Lock when done" mà không có nút Lock (`18`); "Nailed it!" hiện cả khi khớp 0% (`22`). 51 đánh giá Pose Camera gọi app là "fake", "clickbait" (Play, 3.1) | Chữ trong app và ảnh store chỉ nói tới tính năng đã có. Ảnh store chụp từ màn hình thật | Mỗi câu ở giới thiệu và ảnh store gắn với một tiêu chí chấp nhận trong spec. Ảnh store lấy từ screenshot test |
| 9 | Tên, mô tả tư thế sai hoặc thiếu | PoseMate: tên và mô tả sinh theo khuôn không khớp ảnh ("Wedding Couple Walk VIII" là ảnh đôi nhảy lên, `33`; mô tả "dance" cho ảnh ngồi, `13`). SimplePose: mọi tư thế tên "Full body"/"Half body", một tư thế tên thô "no_props" (mục 4) | Mỗi tư thế có tên riêng, ngắn, đã duyệt bằng người. Không có khóa thô hiện ra giao diện | Script kiểm catalog: tên không trùng trong nhóm, không có chuỗi dạng `snake_case`, đủ bản dịch cho mọi ngôn ngữ phát hành |
| 10 | Ảnh mẫu không kèm hướng dẫn thì người dùng thấy như "ảnh dán lên camera" | AI Posing: đánh giá "bukan ngarahin malah cuma foto ditempel dikamera kita" (Play, 3.1); chi tiết tư thế không có mô tả, mẹo (`43`). PoseCamera có thẻ mẹo ngay trên camera, 10 bước chuyển trước sau (`07-camera`); PoseMate có bước, góc, lỗi hay gặp (`13` đến `15`) | Mỗi tư thế có 3 đến 5 bước ngắn và 1 lỗi hay gặp. Thẻ bước bật tắt được trên camera | Script kiểm catalog: 100% tư thế có bước. UI test: thẻ bước hiện, chuyển trước sau |
| 11 | Thiếu tư thế cho nam | Đánh giá PoseCamera: "solo male poses are very bad... good poses are only for couples" (Play, 3.1). SimplePose khi có mạng: Women 159, Men 41 (mục 4) | Nhóm Nam có ít nhất 20% số tư thế một người | Đếm catalog theo `subjectType` và giới |
| 12 | Hỏi đánh giá và bán gói trước khi người dùng thấy giá trị | AI Posing: màn trả phí rồi hộp thoại đánh giá 5 sao chọn sẵn ngay sau giới thiệu (`11`, `12`). PoseMate: hộp thoại Go Premium mỗi lần mở nguội (`08`, `37`). Pose Camera: xin 5 sao sau lần chụp đầu (`34-rating-dialog`) | Bản đầu không có màn trả phí. Không hỏi đánh giá trong bản đầu (xem câu hỏi mục 12) | Không có hộp thoại nào chen giữa lúc mở app và camera (đo cùng bài học 1) |
| 13 | Lưu ảnh lộn xộn, không xem lại được trong app | PoseCamera lưu ở gốc `Pictures/` (mục 5). PoseMate: nút Gallery trên camera mở app khác (`23`). AI Posing lưu trong bộ nhớ riêng, không thấy trong thư viện máy (mục 5) | Lưu vào `Pictures/<TênApp>/` qua MediaStore, không cần quyền đọc bộ nhớ. Ô ảnh vừa chụp trên camera mở màn xem lại trong app | Test trên emulator: ảnh xuất hiện trong MediaStore đúng thư mục. Manifest không có quyền đọc ảnh |
| 14 | minSdk cao làm mất máy cũ | Nhóm Pose Camera và AI Posing dùng minSdk 32 (APK, bảng so sánh) | minSdk 26 theo dự án mẫu, như PoseMate và PoseCamera | `minSdk` trong build bằng 26 |
| 15 | Thiếu tiếng địa phương | Pose Camera không có tiếng Việt (mục 2); đánh giá PoseCamera "me hubiera gustado que tuviera la traducción en español" (Play, 3.1); PoseMate chỉ tiếng Anh | Ra mắt với tiếng Anh và các ngôn ngữ của thị trường dev chọn (mục 12). Tên tư thế và bước dịch cùng catalog | Test kiểm mọi chuỗi trong `strings.xml` có đủ bản dịch; script kiểm catalog có đủ ngôn ngữ |
| 16 | Xin quyền camera có giải thích thì ít bị từ chối hơn | SimplePose có màn giải thích trước hộp thoại hệ thống (`13-open-camera`); PoseMate có nút "Enable camera" trong giới thiệu (`03` đến `05`). Pose Camera, AI Posing xin thẳng | Giữ cách làm này. Thêm trạng thái "chưa có quyền" trên camera với nút mở Cài đặt máy | UI test: từ chối quyền thì `CameraScreen_permission-denied_dark`, có nút mở cài đặt |

Năm cải tiến chính: 1 (2 chạm tới camera, không quảng cáo trước ảnh đầu), 3 (không quảng cáo trên camera), 4 (200 tư thế, bộ khởi đầu 60 tư thế tải sẵn ở lần mở đầu), 5 (lớp phủ chỉnh đủ), 6 và 7 (màn xem lại, báo lỗi có thử lại).

## 5. Tính năng

| Tính năng | Nhóm | Có ở app mẫu nào | Ghi chú |
| --- | --- | --- | --- |
| Camera có nét viền tư thế phủ lên | bắt buộc | Cả 5 app | Nét viền riêng, không dùng ảnh mờ (bài học 5) |
| Dải chọn tư thế ngay trong camera, đổi nhóm bằng chip | bắt buộc | Pose Camera, SimplePose, PoseMate | Pose Camera là khuôn gọn nhất (`19-camera`) |
| Hẹn giờ, lưới, flash, đổi camera, zoom | bắt buộc | Pose Camera, SimplePose, PoseMate; AI Posing thiếu | Hẹn giờ 3/10 giây; zoom nút 0.5x/1x/2x khi máy hỗ trợ |
| Thư viện tư thế theo nhóm, lưới 2 cột | bắt buộc | Cả 5 app | Nhóm: Nữ, Nam, Cặp đôi, Bạn bè, Gia đình, Trẻ em, Du lịch, Cà phê (chốt theo nội dung) |
| Chi tiết tư thế với nút "Chụp với tư thế này" | bắt buộc | PoseCamera, SimplePose, AI Posing, PoseMate | |
| Lưu ảnh vào thư viện máy | bắt buộc | Cả 5 app | Ảnh lưu không kèm nét viền |
| Màn giải thích quyền camera | bắt buộc | SimplePose, PoseMate | Bài học 16 |
| Cài đặt: giao diện sáng tối, âm chụp, ngôn ngữ, quyền riêng tư và đồng ý quảng cáo, chính sách, phiên bản | bắt buộc | Cả 5 app (mức khác nhau) | Đồng ý quảng cáo qua `:core:ads` (ADR-0013) |
| Vào thẳng camera, không quảng cáo trước ảnh đầu | khác biệt | Không app nào làm trọn; PoseMate gần nhất | Bài học 1, 2 |
| Lớp phủ chỉnh đủ: độ mờ, cỡ, kéo, lật, khóa, ẩn, đặt lại | khác biệt | SimplePose đủ; PoseCamera, PoseMate một phần | Bài học 5 |
| Thẻ bước tạo dáng trên camera | khác biệt | PoseCamera (thẻ mẹo), PoseMate (chỉ ở chi tiết) | Bài học 10 |
| Màn xem lại, "Tư thế tiếp" | khác biệt | AI Posing (xem lại), PoseMate ("Next") | Bài học 6 |
| Bộ tư thế khởi đầu tải ở lần mở đầu tiên rồi giữ trên máy, tải thêm từ R2 | khác biệt | SimplePose (24 đóng gói + catalog), PoseMate (Play Asset Delivery) | Bài học 4, 7; mục 12, câu 15 |
| Tư thế cho nam đủ nhiều | khác biệt | Không app nào làm tốt | Bài học 11 |
| Yêu thích (lưu tư thế) | khác biệt | Cả 4 app trừ Pose Camera | Bản đầu chỉ một danh sách "Đã lưu" |
| Bộ sưu tập nhiều bộ, chọn nhiều, chụp lần lượt | sau | SimplePose, PoseCamera (Plan) | |
| Tìm kiếm theo chữ và thẻ | sau | PoseCamera, SimplePose, AI Posing, PoseMate | Bản đầu lọc bằng chip nhóm là đủ |
| Nhận dạng dáng và chấm điểm khớp (ML Kit Pose) | khác biệt (bản đầu, dev chốt 2026-10-21) | AI Posing, PoseMate (SimplePose có dữ liệu 33 điểm, chưa có bộ nhận dạng) | Xem ghi chú dưới bảng |
| Bài mẹo chụp ảnh (Tips) | sau | PoseCamera (200 bài), PoseMate (10 bài) | Nội dung tốn công, không phải lý do chính để cài |
| Thư viện ảnh đã chụp trong app | sau | Pose Camera, AI Posing, PoseMate | Bản đầu: màn xem lại ảnh vừa chụp, còn lại mở thư viện máy |
| So sánh trước sau, chia sẻ ảnh ghép | sau | PoseMate (`22`) | |
| Sửa ảnh (xoay, lật, cắt) | sau | PoseMate (`35`) | |
| Tạo khung tư thế từ ảnh người dùng chọn | sau | poby, TwinCam, PoseSnap (đối thủ, Play 4.1) | Cần tách nét viền trên máy; ứng viên cho bản 2 |
| Chế độ hai điện thoại (Mirror) | sau | SimplePose | Cần mạng cục bộ, thêm WebRTC; ít người dùng |
| Gói gỡ quảng cáo mua một lần | sau | PoseMate, PoseCamera | Cần Play Billing, spec riêng |
| Thông báo nhắc quay lại | không làm | AI Posing, SimplePose | Thêm quyền thông báo, dễ bị coi là làm phiền |
| Màn chọn ngôn ngữ lúc mở, giới thiệu nhiều trang có quảng cáo | không làm | Pose Camera, AI Posing | Bài học 1, 2 |
| Khảo sát khi gỡ app, hộp thoại thoát có quảng cáo | không làm | Nhóm Pose Camera, poseideas (APK) | |
| Thuê bao tuần | không làm | AI Posing, SimplePose | Đánh giá chê "8 euros a week" (Play, 3.1) |
| AI sinh ảnh đổi tư thế, AI quét bối cảnh trên server | không làm | Nhóm pose changer, Superpose | Cần backend, nhóm không có |
| Bộ lọc màu | không làm | AI Posing | Không phải nhu cầu chính; đẩy app sang loại camera làm đẹp |

**Nhận dạng dáng và chấm điểm khớp: dev chốt làm ngay trong bản đầu (2026-10-21).** AI từng đề xuất để sau vì các lý do dưới đây; chúng trở thành rủi ro cần xử lý và tiêu chí chấp nhận:

- Chưa xem được với người thật ở cả hai app có tính năng này (AI Posing mục 9, PoseMate mục 9). Chưa biết người dùng thấy nó có ích hay phiền.
- PoseMate bắt nhầm trần nhà là người: "35% đến 46% Come closer" khi không có ai (PoseMate mục 5). SimplePose hứa trong giới thiệu mà không làm (bài học 8). Làm dở sẽ thành điểm trừ.
- Thêm ML Kit Pose (khoảng 6 đến 12 MB model, APK mục 5, 7) và chạy phân tích khung hình liên tục (AI Posing mỗi khoảng 100 ms) tốn pin; máy minSdk 26 có thể chậm.
- Catalog nên có sẵn trường `keypoints` (33 điểm, như SimplePose) ngay từ bản đầu để không phải làm lại nội dung khi thêm tính năng.

Tiêu chí chấp nhận (đưa vào spec của `:feature:camera`): không hiện điểm khi không có người trong khung (ngưỡng tin cậy); đạt ít nhất 15 khung hình mỗi giây trên một máy tầm thấp; tăng APK không quá 10 MB; một người thật đứng đúng tư thế được ít nhất 80 điểm.

**Phạm vi bản đầu tiên (đề xuất):**

| Module | Nội dung |
| --- | --- |
| `:core:poses` (core mới của app) | Mô hình tư thế, tải catalog, bộ khởi đầu và ảnh qua `:core:assets`, danh sách đã lưu (DataStore). Dùng chung cho thư viện và camera, vì feature không được phụ thuộc nhau (ADR-0001) |
| `:feature:onboarding` | Màn chào và xin quyền camera, chỉ lần đầu |
| `:feature:camera` | Camera, lớp phủ và công cụ, dải tư thế, thẻ bước, nhận dạng dáng và điểm khớp (ML Kit Pose), màn xem lại, lưu ảnh |
| `:feature:poses` | Thư viện theo nhóm, chi tiết tư thế, đã lưu |
| `:feature:settings` | Giao diện, âm chụp, ngôn ngữ, quyền riêng tư và đồng ý quảng cáo, chính sách, phiên bản |

Camera cần CameraX và ML Kit Pose Detection: thêm thư viện vào `gradle/libs.versions.toml` (file được bảo vệ) và quyền `CAMERA` vào manifest, nên cần spec duyệt và plan rủi ro cao duyệt chéo (mục 12).

## 6. Luồng sử dụng

Phương án đề xuất: camera là màn bắt đầu. Thư viện tư thế cách camera một chạm.

**Mở lần đầu:**

`Mở app > Loading (cần mạng) > Chào mừng (nút "Bật camera") > hộp quyền hệ thống > Camera (tư thế đầu của nhóm Nữ hoặc nhóm mặc định đã chọn sẵn)`

Số bước tới chức năng chính: 2 chạm, 0 quảng cáo. So sánh: Pose Camera khoảng 13 chạm và 6 quảng cáo toàn màn hình; AI Posing khoảng 13 chạm; PoseMate 8 chạm; PoseCamera khoảng 4 chạm.

**Mở lại:** `Mở app > Loading (cần mạng) > Camera` (0 chạm, nhớ tư thế dùng lần trước).

Màn loading là feature `loading` có sẵn của dự án mẫu: mỗi lần mở app tải cấu hình mới từ máy chủ, chờ tối đa 10 giây, hiện ít nhất 2 giây (spec `loading`, ADR-0018). Màn này không tính là một chạm và không có quảng cáo.

**Luồng chính (chụp):**

`Camera > chip nhóm > chạm ô tư thế ở dải > chỉnh lớp phủ (nếu cần) > nút chụp > Xem lại > Lưu | Chụp lại | Tư thế tiếp > Camera`

**Luồng duyệt:**

`Camera > nút Thư viện > Tư thế (chip nhóm, lưới) > Chi tiết tư thế > "Chụp với tư thế này" > Camera`

`Chi tiết tư thế > nút lưu > Tư thế > chip "Đã lưu"`

**Luồng phụ:**

- `Camera > bánh răng > Cài đặt > Giao diện | Âm chụp | Ngôn ngữ | Tùy chọn quyền riêng tư | Chính sách`
- `Camera > ô ảnh vừa chụp > Xem lại ảnh cuối`
- Từ chối quyền: `Chào mừng > từ chối > Camera (trạng thái chưa có quyền) > "Mở cài đặt" > Cài đặt máy`
- Mở app khi không có mạng: `Loading > báo "Không thể tải dữ liệu" > "Thử lại"`; không vào được app tới khi có mạng.
- Đang dùng mà mất mạng (ở bất kỳ màn hình nào): `hộp thoại "Mất kết nối mạng" > "Thử lại"`; hộp thoại không tự đóng, chỉ đóng khi bấm "Thử lại" lúc đã có mạng.

## 7. Danh sách màn hình

Camera và màn xem lại luôn nền tối ở cả hai chế độ (như mọi app mẫu), nên chỉ dựng bản `_dark` (câu hỏi mục 12). Màn khác dựng cả `_light` và `_dark`.

| Màn hình | Tên frame | Trạng thái cần dựng | Feature |
| --- | --- | --- | --- |
| Loading (có sẵn của dự án mẫu, đổi giao diện theo Sticker) | `LoadingScreen_progress_light`, `LoadingScreen_progress_dark` | Đang tải, 68% | `:feature:loading` |
| | `LoadingScreen_failed_light`, `LoadingScreen_failed_dark` | Không có mạng hoặc tải lỗi: dừng ở 68%, nút "Thử lại", không vào app | |
| Chào mừng, xin quyền camera | `OnboardingScreen_default_light`, `OnboardingScreen_default_dark` | Bình thường | `:feature:onboarding` |
| Camera | `CameraScreen_default_dark` | Bình thường: có tư thế, dải tư thế, thẻ bước | `:feature:camera` |
| | `CameraScreen_no-pose_dark` | Không chọn tư thế | |
| | `CameraScreen_adjust_dark` | Đang mở bảng chỉnh lớp phủ (độ mờ, cỡ) | |
| | `CameraScreen_locked_dark` | Lớp phủ đã khóa | |
| | `CameraScreen_matching_dark` | Có người trong khung: hiện điểm khớp và lời nhắc chỉnh dáng | |
| | `CameraScreen_matched_dark` | Điểm khớp đạt ngưỡng: báo sẵn sàng chụp | |
| | `CameraScreen_no-person_dark` | Không thấy người: không hiện điểm, có lời nhắc đứng vào khung | |
| | `CameraScreen_countdown_dark` | Đang đếm ngược hẹn giờ | |
| | `CameraScreen_starting_dark` | Đang mở camera (đang tải) | |
| | `CameraScreen_permission-denied_dark` | Chưa có quyền camera | |
| | `CameraScreen_error_dark` | Không mở được camera | |
| | `CameraScreen_offline_dark` | Mất mạng khi đang dùng: hộp thoại lỗi mạng kèm "Thử lại" | |
| Xem lại sau khi chụp | `ReviewScreen_default_dark` | Bình thường | `:feature:camera` |
| | `ReviewScreen_saving_dark` | Đang lưu | |
| | `ReviewScreen_save-error_dark` | Lưu lỗi (hết bộ nhớ) | |
| Thư viện tư thế | `PosesScreen_default_light`, `PosesScreen_default_dark` | Bình thường, lưới 2 cột | `:feature:poses` |
| | `PosesScreen_loading_light`, `PosesScreen_loading_dark` | Đang tải thêm (khung xương thẻ) | |
| | `PosesScreen_offline_light`, `PosesScreen_offline_dark` | Mất mạng khi đang dùng: hộp thoại lỗi mạng kèm "Thử lại" phủ lên màn hình | |
| | `PosesScreen_error_light`, `PosesScreen_error_dark` | Lỗi tải catalog, nút thử lại | |
| | `PosesScreen_saved-empty_light`, `PosesScreen_saved-empty_dark` | Chip "Đã lưu" chưa có tư thế | |
| Chi tiết tư thế | `PoseDetailScreen_default_light`, `PoseDetailScreen_default_dark` | Bình thường, có bước và lỗi hay gặp | `:feature:poses` |
| | `PoseDetailScreen_loading_light`, `PoseDetailScreen_loading_dark` | Đang tải ảnh lớn | |
| | `PoseDetailScreen_error_light`, `PoseDetailScreen_error_dark` | Không tải được ảnh, nút thử lại | |
| Cài đặt | `SettingsScreen_default_light`, `SettingsScreen_default_dark` | Bình thường | `:feature:settings` |

Tổng: 7 màn hình, 39 frame ở khung điện thoại 411 x 891.

**Điện thoại màn hình gập (đã chốt hỗ trợ):** thêm frame khung mở ra **673 x 841** (khung `Devices.FOLDABLE` của Compose) cho các màn hình có bố cục đổi khi màn hình rộng, tên `<Ten>Screen_<trạng thái>_<light|dark>_unfolded`: `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded`, `PosesScreen_default_light_unfolded`, `PosesScreen_default_dark_unfolded`, `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_default_dark_unfolded` (6 frame). Thư viện tăng số cột theo chiều rộng; chi tiết tư thế chia hai cột (ảnh, hướng dẫn); camera giữ khung 3:4 ở giữa, công cụ dời sang cột bên. Screenshot test có thêm ảnh ở khung này. Màn hình gập lại dùng bố cục điện thoại. Máy tính bảng loại ra bằng danh mục thiết bị trên Play Console; giao diện vẫn không vỡ nếu chạy trên máy lớn.

## 8. Điểm nổi bật

- Mở app là chụp. Không phải qua chuỗi quảng cáo, chọn ngôn ngữ hay trang giới thiệu.
- Hơn 200 tư thế cho nữ, nam, cặp đôi, bạn bè và gia đình.
- Nét viền rõ trên mọi nền. Kéo, phóng to, làm mờ, lật, khóa theo ý bạn.
- Mỗi tư thế có vài bước ngắn ngay trên camera: đứng thế nào, tay để đâu, lỗi nên tránh.
- Chụp xong xem lại ngay, rồi sang tư thế tiếp chỉ một chạm.
- Không quảng cáo nào che camera.

## 9. Quảng cáo và dữ liệu (câu hỏi cho dev)

### Vị trí quảng cáo app mẫu dùng

| App | Luồng mở đầu | Màn danh sách, trang chủ | Camera | Chuyển màn | Mật độ đo được |
| --- | --- | --- | --- | --- | --- |
| Pose Camera | Interstitial splash + native toàn màn hình; 2 native ở chọn ngôn ngữ + interstitial + native toàn màn hình; native giới thiệu, 1 trang giới thiệu là quảng cáo, interstitial cuối | Native trong home, banner thu gọn tự bung lại sau khoảng 20 giây (home, gallery) | Banner thu gọn che hàng tư thế | Interstitial khi vào camera, khi back, khi mở ảnh (kèm native toàn màn hình) | Khoảng 6 quảng cáo toàn màn hình mỗi lần mở nguội (`02` đến `14`, `40`, `41`) |
| AI Posing | Interstitial splash, 2 native toàn màn hình, native đáy ở ngôn ngữ và 3 trang giới thiệu | Native thu gọn che thanh điều hướng, native trong lưới mỗi khoảng 4 thẻ | Native nhỏ 76dp | Interstitial khi mở chủ đề, back, mở chi tiết, thoát camera, sau màn trả phí | 8 interstitial trong 31 phút (mục 6) |
| SimplePose (có mạng) | Interstitial splash mỗi lần mở nguội, native trang giới thiệu 3 | Native thu gọn bung 393x301dp ở Khám phá, Tìm, Cài đặt, chi tiết; dải native thường trực dưới thanh điều hướng | Không | Interstitial mỗi lần đóng camera, cách nhau ít nhất 30 giây | Khoảng 2 interstitial mỗi phiên ngắn (mục 6) |
| PoseMate | Không | Một banner 320x50 ở trang chủ, thường không lấp đầy | Không | Không thấy (APK có interstitial, rewarded) | 0 interstitial trong khoảng 56 phút (mục 6) |
| PoseCamera | Không | Không | Không | Không | Không quảng cáo; Pro mua một lần |

Các app quảng cáo dày điều khiển tần suất từ xa: `step_count` (interstitial mỗi N lần bấm), `interval` 30 giây, `refresh_rate` 20 giây cho banner (APK, Chiến lược quảng cáo).

### Câu hỏi 9.1: mức quảng cáo

Dev chọn một mức. AI đề xuất **mức B**. **Dev chọn mức B (vừa), 2026-10-21.**

| Mức | Vị trí (`AdPlacement` dự kiến) | Ghi chú |
| --- | --- | --- |
| A. Nhẹ (như PoseMate) | `poses_banner`: banner neo đáy màn Tư thế | Doanh thu thấp; điểm Play có thể cao |
| **B. Vừa (đề xuất)** | `poses_banner` như trên; `poses_detail_banner`: banner đáy chi tiết tư thế; `review_interstitial`: interstitial khi rời màn xem lại sau khi đã lưu ảnh (điểm dừng tự nhiên) | Không quảng cáo ở chào mừng, camera, xem lại (trong lúc xem), cài đặt |
| C. Dày hơn | Như B, thêm app open khi quay lại app từ nền | Rủi ro lặp lại bài học 1 |

Giới hạn đề xuất cho `review_interstitial`, đặt trong `RemoteSettings` để chỉnh từ xa: không hiện trước ảnh thứ 2 của phiên; cách nhau ít nhất 120 giây; tối đa 1 lần mỗi 3 ảnh lưu. Mọi vị trí mặc định tắt, bật bằng `ads_<vị trí>_enabled` (ADR-0014). Không dùng mediation lúc đầu, chỉ AdMob.

Thước đo: số quảng cáo toàn màn hình mỗi 10 phút phiên (đích tối đa 3), số quảng cáo trước ảnh đầu tiên (đích 0).

- Dev trả lời: mức vừa (B). Không có gói gỡ quảng cáo.

### Câu hỏi 9.2: sự kiện phân tích

Đề xuất (không gửi ảnh, không gửi chữ người dùng nhập): `onboarding_complete`, `camera_permission_result` (granted/denied), `pose_selected` (category, pose_id), `overlay_adjusted` (loại chỉnh), `photo_captured`, `photo_saved`, `pose_detail_opened`, `pose_saved`, `content_load_failed` (lý do). Dùng để đo bài học 1, 4, 7 và chọn tư thế cần làm thêm.

- Dev trả lời: đúng 9 sự kiện đề xuất: `onboarding_complete`, `camera_permission_result`, `pose_selected`, `overlay_adjusted`, `photo_captured`, `photo_saved`, `pose_detail_opened`, `pose_saved`, `content_load_failed`.

### Câu hỏi 9.3: dữ liệu thu thập

Đề xuất: ảnh chỉ lưu trên máy, không tải lên đâu. Không đăng nhập. Chỉ số liệu ẩn danh của Firebase. Đồng ý quảng cáo qua hộp thoại của `:core:ads` (ADR-0013). Câu này đưa vào phần mô tả trên Play như PoseMate làm (Play, 2.7).

- Dev trả lời: ảnh chỉ lưu trên máy, không cần đăng nhập.

## 10. Hướng thiết kế

**Giữ lại** (khuôn người dùng đã quen):

- Camera nền tối, khung 3:4, nút chụp tròn trắng giữa đáy, nút đổi camera bên phải, ô ảnh vừa chụp bên trái.
- Dải tư thế cuộn ngang ngay trên nút chụp, ô đang chọn có viền màu nhấn; chip nhóm cuộn ngang.
- Nét viền trắng có quầng tối (SimplePose, PoseMate).
- Thư viện là lưới 2 cột ảnh thẻ bo góc; chi tiết tư thế có nút chính dính đáy.
- Thanh công cụ camera ở cạnh trên hoặc cột phải; bảng chỉnh lớp phủ nổi trên khung.

**Đổi** (để có nhận diện riêng):

- **Màu:** tránh tím (nhóm Pose Camera, SimplePose, poseideas), xanh lá chuyển sắc (AI Posing), xanh ngọc (PoseMate), nâu đất trên nền kem (PoseCamera), vàng mù tạt (Pose Camera).
- **Icon:** một bộ có giấy phép thương mại rõ ràng cho cả app (ví dụ Material Symbols Rounded hoặc Lucide). Không dùng emoji làm icon (PoseMate dùng).
- **Phông chữ:** tránh Inter, Baloo 2, Be Vietnam Pro, Nunito, Quicksand (đã dùng trong các app mẫu). Chọn phông hỗ trợ tiếng Việt đầy đủ, kiểm tra dấu trước khi chốt.
- **Hình dạng:** bo góc, kiểu nút, kiểu chip riêng; nút của app không giống nút CTA quảng cáo (bài học 3).
- **Giọng văn:** ngắn, thân thiện, không phóng đại ("1000+ dáng chụp hoàn hảo" kiểu AI Posing).
- **Hình minh họa:** không dùng ảnh người mẫu kiểu Hàn tím neon như ảnh store nhóm Pose Camera.

Gợi ý ý hướng (bằng chữ, dựng sau khi dev duyệt):

1. **Phòng tối:** nền tối sâu, một màu nhấn cam san hô ấm, chữ tròn, cảm giác máy ảnh chuyên nghiệp nhưng thân thiện.
2. **Giấy trắng:** nền sáng sạch, màu nhấn xanh dương đậm, tiêu đề phông có chân, nhiều khoảng trắng, hợp người dùng thích tối giản.
3. **Sticker:** nhiều màu, mỗi nhóm tư thế một màu, chip và thẻ dày, hợp người dùng trẻ và mạng xã hội.

Sau khi dev duyệt các mục trên, AI dựng 2 đến 3 hướng trên Figma (`docs/ops/design-workflow.md`, mục Thiết kế app clone):

| Hướng | Ý chính | Màu | Chữ | Icon | Link Figma |
| --- | --- | --- | --- | --- | --- |
| 1. Phòng tối | Nền tối sâu ở cả camera và thư viện, một màu nhấn ấm, chip và nút bo tròn hẳn, thẻ tư thế có nền | Nhấn cam san hô `#FF7A59` (tối), `#C8431D` (sáng); nền `#121014` | Lexend | Material Symbols Rounded | [Direction/Phong-toi](https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=3-2) |
| 2. Giấy trắng | Nền trắng, nhiều khoảng trắng, góc bo nhỏ (8), chip viền mảnh, chip đang chọn tô màu mực | Nhấn xanh dương đậm `#1F3FBF` (sáng), `#8EA2FF` (tối); mực `#14161C` | Fraunces (tiêu đề, có chân), Plus Jakarta Sans (nội dung) | Material Symbols Rounded | [Direction/Giay-trang](https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=3-345) |
| 3. Sticker | Viền đen 2 dp, bóng đổ cứng, mỗi nhóm tư thế một màu, thẻ và chip như nhãn dán | Nhấn xanh `#2F6BFF`; 6 màu nhóm (hồng, vàng, xanh lá, xanh dương, cam, xanh nõn) | Bricolage Grotesque | Material Symbols Rounded | [Direction/Sticker](https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=3-688) |

Mỗi trang gồm bảng phong cách (màu sáng và tối, chữ, bo góc, khoảng cách, icon, thành phần: nút, chip, ô tư thế, nút icon trên camera, thẻ tư thế) và hai màn hình ở khung 411 x 891: `CameraScreen_default_dark` và `PosesScreen_default_<light|dark>`. Ảnh tư thế là hình nét viền tạm, nội dung thật thay sau. Biến và kiểu chữ trong Figma chỉ tạo cho hướng được chọn (design-workflow.md, mục 8.2).

Ảnh so sánh với app mẫu tốt nhất (camera của SimplePose, thư viện của PoseMate) nằm trong thư mục làm việc, không vào repo vì chứa ảnh chụp app mẫu: `figma-pilot/directions/compare_<hướng>.png`. Màn camera của cả ba hướng chưa có nút zoom 0.5x/1x/2x như SimplePose; thêm khi dựng toàn bộ.

Dev chọn: Sticker, ngày 2026-10-21. Dev làm mịn bằng AI Agent của Figma hai điểm: dấu ✓ cho chip và tư thế đang chọn (không chỉ dựa vào màu vàng); nút camera đang bật (hẹn giờ 3s, lưới, hiện tư thế) tô vàng, lưới chia ba hiện trên khung xem trước.

Dựng toàn bộ theo hướng Sticker (design-workflow.md, mục 8.2), cùng file Figma:

- [Design system](https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=12-2): bộ biến `color` (Light, Dark; tên theo `tokens.json`, thêm `success*`, `shadowSticker`, `category/*` cho 8 nhóm, `camera/*` cho màn camera luôn tối), `spacing` (`xs` đến `xl` như `tokens.json`), `radius`, `stroke`; 15 kiểu chữ tên theo `tokens.json`, phông Bricolage Grotesque; hiệu ứng `shadow/sticker`. Component: `Button`, `IconButton`, `CameraIconButton` (`State=Default|Active`, `Content=Icon|Text`), `Chip` và `PoseThumb` (dấu ✓ nằm trong biến thể `Selected`), `SelectedBadge`, `GridOverlay`, `Switch`, `PoseCard`, `SkeletonCard`, `StepCard`, `ZoomControl`, `MatchHint`, `Slider`, `AdjustPanel`, `TopAppBar`, `Snackbar`, `StateMessage`, `SectionHeader`, `SettingsRow`, `Spinner`, `NetworkDialog`, `ProgressBar`, `BrandLogo` (logo tạm, chờ icon app).
- [Screens](https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=16-23): đủ 39 frame điện thoại và 6 frame màn hình gập ở mục 7, gồm màn loading theo Sticker và hộp thoại mất mạng (mục 12, câu 13). Bản `_dark` là bản `_light` gán chế độ Dark. Camera có thêm nút zoom 0.5x/1x/2x.

Thay đổi so với hệ thống thiết kế hiện tại của code: phông chữ (Inter sang Bricolage Grotesque), toàn bộ màu, và các biến ngoài `tokens.json` ở trên. Đưa vào `:core:designsystem` là thay đổi module core, cần plan rủi ro cao (ADR-0008). Ảnh tư thế và ảnh camera trên thiết kế là hình tạm (tên có "ảnh tạm" hoặc `pose-outline`).

## 11. Tài sản mượn tạm dự kiến

Nội dung tư thế là phần tốn công nhất. Mỗi tư thế cần: ảnh mẫu (hai cỡ), ảnh nét viền nền trong, tên, 3 đến 5 bước, một lỗi hay gặp, thẻ, và nên có 33 điểm khớp.

### Nguồn có thể mượn tạm (chỉ staging, ADR-0021 mục 8)

Mọi nguồn dưới đây có **giấy phép không rõ**. Chỉ lấy từ APK đã giải nén trong `decoded/`. Không gọi API hay kho dữ liệu của app mẫu (catalog GitHub của SimplePose, API `aifastwork` của AI Posing): đó là gọi backend của app mẫu, ADR-0021 không cho phép.

| App nguồn | Nội dung | Định dạng | Số lượng | Hợp cho việc gì |
| --- | --- | --- | --- | --- |
| PoseMate 1.0.8 | Dữ liệu tư thế có cấu trúc (tên, bước, góc, khung, lỗi hay gặp, thẻ) và ảnh nhỏ | JSON `assets/poses_data/<nhóm>.json`; WebP `assets/pose_thumbs/` (ảnh thật khoảng 360 px, nét viền khoảng 180 px) | 1.629 tư thế, 3.258 ảnh | Dựng lưới, chi tiết, thử tải catalog lớn. Ảnh nét viền quá nhỏ cho camera; chữ không khớp ảnh (bài học 9). Ảnh lớn nằm trong gói Play Asset Delivery, không có trong APK đã giải nén |
| SimplePose 1.0.1 | Tư thế đóng gói đủ bộ: ảnh mẫu, nét viền lớn, 33 điểm khớp | `assets/pose-data/<mã băm>/`: `pose.json`, `preview_1200.webp`, `preview_540.webp`, `ink_outline@2x.png` (cao 2048) | 24 (Nữ 12, Nam 12) | Mẫu tốt nhất cho camera và cho spike ML Kit |
| Nhóm Pose Camera | Nét vẽ trắng nền trong | WebP drawable `im_<nhóm>_<n>.webp`, khoảng 1164 x 1242 | 32 (`.ideas`), 38 (`.photo`) | Thử nét viền nhiều người (cặp đôi, gia đình, thể thao) |
| `com.camera.guide.ideas.photo.pose` | Cặp ảnh thật và nét viền | WebP 1500 x 1500 + `_overlay.webp` | 24 cặp | Thử lớp phủ với ảnh mẫu tương ứng |
| PoseCamera 1.0.4 | Ảnh ma-nơ-canh và hướng dẫn | WebP 1024 x 1536; JSON `poses/<lang>/poses_<nhóm>.json` (en, pl, pt) | 120 tư thế | Mẫu khuôn chữ hướng dẫn (bước, mẹo, lỗi) |

Đề xuất mượn để code sớm: 24 tư thế SimplePose (đủ ảnh, nét viền, điểm khớp) cho `:feature:camera`, và khoảng 100 mục PoseMate (chỉ ảnh nhỏ và khuôn dữ liệu) cho `:feature:poses`. Đặt tên `borrowed_`, trên R2 staging dưới `borrowed/`, ghi `docs/borrowed-assets.md` kèm SHA-256 trong cùng PR. Thay hết trước khi phát hành.

### Bản thay thế lâu dài

| Cách | Làm thế nào | Ưu | Nhược |
| --- | --- | --- | --- |
| **Tự tạo bằng AI (đề xuất)** | Như quy trình PoseMate (APK mục 7): sinh lưới ảnh 4 x 5 bằng công cụ tạo ảnh, nâng phân giải, cắt từng ô. Nét viền: tách người bằng phân đoạn ảnh rồi lấy đường biên, người làm nội dung sửa tay. Điểm khớp: chạy ML Kit Pose trên ảnh mẫu bằng script. Chữ: AI viết nháp theo khuôn, người duyệt khớp với ảnh | Nhanh, rẻ, ra được hàng trăm tư thế | Phải kiểm điều khoản thương mại của công cụ tạo ảnh; tay, ngón dễ lỗi; chữ dễ lệch ảnh như PoseMate |
| Thuê vẽ nét viền | Họa sĩ vẽ nét viền theo ảnh mẫu tự tạo hoặc ảnh có giấy phép | Nét đẹp, đồng đều, nhận diện riêng | Chậm, tốn tiền theo từng tư thế |
| Chụp ảnh thật | Thuê người mẫu, có giấy đồng ý dùng hình | Giấy phép rõ nhất, ảnh thật | Đắt, chậm, khó đủ nhóm |

Người làm: người phụ trách nội dung (dev chỉ định, mục 12). Đích ra mắt: 200 tư thế, trong đó 60 thuộc bộ khởi đầu (tải ở lần mở đầu tiên).

### Cách lưu trên R2 (ADR-0006, ADR-0015)

- Catalog: `poses/catalog/v<n>/catalog-<sha8>.json`, có `datasetVersion`, `minAppSchema`, danh sách nhóm và tư thế. App biết đường dẫn catalog mới qua một tham số trong `RemoteSettings`; nội dung đổi thì đường dẫn đổi.
- Ảnh: `poses/img/<poseId>/<sha8>/preview_540.webp`, `preview_1200.webp`, `outline.webp` (WebP không mất dữ liệu, có kênh trong suốt).
  - Ghi chú 2026-10-23 (L-025): khuôn trên bị `AssetPath` từ chối (ADR-0015, thư mục mã băm không được coi là phiên bản). Đường dẫn thật theo plan T-009: `poses/img/<poseId>/preview_540-<sha8>.webp`, `preview_1200-<sha8>.webp`, `outline-<sha8>.webp`, mỗi file mã băm của chính nó.
- Mỗi tư thế: `id`, `category`, `subjectType` (một người, cặp đôi, nhóm), `gender`, `framing`, `tags`, `difficulty`, `name` và `steps` theo ngôn ngữ, `mistakes`, `preview`, `outline`, `keypoints` (33 điểm, tùy chọn). Khuôn gộp từ PoseMate, PoseCamera, SimplePose (APK, Gợi ý kỹ thuật 2).
- Không đóng gói tư thế trong APK. Bộ khởi đầu (60 tư thế: catalog, ảnh nhỏ, nét viền) tải từ R2 ở lần mở đầu tiên, trong màn loading, và giữ trên máy (ADR-0015); các lần mở sau chỉ tải phần mới. Tải qua `RemoteAssets.load(AssetPath(...))`, gọi từ ViewModel; kết quả `Unavailable` đưa vào `UiState` với nút thử lại; nếu do mất mạng thì hiện hộp thoại lỗi mạng.
- File mượn chỉ ở R2 staging, dưới `borrowed/poses/...`.

## 12. Câu hỏi còn mở

1. **Thị trường mục tiêu?** Đề xuất: ra mắt toàn cầu bằng tiếng Anh, thêm tiếng Việt, Tây Ban Nha, Bồ Đào Nha (Brazil), Indonesia, Hindi, vì đó là nơi có nhiều đánh giá nhất và có phàn nàn thiếu ngôn ngữ (Play, 5.2 và 3.1). Nếu chỉ một nước, Việt Nam có từ khóa "tạo dáng chụp ảnh" ít cạnh tranh (Play, 5.3).
   - Dev trả lời: Ấn Độ, Brazil, Mexico, Philippines, Indonesia, Thái Lan, Việt Nam. Ngôn ngữ đề xuất theo đó: tiếng Anh (Ấn Độ, Philippines), Hindi, Bồ Đào Nha (Brazil), Tây Ban Nha (Mexico, `es-419`), Indonesia, Thái, Việt. Tiếng Filipino để sau (tiếng Anh phổ biến ở Philippines). Tên tư thế và các bước dịch đủ 7 ngôn ngữ.
2. **Tên và tên gói?** Đề xuất: PoseLine, `com.vglstudio.poseline`. Cần dev tra trực tiếp trên Play trước khi chốt.
   - Dev trả lời: Camera Pose: Lines and Guides, `com.vglstudio.camera.pose.line.guide`.
3. **Mức quảng cáo?** Xem câu hỏi 9.1. Đề xuất mức B.
   - Dev trả lời: mức vừa (B).
4. **Màn bắt đầu là camera hay thư viện?** Đề xuất camera (bài học 1). Đổi lại, màn có banner (thư viện) ít được mở hơn; nếu dev ưu tiên doanh thu banner thì chọn thư viện làm màn bắt đầu như SimplePose, PoseMate (thêm 2 chạm tới camera).
   - Dev trả lời: camera.
5. **Nhận dạng dáng (ML Kit) có vào bản đầu không?** Đề xuất: không. Làm spike sau bản đầu với tiêu chí ở mục 5; trước đó nhờ một người đứng trước camera thử AI Posing và PoseMate để xem tính năng thật trông thế nào.
   - Dev trả lời: có, làm ngay từ đầu. Tiêu chí chấp nhận ở mục 5.
6. **Thêm CameraX và quyền `CAMERA`?** Cả hai cần thiết cho app. Thêm thư viện đụng `gradle/libs.versions.toml` (file được bảo vệ), nên cần plan rủi ro cao duyệt chéo; nếu CameraX hữu ích cho nhiều app thì đề xuất về dự án mẫu (`docs/ops/from-app.md`).
   - Dev trả lời: thêm ngay từ đầu, trong bước khởi tạo app do dev làm (cùng lúc với `scripts/new-app.sh` và `docs/ops/setup-new-app.md`), không qua phiên tự chạy. Thêm cùng lúc thư viện Play In-App Review (câu 8).
7. **Ai làm nội dung tư thế, ngân sách bao nhiêu?** Đề xuất: tự tạo bằng AI, một người duyệt; đích 200 tư thế trước khi phát hành, 60 đóng gói. Cần chọn công cụ tạo ảnh có điều khoản thương mại rõ ràng.
   - Dev trả lời: AI tự tạo, người duyệt; mượn tạm từ APK đã giải nén trong lúc chờ. Ngân sách và công cụ tạo ảnh: chưa chốt.
8. **Có hỏi đánh giá trong app không?** Đề xuất: bản đầu không. Nếu có thì dùng Play In-App Review (thêm SDK, cần spec), chỉ sau khi người dùng đã lưu ít nhất 3 ảnh.
   - Dev trả lời: có. Đề xuất cách làm: Play In-App Review, chỉ sau khi người dùng đã lưu ít nhất 3 ảnh, ở điểm dừng tự nhiên (rời màn xem lại), không cùng lúc với quảng cáo toàn màn hình, tối đa một lần mỗi 30 ngày; tham số trong `RemoteSettings`.
9. **Gói gỡ quảng cáo?** Đề xuất: để bản sau, mua một lần như PoseMate, không thuê bao tuần. Cần Play Billing và spec riêng.
   - Dev trả lời: không có.
10. **Camera có cần bản giao diện sáng không?** Đề xuất: không, camera và màn xem lại luôn nền tối; chỉ dựng frame `_dark` (mục 7).
    - Dev trả lời: không.
11. **Có cần lưu thêm bản ảnh kèm nét viền không?** Không app mẫu nào làm (PoseCamera có nút lưu nhưng chỉ tạo bản sao). Đề xuất: để sau, xem số liệu `photo_saved` trước.
    - Dev trả lời: không.
12. **Hỗ trợ máy tính bảng?** PoseMate có ảnh store riêng cho tablet. Đề xuất: bản đầu chỉ điện thoại, giao diện không vỡ trên tablet.
    - Dev trả lời: không hỗ trợ máy tính bảng; có hỗ trợ điện thoại màn hình gập (mục 7).
13. **App có dùng được khi không có mạng không?** Bản đề xuất ban đầu lấy "dùng được khi mất mạng" làm điểm khác biệt (khoảng trống 4, bài học 4).
    - Dev trả lời (2026-10-21): không. Mọi app của nhóm luôn cần mạng. Mở app khi không có mạng thì không cho vào app (màn loading báo lỗi, có nút "Thử lại"). Đang dùng mà mất mạng thì hiện hộp thoại báo lỗi mạng, có nút "Thử lại".
    - AI cập nhật theo câu trả lời: bỏ mọi chữ hứa dùng được khi không có mạng (điểm nổi bật, màn chào, phụ đề thư viện); trạng thái `offline` của thư viện đổi từ băng báo sang hộp thoại; thêm `CameraScreen_offline_dark`; thêm màn loading vào luồng và danh sách màn hình (đổi giao diện theo Sticker). Việc chặn lúc mở app đã có sẵn ở feature `loading` (ADR-0018). Hộp thoại khi đang dùng là phần mới, chung cho mọi app: đề xuất đặt ở module core và vỏ app, rồi đưa về dự án mẫu (`docs/ops/from-app.md`).
14. **Nhận ra mất mạng khi đang dùng bằng cách nào?** Phương án A: theo dõi trạng thái mạng của máy; cần quyền `ACCESS_NETWORK_STATE` (manifest chưa có, thêm quyền là rủi ro cao), hiện hộp thoại ngay khi máy báo mất mạng, kể cả lúc người dùng chỉ đang chụp. Phương án B: chỉ hiện khi một lần tải (Remote Config, R2, quảng cáo) thất bại vì mạng; không cần quyền mới nhưng có thể báo trễ. AI đề xuất A, vì đúng ý "đang dùng mà mất mạng thì báo" và cách hiện hộp thoại nhất quán.
    - Dev trả lời (2026-10-21): kết hợp cả A và B. Theo dõi trạng thái mạng của máy để báo ngay, và báo cả khi một lần tải thất bại vì mạng. Phương án A thêm quyền `ACCESS_NETWORK_STATE`: cần spec duyệt và plan rủi ro cao (`AndroidManifest.xml`, risk-scoring.md).
15. **Còn giữ 60 tư thế đóng gói trong APK không?** Không còn để dùng khi mất mạng, nhưng vẫn giúp thư viện và dải tư thế hiện ngay, không chờ tải (bài học 7). Đổi lại APK nặng hơn (ước tính vài MB). AI đề xuất giữ.
    - Dev trả lời (2026-10-21): không đóng gói trong APK; tải về ở lần mở đầu tiên.
    - AI đề xuất cách làm: tải bộ khởi đầu trong màn loading của lần mở đầu tiên, thay cho ba file mẫu (`hello.txt`, `content.txt`, `goodbye.txt`) của dự án mẫu; thanh tiến trình tính theo bộ này. Lần mở đầu vì vậy lâu hơn các lần sau. Đây là thay đổi spec `loading` (đang `confirmed`), làm ở bước spec.
