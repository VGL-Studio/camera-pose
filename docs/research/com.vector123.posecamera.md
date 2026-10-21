# PoseCamera (com.vector123.posecamera)

## 1. Thông tin

- Tên hiển thị: PoseCamera. Tên trong app: "Pose Camera". Nhà phát triển: Shenzhen Vector 123 Technology Company Limited (email hỗ trợ `TofuKnifeDev@outlook.com`).
- Package: `com.vector123.posecamera`. Phiên bản 1.0.4 (build 5).
- Ngày khảo sát: 2026-10-19 (sang 2026-10-20). Máy Xiaomi 23021RAAEG, Android 15.
- Viết bằng Flutter (Material 3). Cây giao diện Flutter có nhãn ngữ nghĩa khá đầy đủ (tên tư thế, nút, tab), nên chạm được theo chữ. Màn trả phí là bottom sheet Android gốc (có resource-id `pay_*`).
- Không quảng cáo, không đăng nhập, chạy offline. Tiền đến từ gói Pro mua một lần.
- Ảnh: 38 ảnh trong `captures/com.vector123.posecamera/`. Ảnh camera chụp lúc ống kính bị che nên khung camera tối hoặc xám, không có người.

## 2. Lần mở đầu

Sau `pm clear`:

1. **Không có splash riêng, không chọn ngôn ngữ, không giới thiệu.** Mở thẳng màn **Privacy Policy** (cổng đồng ý). Văn bản dài cuộn được, hai nút cuối màn: "Disagree" (viền) và "Agree" (nền nâu đỏ). Ảnh `01-launch`.
   - Bấm "Disagree": app đóng, về màn hình chính của máy (đã thử nhầm, xem mục 9).
2. Bấm "Agree": vào thẳng home "Pose Templates". Ảnh `02-home`.
3. **Quyền camera** chỉ xin khi bấm "Shoot with This Pose" lần đầu. Chọn "Trong khi dùng ứng dụng". Ảnh `06-camera-permission`. Không xin quyền thông báo, không xin quyền ảnh (lưu ảnh qua MediaStore).
4. Không có quảng cáo, không có hộp thoại UMP. Lần mở sau vào thẳng home (cổng đồng ý chỉ hiện một lần).

Tổng: một chạm từ lúc mở tới home. Rất khác nhóm app "nhà máy".

## 3. Bản đồ màn hình

| Màn hình | Mục đích | Vào từ đâu | Ảnh |
| --- | --- | --- | --- |
| Privacy Gate | Đồng ý chính sách | Lần mở đầu; Settings > Privacy Policy (vẫn có hai nút) | 01 |
| Poses (home) | Featured, Quick Filters, 4 tab nhóm tư thế dạng danh sách | Tab "Poses" | 02, 03, 04 |
| Featured | Lưới 2 cột 8 tư thế nổi bật | Home > "›" cạnh Featured | 27 |
| Search | Tìm theo tên, tag, lọc độ khó | Home > kính lúp | 25, 26 |
| Pose Detail | Ảnh tư thế, hướng dẫn, bước, mẹo chụp, lỗi hay gặp, tag | Chạm một tư thế | 05 (màn đen, xem dưới) |
| Menu tư thế | "Add to Shooting Plan", "Favorite" | Detail > nút trên phải | (chỉ có cây) |
| Camera Overlay | Camera + nét vẽ + thẻ mẹo + bảng công cụ | Detail > "Shoot with This Pose" | 07 đến 21 |
| Capture resolution (sheet) | 720p, 1080p, 4K, Maximum | Camera > công cụ; Settings | 17 |
| Photo Preview | Xem ảnh vừa chụp kèm lớp phủ, chỉnh độ mờ, lưu | Ô ảnh nhỏ trên camera | 22, 23, 24 |
| Paywall (sheet) | Mua Pro | Settings > Pose Camera Pro; hết lượt chụp; vượt giới hạn kế hoạch | 36, 38 |
| Photo Tips | 5 nhóm, 200 mẹo chụp ảnh | Tab "Tips" | 28 |
| Tip Detail | Một mẹo: mô tả, Why It Works, Steps, Use Cases, Notes, Common Mistakes, yêu thích | Chạm thẻ mẹo | 29 |
| Saved | 4 tab: Plan, Recent, Saved Poses, Saved Tips | Tab "Saved" | 30, 31 |
| Settings | Mẹo khi mở camera, độ phân giải, theme, ngôn ngữ, Pro, liên hệ, đánh giá, chính sách, giấy phép | Tab "Settings" | 32, 37 |
| Theme / Language (sheet) | System, Light, Dark / System, English, 简体中文, Polski, Português (Brasil) | Settings | 33, 34, 35 |
| Open Source Licenses | Danh sách thư viện | Settings | (chỉ có cây) |

Sơ đồ luồng:

- Mở lần đầu > Privacy Gate > Agree > Poses.
- Poses > tab nhóm > tư thế > Pose Detail > "Shoot with This Pose" > [quyền camera] > Camera > nút chụp > ảnh lưu thẳng vào thư viện máy, snackbar "Saved to gallery / 4/5 free pose-assisted shots left" > ô ảnh nhỏ > Photo Preview > Lưu / bật tắt lớp phủ / độ mờ.
- Camera > hết 5 lượt miễn phí > nút chụp mở Paywall.
- Pose Detail > menu > Add to Shooting Plan / Favorite > Saved > Plan / Saved Poses. Vượt giới hạn kế hoạch > Paywall.
- Poses > kính lúp > Search > tag hoặc độ khó > danh sách kết quả > Detail.
- Tips > nhóm > mẹo > Tip Detail > tim > Saved > Saved Tips.
- Settings > Pose Camera Pro > Paywall. Settings > Contact Us > Gmail soạn thư (đã back, không gửi).

## 4. Tính năng

- **Thư viện tư thế: 120 tư thế, 4 nhóm** (đếm trên máy bằng cách cuộn hết từng tab, khớp với APK):

  | Nhóm (tab) | Số tư thế |
  | --- | --- |
  | Women | 33 |
  | Couples | 49 |
  | Solo | 18 |
  | Groups | 20 |

  Featured: 8 tư thế (đều là tư thế đôi, 8 tư thế đầu của Couples).
- **Dạng hiển thị:** nét vẽ đen trên nền trắng, kiểu **ma-nơ-canh có khớp** (khớp tròn ở vai, khuỷu, gối, đường lưới chia mặt cơ thể), mặt trống. Không có ảnh người thật. Không nhận diện cơ thể, không chấm điểm khớp tư thế.
- **Danh sách tư thế:** thẻ ngang gồm ảnh nhỏ, tên đậm, mô tả 3 dòng, nhãn độ khó (Easy xanh ngọc, Medium vàng, Hard), dòng tag xám (#Slimming #Longer-looking legs #Full Body ...). Ảnh `03-home-scrolled`.
- **Pose Detail** (dữ liệu có cấu trúc, ví dụ "Foreheads Touching"): ảnh tư thế lớn, mục "Pose Guide" (mô tả), "Steps" (6 bước đánh số), "Shooting Tips" dạng bảng Item / Recommendation (Angle: Eye level, Framing: Medium shot or close-up, Lens: 85mm, Lighting: Soft light or backlight), "Common Mistakes" (3 lỗi), cuối là nhóm + độ khó + tag (#Couple #Half Body #Standing #Romantic #Beginner). Nút cố định ở đáy "Shoot with This Pose" 361x52dp. Menu trên phải: "Add to Shooting Plan", "Favorite" (đổi thành "Remove from Shooting Plan", "Remove favorite").
  - **Màn này bật FLAG_SECURE**: chụp màn hình ra ảnh đen (ảnh `05-pose-detail`). Nội dung lấy từ cây giao diện. Có thể để chống sao chép ảnh tư thế. Các màn khác (home, camera, tips) chụp được bình thường.
- **Quick Filters / Search:** tag nhanh #Slimming, #Long Legs, #Couple, #Group (hàng cuộn ngang, có thể còn tag nữa nhưng công cụ không vuốt ngang được ở hàng đó). Lọc độ khó Easy / Medium / Hard. Ví dụ: #Long Legs ra 12 kết quả (ô tìm điền "Longer-looking legs"), Hard ra 13. Tìm theo tên hoặc tag bằng chữ. Ảnh `25-search`, `26-search-tag`.
- **Lớp phủ trên camera** (ảnh `07` đến `16`): nét vẽ tư thế đặt giữa khung camera, nền trắng của ảnh tư thế làm mờ camera theo độ mờ.
  - **Độ mờ:** thanh trượt 0% đến 60%, mặc định 30%. 0% ẩn hẳn lớp phủ. Ảnh `09`, `10`, `11`.
  - **Cỡ:** thanh trượt 75% đến 145%, mặc định 100%. Ảnh `12`, `13`, `14`.
  - **Lật ngang:** bật tắt, biểu tượng chuyển vàng khi bật. Ảnh `16-flip`.
  - **Đặt lại** (biểu tượng ngắm tâm): đưa cỡ và vị trí về mặc định, giữ độ mờ.
  - **Kéo:** vuốt một ngón trên khung không di chuyển lớp phủ (ảnh `15-drag-right`). Có thể cần kéo chậm hoặc hai ngón; adb chỉ vuốt một ngón nhanh, chưa kết luận chắc.
- **Thẻ mẹo trên camera:** tên tư thế, mẹo hiện tại có số thứ tự, "x/10", nút trước/sau. 10 mẹo = 6 bước + Angle + Framing + Lighting + "Avoid: <lỗi hay gặp>". Nút "Pose Tips" (trên phải) bật tắt thẻ (vàng khi bật). Ảnh `07-camera`, `20-tips-hidden`.
- **Photo Tips: 200 mẹo, 5 nhóm**: Exposure Basics 40, Composition 40, Lighting 30, Focus & Stability 30, Mobile 60. Thẻ có tiêu đề và 2 dòng mô tả. Tip Detail: nhãn nhóm, tiêu đề lớn, mô tả, rồi các thẻ "Why It Works", "Steps", "Use Cases", "Notes", "Common Mistakes" có biểu tượng. Tim để lưu. Có tìm kiếm mẹo. Ảnh `28`, `29`.
- **Saved:** 4 tab.
  - **Plan** (kế hoạch chụp): danh sách tư thế định chụp, đếm "1/1 completed", tự đánh dấu hoàn thành (dấu tích tròn nâu) sau khi đã chụp bằng tư thế đó. Chế độ "Select" có "Select all", "Cancel", ô chọn từng mục. Ảnh `30`, `31`.
  - **Recent:** tư thế đã xem gần đây. **Saved Poses**, **Saved Tips**: mục đã yêu thích.
  - Không có thư viện ảnh đã chụp trong app (chỉ ô ảnh nhỏ trên camera).
- **Settings:** khối đầu (biểu tượng app, "Pose Camera", "1.0.4 (5)"); Capture: "Show pose tips when opening camera" (công tắc, mặc định bật), "Capture resolution"; Display: Theme (System, Light, Dark), Language (System, English, 简体中文, Polski, Português (Brasil)); Pro & Support: Pose Camera Pro, Contact Us (mở Gmail, người nhận `TofuKnifeDev@outlook.com`, tiêu đề lỗi mã hóa "Pose+Camera+Feedback"), Rate App; About & Legal: Privacy Policy, Open Source Licenses; chân trang "Capture every moment with care". Ảnh `32`, `37`.

## 5. Camera

Bố cục (ảnh `07-camera`, `08-camera-adjust`):

- Nền đen toàn màn. Nút back (mũi tên trắng 48dp, x 8, y 42dp). Nút "Pose Tips" 108x39dp ở trên phải, viền xám, chữ và biểu tượng vàng `#ffd740`.
- Thẻ mẹo nổi đè lên trên khung camera (x 8, y 45 đến 100dp ảnh preview, khoảng 361x140dp), nền đen trong mờ, bo góc khoảng 12dp.
- Khung camera tỷ lệ 3:4 rộng toàn màn (393x524dp, từ y khoảng 66dp ảnh preview).
- Hàng đáy: ô ảnh vừa chụp 48dp (trái, x 20dp, viền xám bo góc), nút chụp 72dp trắng có vòng trắng (giữa), nút công cụ 48dp (phải, biểu tượng thanh trượt, vàng khi mở).
- **Bảng công cụ** (bấm nút phải): khung nổi 2 hàng x 4 nút 48dp, nền đen trong mờ, bo góc:
  - Hàng 1: Độ mờ, Cỡ, Lật ngang, Đặt lại lớp phủ.
  - Hàng 2: Độ phân giải chụp (sheet 720p / 1080p mặc định / 4K / Maximum, không khóa), Đổi camera trước/sau, Hẹn giờ (bấm lần lượt: tắt, 3s, 10s), Lưới 3x3.
  - Chọn độ mờ hoặc cỡ thì bảng thay bằng thanh trượt 302x46dp kèm nhãn %.
- Không có flash, không có zoom bằng nút (chưa thử chụm hai ngón), không đổi tỷ lệ khung.
- **Sau khi chụp:** ảnh lưu thẳng vào `Pictures/capture_<timestamp>.jpg` (khoảng 240 KB; ảnh thử tối vì ống kính bị che). Snackbar "Saved to gallery" kèm "4/5 free pose-assisted shots left". Ô ảnh nhỏ cập nhật. **Ảnh lưu không có lớp phủ.**
- **Photo Preview** (chạm ô ảnh): thanh trên "×", "Photo Preview", nút lưu (đĩa mềm), nút bật tắt lớp phủ (biểu tượng lớp). Ảnh hiện kèm nét vẽ, thanh độ mờ ở đáy. Bấm lưu tạo **bản sao y hệt ảnh gốc** (md5 trùng, không gộp nét vẽ), nên nút lưu gần như thừa. Ảnh `22`, `23`, `24`.

## 6. Kiếm tiền

**Không có quảng cáo** (không SDK quảng cáo, không thấy quảng cáo nào trong toàn bộ phiên).

Gói **Pose Camera Pro**, mua một lần (bottom sheet, ảnh `36-pro`, `38-paywall-limit`):

- Tiêu đề "Unlock unlimited guided shoots". Giá **39.000 ₫** (giá Play theo vùng VN).
- Quyền lợi: "✓ Unlimited pose-overlay captures", "✓ Unlimited shooting plans", "✓ One-time purchase, no subscription", "✓ Restore anytime with Google Play".
- Nút "RESTORE PURCHASE" (chữ xám nhỏ) và "UNLOCK FOREVER" (361x56dp, nền nâu đỏ `#8d4b35`, chữ trắng in hoa).
- Không có dùng thử, không có gói thuê bao.

Bị khóa khi chưa mua:

- **Chụp có lớp phủ: 5 lượt miễn phí** (đếm theo lượt chụp, không thấy chữ nói là mỗi ngày). Snackbar đếm ngược sau mỗi lần chụp. Lần cuối: "Saved to gallery / Free trial used up. Unlock Pro to keep shooting." Sau đó bấm nút chụp mở paywall, không lưu ảnh. Có một lần paywall hiện sớm hơn (sau 3 lượt) rồi sau khi đóng vẫn chụp được thêm 2 ảnh; có thể là lỗi đếm.
- **Kế hoạch chụp:** giới hạn số tư thế trong Plan. Thêm tới tư thế thứ 3 hoặc 4 thì paywall hiện (chưa xác minh số chính xác vì máy khóa màn hình ngay sau đó).
- Toàn bộ 120 tư thế, 200 mẹo, tìm kiếm, yêu thích, độ phân giải 4K/Maximum đều mở miễn phí.

## 7. Giao diện

- **Theme sáng mặc định** (theo hệ thống), có Dark. Phong cách Material 3 tông kem ấm, ít màu.
- Màu (lấy mẫu):
  - Nền màn `#f8f6f1`. Bề mặt thẻ `#fffcf7`, viền thẻ màu be nhạt. Nền ảnh tư thế `#ffffff`.
  - **Màu chính nâu đỏ đất `#8d4b35`**: nút Agree, chữ và gạch dưới tab đang chọn, nút paywall. Công tắc bật `#955842`. Dấu tích Plan `#a6715f`.
  - Thanh điều hướng đáy: nền `#f7f5f0`, mục đang chọn có viên thuốc xanh xám `#8b988f` (tối: `#79867e`).
  - Nhãn độ khó: Easy chữ `#1a6b61` trên nền xanh ngọc nhạt, Medium chữ nâu trên nền vàng nhạt (viền `#cbba90`).
  - Camera: nền `#000000`, nhấn vàng `#ffd740`.
  - Tối: nền `#151412`, thẻ `#211f1c`.
  - Paywall: nền trắng, khối quyền lợi `#f2f2f2`, giá xám `#757575`.
- Chữ: Roboto (mặc định Flutter). Tiêu đề màn 22 đến 24sp thường (không đậm), tên tư thế trên thẻ khoảng 18sp đậm, mô tả 15 đến 16sp xám, tag 12sp xám nhạt.
- Biểu tượng Material, kiểu filled/outlined trộn.

Số đo chính (dp):

- **Thanh trên:** tiêu đề x 16, y 49, cao 27; nút tìm 48x48 ở x 345, y 38. Vùng thanh 90.
- **Home:** "Featured" 21 (y 108) + nút "›" 40x40. Băng chuyền Featured: thẻ 322x527 (x 8, y 146), thẻ kế ló ra 47; thẻ bo góc khoảng 12, viền mảnh, tên tư thế ở đáy thẻ. "Quick Filters" (y 705), chip tag 34 cao, rộng 62 đến 100, bo góc khoảng 8, cách 8. Bên dưới là tab Women / Couples / Solo / Groups 4 x 98x48 (dính lên trên khi cuộn, y 94), danh sách thẻ.
- **Thẻ tư thế:** 361 rộng (lề 16), cao khoảng 176 đến 186 (tính cả khoảng cách 10); ảnh nhỏ khoảng 100x150 bo góc 8 bên trái, chữ bắt đầu x khoảng 147.
- **Thanh điều hướng đáy:** dạng nổi (viên thuốc kính "hydro glass"), 353x64 ở x 20, y 789, 4 nút 86x52 (Poses, Tips, Saved, Settings), biểu tượng trên chữ 12sp.
- **Pose Detail:** thanh trên back 48 (x 4, y 44), tiêu đề x 72; nội dung lề 20; tiêu đề mục 21 cao; bước đánh số (số x 29, chữ x 52, cách 33); bảng Shooting Tips 2 cột (36 và 141); nút đáy 361x52 tại y 797.
- **Tips:** tab cuộn ngang cao 48; thẻ 361x106, cách 16.
- **Tip Detail:** nhãn nhóm 44x13 nền hồng nâu nhạt, tiêu đề 29 cao, các thẻ mục 353 rộng bo góc khoảng 12.
- **Settings:** khối app 361x145; nhóm tiêu đề 28 cao; hàng 361x68 (hàng hai dòng 85 đến 106), biểu tượng trong ô vuông bo góc 40, mũi tên phải; các hàng trong một thẻ bo góc khoảng 16.
- **Sheet chọn** (theme, ngôn ngữ, độ phân giải): tay kéo trên cùng, tiêu đề 28 cao, hàng radio 353x56.
- **Camera:** như mục 5.

## 8. Điểm mạnh, điểm yếu

Điểm mạnh:

- Sạch, không quảng cáo, vào app một chạm. Offline hoàn toàn.
- Nội dung sâu nhất trong các app đã xem: 120 tư thế có bước, góc máy, khung hình, ống kính, ánh sáng, lỗi hay gặp, độ khó, tag; thêm 200 mẹo chụp ảnh.
- Lớp phủ chỉnh được độ mờ, cỡ, lật; thẻ mẹo hiện ngay trên camera, chuyển từng bước.
- Kế hoạch chụp có đánh dấu hoàn thành tự động; yêu thích, gần đây, tìm theo tag và độ khó.
- Giá Pro rẻ, mua một lần.

Điểm yếu:

- Chỉ 5 ảnh chụp có lớp phủ miễn phí: người dùng thử chưa kịp thấy giá trị đã gặp paywall. Đếm có vẻ không ổn định (paywall hiện sớm một lần).
- Nút lưu trong Photo Preview chỉ tạo bản sao trùng ảnh gốc; không có tùy chọn lưu ảnh kèm nét vẽ.
- Không có thư viện ảnh trong app; ảnh nằm lẫn ở gốc `Pictures/`.
- Không kéo được lớp phủ (ít nhất với vuốt một ngón). Không có flash, không có nút zoom.
- Ảnh tư thế là ma-nơ-canh nét vẽ, không có ảnh mẫu người thật; Featured chỉ có tư thế đôi.
- Contact Us điền tiêu đề lỗi "Pose+Camera+Feedback".
- Màn Pose Detail chặn chụp màn hình (người dùng không lưu được ảnh tham khảo).
- Chưa có tiếng Việt (chỉ English, 简体中文, Polski, Português).

## 9. Chưa vào được và sự cố

- **Kéo lớp phủ, zoom chụm hai ngón:** adb chỉ vuốt một ngón; vuốt nhanh không làm lớp phủ di chuyển.
- **Màn Pose Detail:** ảnh đen do FLAG_SECURE; chỉ có cây giao diện.
- **Rate App:** không bấm, để tránh mở Play Store.
- **Số tư thế tối đa trong Plan khi miễn phí:** chưa xác minh (paywall hiện khi thêm tư thế thứ 3 hoặc 4).
- **Tag nhanh ngoài 4 tag đầu:** hàng tag cuộn ngang nằm ngoài vị trí vuốt của công cụ.
- **Camera trước:** đã bật rồi tắt, không chụp ảnh để tránh ghi hình người trong phòng.

Sự cố:

1. `taptext "Agree"` chạm nhầm "Disagree" (chuỗi chứa "Agree"), app đóng. Mở lại, chạm theo tọa độ. Ghi nhận luôn hành vi: Disagree đóng app.
2. **Chạm nhầm nút mua.** Trong một vòng lặp chụp liên tiếp, paywall bật lên đúng lúc, lần chạm vào vị trí nút chụp (540,2213 px) rơi vào nút "UNLOCK FOREVER". Màn mua của Google Play (`com.android.vending ... billing.acquire`) mở ra **2 lần**; công cụ tự back cả hai lần (ghi trong `tools/incidents.log` lúc 00:07:56 và 00:08:10). Không có bước xác nhận mua nào được bấm; paywall vẫn hiện bình thường sau đó, Settings vẫn hiện mục mua Pro. Nguyên nhân: không gọi `tree` trước từng lần chạm trong vòng lặp.
3. Contact Us mở Gmail soạn thư (tài khoản trên máy); đã back, không gửi. Gmail có thể giữ một bản nháp.
4. Mất kết nối adb một lần giữa chừng; sau đó máy tắt màn hình và khóa (keyguard). Đã gửi `input keyevent 224` (WAKEUP, không phải chạm) để đánh thức nhưng màn khóa vẫn còn, nên dừng khảo sát ở đây. Đã `stop` app và `home`.
5. Đã tạo 6 file ảnh tối trong `/sdcard/Pictures/` (`capture_*.jpg`) khi chụp thử. Thư mục `Pictures/PoseCamera/` là của app Pose Camera khảo sát trước.
