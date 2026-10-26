---
feature: camera
status: confirmed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-camera-theo-tu-the.md
design: design/README.md
created: 2026-10-21
confirmed: 2026-10-26
---

# SPEC: CAMERA CHỤP THEO TƯ THẾ VÀ MÀN XEM LẠI

## 1. Mục tiêu

- **Người dùng được gì:** mở app là chụp được theo tư thế: nét viền rõ, chỉnh được theo ý, có hướng dẫn từng bước, có điểm khớp cho biết đã đứng đúng chưa, chụp xong xem lại và lưu ngay.
- **Vì sao làm bây giờ:** đây là chức năng chính, lý do người dùng cài app. Các app mẫu bị chê vì quảng cáo che camera (bài học 3), nét viền khó nhìn (bài học 5), không có màn xem lại (bài học 6), tư thế không kèm hướng dẫn (bài học 10). Dev chốt làm nhận dạng dáng ngay từ bản đầu (bản đề xuất, câu 12.5).
- **Đo kết quả bằng gì:** tỷ lệ ảnh được lưu trên ảnh đã chụp (`photo_saved` trên `photo_captured`), mong đợi ít nhất 50%; số ảnh lưu trung bình mỗi người dùng mỗi ngày; số quảng cáo toàn màn hình mỗi 10 phút dùng, tối đa 3 (câu 9.1).

## 2. Phạm vi

**Trong phạm vi:**

- Camera là màn chính của app, luôn nền tối. Khung xem trước tỷ lệ 3:4, nút chụp tròn giữa đáy, nút đổi camera bên phải, ô ảnh vừa chụp bên trái.
- Chọn tư thế ngay trên camera: hàng chip nhóm và dải ô tư thế cuộn ngang; ô đang chọn có viền màu nhấn và dấu ✓.
- Nét viền tư thế màu trắng có quầng tối, nhìn rõ trên nền sáng và tối. Thẻ bước tạo dáng (3 đến 5 bước) trên khung xem trước.
- Chỉnh nét viền: độ mờ, cỡ, kéo, lật, khóa, ẩn, đặt lại. Lựa chọn được giữ khi đổi tư thế.
- Công cụ chụp: flash, hẹn giờ, lưới chia ba, đổi camera trước sau, zoom 0.5x, 1x, 2x khi máy hỗ trợ.
- Nhận dạng dáng người trong khung, chấm điểm khớp với tư thế từ 0 đến 100, lời nhắc chỉnh dáng.
- Màn xem lại sau khi chụp: Lưu vào thư viện, Chụp lại, Chia sẻ, Tư thế tiếp.
- Lưu ảnh vào thư mục ảnh riêng của app trong thư viện máy, không kèm nét viền, không xin quyền đọc ảnh. Android 10 trở lên không hỏi quyền nào; Android 9 trở xuống hỏi quyền lưu tệp vào bộ nhớ máy ở lần lưu đầu tiên, vì các bản này không cho lưu vào thư viện máy khi chưa có quyền đó (câu hỏi 10).
- Xin quyền camera khi chưa có, trạng thái chưa có quyền với nút mở cài đặt máy.
- Quảng cáo toàn màn hình khi rời màn xem lại sau khi đã lưu ảnh (mức B).
- Hỏi đánh giá app trong app sau khi người dùng đã lưu đủ ảnh (bản đề xuất, câu 12.8).
- Điện thoại màn hình gập, khi mở ra: khung xem trước giữ 3:4, nút chụp và công cụ dời sang cột bên.
- Điện thoại khóa chiều dọc ở mọi màn của app. Màn lớn (cạnh ngắn từ 600 dp: máy gập khi mở ra, máy tính bảng) không khóa hướng: app dùng cả màn hình theo hướng của máy, với bố cục mở gập (câu hỏi 7, phần bổ sung; câu hỏi 15).

**Ngoài phạm vi lần này:**

- Thư viện ảnh đã chụp trong app (ô ảnh vừa chụp chỉ mở màn xem lại ảnh cuối), sửa ảnh, bộ lọc màu, so sánh trước sau.
- Lưu thêm bản ảnh kèm nét viền (câu 12.11). Tự chụp khi đạt điểm khớp.
- Quay video, chụp liên tiếp, nhiều người trong một khung có điểm riêng.
- Thư viện tư thế và chi tiết tư thế (spec `poses`); cài đặt (spec `settings`); hộp thoại mất mạng (spec `network`).

## 3. Kịch bản sử dụng

### KB-1. Mở camera và chọn tư thế

Người dùng mở app (đã qua màn chào), muốn chụp theo một tư thế.

Tiêu chí chấp nhận:

- [ ] Khi vào camera thì khung xem trước chạy và nét viền của tư thế dùng lần trước hiện sẵn; lần đầu tiên là tư thế mặc định (câu hỏi 1).
- [ ] Khi vào camera thì hàng chip nhóm hiện "Tất cả" và các nhóm có tư thế; nhóm chưa có tư thế không hiện.
- [ ] Khi chạm một chip thì dải tư thế đổi theo nhóm đó; chip đang chọn có nền màu nhấn và dấu ✓.
- [ ] Khi dải tư thế hiện thì tư thế xếp theo thứ tự: tư thế đã lưu, rồi tư thế đã dùng gần đây (mới dùng nhất trước), rồi các tư thế còn lại theo thứ tự trong catalog; thứ tự này áp dụng cho "Tất cả" và cho từng nhóm (câu hỏi 1).
- [ ] Khi chạm một ô ở dải thì nét viền và thẻ bước đổi sang tư thế đó; ô đang chọn có viền màu nhấn và dấu ✓; app ghi `pose_selected`.
- [ ] Khi mở camera từ nút "Chụp với tư thế này" ở chi tiết tư thế thì camera dùng đúng tư thế đó, dải tư thế cuộn tới ô của nó.
- [ ] Khi ảnh nhỏ của một ô chưa tải xong thì ô hiện khung trống, không chặn việc chọn; tải lỗi thì ô hiện dấu lỗi, chạm lại để thử tải.

Trường hợp đặc biệt:

- [ ] Khi tư thế dùng lần trước không còn trong catalog thì camera dùng tư thế mặc định.
- [ ] Khi chưa chọn tư thế nào (camera ở trạng thái không có tư thế) thì không có nét viền, không có thẻ bước, không có công cụ chỉnh nét viền; khung xem trước hiện lời nhắc "Chọn một tư thế ở dải bên dưới".

### KB-2. Làm theo thẻ bước

Tiêu chí chấp nhận:

- [ ] Khi có tư thế thì thẻ bước hiện ở đầu khung xem trước, ghi bước hiện tại trên tổng số bước (ví dụ "2/4") và nội dung bước.
- [ ] Khi chạm mũi tên sau hoặc trước thì thẻ chuyển sang bước kế hoặc bước trước; ở bước đầu và bước cuối, mũi tên tương ứng mờ đi và không làm gì.
- [ ] Khi đổi tư thế thì thẻ bước quay về bước 1.
- [ ] Khi chạm vào thẻ bước, ở bất kỳ chỗ nào ngoài hai mũi tên, hoặc vuốt thẻ lên, thì thẻ ẩn và một nút nhỏ "Bước" hiện ở góc trên khung xem trước; bấm nút này thì thẻ hiện lại (câu hỏi 2).
- [ ] Khi thẻ bước đang ẩn thì nó ẩn cho tới khi người dùng hiện lại, kể cả khi đổi tư thế hay mở lại app.

### KB-3. Chỉnh nét viền

Tiêu chí chấp nhận:

- [ ] Khi kéo một ngón trên khung xem trước thì nét viền di chuyển theo ngón tay.
- [ ] Khi chụm hoặc mở hai ngón thì nét viền nhỏ lại hoặc to lên, trong khoảng 40% đến 160%.
- [ ] Khi bấm nút chỉnh thì bảng chỉnh hiện trên khung xem trước, có thanh trượt "Độ đậm" (20% đến 100%, 100% là nét đậm nhất) và "Cỡ" (40% đến 160%), và bốn nút Lật, Khóa, Ẩn, Đặt lại; nút chỉnh chuyển sang trạng thái đang bật. Bấm dấu đóng hoặc bấm lại nút chỉnh thì bảng đóng.
- [ ] Khi bấm Lật thì nét viền lật ngang.
- [ ] Khi bấm Khóa thì kéo và chụm không còn làm đổi nét viền; nút khóa chuyển sang trạng thái đang bật; nút chỉnh và nút lật mờ đi; một lời nhắc ngắn "Đã khóa lớp phủ" hiện trên khung xem trước. Bấm lại thì mở khóa.
- [ ] Khi bấm Ẩn (hoặc nút hiện tư thế ở cột công cụ) thì nét viền ẩn; bấm lại thì hiện. Nút hiện tư thế ở trạng thái đang bật khi nét viền đang hiện.
- [ ] Khi bấm Đặt lại thì nét viền về giữa khung, cỡ 100%, độ đậm mặc định, không lật.
- [ ] Khi đổi sang tư thế khác thì độ đậm, cỡ, lật, khóa, ẩn giữ như trước; vị trí kéo cũng giữ.
- [ ] Mỗi lần người dùng chỉnh xong một thao tác thì app ghi `overlay_adjusted` một lần (không ghi liên tục trong lúc kéo).

Trường hợp đặc biệt:

- [ ] Khi kéo nét viền ra sát mép thì luôn còn ít nhất một phần nét viền trong khung, để người dùng kéo lại được.
- [ ] Khi nét viền to hơn khung hay bị kéo ra một phần ngoài khung thì phần ngoài khung không được vẽ: nét viền không đè lên thanh trên, hàng nhóm, dải tư thế hay hàng nút chụp (FB-20261026-03).

### KB-4. Dùng công cụ chụp

Tiêu chí chấp nhận:

- [ ] Khi bấm nút flash thì flash đổi lần lượt: tắt, bật, tự động (mặc định tắt, câu hỏi 3); biểu tượng cho biết chế độ hiện tại. Camera trước không có flash thì nút mờ đi.
- [ ] Khi bấm nút hẹn giờ thì hẹn giờ đổi lần lượt: tắt, 3 giây, 10 giây; khi bật, nút ở trạng thái đang bật và hiện số giây ("3s", "10s").
- [ ] Khi bấm nút lưới thì lưới chia ba hiện hoặc ẩn trên khung xem trước; nút ở trạng thái đang bật khi lưới hiện.
- [ ] Khi bấm nút đổi camera thì camera đổi giữa trước và sau; nét viền và tư thế giữ nguyên.
- [ ] Khi máy có ống kính góc rộng thì nút zoom có 0.5x, 1x, 2x; không có thì chỉ có 1x và 2x. Mức đang chọn có nền màu nhấn.
- [ ] Khi mở lại app thì hẹn giờ, lưới, flash, camera trước sau giữ như lần trước; zoom về 1x.

### KB-5. Nhận dạng dáng và điểm khớp

Tư thế có dữ liệu điểm khớp và có người đứng trong khung.

Tiêu chí chấp nhận:

- [ ] Khi không có người trong khung (độ tin cậy dưới ngưỡng) thì app không hiện điểm; hiện lời nhắc "Lùi lại để cả người vào khung".
- [ ] Khi có người trong khung thì app hiện điểm khớp từ 0 đến 100 và một lời nhắc chỉnh dáng ngắn (ví dụ "Nâng tay trái cao hơn").
- [ ] Khi điểm đạt ngưỡng (mặc định 80, đặt trong `RemoteSettings`, câu hỏi 4) thì lời nhắc đổi thành "Đúng dáng rồi, bấm chụp!", nét viền và viền nút chụp đổi sang màu báo đạt.
- [ ] Khi một người thật đứng đúng tư thế thì điểm đạt ít nhất 80 (kiểm tra thủ công trên máy thật, ghi trong báo cáo phiên thử).
- [ ] Khi đo trên một máy tầm thấp (câu hỏi 5) thì camera vẫn chạy ít nhất 15 khung hình mỗi giây khi bật nhận dạng.
- [ ] Khi tư thế không có dữ liệu điểm khớp, hoặc nét viền đang ẩn, thì app không chạy nhận dạng và không hiện điểm.

Trường hợp đặc biệt:

- [ ] Khi khung chỉ có trần nhà, tường hay đồ vật thì app không hiện điểm (PoseMate báo nhầm "35% Come closer" khi không có ai, bản đề xuất bài học 8).

### KB-6. Chụp và xem lại

Tiêu chí chấp nhận:

- [ ] Khi bấm nút chụp mà hẹn giờ tắt thì app chụp ngay (có âm chụp nếu bật trong Cài đặt), ghi `photo_captured`, mở màn xem lại với ảnh vừa chụp, không có nét viền.
- [ ] Khi bấm nút chụp mà hẹn giờ bật thì số đếm ngược hiện to giữa khung xem trước, nút chụp đổi thành nút hủy; hết giờ thì chụp như trên; bấm nút hủy thì dừng đếm, không chụp.
- [ ] Khi đang đếm ngược thì flash, lưới, hẹn giờ, hàng nhóm và dải tư thế mờ đi và không phản hồi; cột công cụ nét viền và mũi tên thẻ bước mờ đi; bảng chỉnh đang mở thì đóng; kéo và chụm không đổi nét viền. Zoom vẫn dùng được (câu hỏi 16).
- [ ] Khi đang đếm ngược mà chạm vào khung xem trước (ở bất kỳ chỗ nào ngoài nút zoom), đổi camera, hay rời màn camera (thư viện, cài đặt, ô ảnh vừa chụp) thì đếm dừng, không chụp; đổi camera và rời màn vẫn làm tiếp việc của nút.
- [ ] Khi bấm "Lưu vào thư viện" thì nút hiện "Đang lưu…", các nút khác mờ đi; lưu xong ảnh có trong thư viện máy, ở thư mục ảnh riêng của app; app ghi `photo_saved`.
- [ ] Khi bấm "Chụp lại" sau khi đã lưu ảnh thì app quay về camera.
- [ ] Khi rời màn xem lại mà chưa lưu ảnh (bằng "Chụp lại", "Tư thế tiếp" hay nút quay lại) thì hộp thoại "Bỏ ảnh này?" hiện, ghi rằng ảnh chưa được lưu vào thư viện, có hai nút "Bỏ ảnh" và "Lưu ảnh" (câu hỏi 6). Bấm "Bỏ ảnh" thì làm tiếp việc người dùng vừa chọn, ảnh không được lưu; bấm "Lưu ảnh" thì lưu như trên rồi làm tiếp; chạm ra ngoài hay bấm quay lại thì đóng hộp thoại, vẫn ở màn xem lại.
- [ ] Khi bấm "Chia sẻ" thì hộp chia sẻ của hệ thống hiện với ảnh vừa chụp.
- [ ] Khi bấm "Tư thế tiếp" thì app quay về camera với tư thế kế tiếp trong dải đang chọn (hết dải thì về tư thế đầu).
- [ ] Khi quay về camera từ màn xem lại thì tư thế, chỉnh nét viền, công cụ chụp giữ như trước khi chụp.
- [ ] Khi chạm ô ảnh vừa chụp ở camera thì màn xem lại mở với ảnh đã lưu gần nhất; chưa có ảnh nào thì ô trống và không làm gì.

Trường hợp đặc biệt:

- [ ] Khi lưu lỗi vì hết bộ nhớ thì màn xem lại hiện thông báo "Không lưu được ảnh: bộ nhớ máy đã đầy." kèm nút "Thử lại"; ảnh vẫn còn trên màn xem lại.
- [ ] Khi lưu ảnh trên Android 10 trở lên thì app không hỏi quyền nào.
- [ ] Khi bấm lưu lần đầu trên Android 9 trở xuống mà app chưa có quyền lưu tệp vào bộ nhớ máy thì hộp hỏi quyền của hệ thống hiện; cho phép thì ảnh được lưu như trên, các lần lưu sau không hỏi lại.
- [ ] Khi người dùng từ chối quyền lưu thì ảnh không được lưu, ảnh vẫn còn trên màn xem lại, và màn xem lại hiện thông báo cần quyền lưu ảnh kèm nút "Thử lại"; bấm "Thử lại" thì hộp hỏi quyền hiện lại (chữ của thông báo ở câu hỏi 10).
- [ ] Khi người dùng đã từ chối và chọn không hỏi lại thì bấm lưu không hiện hộp hỏi; màn xem lại hiện thông báo hướng dẫn cho phép trong Cài đặt máy kèm nút "Mở cài đặt"; bấm thì trang cài đặt của app mở ra; cho phép rồi quay lại thì thông báo ẩn và bấm lưu là lưu được.
- [ ] Khi từ chối quyền lưu sau khi bấm "Lưu ảnh" ở hộp thoại "Bỏ ảnh này?" thì người dùng ở lại màn xem lại với thông báo như trên, ảnh không bị bỏ.
- [ ] Khi kiểm tra danh sách quyền của app thì không có quyền đọc ảnh (bài học 13); quyền lưu tệp vào bộ nhớ máy chỉ được khai báo cho Android 9 trở xuống. Lưu ý: trên Android 9 trở xuống, hệ thống tự cho app đọc bộ nhớ máy khi đã có quyền lưu, và hộp hỏi quyền ghi chung "ảnh, nội dung đa phương tiện và tệp"; app không đọc ảnh nào ngoài ảnh do chính app lưu.

### KB-7. Quyền camera

Tiêu chí chấp nhận:

- [ ] Khi chưa có quyền camera và máy còn hỏi được (ví dụ cờ màn chào tắt) thì camera hỏi quyền khi mở; app ghi `camera_permission_result`.
- [ ] Khi quyền camera đã bị từ chối vĩnh viễn (máy không còn hiện hộp hỏi) thì camera vào thẳng trạng thái chưa có quyền và app không ghi `camera_permission_result`.
- [ ] Khi bị từ chối thì khung xem trước hiện tiêu đề "Chưa có quyền dùng camera", lời giải thích và nút "Mở cài đặt"; nút chụp và nút đổi camera mờ đi; hàng chip và dải tư thế vẫn dùng được.
- [ ] Khi bấm "Mở cài đặt" thì trang cài đặt của app trong máy mở ra; người dùng cho phép rồi quay lại thì camera tự chạy, không cần mở lại app.

### KB-8. Camera đang mở và không mở được

Tiêu chí chấp nhận:

- [ ] Khi camera đang khởi động thì khung xem trước hiện vòng chờ và chữ "Đang mở camera…"; nút chụp mờ đi.
- [ ] Khi không mở được camera (ví dụ app khác đang dùng) thì khung xem trước hiện tiêu đề "Không mở được camera", lời giải thích và nút "Thử lại"; bấm thì camera thử mở lại.
- [ ] Khi app ra nền rồi quay lại thì camera tự chạy lại, giữ tư thế và các lựa chọn.

### KB-9. Quảng cáo khi rời màn xem lại

Tiêu chí chấp nhận:

- [ ] Khi người dùng rời màn xem lại (bằng Chụp lại, Tư thế tiếp hay nút quay lại) sau khi đã lưu ảnh, và đủ điều kiện ở mục 5, thì một quảng cáo toàn màn hình hiện trước khi về camera.
- [ ] Khi chưa lưu ảnh nào trong lần xem lại đó thì không hiện quảng cáo.
- [ ] Khi cờ `ads_review_interstitial_enabled` tắt hoặc người dùng chưa đồng ý quảng cáo theo hộp thoại đồng ý thì không hiện quảng cáo (ADR-0013, ADR-0014).
- [ ] Khi ở camera và trong lúc xem lại thì không có quảng cáo nào trên màn hình.

### KB-10. Hỏi đánh giá app

Tiêu chí chấp nhận:

- [ ] Khi người dùng đã lưu ít nhất 3 ảnh (tính từ khi cài) và rời màn xem lại sau khi đã lưu ảnh ở lần xem lại đó thì app gọi hộp đánh giá của Google Play, nếu lần gọi trước đã cách ít nhất 30 ngày.
- [ ] Khi quảng cáo toàn màn hình vừa hiện ở cùng lần rời màn xem lại thì app không gọi hộp đánh giá lần đó.
- [ ] Khi cờ `feature_in_app_review_enabled` tắt thì app không gọi hộp đánh giá.

### KB-11. Điện thoại màn hình gập

Tiêu chí chấp nhận:

- [ ] Khi máy gập mở ra thì camera giữ khung 3:4 bên trái, nút chụp, đổi camera, ô ảnh vừa chụp và zoom dời sang cột bên phải; màn xem lại đặt ảnh bên trái, các nút bên phải, đúng thiết kế `_unfolded`.
- [ ] Khi gập lại thì camera và màn xem lại dùng bố cục điện thoại, giữ tư thế và các lựa chọn.
- [ ] Khi app mở trên màn lớn (cạnh ngắn từ 600 dp: máy gập khi mở ra, máy tính bảng) thì app dùng cả màn hình, không bị hệ thống đóng khung với hai dải đen, kể cả khi màn nằm ngang; xoay máy thì app xoay theo, mọi màn vẫn đủ nút chính trong màn hình.
- [ ] Khi gập lại (cạnh ngắn dưới 600 dp) thì app trở về chiều dọc dù máy đang nằm ngang.

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md) (gói bàn giao lập sau khi spec được xác nhận), dựng trên Figma theo hướng Sticker, section Camera và Review của trang Screens.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Camera | Chụp theo tư thế: chọn tư thế, nét viền, thẻ bước, công cụ, điểm khớp | KB-1 đến KB-8, KB-11 |
| Xem lại | Xem ảnh vừa chụp, lưu, chụp lại, chia sẻ, sang tư thế tiếp | KB-6, KB-9, KB-10, KB-11 |

Trạng thái trên thiết kế: camera `default`, `no-pose`, `adjust`, `locked`, `steps-hidden`, `matching`, `matched`, `no-person`, `countdown`, `starting`, `permission-denied`, `error`, `default` khi mở gập (hộp thoại mất mạng thuộc gói thiết kế `network`); xem lại `default`, `saving`, `save-error`, `discard`, `default` khi mở gập; hai thông báo về quyền lưu trên Android 9 trở xuống (KB-6) chưa có thiết kế riêng, dùng cùng kiểu thông báo với `save-error` (câu hỏi 10). Chỉ có bản tối (câu 12.10 của bản đề xuất).

## 5. Quảng cáo

| Vị trí | Định dạng | Khi nào hiển thị | Giới hạn tần suất | Cờ bật tắt |
| --- | --- | --- | --- | --- |
| `review_interstitial` | toàn màn hình | Khi rời màn xem lại sau khi đã lưu ảnh (điểm dừng tự nhiên) | Không trước ảnh lưu thứ 2 của phiên (phiên là một lần chạy của app, từ lúc mở tới khi hệ thống đóng tiến trình); cách lần trước ít nhất 120 giây; tối đa 1 lần mỗi 3 ảnh lưu. Các con số đặt trong `RemoteSettings` để chỉnh từ xa | `ads_review_interstitial_enabled` |

Không vị trí quảng cáo nào ở camera và trong lúc xem lại (bài học 3).

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Tư thế đang dùng | Mở lại app vẫn là tư thế đó | Đến khi người dùng xóa dữ liệu app |
| Tư thế đã dùng gần đây, kèm thời điểm dùng | Xếp dải tư thế và thư viện (đã lưu, gần đây, còn lại) | Đến khi người dùng xóa dữ liệu app |
| Độ đậm, cỡ, lật, khóa, ẩn của nét viền; thẻ bước ẩn hay hiện | Giữ lựa chọn giữa các lần dùng | Đến khi người dùng xóa dữ liệu app |
| Hẹn giờ, lưới, flash, camera trước sau | Giữ lựa chọn giữa các lần dùng | Đến khi người dùng xóa dữ liệu app |
| Số ảnh đã lưu, ngày gọi hộp đánh giá gần nhất | Điều kiện hỏi đánh giá | Đến khi người dùng xóa dữ liệu app |
| Mốc quảng cáo của phiên (số ảnh đã lưu trong phiên, lần hiện quảng cáo gần nhất) | Giới hạn quảng cáo | Trong bộ nhớ, hết khi app đóng; không lưu xuống máy |
| Ảnh đã lưu | Ảnh của người dùng, trong thư viện máy | Đến khi người dùng tự xóa; gỡ app không xóa |

Ảnh chỉ lưu trên máy, không tải lên đâu (câu 9.3). Phân tích dáng chạy hoàn toàn trên máy.

**Quyền của app:** camera (KB-7); lưu tệp vào bộ nhớ máy, chỉ trên Android 9 trở xuống và chỉ hỏi khi lưu ảnh (KB-6). Không có quyền đọc ảnh. Không có sự kiện phân tích cho kết quả hỏi quyền lưu (câu hỏi 11). Trước khi phát hành, dev cập nhật khai báo quyền và mục An toàn dữ liệu (Data safety) trên Play Console cho quyền lưu tệp (câu hỏi 10).

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `camera_permission_result` | Khi hộp hỏi quyền hiện ra và trả kết quả ở camera; không ghi khi quyền đã bị từ chối vĩnh viễn và hộp hỏi không hiện | `result`: `granted`, `denied`; `source`: `camera` | Tỷ lệ cho phép camera |
| `pose_selected` | Khi chọn tư thế ở dải, hoặc mở camera từ chi tiết tư thế | `pose_id`, `category`, `source`: `strip`, `detail`, `next` | Tư thế và nhóm nào được dùng nhiều, cần làm thêm |
| `overlay_adjusted` | Khi xong một thao tác chỉnh nét viền | `type`: `opacity`, `size`, `move`, `flip`, `lock`, `hide`, `reset` | Người dùng có cần các công cụ chỉnh không (bài học 5) |
| `photo_captured` | Khi chụp xong một ảnh | `pose_id`, `timer`: `off`, `3`, `10`; `matched`: có hay không đạt ngưỡng lúc chụp | Bao nhiêu ảnh chụp mỗi phiên; điểm khớp có giúp không |
| `photo_saved` | Khi lưu ảnh thành công | `pose_id` | Tỷ lệ lưu trên chụp |
| `content_load_failed` | Khi tải ảnh tư thế ở dải thất bại | `content`: `thumbnail`, `outline`; `reason`: `network`, `not_found`, `other` | Lỗi tải nội dung có nhiều không (bài học 7) |

## 7. File tĩnh trên R2

| Nhóm file | Dung lượng ước tính | Khi nào tải | Khi không tải được |
| --- | --- | --- | --- |
| Ảnh nhỏ của tư thế (`preview_540.webp`) | 30 đến 60 KB mỗi ảnh | Bộ khởi đầu tải ở màn loading lần đầu (spec `loading`); tư thế khác tải khi ô hiện ở dải | Ô hiện dấu lỗi, chạm để thử lại; mất mạng thì hộp thoại của spec `network` |
| Nét viền (`outline.webp`) | 20 đến 80 KB mỗi tư thế | Như trên; nét viền tư thế đang chọn tải trước | Không hiện nét viền, thẻ bước báo "Chưa tải được nét viền" kèm "Thử lại" |

Đường dẫn và cách lưu theo bản đề xuất, mục 11 (ADR-0015). Dữ liệu tư thế lấy qua module dùng chung `:core:poses` (câu hỏi 8).

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_camera_enabled` | tắt | Bật để dùng camera làm màn chính. Bản staging bật sẵn |
| `feature_pose_matching_enabled` | tắt | Bật nhận dạng dáng và điểm khớp; tắt từ xa nếu máy chậm hay báo sai nhiều |
| `feature_in_app_review_enabled` | tắt | Bật hỏi đánh giá app trong app |
| `ads_review_interstitial_enabled` | tắt | Bật quảng cáo `review_interstitial` (ADR-0014) |

## 9. Câu hỏi còn mở

1. **Tư thế mặc định ở lần dùng đầu tiên?**
   - Đề xuất của AI: tư thế đầu tiên của nhóm "Tất cả" theo thứ tự trong catalog. Người làm nội dung đặt lên đầu một tư thế một người, toàn thân, dễ làm.
   - Trả lời: Tư thế đầu tiền của nhóm "Tất cả" theo thứ tự trong catalog. Tuy nhiên, sau này catalog sẽ được sắp xếp theo thứ tự: đầu tiên là các tư thế đã lưu, thứ hai là các tư thế đã sử dụng gần đây nhất, sau đó mới đến các tư thế còn lại.
2. **Ẩn hiện thẻ bước bằng cách nào?** Bài học 10 cần thẻ bước bật tắt được, nhưng thiết kế chưa có nút riêng.
   - Đề xuất của AI: vuốt thẻ bước lên để ẩn; khi ẩn, một nút nhỏ "Bước" hiện ở góc trên khung để hiện lại. Cần thêm vào thiết kế.
   - Trả lời: Đồng ý với đề xuất của AI.
   - **Bổ sung 2026-10-26 (FB-20261026-02, ticket T-059):** dùng trên máy thật thấy vuốt khó tìm, thẻ che khung xem trước. Tuấn chọn: không thêm nút thu gọn; chạm vào thẻ (ngoài hai mũi tên) cũng ẩn thẻ, vẫn giữ vuốt lên.
3. **Flash có những chế độ nào?**
   - Đề xuất của AI: tắt, bật, tự động; mặc định tắt.
   - Trả lời: Đồng ý với đề xuất của AI.
4. **Ngưỡng điểm báo "Đúng dáng rồi"?**
   - Đề xuất của AI: 80, đặt trong `RemoteSettings` để chỉnh từ xa sau khi thử với người thật.
   - Trả lời: Đồng ý với đề xuất của AI.
5. **Máy tầm thấp dùng để đo 15 khung hình mỗi giây là máy nào?**
   - Đề xuất của AI: một máy Android Go hoặc máy giá rẻ phổ biến ở Ấn Độ, Indonesia (ví dụ dòng Redmi A hoặc Samsung Galaxy A0x); dev chọn máy nhóm có sẵn.
   - Trả lời: Đồng ý với đề xuất của AI.
6. **Rời màn xem lại mà chưa lưu ảnh thì sao?**
   - Đề xuất của AI: bỏ ảnh, không hỏi lại, vì nút "Lưu vào thư viện" luôn thấy rõ và người dùng chủ động bấm "Chụp lại". Nếu muốn an toàn hơn: hỏi "Bỏ ảnh này?" khi rời mà chưa lưu.
   - Trả lời: Đồng ý với đề xuất của AI. Hỏi trước khi rời màn hình.
7. **Khóa màn hình ở chiều dọc?**
   - Đề xuất của AI: điện thoại khóa chiều dọc ở camera và màn xem lại, như mọi app mẫu; máy gập khi mở ra dùng bố cục `_unfolded`. Các màn khác của app cũng khóa dọc để đơn giản.
   - Trả lời: Đồng ý với đề xuất của AI.
   - **Bổ sung 2026-10-26 (FB-20261024-01, plan T-056):** khóa dọc trên màn lớn làm Android 12L đến 15 đóng khung app (hai dải đen) khi màn nằm ngang, ví dụ màn trong nằm ngang của máy gập hay máy tính bảng; từ Android 16 hệ thống bỏ qua khóa này trên màn từ 600 dp. Đề xuất của AI: chỉ khóa dọc khi cạnh ngắn của màn dưới 600 dp; màn lớn theo hướng của máy (mục 2, KB-11).
   - Trả lời (bổ sung): Tuấn, 2026-10-26: đồng ý đề xuất.
8. **Dữ liệu tư thế dùng chung đặt ở đâu?** Camera và thư viện đều cần danh sách tư thế, ảnh, nét viền, danh sách đã lưu; feature không được phụ thuộc nhau (ADR-0001).
   - Đề xuất của AI: module core mới của app `:core:poses` (bản đề xuất, mục 5). Thêm module core là thay đổi rủi ro cao, cần plan duyệt chéo.
   - Trả lời: Đồng ý với đề xuất của AI.
9. **Thư viện CameraX, ML Kit Pose Detection, Play In-App Review và quyền `CAMERA`:** bản đề xuất (câu 12.6) ghi dev thêm ở bước khởi tạo app, không qua phiên tự chạy.
   - Đề xuất của AI: các ticket của spec này phụ thuộc vào việc dev đã thêm xong; AI ghi rõ phụ thuộc trong ticket.
   - Trả lời: Đồng ý. Dev sẽ thêm trước phiên tự chạy.
10. **Lưu ảnh trên Android 8 và 9 cần quyền lưu tệp vào bộ nhớ máy.** App chạy từ Android 8. Từ Android 10, app thêm ảnh vào thư viện máy không cần quyền; Android 8 và 9 chỉ cho làm vậy khi có quyền lưu tệp vào bộ nhớ máy. Nhật chọn khai báo quyền này chỉ cho Android 9 trở xuống và hỏi ở lần lưu đầu (plan T-016, câu 1, phương án A, 2026-10-22).
   - Việc của dev trước khi phát hành (không phải việc của AI): cập nhật khai báo quyền và mục An toàn dữ liệu (Data safety) trên Play Console; kiểm tra trang quyền trên Play hiển thị quyền lưu tệp (và quyền đọc bộ nhớ đi kèm trên Android 9 trở xuống).
   - Chữ của hai thông báo chưa có trong bảng chữ của thiết kế. Đề xuất của AI: từ chối thì "Cần quyền lưu ảnh để lưu vào thư viện." kèm "Thử lại"; từ chối và không hỏi lại thì "Cho phép lưu ảnh trong Cài đặt máy để lưu vào thư viện." kèm "Mở cài đặt". Hai thông báo dùng cùng kiểu với thông báo lưu lỗi của `ReviewScreen_save-error_dark`.
   - Trả lời: Đồng ý với đề xuất của AI.
11. **Có ghi sự kiện cho kết quả hỏi quyền lưu không?** Spec không có sự kiện này. Đề xuất của AI: không ghi; tỷ lệ máy Android 9 trở xuống từ chối quyền lưu xem gián tiếp qua `photo_saved` trên `photo_captured` theo phiên bản Android. Nếu cần đo trực tiếp thì thêm sự kiện mới vào mục 6 trước (điểm chặn 4).
   - Trả lời: Đồng ý với đề xuất của AI.

12. **Dung lượng APK khi thêm nhận dạng dáng** (plan T-017, câu 1). Tiêu chí "dung lượng APK tăng không quá 10 MB" không đạt được: đo với `pose-detection` 18.0.0-beta5 trên một máy `arm64-v8a`, bản tải về tăng khoảng 12 MB, bản cài tăng khoảng 28.5 MB (thư viện gốc 21.6 MB, hai mô hình 5.8 MB).
   - Đề xuất của AI: nâng tiêu chí thành "tải về tăng không quá 15 MB trên một kiến trúc".
   - Trả lời: Bỏ tiêu chí dung lượng (phương án C); ghi số đo ở đây. Phiên vẫn đo dung lượng APK và ghi vào báo cáo phiên.
13. **"Phiên" trong giới hạn quảng cáo `review_interstitial`** (plan T-018, câu 1). Mục 5 ghi "của phiên", bảng dữ liệu mục 6 ghi mốc giữ tới khi xóa dữ liệu app.
   - Đề xuất của AI: phiên là một lần chạy của app; mốc nằm trong bộ nhớ, không lưu xuống máy.
   - Trả lời: Đồng ý với đề xuất của AI (phương án A).
14. **Rời màn xem lại mà lần đó không lưu ảnh có hỏi đánh giá không?** (plan T-019, câu 1).
   - Đề xuất của AI: chỉ hỏi khi lần xem lại đó đã lưu ảnh.
   - Trả lời: Đồng ý với đề xuất của AI.
15. **Camera khi màn lớn nằm ngang** (plan T-056, câu 1). Khung xem trước giữ 3:4 dọc; khi màn nằm ngang, luồng camera là 4:3 ngang nên khung chỉ hiện phần giữa, còn ảnh chụp ra rộng hơn phần đã thấy; nét viền và điểm khớp tính theo cả ảnh phân tích nên lệch so với người trong khung. Android 16 trên màn lớn đã như vậy, vì không khóa được hướng ở đó.
   - Đề xuất của AI: chấp nhận ở T-056, vì bố cục mở gập khi nằm ngang vẫn đủ nút trong màn hình (số đo ở plan T-056, mục 4.1); làm "ảnh chụp, nét viền và điểm khớp đúng phần đã thấy" (`ViewPort` của CameraX) ở một ticket riêng, cần chạy thử trên máy có camera.
   - Trả lời: Tuấn, 2026-10-26: chấp nhận tạm ở T-056; ảnh, nét viền và điểm khớp đúng phần đã thấy làm ở ticket ViewPort riêng.
16. **Các nút khác khi đang đếm ngược** (FB-20261026-04, ticket T-061). KB-6 chỉ nói nút chụp thành nút hủy; mọi nút khác vẫn bấm được và đổi được ảnh sắp chụp.
   - Đề xuất của AI: như các app camera phổ biến, khóa các nút đổi ảnh sắp chụp (flash, lưới, hẹn giờ, tư thế, nét viền); chạm khung xem trước, đổi camera hay rời màn thì dừng đếm; zoom vẫn dùng được.
   - Trả lời: Tuấn, 2026-10-26: đồng ý đề xuất.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu (bản đề xuất đã duyệt) |
| 2026-10-21 | AI | Ghi các câu trả lời của anh Nhật vào nội dung: thứ tự tư thế ở dải (đã lưu, gần đây, còn lại), ẩn hiện thẻ bước, chế độ flash, ngưỡng điểm 80, hỏi "Bỏ ảnh này?" khi rời màn xem lại mà chưa lưu, khóa chiều dọc, dữ liệu tư thế đã dùng gần đây; thêm trạng thái thiết kế `steps-hidden`, `discard` |
| 2026-10-21 | AI | Lập gói bàn giao thiết kế: trạng thái `offline` chuyển sang gói `network` vì hộp thoại do vỏ app hiện, không thuộc `CameraScreen` |
| 2026-10-22 | AI | Ghi quyết định của Nhật sau RUN-20261022-01: nút đổi camera mờ đi khi camera chưa sẵn sàng (đang mở, chưa có quyền, lỗi), thiết kế sửa theo; không ghi `camera_permission_result` khi quyền đã bị từ chối vĩnh viễn và hộp hỏi không hiện (KB-7, mục 6). Tuấn xác nhận lại ngày 2026-10-23 |
| 2026-10-22 | AI | Ghi quyết định của Nhật ở plan T-016 (câu 1, phương án A): Android 9 trở xuống hỏi quyền lưu tệp vào bộ nhớ máy ở lần lưu đầu; thêm tiêu chí từ chối, từ chối và không hỏi lại, quyền của app (mục 2, KB-6, mục 4, mục 6); câu hỏi 10 (chữ thông báo, việc của dev trên Play Console) và 11 (sự kiện). Tuấn xác nhận lại ngày 2026-10-23 |
| 2026-10-23 | AI | Ghi quyết định của Tuấn ở plan T-017, T-018, T-019: bỏ tiêu chí dung lượng APK (KB-5, câu 12); phiên của giới hạn quảng cáo là một lần chạy của app, mốc quảng cáo không lưu xuống máy (mục 5, mục 6, câu 13); chỉ hỏi đánh giá khi lần xem lại đó đã lưu ảnh (KB-10, câu 14). Tuấn xác nhận lại ngày 2026-10-23 |
| 2026-10-26 | AI | Đề xuất theo FB-20261024-01 (plan T-056): màn lớn (cạnh ngắn từ 600 dp) không khóa dọc, app dùng cả màn hình theo hướng của máy; điện thoại vẫn khóa dọc (mục 2, KB-11 thêm hai tiêu chí, câu 7 phần bổ sung, câu 15). Chờ người phụ trách sản phẩm xác nhận |
| 2026-10-26 | Tuấn | Xác nhận phần đổi của plan T-056 (mục 2, KB-11, câu 7 phần bổ sung, câu 15). Chuyển lại `confirmed` |
| 2026-10-26 | Tuấn | Đổi nhãn của thanh trượt độ đậm nét viền thành "Độ đậm" (KB-3, bảng dữ liệu lưu trên máy), vì nhãn cũ đọc ngược nghĩa khi đi cùng phần trăm (L-077, plan T-055); thiết kế Figma sửa cùng lúc. Không đổi hành vi. Giữ `confirmed` |
| 2026-10-26 | AI | Đề xuất theo phản hồi dùng trên máy thật của Tuấn: chạm thẻ bước để ẩn (KB-2, câu hỏi 2 phần bổ sung, FB-20261026-02); nét viền không vẽ ra ngoài khung (KB-3, FB-20261026-03); khóa nút và dừng đếm khi đang đếm ngược (KB-6, câu hỏi 16, FB-20261026-04); KB-3 sửa chữ "độ mờ" còn sót thành "độ đậm". Chờ người phụ trách sản phẩm xác nhận |
| 2026-10-26 | Tuấn | Xác nhận phần đổi theo FB-20261026-02 đến -04 (KB-2, KB-3, KB-6, câu hỏi 2 phần bổ sung, câu hỏi 16). Chuyển lại `confirmed` |
