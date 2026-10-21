---
package: com.aiposing.pose
name: "AI Posing: Pose Ideas, Guide"
version: 1.4.0
surveyed_at: 2026-10-21
surveyed_on: máy thật Xiaomi 23021RAAEG, Android 15, 1080x2400, 440 dpi (2,75)
network: có mạng
status: draft
---

# APP MẪU: AI POSING: POSE IDEAS, GUIDE

## 1. Thông tin

- Tên trên Play: "AI Posing: Pose Ideas, Guide". Nhà phát triển: Alphasoftx Limited. 100,000+ lượt cài (103,754), 465 đánh giá, điểm 4,2 ở VN, Ấn Độ, Mexico (báo cáo web). Mua trong app $8.99 đến $39.99.
- Package `com.aiposing.pose`, bản 1.4.0, một `MainActivity`, Jetpack Compose. Nội dung tải từ API riêng `apiposephoto.aifastwork.com`. ML Kit Pose Detection (báo cáo APK).
- Khảo sát 2026-10-21, 08:03 đến 08:35, có Wi-Fi. App chạy bằng tiếng Việt (người khảo sát chọn ở màn ngôn ngữ).
- 50 ảnh trong `captures/com.aiposing.pose/` (`01` đến `50`). Không có ảnh nào lưu ra bộ nhớ chung của máy.
- **Sự cố mạng phía app:** từ khoảng 08:10 đến 08:21, API trả **HTTP 429** (logcat `PhotosPagingSource: HTTP 429`, `getAllCategory: HTTP 429`). Trong lúc đó danh mục và Khám phá trống hoặc quay vòng mãi, không có thông báo lỗi hay nút thử lại (ảnh `17`, `18`, `20`, `22`). Sau 08:21 nội dung tải bình thường (ảnh `40`).

## 2. Lần mở đầu

Sau `pm clear`, có mạng:

1. **Splash** (ảnh `01`): ảnh chụp ra nền xám, cây giao diện chỉ có chữ "Tạo dáng với AI" (APK có video splash). Khoảng 8 giây.
2. **Interstitial ở splash** (AdMob `AdActivity`, quảng cáo cài app Dola). Nút "Đóng" có ngay khi quảng cáo hiện. Ảnh `02`.
3. **Native toàn màn hình** ngay sau interstitial (ảnh nền 393x740dp, nút X 48dp góc trên phải, CTA xanh 361x48 ở đáy). Nút X không có trong danh sách nút đóng của công cụ, đóng bằng `adcorner tr`. Ảnh `03`.
4. **Chọn ngôn ngữ** (ảnh `04`, `05`): tiêu đề "Language", nút "Done" viên thuốc, thẻ ngôn ngữ 361x72dp có cờ emoji, có bàn tay động chỉ vào dòng đầu (Tiếng Việt, theo ngôn ngữ máy). Không chọn sẵn. Chọn xong giao diện đổi ngôn ngữ ngay ("Ngôn ngữ", "Xong"), thẻ chọn có viền xanh `#018150`. **Native 391x234dp ở đáy** (ảnh lớn, CTA "INSTALL").
5. **Giới thiệu 4 chấm** (nút "Tiếp theo", không có Bỏ qua), mỗi trang có native 234dp ở đáy:
   - Trang 1 "Hỗ trợ chụp ảnh live" / "Chế độ phủ giúp người dùng hình dung tư thế trực tiếp qua đường viền" (minh họa người nhảy có viền nét đứt xanh). Ảnh `06`.
   - Trang 2 "Thư viện cá nhân" / "Tự do lưu các tư thế yêu thích vào thư viện của bạn". Ảnh `07`.
   - Trang 3 là **native toàn màn hình** (một chấm của bộ chỉ trang). Ảnh `08`.
   - Trang 4 "Bắt trọn khoảnh khắc" / "Chụp một tấm selfie nhanh để bắt đầu phác họa chân dung của bạn." Ảnh `09`.
6. **Xin quyền thông báo** (hệ thống) ngay sau "Tiếp theo" trang cuối. Đã từ chối. Ảnh `10`.
7. **Màn trả phí** toàn màn hình (ảnh `11`, xem mục 6).
8. Đóng màn trả phí thì hiện ngay **hộp thoại "ĐÁNH GIÁ CHÚNG TÔI"**: 5 sao chọn sẵn ("Xuất sắc (5) Rất hài lòng"), 6 thẻ lý do khen ("Tư thế đẹp", "Giao diện thân thiện", "Gợi ý thông minh", "Tôi thực sự thích ứng dụng này", "Dùng hướng dẫn AI toàn diện", "Rất mượt mà"), nút "GỬI". Người dùng chưa dùng app đã bị hỏi đánh giá. Đóng bằng back. Ảnh `12`.
9. **Trang chủ** với native thu gọn được 393x308dp phủ nửa dưới, che cả thanh điều hướng. Ảnh `13`.

Tổng tới trang chủ: khoảng 10 chạm; 1 interstitial, 2 native toàn màn hình, 4 native đáy (ngôn ngữ, 3 trang giới thiệu), 1 màn trả phí, 1 hộp thoại đánh giá, 1 hộp thoại quyền. Không thấy hộp thoại đồng ý quảng cáo (UMP), máy ở Việt Nam.

Tới chức năng chính (camera có tư thế): Trang chủ hoặc Khám phá > chạm tư thế (thường có interstitial) > "Chụp ảnh" > quyền camera (chỉ xin lúc này, hộp thoại hệ thống, không có màn giải thích riêng) > camera. Thêm 3 chạm và thường 1 interstitial.

## 3. Bản đồ màn hình

| Màn hình | Mục đích | Vào từ đâu | Ảnh |
| --- | --- | --- | --- |
| Splash | Video, chờ quảng cáo | Mở app | 01, 02, 03 |
| Ngôn ngữ | Chọn ngôn ngữ | Lần mở đầu | 04, 05 |
| Giới thiệu (4 trang, 1 là quảng cáo) | Giới thiệu | Lần mở đầu | 06 đến 09 |
| Màn trả phí | Gói tuần, năm | Sau giới thiệu; Lưu ảnh lần đầu; tư thế có kim cương; nút vương miện | 11, 33, 48 |
| Hộp thoại đánh giá | Xin 5 sao | Sau màn trả phí lần đầu | 12 |
| Trang chủ | Banner xoay, tìm kiếm, "Xu hướng Hot", chip, các mục chủ đề | Tab "Trang chủ" | 13 đến 18 |
| Danh sách chủ đề | Lưới 2 cột tư thế của một chủ đề | Mũi tên hoặc tên mục ở Trang chủ | 20 |
| Khám phá | Lưới so le mọi tư thế, Bộ lọc, tìm kiếm | Tab "Khám phá" | 22, 40, 49 |
| Bộ lọc | 5 nhóm thẻ | Khám phá > "Bộ lọc" | 41 |
| Tìm kiếm | Ô tìm, kết quả lưới | Ô tìm ở Trang chủ, kính lúp ở Khám phá | (cây giao diện) |
| Chi tiết tư thế | Ảnh lớn, tên, tim, "Chụp ảnh" | Chạm thẻ tư thế | 27, 43 |
| Camera | Ảnh mẫu phủ mờ, AI Đề xuất, bộ lọc màu | "Chụp ảnh"; nút camera giữa thanh điều hướng | 28 đến 31, 44 đến 46 |
| Xem lại | "Chụp lại", "Lưu ảnh" | Nút chụp | 32 |
| Tùy chọn xuất | Chia sẻ, định dạng, độ phân giải | "Lưu ảnh" | 35, 36 |
| Bộ sưu tập: Ảnh đã tạo | Dòng thời gian theo tháng | Tab "Bộ sưu tập" | 23, 38 |
| Album tháng | Ảnh đã lưu theo ngày | Chạm một tháng | 24, 39 |
| Bộ sưu tập: Yêu thích | Album tư thế, "Tạo Album Mới" | Tab "YÊU THÍCH" | 25 |
| Album tư thế | Lưới 2 cột, ô "Ảnh Mới" | Chạm album | 26 |
| Cài đặt | Nâng cấp, ngôn ngữ, chính sách, khác | Tab "Cài đặt" | 50 |

Luồng chính: `Trang chủ > mục chủ đề > [interstitial] > danh sách > chi tiết tư thế > Chụp ảnh > [quyền camera] > Camera > chụp > Xem lại > Lưu ảnh > [màn trả phí > interstitial] > Tùy chọn xuất > Lưu ảnh > Camera` (ảnh lưu vào "Ảnh đã tạo"). Back từ danh sách hay từ camera cũng thường có interstitial.

## 4. Tính năng

- **Trang chủ** (ảnh `13` đến `16`):
  - Banner trên cùng tự xoay (393x218dp): "1000+ dáng chụp hoàn hảo MẪU", "Cực chất Của bạn MẠNG XÃ HỘI", "Từ gượng ngùng Đến TUYỆT ĐẸP". Nút vương miện 48dp góc trên phải mở màn trả phí.
  - Ô tìm kiếm trắng 176x48dp chỉ có kính lúp, không có chữ gợi ý.
  - Hàng "story" cuộn ngang: ô "Xu hướng Hot" vàng cam, sau đó các ảnh 73x100dp viền tím xanh; chạm vào mở thẳng chi tiết một tư thế ("Playful Golden Hour Selfie").
  - Chip: "Được đề xuất", "Friends", "Couple", "Family". Lúc API lỗi 429, Friends và Family trống (ảnh `17`, `18`); chưa kiểm lại khi API đã tốt.
  - "Được đề xuất" có **10 mục**, mỗi mục là thẻ 373x205dp có nhãn màu, mũi tên tròn và ảnh xếp chồng: Hot Trending, Selfie Hacks, Street Style, Coffee Vibes, Nature Escape, Home Aesthetic, Studio Glam, Active Mode, Model Mode, Boss Vibes.
- **Danh sách chủ đề** (Hot Trending): lưới 2 cột thẻ 173x235dp, cứ khoảng 4 thẻ có 1 native trong lưới. Tải theo trang (Paging): vuốt 25 lần vẫn chưa hết, ước chừng hơn 50 tư thế mỗi chủ đề.
- **Khám phá** (ảnh `40`, `49`): lưới so le (staggered) 2 cột, thẻ 175 rộng, cao 172 đến 237dp, ảnh người thật, bo góc lớn. **Tư thế trả phí có nhãn kim cương vàng** ở góc trên phải. Native trong lưới khoảng mỗi 4 thẻ. Tỉ lệ thấy được: khoảng 1 trên 4 tư thế có kim cương.
- **Bộ lọc** (ảnh `41`): sheet có 5 nhóm thẻ, nút "Hủy", "Áp dụng":
  - Indoor: Coffee Vibes, Halloween, Home Aesthetic, Studio Glam.
  - Outdoor: Street Style, Nature Escape, Summer Getaway, Halloween.
  - Posture: Selfie Hacks, Portrait Mode, Halloween, Casual OOTD, Minimalist Chic, Boss Vibes, Sitting Pose, Standing Pose, Phone, Bag, Beverage, Book.
  - Mood: Halloween, Active Mode, Model Mode, Playful, Artistic, Newborn, Maternity, Pets, Couple, Family.
  - Style/Trending: Hot Trending, Insta Worthy, Halloween, Boss Vibes, Vintage Mood, Celebrity, Couple, Family.
  - Thẻ không dịch (tiếng Anh), "Halloween" lặp ở 5 nhóm (nội dung theo mùa).
- **Tìm kiếm:** ô "Tìm kiếm", gõ là ra kết quả ngay (lưới 2 cột có native). Gõ sai chính tả "cofee" vẫn ra kết quả.
- **Chi tiết tư thế** (ảnh `43`): ảnh toàn màn, sheet tối xanh ở dưới có ảnh nhỏ, **tên tiếng Anh** ("Man in suit reading magazine"), nút tim 50dp ("Lưu vào album"), nút xanh "Chụp ảnh →" 291x51dp. Không có mô tả, mẹo, độ khó, video hay ảnh nét viền thấy được. Kéo sheet lên không mở thêm gì. Native nhỏ 76dp ở đáy.
- **Tư thế trả phí:** chạm thẻ có kim cương mở thẳng màn trả phí (ảnh `48`). Không thấy lựa chọn "xem quảng cáo để mở" dù APK có chuỗi "Just watch a short video to unlock".
- **Bộ sưu tập** (ảnh `23` đến `26`, `38`, `39`):
  - "ẢNH ĐÃ TẠO": nút sắp xếp, dòng thời gian dọc theo tháng (2026 Tháng 10, 9, 8), mỗi tháng một biểu tượng thư mục. Tháng 9, 8 hiện sẵn dù app mới cài và rỗng. Ảnh chụp trong app lưu ở đây (album "Tháng 10 2026", nhóm theo ngày "21 Tháng 10, 2026").
  - "YÊU THÍCH": nút "Tạo Album Mới" (gói trả phí hứa "Tạo album mới không giới hạn", nên bản miễn phí có giới hạn chưa rõ), 3 album có sẵn "TƯ THẾ", "NGOÀI TRỜI", "TRONG NHÀ", mỗi album 6 ảnh đóng gói (dùng được khi không có mạng). Trong album có ô "Ảnh Mới" để thêm ảnh, nút sửa tên.
- **Cài đặt** (ảnh `50`): thẻ "HOÀN HẢO / Tư thế trong vài giây / Nâng cấp ngay"; Chung: Ngôn ngữ; Quyền riêng tư: Chính sách Quyền riêng tư, Điều khoản sử dụng; Khác: Đánh giá ứng dụng, Liên hệ với chúng tôi, Chia sẻ ứng dụng. Không có cài đặt camera, âm thanh, thông báo hay khôi phục mua.

## 5. Chức năng chính

**Camera có ảnh mẫu phủ** (ảnh `28`, `44`):

- Khung ngắm 4:3 rộng toàn màn (393x524dp từ y 37). Thanh trên: back 48dp trái; đổi camera 48dp và bộ lọc màu 49dp bên phải.
- **Lớp phủ là chính ảnh tư thế (người thật) để mờ** chồng lên khung ngắm, không phải nét viền hay khung xương. Không kéo, không đổi cỡ được. Ảnh mẫu nhỏ 100x125dp góc dưới trái (có X để ẩn); chạm vào thì phóng to 216x292dp giữa màn (ảnh `45`).
- **Thanh trượt độ mờ** ngang dưới khung. Kéo thử không làm đổi độ mờ thấy được (ảnh `46`); một lần kéo bắt đầu sát mép phải bị MIUI hiểu là cử chỉ back, làm thoát camera.
- Đáy: nút "AI Đề xuất" 52dp (biểu tượng người có lấp lánh, có bong bóng chú thích tím lần đầu), nút chụp 76dp, nút thư viện ảnh 52dp.
- **AI Đề xuất** (ảnh `29`): bảng nổi "Được đề xuất" với dải ngang ảnh tư thế 70x88dp, nút X. Chọn một ảnh để đổi tư thế phủ. Dải này lấy tư thế có sẵn; không thấy bước "AI quét xung quanh".
- **Bộ lọc màu** (ảnh `30`, `31`): hàng ô tròn 48dp, tên ở viên thuốc trắng giữa, nút X và ✓: Không, Sáng, Tối, Ấm áp, Lạnh, Mộng mơ, và vài ô nữa (APK: Sepia, Vintage).
- Không có zoom, flash, hẹn giờ, lưới, lật gương (SimplePose có đủ).
- **Sau khi chụp** (ảnh `32`): màn xem lại ảnh chụp (không kèm ảnh phủ), "Chụp lại" và "Lưu ảnh", native nhỏ ở đáy. "Lưu ảnh" lần đầu mở màn trả phí, đóng thì có interstitial, rồi tới sheet **"Tùy chọn xuất"** (ảnh `36`): Chia sẻ Liên kết, TikTok, Facebook, Instagram; Định dạng JPG, PNG, PDF, WEBP; Độ phân giải 100 %; nút "Lưu ảnh". Lưu JPG 100% được ở bản miễn phí (logcat `save_photo {resolution=100, format=JPG}`). Lưu xong quay về camera **mất ảnh phủ** (ảnh `37`). Ảnh lưu trong app, không thấy trong `Pictures/` hay MediaStore.
- Camera dùng luồng 1600x1200 (preview, JPEG) và 640x480 YUV cho phân tích (logcat CameraX).

**Chấm điểm khớp dáng:**

- Không có người trước camera (máy chĩa lên trần), nên **không thấy khung xương, điểm số, màu hay gợi ý nào**. Không có thông báo kiểu "không thấy người". Không thấy công tắc Overlay/Outline trên camera.
- Logcat cho thấy **ML Kit Pose chạy liên tục trên khung hình** khi ở camera (`PoseTaskWithRes`, mỗi khoảng 100 ms).
- Từ mã đã dịch ngược (`fy.java`, nhánh 13): lấy các điểm khớp (`SmoothedLandmark`) của ảnh mẫu và của người trên camera, giữ các loại điểm có ở cả hai, dời về tâm và chia theo cỡ thân, tính khoảng cách trung bình giữa từng cặp điểm, **điểm = (1 − trung bình, kẹp 0..1) × 100**, làm tròn thành số nguyên. Chuỗi hiển thị: "PERFECT", "PERFECT pose", "Perfect! Snap Your Photo!". Ngưỡng điểm và màu chưa xác định.
- Cờ Remote Config `is_show_outline_pose`, `is_show_overlay_pose` điều khiển hai chế độ phủ. Bản khảo sát chỉ thấy chế độ ảnh mờ (overlay), có thể chế độ nét viền đang tắt từ xa.

## 6. Kiếm tiền

AdMob (GMA next-gen) với dàn xếp nhiều mạng; quảng cáo thật (Dola, Clip Studio Paint, Betagen, Dico, Shopee, MEXC, OKX). **8 interstitial trong khoảng 31 phút.**

| Loại quảng cáo | Vị trí | Thời điểm | Tần suất | Thời gian chờ đóng | Ảnh |
| --- | --- | --- | --- | --- | --- |
| Interstitial | Splash | Khoảng 8 giây sau khi mở | Lần mở nguội | "Đóng" có ngay | 02 |
| Native toàn màn hình | Sau interstitial splash | Ngay sau khi đóng | Lần mở đầu | X có ngay (không có trong cây là nút đóng; dùng `adcorner tr`) | 03 |
| Native toàn màn hình | Trang 3 của giới thiệu | Khi vuốt tới | Lần mở đầu | X có ngay | 08 |
| Native đáy 391x234dp | Ngôn ngữ, giới thiệu trang 1, 2, 4 | Luôn có | Mỗi trang | Không đóng được | 04 đến 09 |
| Native thu gọn được 393x308dp | Trang chủ, Bộ sưu tập (che thanh điều hướng) | Khi vào tab | Mỗi lần vào; thu gọn thì biến mất | Nút ⌄ có ngay | 13, 38 |
| Native trong lưới | Khám phá, danh sách chủ đề, kết quả tìm kiếm | Luôn có | Khoảng mỗi 4 thẻ | Không | 40, 49 |
| Native nhỏ 393x76dp | Chi tiết tư thế, camera, màn xem lại | Luôn có | Thường trực | Không | 32, 43, 44 |
| Interstitial | Mở một mục chủ đề | Khi chạm | Khi đã qua khoảng 1 đến 2 phút từ lần trước | "Đóng" sau khoảng 10 giây | 19 |
| Interstitial | Back từ danh sách chủ đề | Khi back | Như trên (2 lần) | Ngay; lần sau là video, "Close" sau vài giây | 21 |
| Interstitial | Đóng màn trả phí khi lưu ảnh | Sau khi đóng | 1 lần | "Đóng" có bộ đếm 1 giây, khoảng 8 giây | 34 |
| Interstitial | Mở chi tiết tư thế (Khám phá, story) | Khi chạm | 2 lần | Ngay | 42 |
| Interstitial | Thoát camera (back) | Khi back | 1 lần | Ngay | 47 |

Không có interstitial khi: mở camera, chụp, mở tab Khám phá, back từ chi tiết tư thế khi mới có quảng cáo dưới 1 phút trước, đóng màn trả phí mở từ vương miện, mở lại danh sách chủ đề 1 phút sau quảng cáo trước. Có vẻ giới hạn theo thời gian (cờ `time_show_ads`) chứ không theo số lần chạm. Không gặp rewarded, không gặp app open (chưa mở lại app từ nền).

**Gói trả phí** (ảnh `11`, `33`, `48`; cùng một màn hình ở mọi chỗ vào):

- Đầu màn: ảnh người mẫu nền cam có nét viền trắng đứt, nút X tròn 48dp góc trên trái (xuất hiện sau khoảng 2 giây), biểu tượng vương miện.
- Quyền lợi (5 dòng, biểu tượng tròn): "Không còn Quảng cáo", "Tư thế không giới hạn", "Tạo album mới không giới hạn", "Chế độ ngoại tuyến", "Không giới hạn định dạng lưu ảnh".
- Gói:

  | Gói | Giá | Ghi chú |
  | --- | --- | --- |
  | Hàng tuần (chọn sẵn, nhãn đỏ "Best Offer") | 99.000 ₫/tuần | Không thấy dùng thử |
  | Hàng năm | 499.000 ₫/năm (9.596 ₫/tuần) | Dải vàng "Tiết kiệm đến 90%" |

- Nút "Bắt đầu ngay" 356x53dp, "Tự động gia hạn, hủy bất kỳ lúc nào.", liên kết Điều khoản sử dụng, Chính sách Quyền riêng tư, Khôi phục mua hàng.
- Gói tuần tương đương khoảng 430.000 ₫/tháng, gói năm khoảng 42.000 ₫/tháng. Gói tháng (`iap_sub_monthly` trong APK) không hiện. Không thấy dùng thử 3 ngày (APK có chuỗi "Try 3 Days For Free").
- Bị khóa thấy được: tư thế có kim cương (khoảng 1/4), có thể các định dạng PNG, PDF, WEBP và số album (chưa thử). Màn trả phí cũng chen vào lần "Lưu ảnh" đầu tiên dù việc lưu miễn phí.

## 7. Giao diện

- **Theme tối**, nền tím than gần đen với vệt xanh lục mờ, màu nhấn **xanh lá chuyển sắc**, chữ tiêu đề cam đậm nghiêng. Phong cách "app tiện ích" nhiều hiệu ứng: viền phát sáng, thẻ nghiêng, nhãn dán màu.
- Màu (lấy mẫu từ ảnh):
  - Nền: `#1c1828`, `#1b132c` (màn trả phí), `#1c1827` (Khám phá); vệt xanh `#1c2d2c`.
  - Thẻ, nút phụ: `#242230`, chip chưa chọn `#1b212f`, hàng cài đặt `#1c212c`.
  - Nút chính chuyển sắc `#5ce587` sang `#38ad71`; chip đang chọn `#1cb279`; viền thẻ chọn `#018150`; mục điều hướng đang chọn `#058a57`.
  - Tiêu đề giới thiệu, chữ "HOÀN HẢO": cam `#ff9d00`. Dải tiết kiệm vàng `#efa806`. Thẻ gói đang chọn nền xanh nhạt `#c7f6e7`, chữ giá xanh đậm.
  - Camera: nền đen `#141414` đến `#1e1e1e`, thanh trượt xanh lá.
- Chữ: Quicksand hoặc tương tự (nét tròn) cho phần lớn giao diện (APK khai báo Arimo, Inter, Roboto). Tiêu đề màn 22sp ("Khám phá", "Bộ sưu tập"), tiêu đề giới thiệu khoảng 29sp đậm nghiêng, tên tư thế khoảng 30sp, nút 20sp, nhãn tab 12sp.
- Biểu tượng nét mảnh trắng, tab chọn đổi sang biểu tượng đặc xanh.

Số đo (dp):

- **Thanh điều hướng đáy:** nổi, bo tròn, nền tối trong mờ, 4 mục 72x65 (Trang chủ, Khám phá, Bộ sưu tập, Cài đặt) và **nút camera tròn 58dp ở giữa** (x 168, y 768) nhô lên, nền chuyển sắc xanh tím.
- **Trang chủ:** banner 393x218; ô tìm 176x48 tại (20,194); hàng story y 276, ô 73x100; chip cao 48, y 405; thẻ mục 373x205 (x 10), nhãn tên mục 126x25 chữ, mũi tên tròn khoảng 60.
- **Khám phá:** nút tìm 89x48 (16,37), tiêu đề giữa, vương miện 51x51 phải; "Bộ lọc" 121x48 (12,91) nền trắng; lưới 2 cột thẻ 175 rộng, lề 14, khe 15.
- **Chi tiết tư thế:** back 48 tại (12,38); sheet từ y khoảng 580, ảnh nhỏ 90x120, tên 247x72 (x 114); tim 50x50 (20,722); nút "Chụp ảnh" 291x51 (82,722).
- **Camera:** khung 393x524 từ y 37; nút trên 48; thanh trượt y khoảng 752; nút đáy 52 / 76 / 52 tại y 773 / 761 / 773; ảnh mẫu nhỏ 100x125 (12,535).
- **Ngôn ngữ:** tiêu đề 30 cao (16,45); "Done" 108x48 (268,36); thẻ 361x72, khe 16.
- **Giới thiệu:** tiêu đề y 429, mô tả 361x40, nút "Tiếp theo" 361x52 (16,559), chấm chỉ trang y khoảng 530, native từ y 638.
- **Màn trả phí:** khung quyền lợi 361 rộng (y 260 đến 470), thẻ gói 356x63 (18,530) và 356x62 (18,617), nút 356x53 (18,733).
- **Cài đặt:** thẻ nâng cấp 361x149 (16,88), nút "Nâng cấp ngay" 152x48; hàng 393x48 chữ x 56; tiêu đề nhóm 25 cao.
- **Bộ sưu tập:** tab chữ 180x48; thẻ album 345x140 bo góc lớn, viền tím, chữ hoa đậm.

## 8. Điểm mạnh, điểm yếu

Điểm mạnh:

- Ảnh tư thế người thật chất lượng cao, nhiều chủ đề, bộ lọc 5 nhóm, tìm kiếm, lưới so le đẹp. Nội dung tải theo trang, có thể thêm theo mùa (Halloween).
- Có nền tảng chấm khớp dáng thật (ML Kit chạy trên camera, công thức điểm rõ). Là app duy nhất trong bốn app đã khảo sát có phần này.
- Có màn xem lại sau khi chụp, xuất nhiều định dạng, chia sẻ thẳng TikTok, Facebook, Instagram, album theo tháng.
- 3 album đóng gói sẵn để dùng khi không có mạng.

Điểm yếu:

- **Quảng cáo rất dày:** 8 interstitial trong 31 phút, 2 native toàn màn hình trước khi tới trang chủ, native che thanh điều hướng, native nhỏ cả trên camera.
- Lần mở đầu nặng: ngôn ngữ, giới thiệu có quảng cáo, quyền thông báo, màn trả phí, rồi **hộp thoại đánh giá 5 sao chọn sẵn trước khi dùng**.
- Màn trả phí chen vào lần lưu ảnh đầu tiên; gói tuần được chọn sẵn và gắn "Best Offer".
- Khi API lỗi (429) màn hình trống hoặc quay vòng mãi, không báo lỗi, không có thử lại.
- Lớp phủ là ảnh mờ của người khác, khó nhìn trên nền phức tạp; không chỉnh vị trí, cỡ; thanh độ mờ gần như không có tác dụng thấy được.
- Camera thiếu zoom, flash, hẹn giờ, lưới. Sau khi lưu mất tư thế đang phủ, phải chọn lại.
- Tên tư thế và thẻ lọc chưa dịch; mục "Tháng 9", "Tháng 8" rỗng vẫn hiện.

## 9. Chưa xem được

- **Chấm điểm khớp dáng:** cần một người đứng trước camera sau (hoặc người thử tự chụp bằng camera trước) để thấy khung xương, số điểm, màu, chữ "PERFECT" và ngưỡng. Nhờ người làm: mở một tư thế, đứng cách 2 m, ghi lại màn hình.
- **Chế độ nét viền (Outline) và video tư thế:** không thấy trên máy; có thể bị tắt bằng Remote Config.
- **AI quét xung quanh** ("Let's our AI scan your surroundings"): không gặp.
- **Màn trả phí từ Cài đặt:** bộ chặn từ chối chạm "Nâng cấp ngay" (coi là nút mua). Đã xem cùng màn hình qua nút vương miện.
- Chọn gói năm để xem có dùng thử không: không chạm thẻ gói (có giá tiền).
- Giới hạn bản miễn phí: số album, định dạng PNG, PDF, WEBP, độ phân giải khác 100%: chưa thử.
- Rewarded, app open khi mở lại từ nền, mở lại nguội: chưa thử.
- Chip Friends, Couple, Family ở Trang chủ khi API đã tốt.
- Bỏ qua theo quy tắc: Đánh giá ứng dụng, Liên hệ với chúng tôi, Chia sẻ ứng dụng, Chính sách, Điều khoản, chia sẻ TikTok, Facebook, Instagram, nút thư viện ảnh trên camera.

## 10. Sự cố trong lúc khảo sát

- `incidents.log` không có dòng mới (file không được tạo). Không chạm nhầm quảng cáo, không rời app.
- Bộ chặn từ chối đúng hai lần: `taptext "Cài đặt"` khớp nút CTA "Cài đặt" của native trong lưới thay vì tab Cài đặt; "Nâng cấp ngay" ở Cài đặt.
- Một lần `drag` bắt đầu ở x 1050 (sát mép phải) bị MIUI coi là cử chỉ back, làm thoát camera và bật interstitial (ảnh `47`).
- `type "coffee"` nhập thành "cofee" (mất một chữ cái).
- API trả HTTP 429 khoảng 11 phút (mục 1).
- Dữ liệu để lại trên máy: một ảnh chụp trần nhà lưu trong app (Bộ sưu tập > Ảnh đã tạo > Tháng 10 2026), nằm trong bộ nhớ riêng của app, mất khi `pm clear`. Không có file trong `/sdcard/Pictures` hay MediaStore. Quyền camera đã cấp, quyền thông báo đã từ chối.

## 11. Khác với ba app trước

- **So với Pose Camera (guide.ideas):** cùng kiểu quảng cáo dày ở lần mở đầu (ngôn ngữ, giới thiệu có native) nhưng AI Posing còn thêm native toàn màn hình, màn trả phí và hộp thoại đánh giá. Nội dung ảnh người thật tải từ API, nhiều hơn hẳn 32 ảnh nét vẽ đóng gói. Có màn xem lại, xuất nhiều định dạng, album.
- **So với PoseCamera (vector123):** vector123 không quảng cáo, mua một lần, nét vẽ ma-nơ-canh, có mẹo; AI Posing thuê bao tuần, ảnh người thật, không có mẹo hay bước hướng dẫn trên màn hình.
- **So với SimplePose:** SimplePose phủ nét viền riêng chỉnh được (độ mờ, cỡ, vị trí, khóa, lật) và camera đủ zoom, flash, hẹn giờ, lưới; AI Posing phủ ảnh mờ không chỉnh được, camera ít công cụ. SimplePose chỉ hứa so khớp xanh, vàng, đỏ mà không có bộ nhận dạng; **AI Posing có ML Kit chạy thật và công thức điểm 0 đến 100**. AI Posing khóa khoảng 1/4 tư thế sau màn trả phí (SimplePose không thấy khóa). Giá: AI Posing 99.000 ₫/tuần, 499.000 ₫/năm, không thấy dùng thử; SimplePose 104.000 ₫/tuần, 208.000 ₫/tháng, 520.000 ₫/năm có 3 ngày dùng thử. Quảng cáo AI Posing dày hơn (8 interstitial so với khoảng 2 mỗi phiên ngắn).
