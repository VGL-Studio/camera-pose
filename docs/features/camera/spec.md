---
feature: camera
status: confirmed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-camera-theo-tu-the.md
design: design/README.md
created: 2026-10-21
confirmed: 2026-10-21
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
- Lưu ảnh vào thư mục ảnh riêng của app trong thư viện máy, không kèm nét viền, không cần quyền đọc ảnh.
- Xin quyền camera khi chưa có, trạng thái chưa có quyền với nút mở cài đặt máy.
- Quảng cáo toàn màn hình khi rời màn xem lại sau khi đã lưu ảnh (mức B).
- Hỏi đánh giá app trong app sau khi người dùng đã lưu đủ ảnh (bản đề xuất, câu 12.8).
- Điện thoại màn hình gập, khi mở ra: khung xem trước giữ 3:4, nút chụp và công cụ dời sang cột bên.
- Điện thoại khóa chiều dọc ở mọi màn của app; máy gập khi mở ra dùng bố cục mở gập (câu hỏi 7).

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
- [ ] Khi vuốt thẻ bước lên thì thẻ ẩn và một nút nhỏ "Bước" hiện ở góc trên khung xem trước; bấm nút này thì thẻ hiện lại (câu hỏi 2).
- [ ] Khi thẻ bước đang ẩn thì nó ẩn cho tới khi người dùng hiện lại, kể cả khi đổi tư thế hay mở lại app.

### KB-3. Chỉnh nét viền

Tiêu chí chấp nhận:

- [ ] Khi kéo một ngón trên khung xem trước thì nét viền di chuyển theo ngón tay.
- [ ] Khi chụm hoặc mở hai ngón thì nét viền nhỏ lại hoặc to lên, trong khoảng 40% đến 160%.
- [ ] Khi bấm nút chỉnh thì bảng chỉnh hiện trên khung xem trước, có thanh trượt "Độ mờ" (20% đến 100%) và "Cỡ" (40% đến 160%), và bốn nút Lật, Khóa, Ẩn, Đặt lại; nút chỉnh chuyển sang trạng thái đang bật. Bấm dấu đóng hoặc bấm lại nút chỉnh thì bảng đóng.
- [ ] Khi bấm Lật thì nét viền lật ngang.
- [ ] Khi bấm Khóa thì kéo và chụm không còn làm đổi nét viền; nút khóa chuyển sang trạng thái đang bật; nút chỉnh và nút lật mờ đi; một lời nhắc ngắn "Đã khóa lớp phủ" hiện trên khung xem trước. Bấm lại thì mở khóa.
- [ ] Khi bấm Ẩn (hoặc nút hiện tư thế ở cột công cụ) thì nét viền ẩn; bấm lại thì hiện. Nút hiện tư thế ở trạng thái đang bật khi nét viền đang hiện.
- [ ] Khi bấm Đặt lại thì nét viền về giữa khung, cỡ 100%, độ mờ mặc định, không lật.
- [ ] Khi đổi sang tư thế khác thì độ mờ, cỡ, lật, khóa, ẩn giữ như trước; vị trí kéo cũng giữ.
- [ ] Mỗi lần người dùng chỉnh xong một thao tác thì app ghi `overlay_adjusted` một lần (không ghi liên tục trong lúc kéo).

Trường hợp đặc biệt:

- [ ] Khi kéo nét viền ra sát mép thì luôn còn ít nhất một phần nét viền trong khung, để người dùng kéo lại được.

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
- [ ] Khi thêm phần nhận dạng thì dung lượng APK tăng không quá 10 MB.

Trường hợp đặc biệt:

- [ ] Khi khung chỉ có trần nhà, tường hay đồ vật thì app không hiện điểm (PoseMate báo nhầm "35% Come closer" khi không có ai, bản đề xuất bài học 8).

### KB-6. Chụp và xem lại

Tiêu chí chấp nhận:

- [ ] Khi bấm nút chụp mà hẹn giờ tắt thì app chụp ngay (có âm chụp nếu bật trong Cài đặt), ghi `photo_captured`, mở màn xem lại với ảnh vừa chụp, không có nét viền.
- [ ] Khi bấm nút chụp mà hẹn giờ bật thì số đếm ngược hiện to giữa khung xem trước, nút chụp đổi thành nút hủy; hết giờ thì chụp như trên; bấm nút hủy thì dừng đếm, không chụp.
- [ ] Khi bấm "Lưu vào thư viện" thì nút hiện "Đang lưu…", các nút khác mờ đi; lưu xong ảnh có trong thư viện máy, ở thư mục ảnh riêng của app; app ghi `photo_saved`.
- [ ] Khi bấm "Chụp lại" sau khi đã lưu ảnh thì app quay về camera.
- [ ] Khi rời màn xem lại mà chưa lưu ảnh (bằng "Chụp lại", "Tư thế tiếp" hay nút quay lại) thì hộp thoại "Bỏ ảnh này?" hiện, ghi rằng ảnh chưa được lưu vào thư viện, có hai nút "Bỏ ảnh" và "Lưu ảnh" (câu hỏi 6). Bấm "Bỏ ảnh" thì làm tiếp việc người dùng vừa chọn, ảnh không được lưu; bấm "Lưu ảnh" thì lưu như trên rồi làm tiếp; chạm ra ngoài hay bấm quay lại thì đóng hộp thoại, vẫn ở màn xem lại.
- [ ] Khi bấm "Chia sẻ" thì hộp chia sẻ của hệ thống hiện với ảnh vừa chụp.
- [ ] Khi bấm "Tư thế tiếp" thì app quay về camera với tư thế kế tiếp trong dải đang chọn (hết dải thì về tư thế đầu).
- [ ] Khi quay về camera từ màn xem lại thì tư thế, chỉnh nét viền, công cụ chụp giữ như trước khi chụp.
- [ ] Khi chạm ô ảnh vừa chụp ở camera thì màn xem lại mở với ảnh đã lưu gần nhất; chưa có ảnh nào thì ô trống và không làm gì.

Trường hợp đặc biệt:

- [ ] Khi lưu lỗi vì hết bộ nhớ thì màn xem lại hiện thông báo "Không lưu được ảnh: bộ nhớ máy đã đầy." kèm nút "Thử lại"; ảnh vẫn còn trên màn xem lại.
- [ ] Khi lưu ảnh thì app không xin quyền đọc ảnh của máy; manifest không có quyền đọc ảnh (bài học 13).

### KB-7. Quyền camera

Tiêu chí chấp nhận:

- [ ] Khi chưa có quyền camera và máy còn hỏi được (ví dụ cờ màn chào tắt) thì camera hỏi quyền khi mở; app ghi `camera_permission_result`.
- [ ] Khi bị từ chối thì khung xem trước hiện tiêu đề "Chưa có quyền dùng camera", lời giải thích và nút "Mở cài đặt"; nút chụp mờ đi; hàng chip và dải tư thế vẫn dùng được.
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

- [ ] Khi người dùng đã lưu ít nhất 3 ảnh (tính từ khi cài) và rời màn xem lại thì app gọi hộp đánh giá của Google Play, nếu lần gọi trước đã cách ít nhất 30 ngày.
- [ ] Khi quảng cáo toàn màn hình vừa hiện ở cùng lần rời màn xem lại thì app không gọi hộp đánh giá lần đó.
- [ ] Khi cờ `feature_in_app_review_enabled` tắt thì app không gọi hộp đánh giá.

### KB-11. Điện thoại màn hình gập

Tiêu chí chấp nhận:

- [ ] Khi máy gập mở ra thì camera giữ khung 3:4 bên trái, nút chụp, đổi camera, ô ảnh vừa chụp và zoom dời sang cột bên phải; màn xem lại đặt ảnh bên trái, các nút bên phải, đúng thiết kế `_unfolded`.
- [ ] Khi gập lại thì camera và màn xem lại dùng bố cục điện thoại, giữ tư thế và các lựa chọn.

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md) (gói bàn giao lập sau khi spec được xác nhận), dựng trên Figma theo hướng Sticker, section Camera và Review của trang Screens.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Camera | Chụp theo tư thế: chọn tư thế, nét viền, thẻ bước, công cụ, điểm khớp | KB-1 đến KB-8, KB-11 |
| Xem lại | Xem ảnh vừa chụp, lưu, chụp lại, chia sẻ, sang tư thế tiếp | KB-6, KB-9, KB-10, KB-11 |

Trạng thái trên thiết kế: camera `default`, `no-pose`, `adjust`, `locked`, `steps-hidden`, `matching`, `matched`, `no-person`, `countdown`, `starting`, `permission-denied`, `error`, `offline` (hộp thoại của spec `network`), `default` khi mở gập; xem lại `default`, `saving`, `save-error`, `discard`, `default` khi mở gập. Chỉ có bản tối (câu 12.10 của bản đề xuất).

## 5. Quảng cáo

| Vị trí | Định dạng | Khi nào hiển thị | Giới hạn tần suất | Cờ bật tắt |
| --- | --- | --- | --- | --- |
| `review_interstitial` | toàn màn hình | Khi rời màn xem lại sau khi đã lưu ảnh (điểm dừng tự nhiên) | Không trước ảnh lưu thứ 2 của phiên; cách lần trước ít nhất 120 giây; tối đa 1 lần mỗi 3 ảnh lưu. Các con số đặt trong `RemoteSettings` để chỉnh từ xa | `ads_review_interstitial_enabled` |

Không vị trí quảng cáo nào ở camera và trong lúc xem lại (bài học 3).

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Tư thế đang dùng | Mở lại app vẫn là tư thế đó | Đến khi người dùng xóa dữ liệu app |
| Tư thế đã dùng gần đây, kèm thời điểm dùng | Xếp dải tư thế và thư viện (đã lưu, gần đây, còn lại) | Đến khi người dùng xóa dữ liệu app |
| Độ mờ, cỡ, lật, khóa, ẩn của nét viền; thẻ bước ẩn hay hiện | Giữ lựa chọn giữa các lần dùng | Đến khi người dùng xóa dữ liệu app |
| Hẹn giờ, lưới, flash, camera trước sau | Giữ lựa chọn giữa các lần dùng | Đến khi người dùng xóa dữ liệu app |
| Số ảnh đã lưu, ngày gọi hộp đánh giá gần nhất, mốc quảng cáo của phiên | Điều kiện hỏi đánh giá và giới hạn quảng cáo | Đến khi người dùng xóa dữ liệu app |
| Ảnh đã lưu | Ảnh của người dùng, trong thư viện máy | Đến khi người dùng tự xóa; gỡ app không xóa |

Ảnh chỉ lưu trên máy, không tải lên đâu (câu 9.3). Phân tích dáng chạy hoàn toàn trên máy.

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `camera_permission_result` | Khi hộp hỏi quyền trả kết quả ở camera | `result`: `granted`, `denied`; `source`: `camera` | Tỷ lệ cho phép camera |
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
8. **Dữ liệu tư thế dùng chung đặt ở đâu?** Camera và thư viện đều cần danh sách tư thế, ảnh, nét viền, danh sách đã lưu; feature không được phụ thuộc nhau (ADR-0001).
   - Đề xuất của AI: module core mới của app `:core:poses` (bản đề xuất, mục 5). Thêm module core là thay đổi rủi ro cao, cần plan duyệt chéo.
   - Trả lời: Đồng ý với đề xuất của AI.
9. **Thư viện CameraX, ML Kit Pose Detection, Play In-App Review và quyền `CAMERA`:** bản đề xuất (câu 12.6) ghi dev thêm ở bước khởi tạo app, không qua phiên tự chạy.
   - Đề xuất của AI: các ticket của spec này phụ thuộc vào việc dev đã thêm xong; AI ghi rõ phụ thuộc trong ticket.
   - Trả lời: Đồng ý. Dev sẽ thêm trước phiên tự chạy.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu (bản đề xuất đã duyệt) |
| 2026-10-21 | AI | Ghi các câu trả lời của anh Nhật vào nội dung: thứ tự tư thế ở dải (đã lưu, gần đây, còn lại), ẩn hiện thẻ bước, chế độ flash, ngưỡng điểm 80, hỏi "Bỏ ảnh này?" khi rời màn xem lại mà chưa lưu, khóa chiều dọc, dữ liệu tư thế đã dùng gần đây; thêm trạng thái thiết kế `steps-hidden`, `discard` |
