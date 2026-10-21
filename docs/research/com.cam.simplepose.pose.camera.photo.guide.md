# SimplePose (com.cam.simplepose.pose.camera.photo.guide)

## 1. Thông tin

- Tên hiển thị: SimplePose (splash ghi "Simple Pose"). Nhà phát triển: Braly (Đà Nẵng). Code nằm trong gói `com.bralydn.simplepose.android`, một `MainActivity` với Navigation Fragment.
- Package: `com.cam.simplepose.pose.camera.photo.guide`. Phiên bản 1.0.1 (build 7), minSdk 28, targetSdk 36.
- Ngày khảo sát: 2026-10-21. Máy Xiaomi 23021RAAEG, Android 15. App tự theo ngôn ngữ máy nên chạy bằng **tiếng Việt**.
- **Hai phiên.** Phiên 1 (ảnh `01` đến `38`): máy không có mạng (không Wi-Fi, không dữ liệu di động), nên không thấy quảng cáo, catalog chỉ có 24 pose đóng gói. **Phiên 2 (cùng ngày, 07:46 đến 08:01, ảnh `39` đến `53`): máy có Wi-Fi**, khảo sát lại lần mở đầu sau `pm clear`, quảng cáo, catalog tải từ mạng, paywall. Các mục ghi "(có mạng)" là quan sát ở phiên 2.
- Ảnh: 53 ảnh trong `captures/com.cam.simplepose.pose.camera.photo.guide/` (`01` đến `53`), cộng hai ảnh thử app đã lưu, kéo về `device/SimplePose_1792542821258.jpg` (phiên 1, ảnh tường) và `device/SimplePose_1792543932694.jpg` (phiên 2).

## 2. Lần mở đầu

Sau `pm clear`, máy không có mạng:

1. **Splash** khoảng 3 đến 5 giây. Nền tối có ảnh, logo vuông bo góc 156dp, chữ "Simple Pose", "ĐANG TẢI DỮ LIỆU LẦN ĐẦU TIÊN…", phần trăm, thanh tiến độ, dòng "Hành động này có thể chứa quảng cáo." Ảnh `01-splash`. Không có app open hay interstitial (không có mạng).
2. **Không có màn chọn ngôn ngữ** ở lần mở đầu (app theo ngôn ngữ máy). Không có hộp thoại đồng ý quảng cáo.
3. **Giới thiệu 3 trang** (ViewPager, vuốt được), nút "Bỏ qua" trên phải, nút back từ trang 2, chấm chỉ trang, nút tím to ở đáy:
   - Trang 1 "Tìm pose theo danh mục" / "Pose được nhóm theo danh mục, giúp bạn tìm đúng phong cách muốn chụp và mang thẳng vào máy ảnh." Minh họa: ảnh người thật lồng nét viền tím phát sáng. Ảnh `02-onboarding-1`.
   - Trang 2 "Màu sắc cho biết bạn đã gần đúng chưa" / "Máy ảnh hiện pose dưới dạng nét viền — không bao giờ là ảnh mẫu. Xanh lá nghĩa là khớp, vàng nghĩa là gần đúng, đỏ nghĩa là chưa đúng." Minh họa ba khung viền đỏ, vàng, xanh. Ảnh `03-onboarding-2`.
   - Trang 3 "Lên kế hoạch buổi chụp" / "Lưu pose vào bộ sưu tập trước khi ra ngoài, sau đó mở máy ảnh và chụp lần lượt từng pose." Nút "Bắt đầu xem". Ảnh `04-onboarding-3`.
   - Không mạng: không có quảng cáo native. Có mạng: trang 3 có native ở đáy, trang 2 thì không (xem phần dưới).
4. Vào **Khám phá** (home). Ảnh `05-home`.
5. **Quyền camera** chỉ xin khi mở camera lần đầu: trước tiên là màn giải thích của app ("Quyền truy cập máy ảnh" / "SimplePose cần máy ảnh để hiện nét pose trên khung ngắm. Không có gì được tải lên — mọi thứ ở lại trên máy này.", nút "Cho phép truy cập máy ảnh" và "Để sau"), rồi hộp thoại hệ thống. Chọn "Trong khi dùng ứng dụng". Ảnh `13-open-camera`, `14-camera-permission`. Không xin quyền thông báo lúc mở đầu.
6. **Mở lại nguội:** splash "ĐANG TẢI…" khoảng 3 giây rồi vào thẳng Khám phá. Không lặp giới thiệu.

Tổng: 2 đến 3 chạm tới home (không mạng). Nhẹ hơn hẳn guide.ideas; gần PoseCamera (vector123).

**Lần mở đầu khi có mạng (phiên 2, sau `pm clear`):**

1. **Splash** "ĐANG CHUẨN BỊ ỨNG DỤNG…" với phần trăm (ảnh `39-splash-online`). Khoảng **13 giây** sau khi mở thì hiện **interstitial** toàn màn hình (AdMob, `AdActivity`; quảng cáo TikTok dạng web, nút "Đóng" và "Cài đặt" ở trên). Nút "Đóng" đã có trong cây giao diện ở lần kiểm tra đầu tiên sau khi quảng cáo tải xong (dưới 6 giây). Ảnh `40-splash-interstitial`.
2. **Không có hộp thoại đồng ý quảng cáo (UMP)** ở máy này (Việt Nam, ngoài EEA). Không có màn chọn ngôn ngữ.
3. **Onboarding 3 trang** như khi không mạng. Trang 1 và 2 không có quảng cáo. **Trang 3 có native** ở đáy (393x196dp từ y 676: ảnh 213x120, logo, nhãn "Ad", tiêu đề, mô tả, nút CTA "MỞ" rộng 377x50); nội dung trang co lại, nút "Bắt đầu xem" lên y 612. Ảnh `41-onboarding-native`.
4. "Bắt đầu xem" > **Khám phá có hai quảng cáo cùng lúc**: native thu gọn được (`collapsible_native`) bung 393x301dp giữa lưới pose (ảnh 393x200, nút thu gọn ⌄ 42dp góc trên phải, tiêu đề, nút "Mở" xanh 361x48) và **dải native nhỏ** 377x72dp **dưới thanh điều hướng** (icon 52, tiêu đề, giá, nhãn Ad, nút "Mở"). Ảnh `42-home-collapsible-native`. Thu gọn thì native giữa biến mất, chỉ còn dải dưới đáy (ảnh `43-home-collapsed-native`).
5. **Mở lại nguội** (có mạng): splash "ĐANG TẢI…" khoảng 5 giây rồi **interstitial** (quảng cáo video web, nút "Close ✓" đã sẵn sàng khi chụp lúc 12 giây), đóng xong vào thẳng Khám phá. Ảnh `53-reopen-ad`.

Tổng khi có mạng: 1 interstitial + 1 native trước khi tới home; tới home thêm 2 native. Vẫn nhẹ hơn guide.ideas (không có chọn ngôn ngữ, không native toàn màn hình).

## 3. Bản đồ màn hình

| Màn hình | Mục đích | Vào từ đâu | Ảnh |
| --- | --- | --- | --- |
| Splash | Tải dữ liệu, chờ quảng cáo | Mở app | 01 |
| Onboarding (3 trang) | Giới thiệu | Lần mở đầu | 02, 03, 04 |
| Khám phá (home) | Chip danh mục, chip kệ (shelf), lưới 2 cột | Tab "Trang chủ" | 05, 06, 07, 08 |
| Chi tiết pose | Ảnh lớn, tên, loại, chip, yêu thích, thêm vào bộ, mở máy ảnh | Chạm thẻ pose | 09, 38 |
| Sheet "Thêm vào bộ sưu tập" | Chọn bộ hoặc tạo bộ mới | Chi tiết > "Thêm vào bộ sưu tập" | 10, 12 |
| Hộp thoại "Bộ sưu tập mới" | Đặt tên (mặc định ngày giờ) | Sheet trên, nút + ở tab Bộ sưu tập | 11 |
| Quyền máy ảnh (của app) | Giải thích trước khi xin quyền | Lần đầu mở camera | 13 |
| Camera | Nét viền pose trên khung ngắm, công cụ | Chi tiết > "Mở máy ảnh"; tab "Máy ảnh"; chọn nhiều > "Mở camera (n)"; bộ sưu tập | 15 đến 23, 28, 29 |
| Mirror: hiện mã QR | Máy chụp hiện QR để máy kia quét | Camera > nút điện thoại | 24 |
| Mirror: quét mã | Máy điều khiển quét QR | Khám phá > nút điện thoại | 36 |
| Mirror: nhập mã | Nhập 6 ký tự thay vì quét | Quét mã > "Gõ mã thay vì quét" | 37 |
| Tìm kiếm | Ô tìm, tag phổ biến, kết quả, lọc khung hình | Tab "Tìm" | 25, 26, 27 |
| Bộ sưu tập | Danh sách bộ (có bộ "Yêu thích" tự tạo) | Tab "Bộ sưu tập" | 30 |
| Chi tiết bộ | Danh sách pose, sắp xếp lại, chụp lần lượt | Chạm một bộ | 31 |
| Cài đặt | Pro, ngôn ngữ, thông báo, giao dịch, quyền, dữ liệu, giới thiệu | Tab "Cài đặt" | 32, 33 |
| Ngôn ngữ | 10 ngôn ngữ | Cài đặt > Ngôn ngữ | 34 |
| Thông báo | 2 công tắc loại nhắc | Cài đặt > Thông báo | 35 |
| Paywall | Gói Pro: 3 gói, dùng thử 3 ngày | Cài đặt > "Xem các gói" | 51 (có mạng) |

Sơ đồ luồng:

- Mở lần đầu > Splash > Onboarding 3 trang > Khám phá.
- Khám phá > chip danh mục > chip kệ > thẻ > Chi tiết pose > "Mở máy ảnh" > [màn giải thích quyền > quyền hệ thống] > Camera > nút chụp > ảnh lưu thẳng vào `Pictures/` (không có màn xem lại).
- Chi tiết pose > "Thêm vào bộ sưu tập" > sheet > "Bộ sưu tập mới" > "Tạo" > nút đổi thành "Đã lưu vào <tên bộ>".
- Bộ sưu tập > bộ > "Mở máy ảnh · bắt đầu từ pose 1" > Camera với dải pose của bộ.
- Khám phá hoặc Tìm > "Chọn" > chạm nhiều thẻ > "Mở camera (n)" > Camera với dải n pose, nút trước/sau.
- Khám phá > nút điện thoại > Mirror quét mã > "Gõ mã thay vì quét". Camera > nút điện thoại > Mirror hiện QR (cần Wi-Fi).
- Cài đặt > "Xem các gói" > Paywall > X về Cài đặt.

## 4. Tính năng

- **Thư viện pose.** Không mạng: 24 pose đóng gói. **Có mạng: catalog tải về ngay ở lần mở đầu, 282 pose** (số trên tiêu đề mục, chip kệ "Tất cả"):

  | Danh mục (chip) | Không mạng | Có mạng |
  | --- | --- | --- |
  | Xu hướng | 6 | 14 (chọn từ các nhóm dưới) |
  | Women | 12 | 159 |
  | Men | 12 | 41 |
  | Couples | 0 | 42 |
  | Besties | 0 | 14 |
  | Friends | 0 | 26 |
  | **Tổng** (không tính Xu hướng) | 24 | **282** |

  Khi không mạng, ba danh mục rỗng vẫn hiện, báo "Danh mục này chưa có pose nào." (ảnh `06-category-empty`). Có mạng thì cả sáu đều có pose (ảnh `44-friends-online`, `45-friends-bottom`). Loại dòng phụ mới: "Group" (Friends), ngoài "Solo". Lỗi nội dung: một thẻ Friends có tên thô **"no_props"** (khóa chưa dịch; ở Tìm kiếm hiện "No props"). Chi tiết pose Friends có chip mới **"Needs a second person"** (ảnh `46-detail-friends-online`).
- **Kệ lọc (shelf), hàng chip thứ hai:** không mạng có 9 chip (Tất cả, Street, Studio, Sitting, Standing, Dress, School uniform, Golden hour, Vintage), ảnh `08-filter-shelf`. Có mạng chỉ còn 4 chip ở mọi danh mục đã xem: Tất cả, Golden hour, Street, Vintage. Lọc trong danh mục đang chọn, tiêu đề đổi số ("Couples · 18" khi chọn Golden hour). Chip kệ đã chọn được giữ khi đổi danh mục.
- **Thẻ pose:** ảnh người thật 175x210dp bo góc, nút tim ở góc trên trái, tên pose là **kiểu khung hình** ("Full body", "Half body"), dòng phụ "Solo". Không có tên riêng cho từng pose, không có độ khó, không có mô tả.
- **Chi tiết pose:** ảnh lớn 361x420dp, tim trên phải, tên ("Full body"), loại ("Solo"), chip (danh mục, khung hình). Hai nút đáy: "Thêm vào bộ sưu tập" (xám) và "Mở máy ảnh" (tím). **Không có hướng dẫn chữ, bước, mẹo** ở bản miễn phí; thẻ Pro trong Cài đặt nói "Hướng dẫn viết cho mọi pose chi tiết", nhưng màn chi tiết không có chỗ khóa hay mời mua. Ảnh `09`, `38`.
- **Không thấy khóa pose nào, kể cả khi có mạng:** đã cuộn hết Friends (26) và một đoạn dài Women, không có biểu tượng khóa, nhãn Pro hay "Xem quảng cáo để hiện thêm %d pose". Paywall hứa "Chụp được mọi pose Pro" nhưng không thấy pose nào được đánh dấu Pro (ảnh `52-women-scrolled`). Không gặp giới hạn mở mỗi ngày.
- **Yêu thích:** chạm tim tạo bộ "Yêu thích" tự động trong tab Bộ sưu tập.
- **Bộ sưu tập** (tính năng "lên kế hoạch buổi chụp"): tạo bộ, tên mặc định là ngày giờ ("21/10/26 7:26"), menu ⋮ có "Đổi tên", "Xoá". Thẻ bộ có ô ghép 4 ảnh nhỏ 64dp, tên, "1 pose". Ghi chú "Bộ sưu tập chỉ lưu trên máy này. Cài lại app hoặc đổi máy sẽ mất." Chi tiết bộ: tiêu đề, "1 pose · lưu trên máy này", danh sách hàng (ảnh 56dp, tên, loại), nút "Sắp xếp lại pose", nút đáy "Mở máy ảnh · bắt đầu từ pose 1". Ảnh `30`, `31`.
- **Chọn nhiều:** nút "Chọn" ở Khám phá và Tìm. Chạm thẻ hiện dấu tích, nút đáy "Mở camera (2)". Camera nhận cả nhóm pose thành dải chọn. Ảnh `27`, `28`.
- **Tìm kiếm:** (có mạng) thêm hàng "Xu hướng" cuộn ngang thẻ 150dp dưới tag, và native thu gọn được bung giữa màn (ảnh `50-search-online-native`). Ô "Tìm pose", gợi ý "Chạm một tag để tìm ngay. Tìm trong mọi pose trên máy này.", 12 tag phổ biến: Street, Golden hour, Grass field, Studio, Standing, Sitting, Candid, Playful, Editorial, Vintage, Dress, Áo dài. Kết quả là lưới 2 cột, thêm hàng lọc khung hình (Tất cả, Full body, Half body). Ảnh `25`, `26`.
- **Lớp phủ trên camera:** nét viền trắng có quầng đen (`imageInk` + `imageInkHalo`), lấy từ ảnh nét viền riêng, **không phải ảnh mẫu mờ**. Ảnh `15`.
  - **Độ mờ:** thanh trượt 20 đến 100, mặc định 75.
  - **Kích thước:** 40 đến 160, mặc định 100. Ảnh `18`.
  - **Vị trí:** kéo một ngón trên nét viền để di chuyển (kéo chậm 1,5 giây thì được; kéo từ ngoài nét thì không). Ảnh `21-overlay-moved`.
  - **Khoá overlay:** nút trong bảng chỉnh, đổi thành "Đã khoá overlay" để chống lỡ kéo. Ảnh `19`.
  - **Lật gương** nét viền (ảnh `16`), **Ẩn/hiện** (biểu tượng mắt, ảnh `22`).
- **So khớp dáng người (màu xanh, vàng, đỏ):** onboarding hứa hẹn, nhưng trên camera nét viền luôn trắng, không có chỉ báo điểm khớp. APK không có ML Kit hay MediaPipe. Khung ngắm trong ảnh chụp màn hình là màu đen nên không có người để thử; kết luận: **chưa có so khớp**, chỉ có dữ liệu 33 điểm khớp trong `pose.json`.
- **Mirror Mode** (hai máy cùng Wi-Fi): máy chụp hiện QR ("Quét mã này bằng máy còn lại"); máy cầm tay quét QR hoặc nhập mã 6 ô. Không có Wi-Fi nên chỉ thấy lỗi "Máy này chưa vào mạng Wi-Fi nào nên không có địa chỉ để đưa vào mã." Ảnh `24`, `36`, `37`. Không có ở hai app trước.
- **Cài đặt:** thẻ Pro (biểu tượng vương miện, "Nâng cấp lên Pro", "Hướng dẫn viết cho mọi pose chi tiết. Không quảng cáo.", nút "Xem các gói"); CHUNG: Ngôn ngữ, Thông báo; GIAO DỊCH: Khôi phục giao dịch, Quản lý gói đăng ký; QUYỀN: Máy ảnh, Thư viện ảnh, Thông báo (trạng thái từng quyền), Mở Cài đặt hệ thống; DỮ LIỆU: Xoá dữ liệu trên máy; GIỚI THIỆU: Gửi phản hồi, Đánh giá SimplePose, Điều khoản & quyền riêng tư, Phiên bản "1.0.1 (7)". Ảnh `32`, `33`.
- **Ngôn ngữ:** 10 (English, Deutsch, Español, Français, Indonesia, 日本語, Nederlands, Polski, Português, Tiếng Việt). Màn này **lệch phong cách**: nền trời xanh nhạt, thẻ trắng có cờ, nút back cong và dấu tích xanh: khuôn "nhà máy" dùng lại, không theo bộ màu tối của app. Ảnh `34`.
- **Thông báo:** "NHỮNG GÌ CHÚNG TÔI CÓ THỂ GỬI": "Nhắc bộ sưu tập", "Nhắc quay lại" (công tắc), ghi chú tắt ở đây là ngừng gửi loại đó. Ảnh `35`.

## 5. Camera

Bố cục (ảnh `15-camera`, dp):

- Nền tối `#1c1a26` ngoài khung, khung ngắm 3:4 rộng toàn màn (393x524, từ y 58).
- Trên trái: nút đóng X 48dp (x 12, y 70).
- **Cột công cụ dọc bên phải** (x 333, mỗi nút 48dp, cách nhau khoảng 8): Flash (Tự động "A" > Bật > Tắt), Hẹn giờ (Tắt > 3s > 10s, có nhãn nhỏ), Lưới 3x3 (tím khi bật), Ẩn/hiện nét pose, Lật gương nét pose, Tinh chỉnh nét pose, Chế độ Mirror.
- **Zoom** dạng viên thuốc ở đáy khung: 0.5×, 1×, 2× (nút đang chọn tròn tím 36dp).
- Dưới khung: thanh "Đã chọn n pose" (viên thuốc 177x48 có biểu tượng ảnh, chạm không mở gì), dải pose cuộn ngang (ô 68x88 khi chọn có viền tím, ô khác 56x72), nút "Pose trước"/"Pose sau" hai bên.
- Đáy: nút chụp trắng 76dp ở giữa, nút đổi camera 48dp bên phải. **Không có ô ảnh vừa chụp, không có thư viện.**
- **Bảng tinh chỉnh** (nút thanh trượt): thẻ nổi bo góc khoảng 16dp, nền `#262433` trong mờ, đè lên nửa dưới khung: "Độ mờ" (thanh 337x48), "Kích thước", nút "Khoá overlay" 337x48. Thanh trượt kiểu chấm tròn tím. Ảnh `17`, `18`.
- Tab "Máy ảnh" mở camera **không có pose** (không có dải pose, không có nét viền). Ảnh `29`. Lưới bật ở lần trước vẫn giữ.
- **Sau khi chụp:** không có màn xem lại, không có thông báo nào thấy được. Ảnh lưu thẳng vào `/sdcard/Pictures/SimplePose_<timestamp>.jpg`, 1200x1600 (3:4), khoảng 360 KB. **Ảnh không có nét viền.** Ảnh `23-after-capture`.
- Ảnh chụp màn hình: khung ngắm ra màu đen, trong khi ảnh lưu có hình thật (tường). Có thể do camera dùng lớp bảo vệ hoặc SurfaceView không vào screencap.

## 6. Kiếm tiền

**Quảng cáo (quan sát khi có mạng, phiên 2).** Không mạng (phiên 1) thì không có quảng cáo nào. AdMob (`com.google.android.libraries.ads.mobile.sdk`), thư viện `com.bralydn.ads`. Quảng cáo thấy được đều là quảng cáo thật (TikTok, Gauth, Shopee, kính áp tròng, video YouTube).

| Loại | Vị trí | Thời điểm | Tần suất | Thời gian chờ đóng | Ảnh |
| --- | --- | --- | --- | --- | --- |
| Interstitial | Splash, lần mở đầu | Khoảng 13 giây sau khi mở | Mỗi lần mở nguội | Nút "Đóng" có ngay khi quảng cáo hiện (dưới 6 giây) | 40 |
| Interstitial | Splash, mở lại nguội | Khoảng 5 giây sau khi mở | Mỗi lần mở nguội | Nút "Close ✓" đã sẵn sàng lúc 12 giây | 53 |
| Interstitial | **Đóng camera** (nút X), cả từ chi tiết pose lẫn tab Máy ảnh | Ngay khi đóng camera | 3/3 lần thử (cách nhau hơn 1 phút) | "Đóng" có trong 4 đến 8 giây | 47, 49 |
| Native | Onboarding trang 3 (không có ở trang 1, 2) | Khi tới trang 3 | Lần mở đầu | Không đóng được | 41 |
| Native thu gọn (bung 393x301dp) | Giữa nội dung Khám phá, Tìm kiếm, Cài đặt; **chi tiết pose sau khi quay lại từ camera** | Khi vào màn | Mỗi lần vào màn; tự bung lại | Nút ⌄ thu gọn có ngay | 42, 48, 50 |
| Native dải nhỏ (377x72dp) | Dưới thanh điều hướng, mọi tab | Luôn có | Thường trực | Không đóng được | 43, 52 |
| Native (thu gọn, 393x137dp) | Đáy chi tiết pose, sau khi thu gọn quảng cáo ở trên | Sau camera | | | 48 |

Không thấy quảng cáo khi: đổi danh mục (6 lần), đổi chip kệ, mở chi tiết pose, **mở camera** (2 lần, kể cả lần đầu có xin quyền), chụp ảnh, back từ chi tiết về Khám phá (sau hơn 30 giây), đổi tab. Không thấy rewarded, không thấy app open riêng (quảng cáo ở splash là interstitial chạy trong `AdActivity`). Không có hộp thoại UMP (máy ở Việt Nam).

Tần suất interstitial: mỗi lần đóng camera đều có, cách nhau hơn 30 giây (khớp `interval: 30` trong APK). Một phiên ngắn mở app, xem pose, chụp 1 tấm, quay lại sẽ gặp 2 interstitial. Native chiếm gần một nửa màn hình khi bung: ở chi tiết pose, ảnh pose co từ 420 xuống 359dp, chip bị đẩy ra ngoài.

**Gói Pro (paywall, có mạng, ảnh `51-paywall`).** Vào từ Cài đặt > "Xem các gói". Màn toàn màn hình, nền ảnh người mẫu với vệt sáng tím xanh và lớp phủ tối, nút X trên trái (48dp, x 12, y 34).

- Tiêu đề "SimplePose" + nhãn viên thuốc tím "Pro"; phụ đề "Mở khoá pose Pro — chụp được chúng, kèm hướng dẫn viết sẵn."
- Quyền lợi (3 dòng, biểu tượng trong ô vuông bo góc): "Chụp được mọi pose Pro"; "Hướng dẫn viết sẵn: tay, trọng tâm và ánh sáng"; "Không quảng cáo, ở mọi nơi".
- Ba thẻ gói 361x68dp, nền `#262433`, nút radio bên trái:

  | Gói | Giá | Dùng thử |
  | --- | --- | --- |
  | Hàng tuần | 104.000 ₫ / 1 tuần | Không |
  | Hàng tháng | 208.000 ₫ / 1 tháng | Không |
  | **Hàng năm** (chọn sẵn, viền tím) | 520.000 ₫ / 1 năm | **Miễn phí 3 ngày** (nhãn tím "MIỄN PHÍ 3 NGÀY") |

- Nút chính tím `#6d5ef7` 361x48 "Dùng thử miễn phí 3 ngày"; dòng pháp lý "Miễn phí 3 ngày, sau đó 520.000 ₫ / 1 năm. Tự động gia hạn cho tới khi huỷ."; liên kết Điều khoản, Quyền riêng tư, Khôi phục.
- Quy đổi: gói tuần tương đương khoảng 450.000 ₫/tháng, gói tháng 208.000 ₫, gói năm khoảng 43.000 ₫/tháng (rẻ hơn gói tuần khoảng 90%). Bị khóa trên thực tế: không thấy pose nào bị khóa; khác biệt rõ nhất là bỏ quảng cáo. Chuỗi "Daily limit reached — %d unlocks per category" trong APK chưa gặp trên máy.

## 7. Giao diện

- **Theme tối**, một màu nhấn tím, phong cách hiện đại kiểu iOS (thẻ bo góc lớn, chip viên thuốc, nhóm cài đặt dạng thẻ). Bộ màu khớp APK `ds_*`.
- Màu (lấy mẫu):
  - Nền màn `#15141c` đến `#17161f`; nền ngoài khung camera và thanh điều hướng đáy `#1c1a26`; bề mặt thẻ, chip chưa chọn, nút phụ `#262433`.
  - **Màu nhấn tím `#6d5ef7`**: chip danh mục đang chọn, nút chính ("Tiếp tục", "Mở máy ảnh", "Xem các gói"), zoom đang chọn, viền chip kệ đang chọn, chữ "Chọn", "Bỏ qua". Biểu tượng tab đang chọn `#8b7ef9`.
  - Chữ chính `#f5f5f7`, chữ phụ xám `#9a9aa6` (APK).
  - Camera: nét viền trắng, quầng đen; nút chụp trắng có vòng trắng.
  - Màn Ngôn ngữ: nền trời xanh nhạt, thẻ trắng (lệch bộ màu).
- Chữ: Be Vietnam Pro (APK). Tiêu đề màn lớn khoảng 28 đến 30sp đậm ("Khám phá"); tiêu đề onboarding khoảng 30sp đậm, thân 17sp xám, căn giữa; chip danh mục 18sp đậm; tên thẻ 16sp đậm; dòng phụ 13sp xám; nhãn tab 12sp.
- Biểu tượng nét mảnh (outline), tím khi chọn.

Số đo chính (dp):

- **Thanh trên Khám phá:** tiêu đề x 16, y 40, cao 35; nút Mirror 48x48 (x 271), nút "Chọn" 58x48 (x 319). Hàng tiêu đề 48 cao từ y 34.
- **Chip danh mục:** hàng y 98, cao 42, rộng 75 đến 119, bo tròn hoàn toàn, cách nhau 12. **Chip kệ:** y 160, cao 38, viền 1dp, chọn thì viền tím.
- **Tiêu đề mục** "Xu hướng · 6": y 237, cao 21.
- **Lưới pose:** 2 cột, lề 16, khe 11; thẻ 175 rộng, ảnh 175x210 bo góc khoảng 16, tim 48 (vòng tròn đen trong mờ 36) ở góc trên trái, tên cách ảnh 8, dòng phụ dưới; mỗi hàng 279 (gồm khoảng cách 20).
- **Thanh điều hướng đáy:** 393x86 từ y 787, nền `#1c1a26`, 5 mục 75x54 (Trang chủ, Tìm, Máy ảnh, Bộ sưu tập, Cài đặt), biểu tượng 24 trên chữ 12sp.
- **Onboarding:** "Bỏ qua" 80x48 (x 297, y 38); ảnh minh họa 345x303 (x 24, y 110) bo góc 16; tiêu đề y 437; chấm chỉ trang y 765 (chấm chọn là viên dài tím); nút chính 345x48 (x 24, y 809) bo tròn.
- **Chi tiết pose:** thanh trên 56 (back 48 tại x 4, tiêu đề x 60); ảnh 361x420 (x 16, y 98) bo góc 16; tên 28 cao y 534; chip 38 cao y 600; hai nút đáy 174x48 và 175x48 tại y 779, cách 12.
- **Sheet thêm vào bộ:** tiêu đề giữa 28 cao, nút "Bộ sưu tập mới" 189x48 (chữ tím), hàng bộ 393x64 (ảnh 48, tên 20, số pose 18, dấu tích 24).
- **Hộp thoại:** rộng 333, tiêu đề 28, ô nhập 333x52, nút "Huỷ" 88x48 và "Tạo" 120x48 (tím) căn phải.
- **Bộ sưu tập:** tiêu đề 28 tại y 34; thẻ 361x88, bo góc khoảng 16, ô ghép 64 (4 ô 31), tên x 104; FAB "+" 56x56 (x 321, y 715).
- **Cài đặt:** thẻ Pro 361x khoảng 180 (x 16, y 122), nút 329x48; nhãn nhóm chữ hoa 30 cao; hàng 361x60, bo góc khoảng 12, cách 6, giá trị xám bên phải và mũi tên.
- **Camera:** như mục 5.

## 8. Điểm mạnh, điểm yếu

Điểm mạnh:

- Giao diện tối đẹp, đồng nhất (trừ màn Ngôn ngữ), chữ Việt đầy đủ, vào app nhanh.
- Ảnh mẫu người thật chất lượng cao, nét viền riêng vẽ kỹ có quầng đen, nhìn rõ trên mọi nền.
- Lớp phủ chỉnh đủ: độ mờ, cỡ, kéo vị trí, khóa, lật, ẩn. Zoom 0.5/1/2×, flash, hẹn giờ, lưới.
- Quy trình "kế hoạch chụp" rõ: bộ sưu tập, chọn nhiều, dải pose trên camera chuyển trước/sau.
- Mirror Mode hai máy (hiếm trong nhóm app này).
- Màn giải thích quyền trước hộp thoại hệ thống, cài đặt thông báo theo loại, trạng thái quyền.

Điểm yếu:

- Không mạng chỉ có 24 pose, 3/6 danh mục rỗng nhưng vẫn hiện. Có mạng thì 282 pose nhưng lệch: Women 159, Besties 14. Một pose tên thô "no_props".
- Quảng cáo dày khi có mạng: interstitial ở splash và **mỗi lần đóng camera**, native bung gần nửa màn ở Khám phá, Tìm, Cài đặt, chi tiết pose, cộng dải native thường trực dưới thanh điều hướng.
- Pose không có tên riêng ("Full body" lặp lại), không mô tả, không hướng dẫn ở bản miễn phí.
- Onboarding hứa so màu xanh/vàng/đỏ nhưng camera không có so khớp.
- Không có màn xem lại sau khi chụp, không có thông báo lưu, không có thư viện trong app. Ảnh chỉ 1200x1600 và không gộp nét viền.
- Thanh "Đã chọn n pose" trông như nút nhưng chạm không làm gì.
- Màn Ngôn ngữ lệch phong cách.

## 9. Chưa vào được và sự cố

- **Paywall:** phiên 1 bị hệ thống quyền từ chối; **phiên 2 đã vào được** (ảnh `51`). Không bấm nút dùng thử, "Khôi phục", "Khôi phục giao dịch", "Quản lý gói đăng ký".
- **Quảng cáo, catalog từ mạng:** đã khảo sát ở phiên 2. UMP không xuất hiện (máy ngoài EEA), nên không biết hộp thoại đồng ý trông thế nào.
- **Pose Pro, giới hạn mở mỗi ngày, rewarded:** không gặp ở phiên 2 dù có mạng.
- **Mirror Mode thực tế:** cần Wi-Fi và máy thứ hai.
- **So khớp dáng:** không có người trong khung, khung ngắm chụp ra đen; dựa vào APK để kết luận là chưa có.
- **Bỏ qua theo quy tắc:** Gửi phản hồi, Đánh giá SimplePose, Điều khoản & quyền riêng tư (mở trình duyệt), Xoá dữ liệu trên máy, Mở Cài đặt hệ thống.
- **Camera trước:** đã bật rồi tắt, không chụp.

Sự cố:

1. Bộ chặn từ chối `tapid buttonPrimary` ("Tiếp tục" ở onboarding) vì coi id `buttonPrimary` là nút mua (báo nhầm). Đã vuốt sang trang và dùng "Bỏ qua" ở trang cuối. Hệ quả: không bấm được "Bắt đầu xem"; có thể hai nút dẫn tới cùng chỗ.
2. Không mở nhầm quảng cáo hay Play Store. `tools/incidents.log` không có dòng mới.
3. Ảnh thử đã lưu trên máy (cần dọn): `/sdcard/Pictures/SimplePose_1792542821258.jpg` (phiên 1) và `/sdcard/Pictures/SimplePose_1792543932694.jpg` (phiên 2). Bản sao trong `captures/com.cam.simplepose.pose.camera.photo.guide/device/`.
5. Phiên 2: không chạm nhầm quảng cáo, không rời app, `tools/incidents.log` không có dòng mới. Dữ liệu phiên 1 (bộ sưu tập) đã mất do `pm clear`.
4. Dữ liệu app còn lại trên máy: một bộ sưu tập "21/10/26 7:26" và bộ "Yêu thích", mỗi bộ 1 pose; lưới camera đã tắt lại; kích thước và độ mờ đã trả về mặc định.

## 10. Khác với hai app trước

- **So với Pose Camera (guide.ideas):** không có chuỗi quảng cáo dày ở mở đầu (ít nhất khi không mạng), không lặp ngôn ngữ và giới thiệu mỗi lần mở; ảnh mẫu người thật thay vì nét vẽ đơn; có danh mục, tìm kiếm, bộ sưu tập, chọn nhiều, Mirror Mode; nét viền chỉnh được độ mờ, cỡ, vị trí, khóa. Không mạng thì ít pose hơn (24 so với 32), có mạng thì nhiều hơn hẳn (282) và không có thư viện ảnh hay màn chi tiết ảnh.
- **So với PoseCamera (vector123):** theme tối tím thay vì sáng kem; ảnh người thật thay vì ma-nơ-canh; lớp phủ là nét viền riêng (không phải ảnh mờ), kéo được vị trí, có khóa, có zoom và flash; bộ sưu tập thay cho Plan, có chọn nhiều để chụp lần lượt. Nội dung: có mạng 282 pose (nhiều hơn 120 của vector123) nhưng không tên, không bước, không mẹo (vector123 có 200 mẹo). Cùng điểm: không có xem lại có ý nghĩa sau khi chụp, ảnh lưu thẳng vào `Pictures/` không kèm nét vẽ. Khác về tiền: SimplePose dùng quảng cáo + thuê bao, vector123 mua một lần, không quảng cáo.
