# Nghiên cứu thị trường app "pose camera" (Google Play và web)

Ngày thu thập: 19/10/2026. Nguồn chính là trang Google Play (bản `hl=en` và `hl=vi`), dữ liệu đánh giá công khai trên Play, App Store (iTunes Search API) và vài bài báo. Không dùng thiết bị adb.

Ghi chú về số liệu:

- "Lượt cài" là mốc Play hiển thị (ví dụ 500,000+). Cột "số cài chính xác" là con số Play nhúng trong dữ liệu trang chi tiết (trường installs đầy đủ), không hiện trên giao diện. Nguồn của cả hai là trang Play của app.
- Điểm và số đánh giá trên Play thay đổi theo quốc gia (`gl`). Bảng ghi điểm ở Mỹ nếu có, nếu không thì ghi điểm ở quốc gia có hiển thị.
- AppBrain chặn truy cập tự động (Cloudflare, lỗi 403). Vì vậy báo cáo không có số liệu SDK, doanh thu ước tính hay xếp hạng từ AppBrain, AppMagic, SensorTower. Muốn có SDK thì cần phân tích APK (việc của agent khác).
- Phiên bản trên Play có thể khác phiên bản đã cài trên máy nhóm. Ví dụ AI Posing trên Play ghi 1.3.9, máy nhóm có 1.4.0.

## 1. Bảng tổng quan

| # | App (package) | Nhà phát triển | Lượt cài | Số cài chính xác | Điểm (số đánh giá) | Phát hành | Cập nhật | Mua trong app | Quảng cáo |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Pose Camera: Photo Ideas Guide (`com.camera.filter.pose.guide.ideas`) | Tokii Apps | 500,000+ | 572,801 | 3,3 (3.686) | 18/04/2026 | 25/06/2026 | Không ghi | Có |
| 2 | AI Posing: Pose Ideas, Guide (`com.aiposing.pose`) | Alphasoftx Limited | 100,000+ | 103,754 | 4,2 ở VN, Mỹ không hiện (465) | 09/12/2025 | 24/09/2026 | $8.99 - $39.99 | Có |
| 3 | Pose Camera: Aesthetic Guides (`com.camera.filter.pose.guide.ideas.photo`) | Ocean Limousine Limited | 50,000+ | 60,514 | 3,6 ở Ấn Độ (65) | 19/06/2026 | 07/09/2026 | Không ghi | Có |
| 4 | Pose Camera - Photos Guide (`com.camera.guide.ideas.photo.pose`) | Smart Experiences | 10,000+ | 49,811 | 2,8 ở VN, 4,1 ở Ấn Độ (395) | 20/05/2026 | 20/05/2026 | Không ghi | Có |
| 5 | Pose Camera: Posing Guide (`com.vector123.posecamera`) | vector123 | 10,000+ | 13,056 | 1,8 ở Ấn Độ (16) | 20/06/2026 | 26/07/2026 | $1.49, mua một lần | Không |
| 6 | Pose Camera - Photos Idea (`com.posecamera.poseideas`) | Trandyapp | 1,000+ | 1,251 | Chưa có | 16/06/2026 | 02/07/2026 | Không ghi | Có |
| 7 | PoseMate - Camera Pose Guide (`com.gws.posemate`) | GoodwillSolution | 100+ | 301 | Chưa có | 14/09/2026 | 19/09/2026 | $0.99 | Có |
| 8 | Simple Pose: Camera Pose Guide (`com.cam.simplepose.pose.camera.photo.guide`) | Braly Danang | 100+ | 261 | Chưa có | Không hiện | 16/10/2026 | $3.99 - $19.99 | Có |

Cả 8 app đều phân loại nội dung "Everyone" (Mọi người). Bảy app thuộc thể loại Photography. Simple Pose thuộc Entertainment.

Nguồn: trang Play của từng app, xem mục Nguồn.

Điểm chung dễ thấy:

- Năm app (1, 3, 4, 6 và phần nào 2) dùng gần như cùng một khung: tên "Pose Camera", mô tả có cùng các mục "Pose Ideas, Trending Aesthetic Poses, Pose Guide overlay, Zoom, Save, Share". App 6 còn chép nguyên câu "Pose Camera: Photo Ideas Guide" của app 1 trong mô tả. Đây là các bản clone của nhau.
- Package của app 1 và app 3 gần giống hệt nhau (`...pose.guide.ideas` và `...pose.guide.ideas.photo`). Ảnh store cùng phong cách tím. Nhà phát triển là các tài khoản khác nhau (Tokii Apps, Ocean Limousine Limited) nhưng danh mục app của hai bên trùng loại (shimeji, OC maker, retro camera).
- App 1 đạt hơn 570 nghìn lượt cài trong khoảng 6 tháng chỉ nhờ quảng cáo. Mô hình là "mua lượt cài, khai thác bằng quảng cáo dày đặc".

## 2. Từng app

### 2.1. Pose Camera: Photo Ideas Guide (Tokii Apps), app dẫn đầu nhóm

- Trang Play: https://play.google.com/store/apps/details?id=com.camera.filter.pose.guide.ideas&hl=en
- 500,000+ lượt cài, số chính xác 572,801. Điểm 3,3 với 3.686 đánh giá ở Mỹ.
- Điểm khác nhau rất mạnh theo nước: Mexico 1,9; Philippines 3,2; Indonesia 3,3; Brazil 3,8; Ấn Độ 4,1; Thái Lan 4,1 (trang Play với `gl=mx`, `ph`, `id`, `br`, `in`, `th`).
- Phát hành 18/04/2026, cập nhật 25/06/2026. Không ghi mua trong app. Có quảng cáo.
- Tiêu đề: "Pose Camera: Photo Ideas Guide". Mô tả ngắn: "Inspiring photo pose ideas for selfies, travel, daily shots, couple and moments".
- Tính năng quảng bá: ý tưởng tạo dáng, tư thế "aesthetic" theo trend, khung tư thế phủ lên camera (overlay), zoom mượt, lưu ảnh, chia sẻ một chạm.
- Ảnh store: 5 ảnh. Thông điệp: "Strike The Perfect Pose", "Find Your Signature Style", "Ready For Every Occasion", "Better Memories Together", "Capture Moments That Matter". Người mẫu phong cách Hàn/Nhật, nhãn dán "CUTE!", "COOL". Tư thế hiện dạng bóng xám (silhouette) theo nhãn Beach, Travel, Date.
- Nhà phát triển có khoảng 20 app tiện ích khác (đàn, la bàn, bàn phím, remote TV, shimeji). Đây là studio làm app hàng loạt (https://play.google.com/store/apps/developer?id=Tokii+Apps).

### 2.2. AI Posing: Pose Ideas, Guide (Alphasoftx Limited), app duy nhất có AI chấm điểm

- Trang Play: https://play.google.com/store/apps/details?id=com.aiposing.pose&hl=en
- 100,000+ lượt cài, số chính xác 103,754. 465 đánh giá. Điểm: Việt Nam 4,2; Ấn Độ 4,2; Mexico 4,2; Indonesia 4,1; Brazil 3,9; Philippines 4,7. Mỹ không hiện điểm.
- Phát hành 09/12/2025, cập nhật 24/09/2026, bản 1.3.9. Mua trong app $8.99 - $39.99. Có quảng cáo.
- Mô tả ngắn: "AI pose guidance, pose ideas & pose references for stunning photos."
- Tính năng quảng bá: "AI Pose Coach" dùng theo dõi khung xương (skeleton tracking) trên camera, phủ đường viền tư thế, chấm "pose matching score". Thư viện "hàng nghìn" tư thế chia theo Indoor, Outdoor, Posture, Mood, Style. Mẹo cho thân, tay, nét mặt, trang phục. Album, yêu thích, xuất JPG, PNG, PDF.
- Ảnh store: 6 ảnh nền tím đậm, chữ vàng: "AI Pose Ideas", "Pose Guide", "Pose References", "Pose Match Score" (ảnh minh họa "Best pose 90%"), "Edit Photos Easily", "Save Pose Ideas".
- Nhà phát triển chỉ có 4 app (IPTV, remote TV, giả lập GBA) (https://play.google.com/store/apps/developer?id=Alphasoftx+Limited).

### 2.3. Pose Camera: Aesthetic Guides (Ocean Limousine Limited)

- Trang Play: https://play.google.com/store/apps/details?id=com.camera.filter.pose.guide.ideas.photo&hl=en
- 50,000+ lượt cài, số chính xác 60,514. Điểm 3,6 với 65 đánh giá ở Ấn Độ.
- Phát hành 19/06/2026, cập nhật 07/09/2026. Không ghi mua trong app. Có quảng cáo.
- Mô tả ngắn: "Get pose ideas fast, use guides, and take better photos every day." Mô tả gần như chép lại app 2.1.
- Ảnh store: nền tím, cặp đôi châu Á. "Pose Camera Aesthetic Guides", "Strike The Perfect Pose", "Find Camera Pose Ideas", "Unique Pose Ideas", "Explore Pose Guide Styles" (lưới hình vẽ nét trắng). Màn camera có thanh chọn Selfie, Couple, Friend, Kid, Sport, Family.

### 2.4. Pose Camera - Photos Guide (Smart Experiences)

- Trang Play: https://play.google.com/store/apps/details?id=com.camera.guide.ideas.photo.pose&hl=en
- 10,000+ lượt cài, số chính xác 49,811. 395 đánh giá. Điểm: Việt Nam 2,8; Indonesia 3,8; Ấn Độ 4,1; Philippines 4,2; Thái Lan 4,5.
- Phát hành và cập nhật cùng ngày 20/05/2026, bản 1.0.0, chưa cập nhật lần nào. Có quảng cáo.
- Mô tả ngắn: "Trendy Selfie Poses, Photo Ideas & Camera Guides for aesthetic shots in one app".
- Tính năng: ý tưởng tư thế aesthetic (gương, cà phê, du lịch, OOTD), "Pose Match Camera", mẫu tư thế, lưu HD.
- Ảnh store: 4 ảnh tím: "Pose Camera Guide Idea", "Perfect Selfie With Guide", "Diversity Of Pose Template" (mục "Hot trending"), "Strike A Pose Together" (Couple, Selfie, Kid, Friend, Family, Sport).

### 2.5. Pose Camera: Posing Guide (vector123), app "sạch" duy nhất

- Trang Play: https://play.google.com/store/apps/details?id=com.vector123.posecamera&hl=en
- 10,000+ lượt cài, số chính xác 13,056. Điểm 1,8 với 16 đánh giá ở Ấn Độ.
- Phát hành 20/06/2026, cập nhật 26/07/2026. Không quảng cáo. Mua một lần $1.49 để mở Pro, không thuê bao.
- Mô tả ngắn: "Photo pose guide with outline overlay for selfies and natural shots".
- Tính năng: chạy offline, hơn 120 mẫu tư thế, hướng dẫn từng bước (cách đứng, tay, góc, lỗi hay gặp), camera có viền tư thế bán trong suốt, chỉnh độ mờ và cỡ, lưới, gương, hẹn giờ. Có cẩm nang nhiếp ảnh (phơi sáng, bố cục, ánh sáng). Có "shooting plan". Miễn phí 5 lần chụp có hướng dẫn.
- Ảnh store: phong cách tối giản, chữ serif, tông kem. "Match the Outline", "120+ Pose Ideas", "Step-by-Step Pose Guides", "Photo Skills Made Simple", "Save Poses, Plan Shoots". Tư thế vẽ nét chì.

### 2.6. Pose Camera - Photos Idea (Trandyapp)

- Trang Play: https://play.google.com/store/apps/details?id=com.posecamera.poseideas&hl=en
- 1,000+ lượt cài, số chính xác 1,251. Chưa có điểm.
- Phát hành 16/06/2026, cập nhật 02/07/2026. Có quảng cáo.
- Mô tả ngắn: "Get instant pose ideas for selfies, travel & everyday photos. Pose like a pro!" Mô tả chép khung của app 2.1.
- Ảnh store: ảnh dựng bằng AI, tông tím neon. "Pose Camera", "Strike a Pose" (bước "Match the Outline"), "Perfect Selfie With Guide", màn chính "PosePro" với danh mục Selfie, Couple, Friends, Sport.

### 2.7. PoseMate - Camera Pose Guide (GoodwillSolution)

- Trang Play: https://play.google.com/store/apps/details?id=com.gws.posemate&hl=en
- 100+ lượt cài, số chính xác 301. Phát hành 14/09/2026, cập nhật 19/09/2026. Mua trong app $0.99. Có quảng cáo.
- Mô tả ngắn: "Capture perfect photos with pose ideas, camera outlines, guides & tips."
- Tính năng: viền tư thế trên camera, 10 nhóm (selfie, couple, nhóm, du lịch, cưới, fitness, công sở, thời trang, trẻ em và bầu, theo mùa), mẹo góc máy và ánh sáng. Nhấn mạnh quyền riêng tư: ảnh không tải lên đám mây.
- Ảnh store: tông xanh lá. "1,600+ Curated Poses", "Detailed Pose Guidance", "Live Camera Pose Overlays", "Pro Photography Tips". Có 4 ảnh riêng cho máy tính bảng ("Optimized for Tablets").

### 2.8. Simple Pose: Camera Pose Guide (Braly Danang)

- Trang Play: https://play.google.com/store/apps/details?id=com.cam.simplepose.pose.camera.photo.guide&hl=en
- 100+ lượt cài, số chính xác 261. Cập nhật 16/10/2026. Mua trong app $3.99 - $19.99 (gói tuần, tháng, năm). Có quảng cáo.
- Mô tả ngắn: "Pose camera with a live outline overlay. Line up the pose, then take the shot".
- Tính năng nổi bật nhất nhóm về trải nghiệm chụp: kéo, phóng to, làm mờ, lật gương, khóa viền tư thế. "Chế độ hai điện thoại": một máy trên chân máy, một máy cầm tay làm remote, ghép bằng mã ngắn trong cùng mạng. Bộ sưu tập tư thế lập trước chuyến đi, vuốt đổi tư thế ngay trong khung ngắm. Tư thế là ảnh thật, không phải hình que. Pro mở hướng dẫn viết cho tư thế Pro và bỏ quảng cáo.
- Câu định vị: "Not a pose library. A pose camera." và "Pinterest already gave you the photo ideas".
- Ảnh store: 7 ảnh nền tím than, chữ trắng và tím: "Your Personal Pose Guide", "Follow Poses Like a Pro", "Your Couple's Pose Guide", "Smart & Easy to User" (lỗi chính tả), "Your Squad's Picture-Perfect", "More Poses More Possibilities", "Share Your Perfect Memories". Ảnh store không nói tới chế độ hai điện thoại.

### 2.9. Từ khóa trong tiêu đề và mô tả ngắn

| Từ khóa | Số app dùng trong tiêu đề (trên 8) |
|---|---|
| Pose / Posing | 8 |
| Guide / Guides | 7 |
| Camera | 7 |
| Photo / Photos | 3 |
| Ideas / Idea | 3 |
| Aesthetic | 1 |
| AI | 1 |

Từ khóa hay gặp trong mô tả ngắn: selfie, travel, couple, aesthetic, outline, overlay, pose ideas, pose guide. Bản tiếng Việt của mô tả ngắn dùng "ý tưởng tạo dáng", "hướng dẫn tạo dáng", "tư thế selfie" (trang Play `hl=vi`).

## 3. Chủ đề đánh giá của người dùng

Dữ liệu: các đánh giá mới nhất lấy từ Play theo ngôn ngữ en, vi, id, pt, es cho 8 app. Tổng khoảng 700 đánh giá. App 2.1 chiếm 504 đánh giá tiếng Anh: 366 đánh giá 1 sao, 101 đánh giá 5 sao. Trong 504 đánh giá đó, 293 nhắc tới quảng cáo, 286 trong số này là 1 sao. Nguồn: trang đánh giá trên Play của từng app (mục Nguồn).

Lưu ý phân bố ngôn ngữ của đánh giá app 2.1: tiếng Anh 504 (nhiều câu Hinglish, vài câu tiếng Bengal và Ả Rập), tiếng Tây Ban Nha 37, tiếng Indonesia 24, tiếng Việt 13, tiếng Bồ Đào Nha 4.

### 3.1. Phàn nàn (1 đến 3 sao)

1. **Quảng cáo quá dày, chiếm áp đảo.** Gặp ở cả 6 app có quảng cáo và nhiều đánh giá.
   - "5 ads before even reaching the main UI of the app." (app 2.1)
   - "Three ads before I even got past the splash screen, then literally every single tap triggers another ad." (app 2.1)
   - "Một list quảng cáo đập vào mặt từ lúc vào app đến lúc chụp được cái ảnh, cái khung chụp ảnh cũng bị quảng cáo đè nữa chứ." (app 2.1)
   - "Opening the app for the 1st time, 2-3 ads. selecting the language, 4 ads, swiping through each slide of instructions, ads..." (app 2.1). Câu này mô tả đúng luồng mở app mẫu: splash, chọn ngôn ngữ, giới thiệu, mỗi bước một quảng cáo toàn màn hình.
   - "It was very difficult to find the actual app buttons because they were mixed with ad buttons." (app 2.1)
   - "the pose only have 2 or one . and u have to pay" (app 2.2)
2. **Quá ít tư thế, chất lượng tư thế kém.** Nhiều người đếm được "6 poses".
   - "Too many ads and only 6 poses available." (app 2.1)
   - "the poses look like a child drew them." (app 2.1)
   - "very very limited pose so much ad" (app 2.3)
   - "solo male poses are very bad... good poses are only for couples and the group pictures!" (app 2.5)
3. **Không giống ảnh store, cảm giác lừa đảo.** Từ "fake", "scam", "clickbait" xuất hiện trong 51 đánh giá của app 2.1.
   - "the actual features look nothing like the screenshots shown in the store. Total clickbait" (app 2.1)
   - "pose ternyata cuma gitu gitu doang... bukan ngarahin malah cuma foto ditempel dikamera kita" (app 2.2: tư thế chỉ là ảnh dán lên camera, không hướng dẫn thật).
4. **Chậm, tải lâu, cần mạng, tốn pin và dữ liệu.** 42 đánh giá của app 2.1 nhắc "load", "crash", "stuck".
   - "it's not offline you cant use it offline" (app 2.1)
   - "loadingnya lama padahal sinyal bagus" (app 2.3)
   - "descarga el teléfono rapidísimo" (app 2.1)
5. **Giá thuê bao cao so với giá trị.**
   - "who thought 8 euros a week for an app like this is justified? chatgpt plus costs less" (app 2.2)
6. **Khó dùng phần camera.**
   - "outline is too thin to see well, but at least it has a countdown" (app 2.4)
   - "its good but hard to get in the poses" (app 2.5)
   - "camera quality is very poor" (app 2.1)
   - "Even we cannot directly integrate our Phone Camera with the App." (app 2.1)
7. **Thiếu ngôn ngữ địa phương.**
   - "me hubiera gustado que tuviera la traducción en español" (app 2.5)

### 3.2. Điều người dùng khen

- Ý tưởng đúng nhu cầu: "Excellent idea for people who don't know how to pose" (app 2.2). "it help me take a good look picture because I'm a girl who sometimes didn't know how to pose" (app 2.1).
- Kết quả thật khi app ít quảng cáo: "I learnt 2 poses and took photos which came out amazing... it's definitely a game changer for me" (app 2.5).
- Có ích cho mạng xã hội: một người dùng Ấn Độ nói nhờ tư thế mà video Instagram có nhiều lượt xem (app 2.1).
- Hẹn giờ đếm ngược được khen (app 2.4).
- Phần lớn đánh giá 5 sao rất ngắn ("good", "nice", "wow"), thường ở bản 1.0.0 và tập trung từ Ấn Độ, Bangladesh. Nên coi điểm cao ở Ấn Độ, Thái Lan là tín hiệu yếu.

## 4. Đối thủ khác

### 4.1. Android

| App (package) | Nhà phát triển | Lượt cài (số chính xác) | Điểm (số đánh giá, Mỹ) | Phát hành | Mô hình | Điểm đáng chú ý |
|---|---|---|---|---|---|---|
| Ulike (`com.gorgeous.liteinternational`) | Bytedance | 50,000,000+ | 4,7 ở VN (716.744) | lâu năm | Miễn phí, có trả phí | Camera selfie có hướng dẫn tạo dáng; bài Thế Giới Di Động xếp đầu danh sách app tạo dáng |
| Easy Pose (`com.madcat.easyposer`) | MadcatGames | 10,000,000+ (21,748,960) | 3,5 (97.377) | 2018 | Ads + IAP $2.99-$12.99 | Mô hình 3D cho người vẽ, không phải camera |
| Photo pose for men (`com.mens.photo.poses`) | Deep Developers | 1,000,000+ (3,332,040) | 4,15 (30.464) | 2019 | Ads + IAP | Ngách tư thế cho nam |
| Photo Ideas for Photoshoot (`org.chlabs.photoideas`) | Champignon Labs | 500,000+ (922,668) | 4,09 (2.746) | 2020 | IAP $0.99-$24.99 | Thư viện ý tưởng kiểu blogger |
| Unscripted Photography Posing (`com.unscripted.posing.app`) | Unscripted | 500,000+ (764,872) | 4,29 (1.935) | 2019 | IAP $14.99-$499.99 | Cho thợ ảnh chuyên nghiệp, có hợp đồng, gallery |
| Posica (`pl.pose.posica`) | Sunrise & Sunset Times | 500,000+ (644,313) | 4,41 (1.721) | 2020 | IAP $3.99-$24.99 | Tư thế + preset Lightroom + giờ vàng |
| Pose me - photo assistant (`com.cantalk.photopose`) | CanTalk | 100,000+ (136,019) | 2,67 (168) | 2019 | IAP $2.99-$6.49 | Xuất hiện trong bài top app tạo dáng ở VN |
| One More Please: Pose Camera (`com.getposely.app`) | GetPosely | 50,000+ (61,042) | Chưa có | 21/08/2026 | Chưa ghi | Lên 61 nghìn cài trong 2 tháng; ý tưởng tư thế, camera có hướng dẫn, trình chỉnh sửa |
| Fucasy: Photo Pose Guide (`com.fucasy.photoposeguide`) | Fucasy Studio | 10,000+ (11,494) | Chưa có | 28/06/2026 | Ads + IAP | Cùng nhóm overlay |
| poby — Pose Guide Camera (`com.poseby.poby`) | 좋은 하루 (Hàn Quốc) | 10,000+ (11,724) | Chưa có | 20/05/2026 | IAP $0.99-$3.99, không ads | Biến ảnh bất kỳ thành khung tư thế để chụp lại đúng bố cục |
| TwinCam (`com.twincam.app`) | Foodabi | 1,000+ | Chưa có | 30/07/2026 | IAP | Chép tư thế từ ảnh Instagram, Pinterest |
| PoseSnap: AI Pose Camera (`com.samofy.posesnap`) | Samofy Limited | 500+ | Chưa có | 28/08/2026 | IAP | Chép tư thế từ ảnh, AI coach, chụp rảnh tay |
| Posme AI: Pose Director (`com.rulead.posme`) | Rulead | 100+ | Chưa có | 13/07/2026 | IAP | AI vẽ tư thế lên camera và chỉnh người dùng vào tư thế |
| Nhóm "AI Pose Changer" (`com.hd.posechanger.aipose`, `com.vw.posechangerai`, `com.betterpose.rochapps`) | nhiều | 5,000+ đến 10,000+ | Chưa có | 2025-2026 | Ads + IAP | Dùng AI sinh ảnh đổi tư thế thay vì hướng dẫn chụp |

Nguồn: trang Play của từng app (mục Nguồn).

### 4.2. iOS

| App | Nhà phát triển | Số đánh giá (Mỹ) | Điểm | Phát hành | Điểm đáng chú ý |
|---|---|---|---|---|---|
| UNSCRIPTED Photography Poses | OMJ Holdings | 6.447 | 4,64 | 2019 | Hơn 40.000 ý tưởng tư thế, công cụ cho thợ ảnh |
| Easy Pose - Best Posing App | Park Kiyoung | 1.117 | 4,02 | 2018 | Mô hình 3D |
| Posa Photography Posing Ideas | Imperia Labs | 365 | 4,50 | 2023 | Thư viện tư thế |
| Superpose: AI Pose Camera | Superpose Labs | 292 | 4,80 | 18/05/2026 | AI sinh 4 gợi ý tư thế từ ảnh chụp cảnh thật, có "match pose" |
| Pose Genius: Poses in Camera | FKR, Inc. | 235 | 4,64 | 23/02/2026 | Gợi ý tư thế theo bối cảnh, chấm độ khớp, sửa góc máy |
| Posica | Pawel Zarzycki | 227 | 4,54 | 2020 | Bản iOS của Posica |
| Photogenik: Pose & Photo Coach | Photogenik Inc. | 199 | 4,68 | 17/06/2026 | Coach tư thế |

Nguồn: iTunes Search/Lookup API và trang App Store (mục Nguồn).

Superpose đáng chú ý nhất. Hai nhà sáng lập từng làm ở TikTok. App có hơn 22.000 lượt tải và tạo hơn 190.000 tư thế. Họ gọi được 2,2 triệu USD từ Khosla Ventures, Meitu và OVTR VC. Gói miễn phí 5 lần tạo mỗi ngày, gói 5 lần giá $2.99, gói 20 lần giá $9.99 (https://www.aichatdaily.com/ai-tools/ex-tiktok-duo-launch-superpose-ai-pose-coach-iphone).

Các app iOS mới khác cùng hướng camera coach: Kadra (khung silhouette, tự chụp khi khung đúng), PhotoMentor, EZ Pose, PoseSnap (https://www.appgoblin.info/apps/6791921111, https://apps.apple.com/app/id6756925109, https://mwm.ai/apps/ez-pose/6785361046, https://mwm.ai/apps/posesnap-ai-pose-camera/6768783594).

## 5. Nhận định thị trường

### 5.1. Người dùng

- Người dùng chính là nữ trẻ, chụp selfie, OOTD, du lịch, cà phê, cặp đôi. Ảnh store của mọi app dùng người mẫu nữ châu Á, phong cách Hàn, "aesthetic".
- Nhóm thứ hai là cặp đôi và nhóm bạn. App nào cũng có danh mục Couple, Friends, Family.
- Nam giới bị bỏ quên. Có phàn nàn trực tiếp (app 2.5). Ngách "Photo pose for men" đạt hơn 3,3 triệu lượt cài (https://play.google.com/store/apps/details?id=com.mens.photo.poses&hl=en).
- Nhóm thợ ảnh chuyên nghiệp là phân khúc khác, trả tiền cao (Unscripted bán IAP tới $499.99). Không nên trộn với app đại chúng.

### 5.2. Quốc gia

- Ấn Độ là nguồn đánh giá lớn nhất (tiếng Anh, Hinglish). Đánh giá tại đây dễ dãi hơn: app 2.1 được 4,1 ở Ấn Độ nhưng 1,9 ở Mexico.
- Mỹ Latinh (Mexico, Brazil), Indonesia, Việt Nam, Philippines, Thái Lan, Bangladesh có đánh giá đáng kể. Người dùng Mexico và Việt Nam khắt khe nhất với quảng cáo.
- Đây là dấu hiệu các app mẫu chạy quảng cáo mua lượt cài ở thị trường giá rẻ, kiếm tiền bằng quảng cáo. Kết luận này suy từ ngôn ngữ đánh giá, chưa có số liệu theo quốc gia từ SensorTower hay AppMagic.

### 5.3. Từ khóa ASO

- Lõi: "pose camera", "pose guide", "photo pose ideas", "posing app", "pose ideas".
- Bổ trợ: "selfie poses", "couple poses", "aesthetic", "outline", "overlay", "photo ideas", "AI pose", "pose coach".
- Tiếng Việt: "tạo dáng chụp ảnh", "ý tưởng tạo dáng", "hướng dẫn tạo dáng". Tìm kiếm "tạo dáng chụp ảnh" trên Play Mỹ chủ yếu trả về app chỉnh ảnh lớn (B612, SNOW, Ulike), chỉ một app pose (vector123). Từ khóa tiếng Việt còn ít cạnh tranh trực tiếp.
- Cạnh tranh tiêu đề "Pose Camera" rất dày: ít nhất 6 app trong danh sách và kết quả tìm kiếm dùng nguyên cụm này.

### 5.4. Xu hướng

1. **Từ thư viện sang camera.** Thế hệ cũ (Posica, Photo Ideas, Unscripted) là thư viện ảnh. Thế hệ 2026 đặt viền tư thế lên camera. Gần như mọi app mới đều có overlay.
2. **AI chấm khớp tư thế.** AI Posing quảng cáo skeleton tracking và điểm khớp. Pose Genius, Posme, Kadra làm coach thời gian thực. Chưa app Android nào làm tốt và có đánh giá tốt.
3. **AI tạo tư thế theo bối cảnh.** Superpose, Posely AI, SnapPose chụp cảnh thật rồi sinh tư thế phù hợp. Đây là hướng được gọi vốn.
4. **Chép tư thế từ ảnh bất kỳ.** poby, TwinCam, PoseSnap cho dán ảnh Pinterest, Instagram, TikTok làm khung. Đây là cách bắt trend TikTok mà không cần tự làm nội dung.
5. **AI đổi tư thế trong ảnh** (pose changer) là ngách khác, gần app sinh ảnh AI. Không phải đối thủ trực tiếp của camera.
6. **Clone hàng loạt.** Nhiều studio dùng chung khung app, tên gần giống nhau, ảnh store tím neon. Lượt cài lớn nhưng điểm thấp và vòng đời ngắn (app 2.4 không cập nhật từ khi phát hành).

### 5.5. Khoảng trống cho app mới

1. **Ít quảng cáo, mở app là tới camera.** Phàn nàn số một áp đảo. Không quảng cáo xen kẽ ở splash, chọn ngôn ngữ, giới thiệu. Không quảng cáo trên khung camera. Đây là điểm khác biệt rẻ nhất và rõ nhất.
2. **Thư viện đủ lớn ngay bản đầu.** Người dùng đếm và chê "6 poses". Cần hàng trăm tư thế miễn phí, chia rõ theo Solo nữ, Solo nam, Couple, Nhóm, Gia đình, Trẻ em.
3. **Ảnh store trung thực.** Ảnh chụp đúng màn hình thật. Giảm đánh giá "clickbait".
4. **Viền tư thế dễ nhìn và chỉnh được.** Nét dày, đổi màu, chỉnh độ mờ, kéo, phóng to, gương, khóa. Kèm hẹn giờ, lưới. Simple Pose và vector123 đã có, nhưng lượt cài nhỏ.
5. **Chạy offline, nhẹ.** Có phàn nàn về tải lâu và không dùng được khi mất mạng. Gói tư thế cơ bản đặt sẵn trong app, phần mở rộng tải về và giữ trên máy.
6. **Tư thế cho nam.** Ngách có nhu cầu đã chứng minh, ít app pose camera làm tốt.
7. **Bản địa hóa.** Tiếng Việt, Tây Ban Nha, Bồ Đào Nha, Indonesia, Hindi. Có phàn nàn thiếu tiếng Tây Ban Nha.
8. **Bước AI sau.** Khi bản cơ bản ổn định: chấm khớp tư thế bằng nhận diện khung xương trên máy, hoặc tạo khung tư thế từ ảnh người dùng chọn (kiểu poby). Giá nên là mua một lần hoặc thuê bao rẻ. Mức 8 euro mỗi tuần đã bị chê.

## 6. Nguồn

Trang Google Play của 8 app mẫu (thêm `&hl=vi` cho bản tiếng Việt, `&gl=<mã nước>` cho điểm theo quốc gia):

- https://play.google.com/store/apps/details?id=com.camera.filter.pose.guide.ideas&hl=en
- https://play.google.com/store/apps/details?id=com.aiposing.pose&hl=en
- https://play.google.com/store/apps/details?id=com.camera.filter.pose.guide.ideas.photo&hl=en
- https://play.google.com/store/apps/details?id=com.camera.guide.ideas.photo.pose&hl=en
- https://play.google.com/store/apps/details?id=com.vector123.posecamera&hl=en
- https://play.google.com/store/apps/details?id=com.posecamera.poseideas&hl=en
- https://play.google.com/store/apps/details?id=com.gws.posemate&hl=en
- https://play.google.com/store/apps/details?id=com.cam.simplepose.pose.camera.photo.guide&hl=en

Đánh giá: lấy từ phần đánh giá công khai của các trang trên (endpoint `batchexecute` của Play, sắp xếp mới nhất, theo ngôn ngữ en, vi, id, pt, es).

Trang nhà phát triển:

- https://play.google.com/store/apps/developer?id=Tokii+Apps
- https://play.google.com/store/apps/developer?id=Ocean+Limousine+Limited
- https://play.google.com/store/apps/developer?id=Smart+Experiences
- https://play.google.com/store/apps/developer?id=Alphasoftx+Limited
- https://play.google.com/store/apps/developer?id=Trandyapp

Đối thủ Android:

- https://play.google.com/store/apps/details?id=com.gorgeous.liteinternational&hl=en&gl=vn
- https://play.google.com/store/apps/details?id=com.madcat.easyposer&hl=en
- https://play.google.com/store/apps/details?id=com.mens.photo.poses&hl=en
- https://play.google.com/store/apps/details?id=org.chlabs.photoideas&hl=en
- https://play.google.com/store/apps/details?id=com.unscripted.posing.app&hl=en
- https://play.google.com/store/apps/details?id=pl.pose.posica&hl=en
- https://play.google.com/store/apps/details?id=com.cantalk.photopose&hl=en
- https://play.google.com/store/apps/details?id=com.getposely.app&hl=en
- https://play.google.com/store/apps/details?id=com.fucasy.photoposeguide&hl=en
- https://play.google.com/store/apps/details?id=com.poseby.poby&hl=en
- https://play.google.com/store/apps/details?id=com.twincam.app&hl=en
- https://play.google.com/store/apps/details?id=com.samofy.posesnap&hl=en
- https://play.google.com/store/apps/details?id=com.rulead.posme&hl=en
- https://play.google.com/store/apps/details?id=com.hd.posechanger.aipose&hl=en
- https://play.google.com/store/apps/details?id=com.vw.posechangerai&hl=en
- https://play.google.com/store/apps/details?id=com.betterpose.rochapps&hl=en
- https://play.google.com/store/apps/details?id=in.pose.ly.ai.pose&hl=en
- https://play.google.com/store/apps/details?id=com.smartapps.poseai&hl=en

Đối thủ iOS:

- https://apps.apple.com/us/app/unscripted-photography-poses/id1438843099
- https://apps.apple.com/us/app/easy-pose-best-posing-app/id1335872207
- https://apps.apple.com/us/app/posa-photography-posing-ideas/id6447232755
- https://apps.apple.com/us/app/superpose-ai-pose-camera/id6759357866
- https://apps.apple.com/us/app/pose-genius-poses-in-camera/id6755207657
- https://apps.apple.com/us/app/posica-pose-lightroom-preset/id1544270571
- https://apps.apple.com/us/app/photogenik-pose-photo-coach/id6779983641
- iTunes Search API: https://itunes.apple.com/search?term=pose+camera&entity=software&country=us

Bài viết:

- Superpose: https://www.aichatdaily.com/ai-tools/ex-tiktok-duo-launch-superpose-ai-pose-coach-iphone
- Kadra: https://www.appgoblin.info/apps/6791921111
- PhotoMentor: https://apps.apple.com/app/id6756925109
- EZ Pose: https://mwm.ai/apps/ez-pose/6785361046
- PoseSnap iOS: https://mwm.ai/apps/posesnap-ai-pose-camera/6768783594
- App cho thợ ảnh: https://unscriptedphotographers.com/blog/best-posing-apps-for-photographers
- Top app tạo dáng ở Việt Nam: https://www.thegioididong.com/game-app/top-6-app-tao-dang-chup-anh-dep-chuyen-nghiep-nhat-tren-1340085
- AppBrain (bị chặn, không lấy được số liệu): https://www.appbrain.com/app/com.camera.filter.pose.guide.ideas
