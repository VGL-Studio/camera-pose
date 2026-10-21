---
package: com.gws.posemate
name: PoseMate
version: 1.0.8
surveyed_at: 2026-10-21
surveyed_on: máy thật Xiaomi 23021RAAEG, Android 15, 1080x2400, 440 dpi (2,75)
network: có mạng
status: draft
---

# APP MẪU: POSEMATE

## 1. Thông tin

- Tên trên Play: "PoseMate - Camera Pose Guide". Nhà phát triển: GoodwillSolution. 100+ lượt cài (301), chưa có điểm. Phát hành 14/09/2026, cập nhật 19/09/2026. Mua trong app $0.99 (báo cáo web).
- Package `com.gws.posemate`, bản 1.0.8. Compose, Hilt, Room, Coil 3, một `MainActivity`, CameraX, ML Kit Pose, chỉ AdMob, Play Billing (`premium_ad_free`), Play Asset Delivery (gói `poses`). Không dùng Remote Config. Không cùng mã nguồn với app nào khác trong bộ khảo sát (báo cáo APK).
- Khảo sát 2026-10-21, 08:39 đến 09:35, có Wi-Fi, máy ở Việt Nam. App chỉ có tiếng Anh. Ngày giờ trong app theo locale máy ("thg 10 21, 2026").
- 37 ảnh trong `captures/com.gws.posemate/` (`01` đến `37`).
- Ảnh chụp thử để lại trên máy (cần người dọn): `/sdcard/Pictures/PoseMate/pose_20261021_084858.jpg`, `pose_20261021_092836.jpg`, `pose_20261021_093145.jpg`.

## 2. Lần mở đầu

Sau `pm clear`:

1. **Không có splash riêng, không chọn ngôn ngữ.** Mở thẳng giới thiệu trang 1, phủ lên là **hộp thoại xin gửi số liệu**: "Help improve PoseMate" / "Share anonymous usage statistics and crash reports via Firebase. You can change this anytime in Settings → Privacy." Ô đánh dấu **chọn sẵn**, nút "Not now" và "Continue" (chữ xanh). Đã chọn "Continue". Ảnh `01`.
2. **Giới thiệu 3 trang** (Skip góc trên phải, chấm chỉ trang, nút "Next" to ở đáy):
   - Trang 1 "Find your look" / "1,600+ guided poses for every vibe — solo, couples, portraits and more." Biểu tượng lấp lánh trong vòng tròn bạc hà. Ảnh `02`.
   - Trang 2 "Shoot with a ghost" / "Line yourself up with the on-screen outline and tap once. Nailing the pose has never been easier." Có nút **"Enable camera"** ngay trong trang: bấm thì hiện hộp thoại quyền camera của hệ thống (ảnh `04`), cho phép xong nút đổi thành "Camera enabled" có dấu tích (ảnh `03`, `05`). Đây là cách xin quyền có giải thích, không ép.
   - Trang 3 "What do you shoot most?" / "Your library opens on this vibe first." 10 chip chọn sở thích: Beauty & Glamour, Bold & Sensuous, Celebration, Couple, Cozy & Seasonal, Family, Fitness, Food & Drink, Funny & Crazy, Group Shots. Chip chọn đổi nền xanh, chữ trắng. Nút "Start shooting". Ảnh `06`, `07`.
3. **Hộp thoại "Go Premium"** hiện ngay trên trang chủ (ảnh `08`, xem mục 6). Đóng bằng "Not now".
4. **Trang chủ Poses** (ảnh `09`).

Tổng: 6 chạm tới trang chủ (Continue, Next, Enable camera + cho phép, Next, chọn chip, Start shooting, Not now). **0 quảng cáo** ở luồng mở đầu. Không xin quyền thông báo.

- **Không có hộp thoại đồng ý quảng cáo (UMP).** Logcat cho thấy app có gọi UMP nhưng cấu hình AdMob thiếu: `W ConsentManager: Consent info update failed: Publisher misconfiguration: ... no form(s) configured for the input app ID ... ca-app-pub-<đã che>`. Máy ở Việt Nam nên dù có form cũng có thể không hiện.
- **Chọn "Couple" ở trang 3 không có tác dụng thấy được**: trang chủ mở ở chip "All", lưới bắt đầu bằng Group Shots (ảnh `09`).
- Chạy nền: ngay lúc mở, app tải gói asset `poses` từ Play (`AssetPackServiceImpl : startDownload([poses])`, xong lúc 08:40:19 `onNotifyModuleCompleted(poses...)`), khoảng 70 giây. Không có thanh tiến độ nào trên giao diện.
- **Mở lại nguội** (ảnh `37`): vào thẳng trang chủ, hộp thoại "Go Premium" lại hiện. Không có app open, không interstitial.

## 3. Bản đồ màn hình

| Màn hình | Mục đích | Vào từ đâu | Ảnh |
| --- | --- | --- | --- |
| Hộp thoại số liệu | Xin bật Firebase Analytics, Crashlytics | Lần mở đầu | 01 |
| Giới thiệu (3 trang) | Giới thiệu, xin quyền camera, chọn sở thích | Lần mở đầu | 02 đến 07 |
| Hộp thoại Go Premium | Bán gói gỡ quảng cáo | Mỗi lần mở nguội | 08, 37 |
| Poses (trang chủ) | Tìm kiếm, lưới tư thế, chip 27 nhóm, nút ngẫu nhiên | Tab "Poses" | 09 đến 12, 34, 36 |
| Chi tiết tư thế | Ảnh, mô tả, thẻ, hướng dẫn, thông số máy, lỗi hay gặp | Chạm thẻ; nút ngẫu nhiên | 13 đến 15, 33 |
| Camera | Nét viền phủ, điểm khớp, dải tư thế, zoom | "Shoot with this pose" | 16 đến 21 |
| Cài đặt camera | Hộp thoại các công tắc camera | Bánh răng trên camera | 17 |
| Sau khi chụp ("Nailed it!") | So sánh trước sau, chia sẻ, sửa, tư thế tiếp | Nút chụp | 22 |
| Sửa ảnh | Xoay, lật, cắt theo tỷ lệ | "Rotate & crop" | 35 |
| Chọn app xem ảnh (hệ thống) | Mở ảnh bằng app khác | Nút Gallery trên camera | 23 |
| Tips | 10 bài kỹ năng chụp, chip 4 nhóm | Tab "Tips" | 24 |
| Chi tiết bài | Why It Works, Steps, Use Cases | Chạm bài | 25 |
| Saved (My shooting list) | 3 tab: Saved, Tried, Tips; điểm chụp | Tab "Saved"; cúp trên thanh trên | 26, 27 |
| Photos (My photos) | Ảnh đã chụp, Favorites, Import | Tab "Photos" | 28, 29 |
| Settings | Premium, giao diện, camera, chung, giới thiệu, quyền riêng tư, pháp lý | Tab "Settings" | 30, 31 |

Luồng chính: `Poses > (chip nhóm) > chi tiết tư thế > Shoot with this pose > Camera > chụp > Nailed it! > Keep shooting | Next: <tư thế sau> | Share | Rotate & crop`. Ảnh tự lưu vào `Pictures/PoseMate/` và vào "Tried".

## 4. Tính năng

- **Trang chủ Poses** (ảnh `09`):
  - Thanh trên: logo app 40dp, ô tìm kiếm viên thuốc với chữ gợi ý chạy ngang ("Search Pose Mate for 1629+ pose ideas"), chip **điểm chụp** (cúp vàng + số tư thế đã thử).
  - Lưới 2 cột ảnh người thật nền studio trắng xám (ảnh AI). Ở chế độ "All", mỗi thẻ có nhãn nhóm góc dưới phải ("Group Shots", "Maternity"). Khi chọn một nhóm thì không có nhãn. Thẻ không có tên tư thế (tên chỉ ở mô tả trợ năng).
  - **Hàng chip nhóm nằm ở đáy**, ngay trên thanh điều hướng (khác mọi app trước): All + **27 nhóm**: Beauty & Glamour, Bold & Sensuous, Celebration, Couple, Cozy & Seasonal, Family, Fitness, Food & Drink, Funny & Crazy, Group Shots, Indoor, Kids & Play, Korean Style, Lifestyle & Routine, Maternity, Musicians, Nature, Newborn, Parents, Pets, Professional, Selfies, Seniors & School, Social Media, Streetwear & Urban, Travel, Weddings (ảnh `10`).
  - FAB **"Random Pose"** (56dp, xanh, biểu tượng xáo trộn) mở chi tiết một tư thế ngẫu nhiên trong nhóm đang chọn. Cuộn xuống thì có thêm nút tròn trắng "Scroll to top" (ảnh `12`).
  - **Tìm kiếm** lọc ngay khi gõ, nhưng **chỉ trong nhóm đang chọn**: gõ "beach" khi đang ở Weddings ra "No poses found / Try a different search or category"; chuyển sang All thì ra Surf and Clipboard Crew, Beach Leisure Group, Maternity Mother Beach I đến IV (ảnh `34`).
  - Không có quảng cáo trong lưới, không có tư thế bị khóa.
- **Nội dung tư thế**: tư thế đi theo **loạt 12 biến thể** đánh số La Mã (Wedding Bride Bouquet, ... XII; Wedding Bride Stairs, Wedding Bride Veil, Wedding Couple Dance, Wedding Couple Walk). Sắp xếp theo chữ nên "IX" đứng trước "V". Tên và mô tả **sinh theo khuôn**: "A romantic wedding reference pose showing the wedding couple dance iii stance..." trong khi ảnh là đôi ngồi trên ghế (ảnh `13`); "Wedding Couple Walk VIII" là ảnh đôi đang nhảy lên (ảnh `33`). Thẻ cũng giống hệt nhau giữa các tư thế (Weddings, Profile, Easy, Standing, Couple, Indoor, Studio). Nội dung nhiều nhưng không khớp ảnh.
- **Chi tiết tư thế** (ảnh `13` đến `15`), cuộn dọc:
  - Ảnh lớn bo góc, nút back và bookmark tròn mờ phủ trên ảnh, nhãn độ khó "Easy" góc dưới trái. Bookmark khi lưu đổi nền xanh (ảnh `33`).
  - Hai nút viền "Previous" / "Next" để duyệt tư thế liền kề trong nhóm.
  - Nhãn nhóm chữ hoa xanh, tên, mô tả, chip thẻ.
  - Thẻ "Pose guide" (một đoạn), "Steps" (6 bước đánh số), "Mobile settings" (Aperture f/1.8, Shutter 1/250s, ISO 100, White balance Auto, Focal length 26mm, Focus Auto), "DSLR settings" (f/2.8, 1/200s, ISO 200, Daylight, 50mm, Single AF), "Shooting tips" (Angle, Framing, Lens, Lighting), "Avoid these mistakes" (3 dòng, thẻ nền hồng nhạt, biểu tượng cảnh báo đỏ).
  - Nút chính dính đáy "Shoot with this pose" có biểu tượng camera.
  - Đây là bộ hướng dẫn dày nhất trong 5 app; riêng thông số Mobile và DSLR thì không app nào khác có.
- **Tips** (ảnh `24`, `25`): tiêu đề "LEARN / Photo skills / Exposure, composition & light", chip All, Exposure, Composition, Lighting, Mobile. Chỉ **10 bài**: Aperture-Priority Mode, Backlighting Technique, Fill Flash for Outdoor Portraits, Framing Your Subject, Golden Hour Lighting, Mastering Portrait Mode, Mobile Photography Tips, Rule of Thirds, The Exposure Triangle, Understanding Depth of Field. Thẻ có nhãn nhóm (viên thuốc bạc hà), tiêu đề, 2 dòng tóm tắt, nút mũi tên tròn. Chi tiết: "💡 Why It Works", "📋 Steps" (5 bước), "📷 Use Cases" (dùng emoji làm biểu tượng).
- **Saved / My shooting list** (ảnh `26`, `27`): 3 tab Saved (tư thế đã bookmark; trống thì "Nothing saved yet / Tap the bookmark on any pose to save it"), **Tried** (thẻ "POSE SCORE" gradient xanh: số tư thế đã thử, cúp; danh sách tư thế đã chụp với số ảnh mỗi tư thế), Tips (bài đã lưu). "Điểm" chỉ là số tư thế đã chụp ít nhất một lần, không liên quan tới độ khớp.
- **Photos / My photos** (ảnh `28`, `29`): "N photos captured", tab All Photos, Favorites, **Import** (nhập ảnh từ máy, chưa thử). Lưới ô vuông 111dp có tim. Xem ảnh: ảnh lớn, tên tư thế, ngày giờ, nút "Share before / after" và tim. Không thấy nút xóa.
- **Settings** (ảnh `30`, `31`): thẻ Premium, **Appearance** (System, Light, Dark, ba ô lớn có biểu tượng), Camera (Mirror Front Camera, Show Grid Lines, Show "Nailed it!" Popup), General (Auto Save Photos, bật sẵn), About (Rate Us, App Version 1.0.8, Check for Updates), Privacy (Share Analytics & Crash Reports, **Privacy Options** "Review ad consent choices (GDPR)"), Legal (Privacy Policy, Terms of Use). Không có chọn ngôn ngữ, không có More apps, Feedback.
- **Giao diện tối** đầy đủ (ảnh `32`).

## 5. Chức năng chính

Camera mở từ "Shoot with this pose" (ảnh `16` đến `21`). Không có tab camera riêng: luôn đi qua một tư thế.

- **Lớp phủ**: nét viền trắng (line art có quầng đen, giống bản vẽ phác) của đúng tư thế, phủ toàn khung. Không phủ ảnh mẫu. Ảnh nét viền có nền tối mờ hình chữ nhật, thấy rõ khi camera sau tối (ảnh `19`).
- **Thanh trên** (48dp mỗi nút): Back, Timer (bấm xoay vòng tắt, 3s, 5s, 10s; nút đổi nền vàng, chữ "Timer: 3s" vàng ở mép phải trên dải tư thế, ảnh `21`), Flash (chỉ khi camera sau), Camera Settings.
- **Hộp thoại Camera Settings** (ảnh `17`): Front Camera, Mirror Front Camera (bật sẵn, ảnh lưu không lật), Torch Mode, Show Grid Lines, Reference Images ("Browse category poses from the camera", bật sẵn: chính là dải tư thế), **Stencil Opacity** (thanh trượt vàng, mặc định 50%), **Move Stencil** (kéo, chụm để đổi cỡ nét viền), Click Sound, nút "Done" vàng to. Lỗi bố cục: công tắc "Reference Images" tràn ra ngoài mép phải hộp thoại, mô tả bị cắt.
- **Move Stencil** bật thì hiện dải hướng dẫn "Pinch to zoom, drag to move. Tap Lock when done." và nút Reset vàng (ảnh `18`, `19`). Kéo được nét viền. **Không thấy nút "Lock"** nào trên màn hình dù câu hướng dẫn nhắc tới.
- **Lưới ba phần** chạy xuyên cả vùng nút điều khiển ở đáy (ảnh `18`).
- **Nhận dạng dáng (ML Kit Pose)**: viên thuốc trên dải tư thế hiện **phần trăm khớp và lời nhắc**: "0% Step into frame" khi không có người; với camera trước chĩa lên trần nhà (không có người), ML Kit vẫn bắt nhầm và hiện "35% / 36% / 37% / 43% / 46% Come closer" (4 trên 12 lần đọc cây giao diện). Không có người trước camera nên chưa thấy mức cao, màu đổi theo mức, hay chụp tự động (xem mục 9). Nét viền không đổi màu.
- **Dải tư thế**: ảnh nhỏ 48x64dp các tư thế cùng nhóm, tư thế đang dùng có viền vàng `#e8a100`. Hai nút "Previous pose" / "Next pose" cạnh nút chụp.
- **Zoom**: 1x, 2x, 3x, 5x, 10x (chip tròn, đang chọn nền vàng).
- **Hàng đáy**: Gallery (mở bộ chọn app hệ thống để xem ảnh, ảnh `23`: thoát khỏi app, đã back), Previous pose, nút chụp trắng 72dp, Next pose, Switch Camera. Ở camera trước, các nút có nền tròn tối mờ (ảnh `20`).
- **Sau khi chụp** (ảnh `22`): bottom sheet có confetti: hai ảnh cạnh nhau "REFERENCE" và "MY SHOT", "**Nailed it! 🎉**" và tên tư thế. Nút: "Share before / after" (chính, xanh), "Rotate & crop", "Share photo", "Next: <tư thế sau>", "Keep shooting". Câu khen hiện **cả khi độ khớp 0%** (ảnh trần nhà): không gắn với kết quả nhận dạng.
- **Sửa ảnh** (ảnh `35`): "Edit photo", xoay trái phải 90°, lật ngang, góc xoay "0°", tỷ lệ cắt Original, 1:1, 4:5, 3:4, 16:9, "Save as new image", "Save this image".
- **Lưu**: Auto Save bật sẵn, ảnh 1200x1600 JPEG vào `Pictures/PoseMate/pose_<ngày>_<giờ>.jpg`. Tư thế đã chụp vào "Tried", điểm trên thanh trên tăng.

## 6. Kiếm tiền

**Quảng cáo rất nhẹ.** Chỉ thấy một banner trong cả phiên.

| Loại quảng cáo | Vị trí | Thời điểm | Tần suất | Thời gian chờ đóng | Ảnh |
| --- | --- | --- | --- | --- | --- |
| Banner 320x50 (`ad_iframe`, 321x51dp) | Trang chủ Poses, giữa hàng chip và thanh điều hướng, x 36, y 721 | Khi về trang chủ; nhiều lần không có quảng cáo để hiện | Chỉ trang chủ. Không có ở Tips, Saved, Photos, Settings, chi tiết, camera | Không đóng được | 36 |

- Quảng cáo thấy được: OKX (sàn tiền mã hóa), qua Moloco (`molocoads_view` trong cây giao diện), tức AdMob có nguồn cầu từ Moloco.
- Logcat: nhiều lần `I Ads : Ad failed to load : 3` (không có quảng cáo phù hợp) lúc 08:50 và 09:29, khi về trang chủ. App mới, ít lượt cài, tỷ lệ lấp đầy thấp.
- **Không thấy interstitial, rewarded, app open** trong cả phiên (3 lần chụp, nhiều lần vào ra camera, mở lại nguội). APK có interstitial và rewarded (báo cáo APK) nhưng chưa thấy dùng ở đâu.
- Không có hộp thoại UMP (lỗi cấu hình phía nhà phát triển, mục 2). Settings có mục "Privacy Options".

**Gói trả phí "Go Premium"**: mua một lần, gỡ quảng cáo.

- Hộp thoại (ảnh `08`): ngôi sao xanh, "Go Premium" / "Enjoy PoseMate without interruptions", "⊘ Remove all ads", "✓ One-time purchase, yours forever", nút "★ Buy on Google Play", "Not now". **Không ghi giá** trong app (giá $0.99 theo trang Play). Không dùng thử, không thuê bao.
- Hiện **mỗi lần mở nguội** (ảnh `08`, `37`) và có thẻ cố định đầu Settings ("Remove all ads forever").
- Không có gì bị khóa: toàn bộ 1.629 tư thế, camera, Tips, sửa ảnh đều miễn phí.
- Logcat lúc mở: `W BillingClient: Reconnection failed with result: 5` (lỗi kết nối billing, không ảnh hưởng thấy được).

## 7. Giao diện

- **Material 3**, nền sáng ngả xanh xám, một màu chính xanh lục ngọc, có chế độ tối. Bố cục rộng rãi, thẻ trắng bo góc lớn, chip viên thuốc, thanh điều hướng M3 có ô chỉ báo viên thuốc. Phông hệ thống (Roboto), không có phông riêng. Biểu tượng Material (filled khi chọn, outlined khi không).
- Màu sáng (lấy mẫu):
  - Nền `#f6f7f3`. Thẻ `#ffffff`.
  - **Màu chính `#0e7a5f`**: nút chính, chip đang chọn, ô chỉ báo tab, FAB, toggle bật, nhãn nhóm chữ hoa.
  - Nền chip chưa chọn, ô tìm kiếm, ô Light/Dark `#e4e8e0`; chữ chip `#5b6f64` (cũng là chữ phụ).
  - Bạc hà (primary container) `#c9f2de`: vòng tròn giới thiệu, thẻ Premium, nhãn nhóm của Tips, ô "System" đang chọn, ô số ảnh.
  - Thanh điều hướng `#e3efec`. Chữ chính `#14231c`.
  - Chip điểm: nền `#f4eacf`, cúp `#eec973`.
  - Nhấn phụ cam đất `#c46a3d` (hình khối trang giới thiệu 3).
  - Camera: vàng hổ phách `#e8a100` (zoom chọn, viền tư thế, timer, thanh trượt, nút Done).
  - Thẻ lỗi hay gặp: nền `#f7eeeb`, biểu tượng `#f06868`.
  - Thẻ Pose Score: gradient `#0e7a5f` sang `#0b5f4a`.
- Màu tối (ảnh `32`): nền `#0b120f`, ô tìm kiếm `#28332e`, thanh điều hướng `#1a2f27`, chip chọn và FAB đổi sang xanh bạc hà sáng.
- Chữ (ước lượng từ chiều cao dòng): tiêu đề màn hình ("Photo skills", "Settings") khoảng 26sp đậm, có nhãn nhỏ chữ hoa xanh 12sp phía trên ("LEARN", "COLLECTION", "GALLERY", "PREFERENCES") và dòng phụ 14sp dưới; tên tư thế ở chi tiết khoảng 26sp đậm; tiêu đề giới thiệu khoảng 24sp đậm, thân 16sp căn giữa; chữ nút 16sp đậm; chip 14sp; nhãn tab 12sp.

Số đo chính (dp):

- **Thanh trên Poses**: logo 40x40 (x 12, y 41), ô tìm kiếm 253x48 (x 60, y 36) bo tròn hoàn toàn, chip điểm 60x48 (x 321).
- **Lưới**: 2 cột, lề 12, khe 8; thẻ 180x220, bo góc khoảng 20; hàng cách nhau 228. Nhãn nhóm viên thuốc trắng góc dưới phải. FAB 56x56 (x 325, y 656); nút lên đầu 48 phía trên FAB.
- **Hàng chip nhóm**: y 728, vùng chạm cao 48 (nền nhìn thấy khoảng 40), rộng 49 đến 154, khe 8.
- **Banner**: 321x51 tại x 36, y 721 (đẩy hàng chip lên).
- **Thanh điều hướng**: 393x80 từ y 777, 5 mục 72x80 (Poses, Tips, Saved, Photos, Settings), ô chỉ báo viên thuốc khoảng 64x32 nền xanh, biểu tượng trắng khi chọn; nhãn 12sp.
- **Giới thiệu**: Skip 58x48 (x 311, y 34); vòng tròn minh họa khoảng 120; tiêu đề y 445 (trang 1); nút "Enable camera" 141x48 bo tròn; chấm chỉ trang y khoảng 752 (chấm chọn là viên dài xanh); nút chính 345x56 (x 24, y 777).
- **Hộp thoại** (số liệu, Premium): rộng 320, bo góc khoảng 28; nút "Buy on Google Play" 272x52.
- **Chi tiết tư thế**: ảnh 353x380 (x 20, y 34) bo góc khoảng 24; nút tròn 48 trên ảnh; Previous/Next 170x48 (y 426, khe 12) viền; nhãn nhóm y 490; tên y 508 cao 30; mô tả y 545; chip thẻ 48 cao (nền khoảng 36), hàng cách 56; thẻ nội dung 353 rộng, đệm 20; CTA 353x60 (x 20, y 785) dính đáy.
- **Camera**: 4 nút trên 48x48 tại y 46; viên điểm khớp y 611; dải tư thế y 645 cao 76 (ảnh 48x64 trong ô 60x76, khe 8); chip zoom 48 tại y 720; hàng đáy y 781: nút 48 và nút chụp 72x72 (x 160, y 769).
- **Hộp thoại Camera Settings**: 320x652 (x 36, y 119), hàng công tắc 52x48, nút Done 256x48.
- **Tips**: thẻ 353x125 (x 20), cách 8, nhãn viên thuốc, nút mũi tên tròn khoảng 48 bên phải.
- **Saved**: thẻ Pose Score 353 rộng khoảng 130 cao, bo góc khoảng 24; hàng tư thế ảnh 64x64.
- **Photos**: ô 111x111, 3 cột.
- **Settings**: thẻ nhóm trắng bo góc khoảng 24, nhãn nhóm chữ hoa xanh; ba ô giao diện khoảng 105x72.

## 8. Điểm mạnh, điểm yếu

Điểm mạnh:

- Luồng mở đầu sạch nhất bộ cùng PoseCamera (vector123): không quảng cáo, xin quyền camera có giải thích, hỏi đồng ý số liệu trước khi bật Firebase.
- Kho lớn nhất (1.629 tư thế, 27 nhóm), mỗi tư thế có hướng dẫn rất dày (bước, thông số Mobile và DSLR, góc máy, lỗi hay gặp).
- Camera đủ công cụ: độ mờ nét viền, kéo và đổi cỡ nét viền, lưới, hẹn giờ, đèn, zoom, đổi tư thế ngay trong khung, ML Kit cho phần trăm khớp và lời nhắc.
- Sau khi chụp có màn so sánh trước sau và nút chia sẻ ghép, sửa ảnh cơ bản, "Next" sang tư thế tiếp: vòng chụp liền mạch.
- Quảng cáo gần như không thấy; một banner nhỏ ở trang chủ. Gói gỡ quảng cáo một lần, không khóa nội dung.
- Kiến trúc và thiết kế M3 gần với dự án mẫu (Compose, M3, theme sáng tối).

Điểm yếu:

- Nội dung sinh theo khuôn: tên, mô tả, thẻ không khớp ảnh ("Dance", "Walk" cho ảnh ngồi, nhảy; thẻ "Standing" cho mọi tư thế). Thẻ ở lưới không có tên.
- Sở thích chọn ở giới thiệu bị bỏ qua.
- Tìm kiếm chỉ trong nhóm đang chọn, không báo cho người dùng.
- "Nailed it!" hiện cả khi khớp 0%; điểm chụp chỉ là số tư thế đã thử. ML Kit bắt nhầm trần nhà là người.
- Lỗi giao diện nhỏ: công tắc tràn hộp thoại; câu "Tap Lock" mà không có nút Lock; lưới đè lên nút điều khiển; công tắc lưới trong Settings không khớp với lựa chọn trong camera (bật trong camera, Settings vẫn tắt, ảnh `30`).
- Nút Gallery trên camera mở app khác thay vì tab Photos của chính app.
- Hộp thoại Premium mỗi lần mở nguội, không ghi giá.
- Hàng chip nhóm ở đáy với 27 mục phải vuốt ngang nhiều lần; không có màn tổng quan nhóm.

**So với bốn app đã khảo sát:**

- Khác guide.ideas và AI Posing (kiểu "nhà máy"): không có quảng cáo ở mở đầu, không native, không interstitial, không hỏi đánh giá, không màn ngôn ngữ.
- Khác SimplePose: SimplePose đổi màu nét viền xanh vàng đỏ theo độ khớp và có chế độ hai điện thoại; PoseMate hiện phần trăm và lời nhắc chữ, nét viền giữ màu trắng. SimplePose có gói tuần, tháng, năm; PoseMate một lần $0.99. PoseMate có giao diện sáng, SimplePose tối.
- Gần PoseCamera (vector123) nhất về nội dung (cùng khuôn bước, góc, khung, ống kính, lỗi hay gặp, có mục Tips) nhưng kho lớn gấp 13 lần (1.629 so với 120), ảnh thật thay vì ma-nơ-canh, và không giới hạn lượt chụp; vector123 không quảng cáo, khóa sau 5 lượt chụp.
- Chỉ PoseMate có: thông số máy Mobile/DSLR từng tư thế, so sánh trước sau sau khi chụp, sửa ảnh (xoay, cắt), dải tư thế và Previous/Next trong camera, timer 3/5/10s, zoom 10x, nút ngẫu nhiên, theme tối chọn tay, hộp thoại đồng ý số liệu, "Pose Score".

## 9. Chưa xem được

- **Nhận dạng dáng với người thật**: không có người trước camera. Chưa thấy phần trăm khớp cao, lời nhắc khác ngoài "Step into frame" và "Come closer", có đổi màu hay chụp tự động không. Cần một người đứng trước camera sau (hoặc camera trước) với một tư thế một người (Selfies), quan sát 1 đến 2 phút.
- Camera sau cho ảnh đen vì máy nằm trên bàn; ảnh camera trước là trần nhà.
- **Interstitial và rewarded**: có trong APK nhưng không gặp. Cần một phiên dài hơn (nhiều lần chụp, vào ra nhóm, đổi tab) và thử ở nước khác hoặc có VPN; cũng có thể chúng chưa được nối vào giao diện.
- **Hộp thoại UMP**: không hiện vì lỗi cấu hình của nhà phát triển (logcat), cũng như máy ở Việt Nam.
- Giá thật trong app: hộp thoại không ghi giá, không bấm "Buy on Google Play" theo quy tắc.
- Không thử: "Share before / after", "Share photo" (chia sẻ), Import ảnh, Rate Us, Check for Updates, Privacy Policy, Terms of Use, Privacy Options (chỉ ghi là có). Không mở nhóm Bold & Sensuous.
- Chưa đếm số tư thế từng nhóm trên giao diện (số liệu theo nhóm có trong báo cáo APK).

## 10. Sự cố trong lúc khảo sát

- Không có dòng mới trong `incidents.log` (file không được tạo trong phiên này). Không mở nhầm quảng cáo, Play hay app khác. App không văng.
- Nút Gallery trên camera mở bộ chọn app xem ảnh của hệ thống (ảnh `23`); đã back ngay, không chọn app nào.
- Lỡ gọi `ax.sh tapid ""` (chuỗi rỗng): công cụ không báo lỗi mà chạm vào phần tử đầu tiên (540, 1200) trên màn camera. Không gây hậu quả.
- Trên máy để lại 3 ảnh: `/sdcard/Pictures/PoseMate/pose_20261021_084858.jpg`, `pose_20261021_092836.jpg`, `pose_20261021_093145.jpg`. Cần người dọn (xóa thư mục `/sdcard/Pictures/PoseMate/`).
- App để ở theme "System" như ban đầu. Đã `stop` app và về màn hình chính.
