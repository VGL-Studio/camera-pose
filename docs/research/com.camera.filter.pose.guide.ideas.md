# Pose Camera (com.camera.filter.pose.guide.ideas)

## 1. Thông tin

- Tên hiển thị: Pose Camera. Tên trên chính sách: "Pose Camera: Photo Ideas Guide". Nhà phát triển: tokiiapps (trang chính sách `tokiiapps.top`).
- Package: `com.camera.filter.pose.guide.ideas`. Phiên bản 1.0.2 (versionCode 102), minSdk 32, targetSdk 36.
- Ngày khảo sát: 2026-10-19. Máy Xiaomi 23021RAAEG, Android 15.
- App nhỏ, viết bằng View (XML, ViewBinding), CameraX. 9 màn hình: Splash, Language, Intro, Main (home), Camera, Setting, Gallery, Detail, cùng 3 hộp thoại (Info, Delete, Rating). Thư viện quảng cáo `com.cyber.ads` bọc AdMob (có mediation Mintegral). Không có gói trả phí.
- Ảnh: 42 ảnh trong `captures/com.camera.filter.pose.guide.ideas/`. Ảnh `01-launch` còn từ lần trước (trước `pm clear`). Ảnh camera trước (có người trong phòng) đã xóa, không lưu.

## 2. Lần mở đầu

Sau `pm clear`, theo thứ tự:

1. **Splash** (`SplashActivity`), khoảng 9 đến 10 giây. Nền tối, logo giữa, chữ "Loading, Please wait", thanh tiến độ và dòng "This action can contain ads". Ảnh `02-splash`.
2. **Interstitial splash** (AdMob, `AdActivity`) hiện khoảng 9 giây sau khi mở. Nút "Đóng" nằm trong quảng cáo, nút back cũng đóng được. Ảnh `03-first-interstitial`.
3. **Native toàn màn hình** ngay sau khi đóng interstitial: ảnh hoặc video lớn, nút CTA vàng to ở dưới, nút X nhỏ 26dp ở góc trên phải (330,36dp). Ảnh `04-splash-native-fullscreen`.
4. **Chọn ngôn ngữ** (`LanguageActivity`). Hai quảng cáo native: một nhỏ 58dp dưới tiêu đề, một lớn 337dp chiếm nửa dưới màn hình. Không chọn sẵn ngôn ngữ nào. Nút Next (dấu tích) chỉ hiện ở góc trên phải sau khi chọn. 12 ngôn ngữ: हिंदी, Español, Français, English, عربي, বাংলা, Русский, Português, Bahasa Indonesia, Deutsch, Italiano, 한국어. Không có tiếng Việt. Chọn xong, quảng cáo native lớn tải lại. Ảnh `05-language`, `06-language-selected`.
5. Bấm Next: **interstitial** (nút "ĐÓNG" ở dưới trái), rồi lại **native toàn màn hình**. Ảnh `07-language-interstitial`, `08-native-fullscreen`.
6. **Giới thiệu** (`IntroActivity`, ViewPager), 4 trang:
   - Trang 1 "Take Photo with Guide" / "Follow on-screen pose guide and take better photos". Ảnh `09-onboarding-1`.
   - Trang 2 "Get Pose Inspiration" / "Explore pose ideas that fit every style and moment". Native 184dp ở đáy, nút Next dời lên giữa bên phải. Ảnh `10-onboarding-2`.
   - Trang 3 là một **trang quảng cáo native toàn màn hình** chen giữa giới thiệu, nút "Next" chữ nhỏ ở góc trên phải. Ảnh `11-onboarding-ad-page`.
   - Trang 4 "Save and Share Photos" / "Save your best photos and share them with friends". Ảnh `12-onboarding-3`.
7. Bấm Next trang cuối: **interstitial** (thanh đếm ở trên, nút "Đóng" ở dưới giữa), rồi vào home. Ảnh `13-intro-interstitial`.
8. **Home** mở ra với một **banner thu gọn được** (collapsible, dạng native) đang bung to che nửa dưới. Ảnh `14-home-collapsible-ad`.
9. **Quyền camera** chỉ xin khi vào camera lần đầu (hộp thoại hệ thống). Chọn "Trong khi dùng ứng dụng". Ảnh `17-camera-permission`. Không xin quyền thông báo. Không thấy hộp thoại đồng ý quảng cáo (UMP/GDPR), có thể vì máy ở Việt Nam.

**Mỗi lần mở nguội sau đó vẫn lặp lại gần hết luồng này:** splash, interstitial khoảng 6 giây sau khi mở, native toàn màn hình, chọn ngôn ngữ (lại không chọn sẵn), interstitial, native toàn màn hình, giới thiệu 4 trang. Ảnh `40-relaunch-splash-ad`, `41-relaunch-language`. Người dùng phải qua khoảng 6 quảng cáo toàn màn hình mới tới home.

## 3. Bản đồ màn hình

| Màn hình | Mục đích | Vào từ đâu | Ảnh |
| --- | --- | --- | --- |
| Splash | Tải, chờ quảng cáo | Mở app | 02 |
| Language | Chọn ngôn ngữ | Sau splash (mỗi lần mở), Setting > Language | 05, 06, 27 |
| Intro | 3 trang giới thiệu + 1 trang quảng cáo | Sau Language (mỗi lần mở) | 09 đến 12 |
| Home (`MainActivity`) | Banner vào camera, thư viện, 6 ảnh gợi ý | Sau Intro | 15, 16 |
| Camera | Màn hình chính: camera kèm nét vẽ tư thế | Home: banner "Try Now" hoặc ảnh gợi ý | 19 đến 25, 28, 38, 42 |
| Setting | Âm chụp, ngôn ngữ, chia sẻ app, chính sách | Nút bánh răng trên camera | 26 |
| Policy | Trang chính sách trên web (`tokiiapps.top/policy.php`) | Setting > Policy, mở Chrome | (không chụp) |
| Gallery | Lưới ảnh đã chụp trong app | Home > Your Gallery | 36 |
| Detail ("Image") | Xem một ảnh, chia sẻ, xóa, thông tin | Ô ảnh nhỏ trên camera, ô trong Gallery | 30 |
| Hộp thoại Information | Tên, đường dẫn, ngày, dung lượng | Detail > (i) | 31 |
| Hộp thoại Delete | Xác nhận xóa | Detail > thùng rác | 32 |
| Hộp thoại Rating | Xin đánh giá 5 sao | Về home sau khi chụp | 34 |

Sơ đồ luồng:

- Splash > [Interstitial > Native toàn màn hình] > Language > [Interstitial > Native toàn màn hình] > Intro (4 trang) > [Interstitial] > Home.
- Home > banner "Try Now" > [Interstitial > Native toàn màn hình, không phải lần nào cũng có] > Camera (không chọn tư thế).
- Home > ảnh gợi ý > Camera (chọn sẵn tư thế tương ứng).
- Camera > nút chụp > ảnh lưu thẳng, ô ảnh nhỏ cập nhật > chạm ô ảnh > [Interstitial] > Detail > Share / Delete / Info.
- Camera > Back > [Interstitial > Native toàn màn hình] > Home > (lần đầu) hộp thoại Rating.
- Camera > bánh răng > Setting > Language / Share app / Policy (Chrome).
- Home > Your Gallery > [Interstitial > Native toàn màn hình] > Gallery > ảnh > [Interstitial] > Detail. Back khỏi Gallery > [Interstitial > Native toàn màn hình] > Home.

## 4. Tính năng

- **Gợi ý tư thế trên camera:** tính năng chính và gần như duy nhất. 6 nhóm, tổng 32 tư thế (đếm trên máy, khớp với mã `poseData` trong APK):

  | Nhóm | Số tư thế |
  | --- | --- |
  | Selfie | 9 |
  | Couple | 5 |
  | Friend | 5 |
  | Family | 4 |
  | Kid | 4 |
  | Sport | 5 |

- **Dạng hiển thị:** nét vẽ trắng đơn (line art) phủ lên khung camera, cỡ cố định, căn giữa. Nét đậm ở Selfie/Couple, nét mảnh có chi tiết bối cảnh ở Sport (ví dụ cả rổ bóng). Không kéo, không chỉnh cỡ, không chỉnh độ mờ, không lật được (kéo trên khung không làm nét vẽ di chuyển, ảnh `22-couple-pose-drag`). Không có khung xương, không nhận diện cơ thể, không chấm điểm khớp tư thế.
- Nét vẽ chỉ để hướng dẫn: **ảnh lưu ra không có nét vẽ** (ảnh `30-photo-detail`).
- Không có màn hình chi tiết tư thế. Không có mô tả hay mẹo tạo dáng.
- **Home "Pose Inspiration":** 6 ảnh thật (ảnh người mẫu, lưới 2 cột) cố định, không cuộn thêm. Chạm một ảnh mở camera với tư thế nét vẽ tương ứng đã chọn (ảnh 1 mở Selfie, tư thế 1).
- **Thư viện:** chỉ hiện ảnh chụp bằng app (thư mục `Pictures/PoseCamera`), lưới 3 cột. Detail có chia sẻ (bảng chia sẻ hệ thống, chưa thử theo quy tắc), xóa (có xác nhận), thông tin.
- **Cài đặt:** bật/tắt âm chụp (mặc định bật), đổi ngôn ngữ, chia sẻ app, chính sách (mở trình duyệt ngoài).
- Không có chỉnh sửa ảnh, bộ lọc, tìm kiếm, yêu thích, tải thêm tư thế, đăng nhập.

## 5. Camera

Bố cục từ trên xuống (ảnh `19-camera`):

- **Thanh trên** 60dp (y 24dp), nền đen, 5 nút mỗi nút 72dp rộng: Back, Flash, Timer, Grid, Settings.
  - **Flash:** bấm mở thanh con thay chỗ thanh trên ("< Flash" + 3 lựa chọn 48dp: tắt, bật, luôn bật/đèn pin). Ảnh `23-flash-options`.
  - **Timer:** thanh con "< Timer" với tắt, 3 giây, 10 giây; lựa chọn đang dùng có nền tròn `#2b2b2b`, biểu tượng vàng. Chọn xong tự đóng thanh con. Đếm ngược hiện số vàng lớn giữa khung. Ảnh `24-timer-options`, `42-timer-countdown`.
  - **Grid:** bật/tắt lưới 3x3 đường trắng mảnh, biểu tượng chuyển vàng khi bật. Ảnh `25-grid-on`.
  - **Settings:** mở màn hình Setting.
- **Khung camera** 393x465dp (y 84dp, tỷ lệ gần 3:4 hơi cắt). Không đổi được tỷ lệ khung.
- **Vùng điều khiển** nền đen, 265dp: ô ảnh vừa chụp 48dp tròn (x 54dp, xám khi chưa có ảnh), nút chụp 80dp trắng có viền trắng (giữa), nút đổi camera 48dp nền xám tối.
- **Hàng nhóm** dạng chip chữ cao 35dp, cuộn ngang: nhóm đang chọn nền `#2b2b2b` bo tròn hoàn toàn, chữ trắng; nhóm khác chữ xám không nền.
- **Hàng tư thế** ô 50x50dp nền `#2b2b2b` bo góc, cách 8dp; ô đầu là "không dùng tư thế" (vòng gạch chéo); ô đang chọn viền vàng `#e5b43f`.
- **Zoom:** chụm hai ngón (có `tvZoomRatio` hiện tạm thời theo mã); không có nút 1x/2x. Không thử được vì adb chỉ chạm một ngón.
- **Sau khi chụp:** có tiếng màn trập, ảnh lưu thẳng vào `Pictures/PoseCamera/yyyy-MM-dd-HH-mm-ss-SSS.jpg` (khoảng 520 KB), không có màn xem lại, ô ảnh nhỏ cập nhật. Chạm ô ảnh: interstitial rồi màn Detail. Ảnh `28-after-capture`, `29-detail-interstitial-video`, `30-photo-detail`.
- Mở camera từ banner thì không chọn tư thế nào; mở từ ảnh gợi ý thì chọn sẵn.

## 6. Kiếm tiền

Không có gói trả phí, không có màn hình trả phí (có thư viện billing trong APK nhưng không thấy dùng). Toàn bộ doanh thu từ quảng cáo, rất dày.

Các vị trí theo `RemoteConfig` trong APK: `ADS_SPLASH`, `NATIVE_LANGUAGE`, `NATIVE_SMALL_LANGUAGE`, `INTER_LANGUAGE`, `NATIVE_INTRO`, `NATIVE_INTRO_FULL`, `INTER_INTRO`, `INTER_HOME`, `INTER_BACK`, `INTER_VIEW_IMAGE`, `BANNER_HOME`, `BANNER_CAMERA`, `BANNER_GALLERY`, `NATIVE_CUSTOM_HOME`.

| Loại | Vị trí | Thời điểm | Tần suất | Chờ đóng |
| --- | --- | --- | --- | --- |
| Interstitial (`ADS_SPLASH`) | Toàn màn hình | Khoảng 6 đến 10 giây sau khi mở nguội | Mỗi lần mở nguội | Nút "Đóng" có ngay; back cũng đóng |
| Native toàn màn hình | Toàn màn hình, X 26dp góc trên phải | Ngay sau gần như mọi interstitial | Đi kèm interstitial | X có ngay |
| Native lớn + native nhỏ | Màn chọn ngôn ngữ | Mỗi lần mở | Tải lại khi chọn ngôn ngữ | Không đóng được |
| Interstitial (`INTER_LANGUAGE`) | Toàn màn hình | Bấm Next ở Language | Mỗi lần mở | "ĐÓNG" có ngay |
| Native (`NATIVE_INTRO`) | Đáy trang giới thiệu 2 | Mỗi lần mở | | |
| Native trang riêng (`NATIVE_INTRO_FULL`) | Trang thứ 3 của giới thiệu | Mỗi lần mở | | Bấm "Next" nhỏ góc trên |
| Interstitial (`INTER_INTRO`) | Toàn màn hình | Hết giới thiệu | Mỗi lần mở | "Đóng" có ngay |
| Native trong trang (`NATIVE_CUSTOM_HOME`) | Home, dưới banner, 345x184dp, có video | Luôn hiện | | |
| Banner thu gọn được (`BANNER_HOME/CAMERA/GALLERY`) | Đáy màn hình, 58dp khi thu, 347dp khi bung | Home, Camera, Gallery | **Tự bung lại khoảng 20 giây sau mỗi lần thu**, che hàng nhóm và hàng tư thế trên camera | Nút thu (mũi tên) 32dp |
| Interstitial (`INTER_HOME`) | Toàn màn hình | Vào camera từ banner, vào Gallery | Không phải lần nào cũng có (có giới hạn tần suất) | Nút Close/Đóng có sau 0 đến 6 giây |
| Interstitial (`INTER_BACK`) | Toàn màn hình | Back từ Camera hay Gallery về home | Thường xuyên | Close sau khoảng 4 giây |
| Interstitial (`INTER_VIEW_IMAGE`), có video | Toàn màn hình | Mở ảnh từ camera hoặc Gallery | Thường xuyên | Close sau khoảng 6 đến 10 giây |

Ghi chú:

- Một vài interstitial là quảng cáo cài app với nút "Đóng" nhỏ đặt ở góc trái trên hoặc ngay cạnh nút "Cài đặt".
- Native toàn màn hình xuất hiện sau interstitial có vùng ảnh phủ đúng chỗ nút Back của camera. Chạm vào chỗ đó mở Google Play (xem mục 9, sự cố).
- Hộp thoại xin đánh giá 5 sao (nút "RATE" xanh `#316ee5`, "Maybe Later") hiện khi về home sau lần chụp đầu. Ảnh `34-rating-dialog`.

## 7. Giao diện

- **Theme tối hoàn toàn.** Nền `#000000`. Bề mặt thẻ và chip `#2b2b2b`. Hàng "Your Gallery" `#1a1a1a`, nút tròn mũi tên `#353535`. Hộp thoại `#1f1f1f`, nút phụ trong hộp thoại `#454545`, nút xóa đỏ nhạt `#cf726a`.
- **Màu nhấn vàng mù tạt `#e5b43f`** (`#e7ba4d` ở giữa nút do chuyển sắc nhẹ): nút Next của giới thiệu, chấm chỉ trang, viền tư thế đang chọn, biểu tượng đang bật, công tắc, số đếm ngược. Nút CTA quảng cáo cũng vàng cùng tông, dễ nhầm với nút của app.
- Chữ: sans-serif hệ thống (Roboto). Tiêu đề màn hình khoảng 20sp trắng; chữ thân 16sp; chữ phụ xám.
- Biểu tượng: nét mảnh trắng, kiểu outline.

Số đo chính (dp):

- **Home:** khối "Welcome to / POSE GUIDE" ở x 12, y 24, cao 54 (chữ "to" nhỏ 12sp). Nội dung lề 24. Banner camera 345x172 (y 90) bo góc khoảng 24, ảnh minh họa tím có nút "Try Now" vàng. Native 345x184 (y 274), góc bo 16. Hàng "Your Gallery" 345x40, bo tròn hoàn toàn, biểu tượng ảnh trái, nút tròn mũi tên phải. Tiêu đề "Pose Inspiration" 16sp (y 523). Lưới gợi ý 2 cột, thẻ 162x173, khoảng cách 21 ngang, 20 dọc, bo góc khoảng 8, viền trắng mảnh. Banner đáy 58.
- **Language:** thanh trên 64 (tiêu đề "Language" x 16). Hàng ngôn ngữ 361x64, bo tròn hoàn toàn, nền `#2b2b2b`, cờ tròn 32 (x 32), chữ x 80, radio 48 bên phải viền vàng nhạt; khoảng cách 12.
- **Intro:** ảnh trên cùng 393x393, tiêu đề 20sp (y 401, x 16), mô tả xám 16sp (y 436). Chấm chỉ trang 44x7 (y 798). Nút "Next" 361x44 bo tròn hoàn toàn, vàng, chữ trắng đậm (y 812).
- **Camera:** như mục 5.
- **Setting:** thanh trên 64 (nút back 56x64, tiêu đề x 56). Hàng 345x72, lề 24, khoảng cách 12, bo góc khoảng 12, nền `#2b2b2b`; biểu tượng trong vòng tròn 40 nền `#575757`; chữ 20sp x 84; công tắc 34x18 hoặc mũi tên phải.
- **Gallery:** thanh trên 64, lưới 3 cột ô vuông 108, lề 16 (ô đầu ở x 22).
- **Detail:** thanh trên 64 ("Image"), ảnh rộng toàn màn 393x524 (y 72), thanh công cụ đáy 46 chia ba: chia sẻ, xóa, thông tin.
- **Hộp thoại:** rộng 320 (x 36), bo góc khoảng 16, tiêu đề 20sp giữa, hai nút 124x51 cạnh nhau hoặc một nút 272x51.

## 8. Điểm mạnh, điểm yếu

Điểm mạnh:

- Rất đơn giản: một màn camera chứa đủ mọi thứ, chọn nhóm và tư thế bằng một chạm.
- Nét vẽ line art đẹp, rõ trên nền camera; có tư thế nhiều người (đôi, bạn, gia đình) và thể thao.
- Ảnh lưu sạch, không dính nét vẽ hay hình mờ.
- Điều khiển camera cơ bản đủ: flash 3 chế độ, hẹn giờ 3/10 giây, lưới, đổi camera, zoom chụm.

Điểm yếu:

- Quảng cáo cực dày và có tính lừa: khoảng 6 quảng cáo toàn màn hình mỗi lần mở app; interstitial đi kèm native toàn màn hình gần như mọi lần chuyển màn; banner tự bung lại sau khoảng 20 giây và che hàng chọn tư thế; native phủ đúng chỗ nút Back; trang quảng cáo chen vào giới thiệu; nút CTA cùng màu vàng với nút của app.
- Chọn ngôn ngữ và giới thiệu lặp lại mỗi lần mở (không nhớ lựa chọn). Ngôn ngữ không chọn sẵn.
- Nội dung mỏng: 32 tư thế, 6 ảnh gợi ý, không cập nhật từ mạng.
- Nét vẽ không chỉnh được (vị trí, cỡ, độ mờ, lật), không có mẹo tạo dáng, không nhận diện cơ thể.
- Không có màn xem lại sau khi chụp; không đổi được tỷ lệ khung.
- Không có tiếng Việt.

## 9. Chưa vào được, sự cố

- **Share app, chia sẻ ảnh:** không thử theo quy tắc (không chia sẻ ra app khác).
- **Zoom chụm hai ngón:** adb không giả lập được hai ngón.
- **Hộp thoại đồng ý quảng cáo (UMP):** không xuất hiện.
- **Màn trả phí:** không có trong app.
- **Policy:** mở Chrome tới `tokiiapps.top/policy.php?appname=Pose%20Camera:%20Photo%20Ideas%20Guide...`, đã back ngay, không chụp.
- **Sự cố 1:** máy mất kết nối adb hai lần ngay sau `pm clear` và sau interstitial đầu (khoảng 30 giây đến 1 phút mỗi lần), tự kết nối lại. Đã `pm clear` lại để chạy từ đầu sạch.
- **Sự cố 2, chạm nhầm quảng cáo hai lần:** sau khi đóng interstitial lúc back khỏi camera, một native toàn màn hình hiện lên phủ chỗ nút Back; hai lần chạm vào tọa độ nút Back (132,148 px) rơi vào vùng ảnh của quảng cáo và mở trang cài app trong Google Play (Bigo Live). Đã back ngay cả hai lần, không cài gì. Nguyên nhân: chạm theo tọa độ cũ mà không xem lại cây giao diện ngay trước khi chạm.
- **Sự cố 3:** một lần chuỗi chạm Timer > 3 giây > back thanh con chạy lệch (thanh con tự đóng sau khi chọn), lệnh chạm thứ ba thành nút Back của camera và về home.
