# KỊCH BẢN KIỂM THỬ THỦ CÔNG TRÊN MÁY THẬT: CAMERA POSE (BẢN STAGING)

Soạn ngày 2026-10-26 cho bản `0.1.0-staging.10` (sau RUN-20261026-01), từ spec của 6 feature đã lên staging (loading, onboarding, camera, poses, settings, network) và các dòng "Kiểm tra thủ công" trong plan T-001 đến T-031, T-050, T-051. Mỗi mục ghi: điều kiện đầu, các bước, kết quả mong đợi. Đánh dấu `[x]` khi đạt; không đạt thì ghi giờ và điều thấy được vào `notes.md` của phiên thử.

Tổng thời gian ước tính: khoảng 2 giờ cho phần bắt buộc (mục 1 đến 9), thêm 30 phút cho phần tùy máy (mục 10).

## 0. Chuẩn bị

- **Bản build:** bản staging mới nhất từ Firebase App Distribution, từ `0.1.0-staging.10` trở lên. Gói `com.vglstudio.camera.pose.line.guide`. Ghi số phiên bản (Cài đặt > Phiên bản, dạng `0.1.0-staging.NN`).
- **Máy:** ghi hãng, mẫu, bản Android. Có ống kính góc rộng không. Máy có Google Play không.
- **Ghi log:** nối `adb` theo [test-sessions.md](test-sessions.md) mục 1, rồi:

  ```sh
  git pull                                         # để mã phiên không trùng
  scripts/device-session.sh start thu-tay-toan-app
  # ... thử, ghi giờ vào test-sessions/TS-.../notes.md ...
  scripts/device-session.sh stop
  ```

  Muốn trạng thái "vừa cài" thì dùng `scripts/device-session.sh reset` (xóa dữ liệu app, có ghi dấu vào log). **Không xóa dữ liệu bằng tay trong Cài đặt máy.**
- **Xem log trực tiếp** (cửa sổ terminal thứ hai): `adb logcat -v threadtime | grep ' VGL\.'`
- **Đồ cần có:** một chỗ sáng (tường trắng), một chỗ tối, một người đứng làm mẫu (mục 5), một app camera khác của máy.

### Điều đã biết, không ghi thành lỗi mới

| Hiện tượng | Lý do |
| --- | --- |
| Máy gập mở ra: app bị đóng khung, thư viện chỉ 2 cột | App khóa hướng dọc, FB-20261024-01 (`new`) |
| Không thấy quảng cáo nào, kể cả sau khi đồng ý | Bản staging dùng cấu hình Firebase giả, không nhận Remote Config, nên mọi vị trí quảng cáo đều tắt (ADR-0012, ADR-0014) |
| Không thấy hộp đánh giá của Google Play | Hộp chỉ hiện với bản cài từ Google Play; staging chỉ kiểm được bằng log (mục 6.6) |

## 1. Mở lần đầu khi không có mạng (loading KB-2)

Điều kiện: vừa `reset`, bật chế độ máy bay.

1. Mở app.
   - [ ] Màn loading hiện: logo, tên "Camera Pose: Lines and Guides", khẩu hiệu, nhãn "Đang tải dữ liệu", thanh tiến trình, phần trăm, chân trang "Ảnh chỉ lưu trên máy của bạn".
   - [ ] Trong tối đa 15 giây: tiêu đề "Không thể tải dữ liệu", lời hướng dẫn, nhãn đổi thành "Tiến độ đã dừng", thanh và phần trăm đổi màu báo lỗi, có nút "Thử lại".
   - [ ] Không có hộp thoại "Mất kết nối mạng" (màn loading báo lỗi theo cách riêng).
   - [ ] Log có `W VGL.Assets: Could not download ... (reason=NETWORK, error=UnknownHostException)`.
2. Bấm "Thử lại" khi vẫn bật chế độ máy bay.
   - [ ] Vẫn báo lỗi, thanh không lùi.
3. Tắt chế độ máy bay, chờ có mạng, bấm "Thử lại".
   - [ ] Thanh tiếp tục từ chỗ đã đạt, tăng theo từng file tới 100%, không lùi.
   - [ ] Tự chuyển sang màn chào, không cần bấm gì.

## 2. Màn chào và quyền camera (onboarding KB-1, KB-2, KB-3)

### 2.1 Cho phép camera

Điều kiện: tiếp mục 1 (đang ở màn chào lần đầu).

1. Xem màn chào.
   - [ ] Có tiêu đề "Chụp dáng đẹp, không cần đoán", 3 điểm giới thiệu, nút "Bật camera", ghi chú về quyền camera. Không có quảng cáo.
   - [ ] Hộp thoại đồng ý quảng cáo có thể phủ lên (staging coi mọi máy ở EEA): chọn **Đồng ý** lần này, ghi lại lựa chọn.
2. Bấm "Bật camera".
   - [ ] Hộp hỏi quyền camera của hệ thống hiện.
3. Chọn "Cho phép".
   - [ ] Camera chạy ngay, nét viền của tư thế mặc định đã có. Từ lúc loading xong tới đây đúng 2 chạm.
   - [ ] Log có `VGL.Onboarding` "Enable camera tapped", "Camera permission granted".
4. Đóng hẳn app (vuốt khỏi đa nhiệm), mở lại.
   - [ ] Loading xong vào thẳng camera, không có màn chào.

### 2.2 Quay lại ở màn chào

1. `reset`, mở app, chờ màn chào. Bấm Back.
   - [ ] App đóng.
2. Mở lại.
   - [ ] Màn chào hiện lại.

### 2.3 Từ chối camera

1. Ở màn chào (tiếp 2.2), bấm "Bật camera", chọn "Không cho phép".
   - [ ] Sang camera ở trạng thái chưa có quyền: tiêu đề "Chưa có quyền dùng camera", lời giải thích, nút "Mở cài đặt"; nút chụp và đổi camera mờ; hàng chip và dải tư thế vẫn chạm được.
   - [ ] Không quay lại màn chào. Log `VGL.Onboarding` "Camera permission denied".
2. Bấm Back ở camera.
   - [ ] App đóng.
3. Mở lại.
   - [ ] Vào thẳng camera (không có màn chào), hộp hỏi quyền hiện lần nữa. Từ chối.
4. Đóng hẳn, mở lại lần ba.
   - [ ] Không còn hộp hỏi quyền (đã từ chối hai lần). Log `Camera permission denied without dialog`.
5. Bấm "Mở cài đặt", cho phép Camera, bấm Back về app.
   - [ ] Camera tự chạy, không phải mở lại app.

## 3. Camera cơ bản và công cụ chụp (camera KB-4, KB-7, KB-8)

Điều kiện: đã có quyền camera.

1. Khung xem trước.
   - [ ] Hình chạy trong khung 3:4, vật tròn không bị kéo dài.
   - [ ] Xoay ngang máy: giao diện không xoay (khóa dọc).
   - [ ] Lúc mở camera thấy vòng chờ "Đang mở camera…" thoáng qua, nút chụp mờ (có thể rất nhanh, không bắt buộc thấy).
2. Flash (phòng tối, camera sau): bấm lần lượt.
   - [ ] Thứ tự tắt, bật, tự động; mặc định tắt; biểu tượng đổi theo.
   - [ ] Bật: đèn nháy khi chụp. Tự động: nháy khi tối. Tắt: không nháy.
3. Đổi camera sang camera trước.
   - [ ] Nút flash mờ. Tư thế và nét viền giữ nguyên.
4. Hẹn giờ.
   - [ ] Bấm lần lượt: tắt, 3s, 10s; nút hiện "3s", "10s" khi bật.
   - [ ] Hẹn 3s, bấm chụp: số 3, 2, 1 to giữa khung, nút chụp thành nút hủy, rồi chụp.
   - [ ] Hẹn 10s, bấm chụp, bấm hủy ở khoảng giây thứ 5: không chụp.
5. Lưới.
   - [ ] Bấm: lưới chia ba hiện, nút ở trạng thái bật; bấm lại: ẩn.
6. Zoom.
   - [ ] Máy có ống kính góc rộng: có 0.5x, 1x, 2x; không có: chỉ 1x, 2x. Ghi kết quả và tên máy.
   - [ ] Bấm 2x rồi 1x (và 0.5x): hình đổi theo; mức đang chọn có nền màu nhấn. Log không có `Zoom could not be set`.
7. Giữ lựa chọn: bật hẹn giờ 3s, lưới, flash tự động, camera trước, zoom 2x. Đóng hẳn app, mở lại.
   - [ ] Hẹn giờ, lưới, flash, camera trước giữ; zoom về 1x.
8. Ra nền, vào lại: bấm Home, chờ 10 giây, mở lại.
   - [ ] Camera tự chạy, tư thế và lựa chọn giữ.
9. Camera bị app khác giữ: mở app camera của máy, chia đôi màn hình với Camera Pose (hoặc mở app kia rồi quay lại nhanh).
   - [ ] Camera Pose hiện "Không mở được camera", lời giải thích, nút "Thử lại".
   - [ ] Đóng app kia, bấm "Thử lại": camera chạy. (Máy không tái hiện được thì ghi "không tái hiện được".)

## 4. Chọn tư thế, thẻ bước, chỉnh nét viền (camera KB-1, KB-2, KB-3)

1. Hàng chip và dải tư thế.
   - [ ] Chip "Tất cả" và các nhóm có tư thế; chạm một chip: dải đổi theo, chip có nền màu nhấn và dấu ✓.
   - [ ] Chạm một ô: nét viền và thẻ bước đổi; ô có viền màu nhấn và dấu ✓.
   - [ ] Cuộn dải tới tư thế ngoài bộ khởi đầu: ô trống rồi có ảnh.
2. Nét viền trên nền khác nhau.
   - [ ] Hướng vào tường trắng và vào chỗ tối: nét viền đọc rõ ở cả hai.
3. Thẻ bước.
   - [ ] Thẻ ở đầu khung, ghi "1/N" và nội dung bước.
   - [ ] Mũi tên sau, trước chuyển bước; ở bước đầu và cuối mũi tên tương ứng mờ, chạm không làm gì.
   - [ ] Đổi tư thế: thẻ về bước 1.
   - [ ] Vuốt thẻ lên bằng ngón tay: thẻ ẩn, nút nhỏ "Bước" hiện ở góc trên; dải và khung xem trước không nhảy. Bấm "Bước": thẻ hiện lại.
4. Kéo, chụm.
   - [ ] Kéo một ngón: nét viền theo tay, mượt.
   - [ ] Chụm, mở hai ngón: nét viền nhỏ, to trong khoảng 40% đến 160%.
   - [ ] Kéo sát mép: vẫn còn một phần nét viền trong khung để kéo lại.
   - [ ] Chạm thẻ bước, nút công cụ, nút zoom: nét viền không nhảy.
5. Bảng chỉnh.
   - [ ] Bấm nút chỉnh: bảng có "Độ mờ" (20% đến 100%), "Cỡ" (40% đến 160%), Lật, Khóa, Ẩn, Đặt lại; nút chỉnh ở trạng thái bật. Bấm dấu đóng hoặc nút chỉnh: bảng đóng.
   - [ ] Lật: nét viền lật ngang.
   - [ ] Khóa: kéo, chụm không còn tác dụng; nút chỉnh và lật mờ; hiện "Đã khóa lớp phủ". Bấm lại: mở khóa.
   - [ ] Ẩn (hoặc nút hiện tư thế ở cột công cụ): nét viền ẩn, bấm lại hiện.
   - [ ] Đặt lại: về giữa, cỡ 100%, độ mờ mặc định, không lật.
   - [ ] Đổi tư thế: độ mờ, cỡ, lật, khóa, ẩn, vị trí kéo giữ như trước.
6. Giữ khi mở lại: chọn một tư thế khác mặc định, độ mờ 40%, cỡ 130%, lật, khóa, vuốt ẩn thẻ bước. Đóng hẳn app, mở lại.
   - [ ] Cùng tư thế; bốn lựa chọn giữ; nét viền ở giữa khung; thẻ vẫn ẩn, nút "Bước" hiện.
7. Bấm Đặt lại, mở khóa để các mục sau dễ thử.

## 5. Nhận dạng dáng và điểm khớp (camera KB-5)

Cần một người đứng làm mẫu, chọn tư thế toàn thân một người.

1. Hướng camera lên trần, vào tường trống, vào bàn ghế, mỗi chỗ khoảng 30 giây.
   - [ ] Chỉ có "Lùi lại để cả người vào khung", không có số điểm.
2. Người mẫu đứng cả người trong khung, làm đúng tư thế.
   - [ ] Có điểm 0 đến 100 và lời nhắc ngắn; khi đúng: điểm từ 80, "Đúng dáng rồi, bấm chụp!", nét viền và viền nút chụp đổi màu báo đạt. Ghi điểm cao nhất đạt được.
3. Người mẫu hạ tay trái.
   - [ ] Điểm giảm, lời nhắc gọi đúng tay (ví dụ "Nâng tay trái cao hơn").
4. Đổi sang camera trước, lặp lại bước 3.
   - [ ] Lời nhắc vẫn gọi đúng tay.
5. Ẩn nét viền.
   - [ ] Không còn điểm.
6. Tốc độ: đứng trong khung 30 giây khi nhận dạng chạy.
   - [ ] Khung xem trước mượt. Ghi số trong log `VGL.Camera` "Pose analysis: <n> fps" (mong đợi từ 15).

## 6. Chụp, xem lại, lưu, chia sẻ (camera KB-6, KB-10)

1. Chụp với hẹn giờ tắt.
   - [ ] Nghe âm chụp; màn xem lại mở với ảnh vừa chụp, không có nét viền.
2. Bấm Back khi chưa lưu.
   - [ ] Hộp "Bỏ ảnh này?" ghi ảnh chưa lưu, có "Bỏ ảnh" và "Lưu ảnh".
   - [ ] Chạm ngoài hộp: hộp đóng, vẫn ở màn xem lại. Lặp lại với "Chụp lại" và "Tư thế tiếp": hộp cũng hiện.
3. Bấm "Chia sẻ" trước khi lưu, chọn một app (ví dụ Gmail, để nháp).
   - [ ] Hộp chia sẻ của hệ thống hiện; ảnh đi kèm.
4. Bấm "Lưu vào thư viện".
   - [ ] Nút hiện "Đang lưu…", nút khác mờ; không có hộp hỏi quyền (Android 10 trở lên).
   - [ ] Mở app Ảnh: có album "Camera Pose", ảnh đúng chiều, không có nét viền.
5. Chia sẻ sau khi lưu.
   - [ ] Ảnh đi kèm.
6. Bấm "Tư thế tiếp".
   - [ ] Về camera với ô kế tiếp ô lúc chụp (ô cuối thì về ô đầu); nét viền và công cụ giữ nguyên. Không có hộp "Bỏ ảnh này?" (đã lưu).
7. Ô ảnh vừa chụp ở camera.
   - [ ] Ô hiện ảnh vừa lưu; chạm: màn xem lại mở với ảnh đó.
   - [ ] Xóa ảnh đó trong app Ảnh, quay lại camera: ô trống, chạm không làm gì.
8. Chụp khi cầm máy ngang, lưu.
   - [ ] Ảnh trong app Ảnh đúng chiều.
9. Chụp, "Chụp lại", "Lưu ảnh" ở hộp "Bỏ ảnh này?".
   - [ ] Ảnh được lưu rồi mới về camera.
10. Hỏi đánh giá: sau khi đã lưu tổng cộng ít nhất 3 ảnh, chụp, lưu, rời màn xem lại.
    - [ ] Log `VGL.InAppReview` có `Review asked` (hoặc `Review failed` nếu máy không có Google Play). Lần rời tiếp theo có `Review skipped: not_due`.
11. Tiến trình bị đóng: chụp, ở màn xem lại bấm Home, chạy `adb shell am kill com.vglstudio.camera.pose.line.guide`, mở lại app.
    - [ ] Vẫn là ảnh vừa chụp, lưu được.

## 7. Thư viện và chi tiết tư thế (poses KB-1 đến KB-5)

1. Ở camera chọn một tư thế, bấm nút thư viện.
   - [ ] Thư viện mở, chip "Tất cả" chọn, lưới 2 cột, dòng dưới tiêu đề ghi số tư thế và số nhóm.
   - [ ] Thẻ có ảnh, tên đọc được (không có chuỗi kiểu `no_props`), loại, số bước, nút lưu. Thẻ của bộ khởi đầu có ảnh ngay.
   - [ ] Cuộn hết lưới: thẻ chưa có ảnh hiện khung xương rồi ảnh, lưới không giật, không xô lệch.
2. Bấm quay lại trên thanh trên, rồi mở lại thư viện và bấm Back của máy.
   - [ ] Cả hai lần về camera, còn đúng tư thế đã chọn.
3. Lọc và lưu.
   - [ ] Chạm chip một nhóm: lưới chỉ còn nhóm đó, chip có nền màu nhấn và dấu ✓.
   - [ ] Chưa lưu gì, chạm "Đã lưu": "Chưa lưu tư thế nào", lời hướng dẫn, nút "Xem tất cả tư thế" đưa về "Tất cả".
   - [ ] Lưu 2 tư thế, chạm "Đã lưu": cả hai hiện, mới lưu nhất ở đầu. Bỏ lưu một: nó biến khỏi "Đã lưu".
   - [ ] Về "Tất cả" và về camera: tư thế đã lưu đứng đầu lưới và đầu dải, rồi tới tư thế mới dùng.
   - [ ] Đóng hẳn app, mở lại: danh sách đã lưu còn.
4. Chi tiết: chọn một nhóm, cuộn xuống giữa lưới, chạm một thẻ.
   - [ ] Chi tiết có tên ở thanh trên, ảnh lớn (vòng chờ rồi ảnh), dòng loại, nhóm, khung hình, số bước, các bước đánh số, "Lỗi hay gặp", nút "Chụp với tư thế này" ghim ở đáy.
   - [ ] Cuộn hết: nút luôn thấy, không che chữ cuối.
   - [ ] Lưu ở chi tiết, quay lại: thẻ đã lưu; chip và vị trí cuộn như trước.
5. Ở chi tiết bấm "Chụp với tư thế này".
   - [ ] Camera mở với đúng tư thế đó, dải cuộn tới ô của nó.
   - [ ] Bấm Back ở camera: app đóng (không quay lại chi tiết).
6. Bánh răng ở thư viện.
   - [ ] Màn cài đặt mở; quay lại: về thư viện.

## 8. Mất mạng khi đang dùng (network KB-1 đến KB-3)

Điều kiện: đã qua loading, đang ở camera. Xem log riêng: `adb logcat -v threadtime | grep ' VGL\.Network'`.

1. Bật chế độ máy bay, đếm giờ.
   - [ ] Sau khoảng 3 giây, hộp "Mất kết nối mạng" hiện với lời hướng dẫn và nút "Thử lại"; đúng chế độ sáng, tối của máy.
2. Tắt rồi bật lại Wi-Fi trong dưới 3 giây (lúc chưa có hộp thoại).
   - [ ] Không có hộp thoại.
3. Đang có hộp thoại: chạm ra ngoài, chạm vào chỗ có nút phía sau.
   - [ ] Không có gì xảy ra, hộp vẫn còn, chỉ có một hộp.
4. Bấm "Thử lại" khi vẫn mất mạng.
   - [ ] Nút quay khoảng 2 giây, rồi hiện "Vẫn chưa có mạng"; hộp vẫn còn.
5. Bật lại mạng, không bấm gì, chờ 10 giây.
   - [ ] Hộp vẫn còn. Bấm "Thử lại": hộp đóng, dùng tiếp từ chỗ cũ.
6. Mất mạng lúc có hộp, bấm Back.
   - [ ] App ra nền như bấm Home. Mở lại khi vẫn mất mạng: hộp còn.
7. Đếm ngược bị dừng: bật hẹn giờ 10s, bấm chụp, bật chế độ máy bay ngay.
   - [ ] Hộp thoại hiện thì đếm dừng, không chụp.
8. Ảnh chưa tải ở camera: `reset`, qua loading có mạng, bật chế độ máy bay, cuộn dải tới ô chưa có ảnh.
   - [ ] Ô hiện dấu lỗi, hộp thoại hiện. Bật mạng, "Thử lại": ô tự tải lại.
9. Thư viện: chế độ máy bay, cuộn tới thẻ có ảnh chưa tải.
   - [ ] Hộp thoại hiện. Bật mạng, "Thử lại": ảnh hiện.
10. Chi tiết: chế độ máy bay, mở chi tiết một tư thế chưa xem (ảnh lớn chưa tải).
    - [ ] Hộp thoại hiện (không phải thông báo lỗi riêng). Bật mạng, "Thử lại": ảnh hiện.
11. Lưu ảnh khi mất mạng: chụp, bấm lưu, bật chế độ máy bay ngay.
    - [ ] Ảnh vẫn lưu xong, hộp thoại hiện sau đó.
12. Mở lại khi mất mạng (loading KB-3): đóng hẳn app, bật chế độ máy bay, mở lại.
    - [ ] Màn loading báo lỗi, không vào camera (cấu hình không tải được). Không có hộp thoại mạng.
    - [ ] Bật mạng, "Thử lại": tới 100% gần như ngay (catalog và bộ khởi đầu đã có), vào camera.

## 9. Cài đặt (settings KB-1 đến KB-6)

1. Bấm bánh răng ở camera.
   - [ ] Màn cài đặt mở, không có quảng cáo. Back: về camera.
2. Giao diện.
   - [ ] Bấm "Giao diện": hộp chọn Theo hệ thống, Sáng, Tối; lựa chọn hiện tại được đánh dấu; mặc định "Theo hệ thống".
   - [ ] Máy để sáng, chọn "Tối": màn đổi tối ngay, biểu tượng thanh trạng thái màu sáng, dòng "Giao diện" ghi "Tối".
   - [ ] Về camera và màn xem lại: vẫn nền tối. Mở thư viện, chi tiết: theo giao diện tối.
   - [ ] Đóng hẳn app, mở lại: vẫn tối, màn loading không chớp sáng.
   - [ ] Chọn "Theo hệ thống", ở lại màn cài đặt, kéo thanh thông báo đổi chế độ tối của máy: màn đổi màu, vẫn ở màn cài đặt.
3. Âm khi chụp.
   - [ ] Tắt "Âm khi chụp", chụp: không có tiếng của app (máy bán ở nước bắt buộc âm chụp có thể vẫn kêu, ghi lại). Bật lại: có tiếng.
   - [ ] Đóng hẳn, mở lại: lựa chọn giữ.
4. Ngôn ngữ.
   - Android 13 trở lên:
     - [ ] Bấm "Ngôn ngữ": trang ngôn ngữ của app trong Cài đặt máy mở, có 7 ngôn ngữ.
     - [ ] Chọn English, quay lại: màn cài đặt tiếng Anh, dòng ghi "English". Các màn khác kiểm ở mục 9.7.
     - [ ] Chọn "Tiếng Việt", quay lại: dòng ghi "Tiếng Việt".
   - Android 12 trở xuống:
     - [ ] Bấm "Ngôn ngữ": hộp chọn trong app có "Theo hệ thống" và 7 ngôn ngữ, mỗi tên viết bằng chính ngôn ngữ đó.
     - [ ] Chọn "ไทย": app dựng lại, vẫn ở màn cài đặt, chữ tiếng Thái. Đóng hẳn, mở lại: vẫn tiếng Thái. Chọn "Theo hệ thống": về ngôn ngữ máy.
5. Quyền riêng tư.
   - [ ] Dòng "Tùy chọn quyền riêng tư" hiện; bấm: hộp thoại đồng ý quảng cáo hiện lại. Đổi sang **Từ chối**, đóng: về màn cài đặt.
   - [ ] Đóng hẳn, mở lại: hộp thoại đồng ý không tự hiện lại.
   - [ ] Bấm "Chính sách quyền riêng tư": trình duyệt mở `https://studio.vgl.io.vn/privacy-policy`, trang tải được.
6. Phiên bản.
   - [ ] Dòng "Phiên bản" ghi đúng `0.1.0-staging.NN` của bản đang cài; không bấm được, không có mũi tên.

7. Chữ của mọi màn theo ngôn ngữ đã chọn (T-050, T-051). Lặp lại với English, Português, हिन्दी, rồi Tiếng Việt (đổi như bước 4):
   - [ ] Camera: chip "Tất cả", nút "Bước", thẻ bước, bảng chỉnh (Độ mờ, Cỡ, Lật, Khóa, Ẩn, Đặt lại), lời nhắc điểm khớp, trạng thái chưa có quyền hay không mở được camera (nếu gặp).
   - [ ] Màn xem lại: "Lưu vào thư viện", "Chụp lại", "Tư thế tiếp", "Chia sẻ", hộp "Bỏ ảnh này?".
   - [ ] Thư viện: tiêu đề, dòng số tư thế và số nhóm, chip "Đã lưu", màn chưa lưu tư thế nào; chi tiết: nhãn các khối và nút "Chụp với tư thế này".
   - [ ] Hộp thoại mất mạng (bật chế độ máy bay ở camera).
   - [ ] Màn loading, màn chào (sau `reset`).
   - [ ] Không còn chữ tiếng Việt lẫn vào khi đã chọn ngôn ngữ khác; tên và bước của tư thế theo catalog, có thể chưa dịch hết.
   - [ ] Ở हिन्दी và Português: chữ không bị cắt hay tràn ở chip, nút, thẻ bước, nhất là "Próxima pose" ở màn xem lại trên máy màn nhỏ.
   - Bản dịch do AI làm: thấy câu sai nghĩa hay gượng thì ghi lại, người biết tiếng sẽ duyệt trước khi phát hành.

## 10. Phần tùy máy và trợ năng

Làm phần nào máy có điều kiện; không làm được thì ghi "không có máy".

### 10.1 Màn nhỏ, chữ lớn

1. Đặt cỡ chữ và cỡ hiển thị của máy lớn nhất, `reset`, mở app.
   - [ ] Màn loading (cả trạng thái lỗi ở mục 1): nút "Thử lại" không bị cắt, chân trang không đè chữ.
   - [ ] Màn chào: phần trên cuộn được, nút "Bật camera" và ghi chú luôn thấy.
   - [ ] Chi tiết tư thế: cuộn được, nút "Chụp với tư thế này" luôn thấy.

### 10.2 Xoay máy lúc loading

1. `reset`, mở app, xoay máy khi thanh đang tăng (bật xoay tự động).
   - [ ] Thanh không về 0, không chờ lại từ đầu.

### 10.3 TalkBack

1. Bật TalkBack, ở camera.
   - [ ] Bốn nút công cụ đọc đúng tên và trạng thái; nút thư viện, cài đặt, flash, đổi camera không đọc "chưa chọn".
   - [ ] Thanh trượt đọc "Độ mờ, 70%", chỉnh được bằng cử chỉ tăng, giảm.
   - [ ] Thẻ bước có hành động "Ẩn thẻ bước"; chip và ô đọc đúng trạng thái chọn.
2. Mất mạng khi bật TalkBack.
   - [ ] Chỉ đọc và lướt được trong hộp thoại; chạm vùng phía sau không bấm được gì.

### 10.4 Máy gập (máy thật hoặc emulator Pixel Fold)

1. Ở màn ngoài: camera chọn tư thế, kéo nét viền, bật lưới và hẹn giờ. Mở máy.
   - [ ] Cùng tư thế, cùng vị trí nét viền, camera chạy lại, lựa chọn giữ. (Bố cục bị đóng khung: điều đã biết, FB-20261024-01.)
2. Chụp, ở màn xem lại chưa lưu, gập lại.
   - [ ] Ảnh còn; rời màn xem lại vẫn hỏi "Bỏ ảnh này?".
3. Thư viện: chọn chip, cuộn xuống, gập, mở.
   - [ ] Chip và vị trí cuộn giữ.

### 10.5 Android 8 hoặc 9 (lưu ảnh cần quyền)

1. Cài mới, chụp, bấm lưu.
   - [ ] Hộp hỏi quyền lưu hiện. "Từ chối": thông báo cần quyền lưu kèm "Thử lại", ảnh còn.
   - [ ] "Thử lại", chọn không hỏi lại rồi từ chối: thông báo kèm "Mở cài đặt". Mở cài đặt, cho phép, quay lại: thông báo ẩn; bấm lưu: lưu được.
   - [ ] Chụp tiếp, lưu: không hỏi quyền nữa. Log `VGL.Review` có "Asking for the storage permission", "Storage permission denied", "Storage permission blocked".
2. Trên Android 11 trở xuống: nét viền vẫn có quầng, đọc rõ trên tường trắng.

### 10.6 Quyền của app (Android 10 trở lên)

- [ ] Cài đặt máy > Ứng dụng > Camera Pose > Quyền: chỉ có Camera; không có quyền bộ nhớ, ảnh hay vị trí.

## 11. Không kiểm được trên staging

| Tiêu chí | Lý do | Cách kiểm thay |
| --- | --- | --- |
| Quảng cáo toàn màn hình khi rời màn xem lại, banner ở thư viện và chi tiết (camera KB-9, poses KB-6) | Staging không nhận Remote Config, khóa `ads_*_enabled` luôn tắt | Bản build nhận Remote Config thật; theo plan T-018 mục 4 và T-023 mục 4 |
| Hộp đánh giá thật (camera KB-10) | Chỉ hiện với bản cài từ Google Play | Internal app sharing; trên staging chỉ kiểm log (mục 6.10) |
| Tắt từ xa loading, màn chào, hộp thoại mạng, điểm khớp | Cờ bật sẵn ở bản staging | Unit test; kiểm trên production khi bật cờ |
| Hết bộ nhớ khi lưu | Khó tạo trên máy thật | Emulator bộ nhớ nhỏ (plan T-016) |
| Dòng "Tùy chọn quyền riêng tư" ẩn ở Việt Nam | Staging ép vùng EEA | Bản production đầu tiên |

## 12. Sau khi thử

1. `scripts/device-session.sh stop`, xem `summary.md` (crash, ANR).
2. Trong `notes.md`: tên máy, bản Android, số phiên bản app, danh sách mục không đạt kèm giờ, mục bỏ qua kèm lý do.
3. Nhờ AI: `Phân tích phiên thử test-sessions/TS-YYYYMMDD-NN`. AI viết báo cáo và phản hồi `FB-...` theo [docs/CLAUDE.md](../CLAUDE.md), mục Khi phân tích một phiên thử.
