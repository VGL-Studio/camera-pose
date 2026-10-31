---
feature: onboarding
status: changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-man-chao-xin-quyen-camera.md
  - requests/2026-10-31-luong-gioi-thieu-nhieu-trang.md
design: design/README.md
created: 2026-10-21
confirmed: 2026-10-21
---

# SPEC: MÀN CHÀO VÀ XIN QUYỀN CAMERA

## 1. Mục tiêu

- **Người dùng được gì:** lần đầu mở app, người dùng hiểu ngay app làm gì và vào camera chỉ sau 2 chạm, không gặp quảng cáo hay màn hình chen giữa.
- **Vì sao làm bây giờ:** phàn nàn số một về app mẫu là chuỗi quảng cáo và màn hình trước khi tới camera (bản đề xuất, bài học 1, 2). Giải thích trước khi xin quyền giúp người dùng ít từ chối quyền camera hơn (bài học 16).
- **Đo kết quả bằng gì:** tỷ lệ người dùng mới cho phép camera (`camera_permission_result` có kết quả cho phép, trên số `onboarding_complete`), mong đợi ít nhất 80%. Số chạm từ lúc màn loading xong tới camera có tư thế: đúng 2. Số quảng cáo toàn màn hình trước ảnh đầu tiên: 0.

## 2. Phạm vi

**Trong phạm vi:**

- Một màn chào duy nhất, hiện sau màn loading ở lần mở đầu tiên: hình minh họa, tiêu đề, một đoạn giới thiệu, ba điểm chính, nút "Bật camera", dòng ghi chú về quyền camera và nơi lưu ảnh.
  - **Đổi theo yêu cầu 2026-10-31 (chờ câu 4 tới 8):**
    - Màn chào thành luồng giới thiệu từ 3 đến 5 trang (KB-5).
    - Người dùng đi qua các trang bằng nút "Tiếp".
    - Trang cuối có nút "Bật camera" và dòng ghi chú như trên.
    - Mục tiêu "2 chạm" và "không quảng cáo" ở mục 1 đổi theo câu 5.
- Quảng cáo native ở bước của luồng giới thiệu (KB-6, mục 5; yêu cầu 2026-10-31).
- Bấm "Bật camera" thì máy hỏi quyền camera; dù người dùng cho phép hay từ chối, app đều sang camera.
- Màn chào chỉ hiện một lần. Từ lần mở sau, app vào thẳng camera.
- Chữ trên màn chào theo ngôn ngữ của máy, có đủ các ngôn ngữ phát hành (bản đề xuất, câu 12.1).

**Ngoài phạm vi lần này:**

- Màn chọn ngôn ngữ, đăng nhập. Quảng cáo toàn màn hình trong luồng giới thiệu (câu 6).
- Cách camera hiện khi chưa có quyền (thuộc spec `camera`).
- Chặn khi không có mạng: màn loading đã chặn lúc mở app; mất mạng khi đang ở màn chào thì theo spec `network`.

## 3. Kịch bản sử dụng

### KB-1. Mở app lần đầu, cho phép camera

Người dùng vừa cài app, mở lần đầu khi có mạng.

Tiêu chí chấp nhận:

- [ ] Khi màn loading xong ở lần mở đầu tiên thì màn chào hiện, đúng thiết kế, không có quảng cáo.
- [ ] Khi bấm "Bật camera" thì máy hiện hộp hỏi quyền camera của hệ thống.
- [ ] Khi người dùng cho phép thì app mở camera, khung xem trước chạy, nét viền của tư thế mặc định đã hiện sẵn (câu hỏi 2).
- [ ] Khi đếm từ lúc màn loading xong tới lúc camera có tư thế thì đúng 2 chạm: "Bật camera" và "Cho phép".
- [ ] Khi bấm "Bật camera" thì app ghi `onboarding_complete` một lần; khi hộp hỏi quyền hiện ra và người dùng chọn thì app ghi `camera_permission_result`.

Trường hợp đặc biệt:

- [ ] Khi bấm nút quay lại của máy ở màn chào thì app đóng.
- [ ] Khi xoay máy ở màn chào thì màn hình không bị cắt chữ hay nút; nội dung dài hơn màn hình thấp thì cuộn được, nút "Bật camera" luôn thấy được.
- [ ] Khi máy đặt ngôn ngữ chưa có trong app thì màn chào hiện tiếng Anh.

### KB-2. Mở app lần đầu, từ chối camera

Người dùng không muốn cho phép camera ngay.

Tiêu chí chấp nhận:

- [ ] Khi người dùng từ chối quyền thì app vẫn sang camera, ở trạng thái chưa có quyền (spec `camera`), không quay lại màn chào.
- [ ] Khi người dùng đã chọn "không hỏi lại" từ trước (máy không hiện hộp hỏi quyền nữa) và bấm "Bật camera" thì app sang thẳng camera ở trạng thái chưa có quyền và không ghi `camera_permission_result`, giống camera (spec `camera`, KB-7).

### KB-3. Mở lại app

Người dùng đã qua màn chào ở lần trước.

Tiêu chí chấp nhận:

- [ ] Khi đã bấm "Bật camera" ở một lần trước thì lần mở sau, màn loading xong là vào thẳng camera, không hiện màn chào, kể cả khi lần trước từ chối quyền.
- [ ] Khi người dùng đóng app ở màn chào mà chưa bấm "Bật camera" thì lần mở sau màn chào hiện lại.
- [ ] Khi người dùng xóa dữ liệu của app trong cài đặt máy thì lần mở sau màn chào hiện lại.

### KB-4. Tắt màn chào từ xa

Người phụ trách tắt màn chào khi có sự cố.

Tiêu chí chấp nhận:

- [ ] Khi cờ `feature_onboarding_enabled` tắt thì app vào thẳng camera sau màn loading; camera tự hỏi quyền nếu chưa có (spec `camera`); app không ghi `onboarding_complete`.

### KB-5. Đi qua luồng giới thiệu (yêu cầu 2026-10-31)

Người dùng vừa cài app, mở lần đầu; màn loading xong. Số trang, nội dung từng trang và nút bỏ qua theo câu 4 và 5.

Tiêu chí chấp nhận:

- [ ] Khi màn loading xong ở lần mở đầu tiên thì trang 1 của luồng giới thiệu hiện, đúng thiết kế.
- [ ] Khi bấm "Tiếp" thì trang kế tiếp hiện; nút "Tiếp" nằm cùng một chỗ trên mọi trang, không nhảy khi thẻ quảng cáo hiện hay ẩn.
- [ ] Khi tới trang cuối thì nút "Bật camera" thay nút "Tiếp"; bấm nút thì theo KB-1 và KB-2 như hiện nay.
- [ ] Khi bấm nút quay lại của máy ở trang 2 trở đi thì về trang trước; ở trang 1 thì app đóng.
- [ ] Khi xoay máy hay mở gập thì trang đang xem giữ nguyên, chữ và nút không bị cắt; trang dài hơn màn hình thấp thì cuộn được.
- [ ] Khi đóng app giữa luồng mà chưa bấm "Bật camera" thì lần mở sau luồng bắt đầu lại từ trang 1 (KB-3).

### KB-6. Quảng cáo native ở luồng giới thiệu (yêu cầu 2026-10-31)

Trang nào có thẻ, có trang quảng cáo toàn trang hay không, và lúc phát hành bật hay tắt theo câu 6.

Tiêu chí chấp nhận:

- [ ] Khi vị trí `native_onboarding` bật và có quảng cáo thì thẻ native 250 dp hiện ở đáy vùng trang, ở các trang mà câu 6 chọn; nhãn "Ad" luôn thấy rõ.
- [ ] Khi chuyển trang thì thẻ đổi sang quảng cáo khác nếu pool còn quảng cáo.
- [ ] Khi chưa có quảng cáo, không có quảng cáo hay người dùng chưa đồng ý quảng cáo thì chỗ của thẻ không làm nút "Tiếp" nhảy, và luồng đi tiếp bình thường.
- [ ] Khi chạm vào thẻ thì chỉ mở quảng cáo; vùng chạm của nút "Tiếp" không chồng lên thẻ.
- [ ] Khi vị trí tắt (`is_show_native_onboarding` tắt) thì luồng không có thẻ, không có chỗ trống của thẻ.

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md) (gói bàn giao lập sau khi spec được xác nhận), dựng trên Figma theo hướng Sticker, frame `OnboardingScreen_default_light`, `OnboardingScreen_default_dark`.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Chào mừng | Giới thiệu app trong một màn, mở đường xin quyền camera | KB-1, KB-2, KB-3 |
| Luồng giới thiệu (yêu cầu 2026-10-31) | 3 đến 5 trang thay màn chào; trang cuối là trang xin quyền camera; thẻ native ở đáy vùng trang | KB-5, KB-6 |

Luồng giới thiệu cần thiết kế mới trên Figma theo hướng Sticker: mỗi trang có frame sáng, tối, một frame có thẻ quảng cáo (trạng thái `ad`), khung thấp `w360dp-h640dp` và khi mở gập. Chữ của từng trang theo câu 4.

Chữ trên thiết kế (tiếng Việt):

| Vị trí | Chữ |
| --- | --- |
| Tiêu đề | Chụp dáng đẹp, không cần đoán |
| Giới thiệu | Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền. |
| Điểm 1 | Hơn 200 tư thế: một người, cặp đôi, nhóm bạn |
| Điểm 2 | Chỉnh độ mờ, cỡ và vị trí nét viền |
| Điểm 3 | Lưu tư thế yêu thích để chụp lại sau |
| Nút | Bật camera |
| Ghi chú | App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn. |

## 5. Quảng cáo

Trước yêu cầu 2026-10-31: không có. Màn chào không hiện quảng cáo (bài học 1, câu 9.1).

Theo yêu cầu 2026-10-31 (chờ câu 6):

| Vị trí | Định dạng | Khi nào hiển thị | Giới hạn tần suất | Cờ bật tắt |
| --- | --- | --- | --- | --- |
| `native_onboarding` | native_step (thẻ 250 dp ở đáy vùng trang) | Các trang của luồng giới thiệu theo câu 6, chỉ ở lần mở đầu tiên | Đổi quảng cáo mỗi khi chuyển trang, theo pool của khung | `is_show_native_onboarding` |

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Đã qua màn chào (có hoặc không) | Lần sau vào thẳng camera | Đến khi người dùng xóa dữ liệu app |

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `onboarding_complete` | Khi bấm "Bật camera" ở màn chào | Không có | Bao nhiêu người dùng mới qua được màn chào |
| `onboarding_step_viewed` (đề xuất, chờ câu 7) | Khi một trang của luồng giới thiệu hiện lần đầu trong luồng | `step`: số thứ tự trang, từ 1 | Người dùng bỏ app ở trang nào |
| `camera_permission_result` | Khi hộp hỏi quyền của hệ thống hiện ra và trả kết quả, ở màn chào hoặc ở camera; không ghi khi quyền đã bị từ chối vĩnh viễn và hộp hỏi không hiện. Từ Android 11, chạm ra ngoài hộp hỏi ở lần hỏi đầu cũng không được ghi (plan T-026, câu hỏi 1), nên số `denied` thấp hơn thực tế; tỷ lệ cho phép trên `onboarding_complete` không đổi | `result`: `granted` hoặc `denied`; `source`: `onboarding` hoặc `camera` | Tỷ lệ cho phép camera, màn giải thích có giúp không |

## 7. File tĩnh trên R2

Không có. Hình minh họa vẽ bằng thành phần của hệ thống thiết kế, không tải file.

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_onboarding_enabled` | tắt | Bật để hiện màn chào ở lần mở đầu tiên. Bản staging bật sẵn |
| `is_show_native_onboarding` | tắt | Bật thẻ native ở luồng giới thiệu (KB-6, câu 6). Khóa chung `is_show_ads` tắt thì mọi quảng cáo tắt |

## 9. Câu hỏi còn mở

1. **Ai dịch chữ sang 7 ngôn ngữ phát hành (Anh, Hindi, Bồ Đào Nha Brazil, Tây Ban Nha Mỹ Latinh, Indonesia, Thái, Việt)?** Câu này dùng chung cho mọi spec của app.
   - Đề xuất của AI: AI dịch trong cùng ticket với chữ tiếng Anh và tiếng Việt; một người biết tiếng duyệt trước khi phát hành. Ngôn ngữ gốc trong `strings.xml` là tiếng Anh, máy đặt ngôn ngữ chưa có thì hiện tiếng Anh.
   - Trả lời: Đồng ý với đề xuất của AI.
2. **Tư thế mặc định ở lần mở camera đầu tiên là tư thế nào?** Bản đề xuất ghi "tư thế đầu của nhóm Nữ hoặc nhóm mặc định đã chọn sẵn".
   - Đề xuất của AI: tư thế đầu tiên của nhóm "Tất cả" theo thứ tự trong catalog; người làm nội dung đặt một tư thế dễ, toàn thân, một người lên đầu. Chi tiết ở spec `camera`.
   - Trả lời: Đồng ý với đề xuất của AI.
3. **Điểm "Hơn 200 tư thế" chỉ đúng khi catalog đã đủ 200 tư thế (bài học 8: chữ chỉ nói điều app đã có).**
   - Đề xuất của AI: giữ chữ này; script kiểm catalog chặn phát hành khi chưa đủ 200. Nếu phát hành sớm hơn thì đổi chữ trước.
   - Trả lời: Đồng ý với đề xuất của AI. Không cần chặn phát hành. Người tạo content sẽ phải tự tuân thủ.

4. **Luồng giới thiệu có mấy trang, mỗi trang nói gì?** Yêu cầu ghi "khoảng 3-5 màn".
   - Đề xuất của AI: 3 trang, chia lại nội dung đã có của màn chào:
     - Trang 1: "Chụp dáng đẹp, không cần đoán", đứng vào nét viền rồi chụp.
     - Trang 2: hơn 200 tư thế; lưu tư thế yêu thích.
     - Trang 3: chỉnh độ mờ, cỡ và vị trí nét viền; nút "Bật camera" và dòng ghi chú về quyền.
   - Có tư thế premium (spec poses, câu 6) thì thêm một trang giới thiệu tư thế premium, thành 4 trang.
   - Chữ mới cần dịch đủ 7 ngôn ngữ (câu 1).
   - Trả lời:
5. **Số chạm tới camera và nút bỏ qua.** Mục tiêu ở mục 1 ghi đúng 2 chạm và không gặp quảng cáo trước camera. Lý do của mục tiêu: phàn nàn số một về app mẫu là chuỗi màn và quảng cáo trước camera (bài học 1, 2). Luồng nhiều trang có quảng cáo làm trái mục tiêu này.
   - Đề xuất của AI:
     - Mục tiêu đổi thành: số trang + 1 chạm, không có quảng cáo toàn màn hình trước ảnh đầu tiên.
     - Có nút "Bỏ qua" ở góc trên của các trang trước trang cuối. Nút đưa thẳng tới trang cuối, không vào thẳng camera, để người dùng vẫn đọc ghi chú về quyền.
     - Theo dõi tỷ lệ cho phép camera và tỷ lệ giữ chân ngày 1 trước và sau bản có luồng mới.
   - Trả lời:
6. **Thẻ native ở trang nào, có trang quảng cáo toàn trang không, lúc phát hành bật hay tắt?** Khung có thẻ 250 dp đổi quảng cáo khi chuyển trang (`NativeStepAd`). Khung cũng có trang quảng cáo toàn trang chèn giữa hai trang (`NativeFullScreenStep`, một vị trí `native_fullscreen` riêng).
   - Đề xuất của AI:
     - Thẻ ở mọi trang trừ trang cuối, để trang xin quyền không có quảng cáo bên cạnh nút "Bật camera".
     - Không có trang toàn trang.
     - Vị trí tắt lúc phát hành, bật dần trên Firebase khi tỷ lệ cho phép camera và giữ chân ngày 1 không giảm quá 2 điểm, như câu 9 của spec ads.
     - Bản staging bật.
     - Ở khung thấp (`w360dp-h640dp`), thẻ 250 dp chiếm gần nửa màn: plan đo chỗ còn lại cho nội dung trang và nêu cách xử lý (trang cuộn được, hay ẩn thẻ khi màn quá thấp).
   - Trả lời:
7. **Có ghi sự kiện cho từng trang không?** Không có sự kiện thì không biết người dùng bỏ app ở trang nào. Yêu cầu không nêu sự kiện mới.
   - Đề xuất của AI: thêm `onboarding_step_viewed` (`step`), ghi khi một trang hiện lần đầu trong luồng. Sự kiện quảng cáo do khung ghi như mọi vị trí.
   - Trả lời:
8. **Đồng ý quảng cáo ở lần mở đầu tiên.** Hộp thoại đồng ý của khung hiện ở lần mở đầu, trước hay trong luồng giới thiệu. Người dùng chưa trả lời thì chưa nạp được quảng cáo, nên thẻ ở trang 1 có thể chưa có quảng cáo.
   - Đề xuất của AI: chấp nhận. Khung nạp pool khi đồng ý xong, thẻ hiện từ trang có quảng cáo. Plan kiểm thứ tự hộp thoại đồng ý, màn loading và luồng trên máy thật.
   - Trả lời:

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu (bản đề xuất đã duyệt) |
| 2026-10-22 | AI | Theo quyết định của Nhật (cùng quy tắc với spec `camera`, KB-7, và plan T-026): chỉ ghi `camera_permission_result` khi hộp hỏi quyền hiện ra; không ghi khi quyền đã bị từ chối vĩnh viễn (KB-1, KB-2, mục 6). Chờ xác nhận lại |
| 2026-10-31 | AI | Yêu cầu của Nhật (`requests/2026-10-31-luong-gioi-thieu-nhieu-trang.md`): luồng giới thiệu 3 đến 5 trang có thẻ native `native_onboarding` (mục 2, KB-5, KB-6, mục 4, 5, 6, 8; câu 4 tới 8). Chuyển `changed`, chờ trả lời và xác nhận |
