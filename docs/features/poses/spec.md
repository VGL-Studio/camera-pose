---
feature: poses
status: confirmed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-thu-vien-tu-the.md
design: design/README.md
created: 2026-10-21
confirmed: 2026-10-21
---

# SPEC: THƯ VIỆN TƯ THẾ VÀ CHI TIẾT TƯ THẾ

## 1. Mục tiêu

- **Người dùng được gì:** tìm tư thế hợp hoàn cảnh (một người, cặp đôi, bạn bè, du lịch, cà phê…), đọc cách tạo dáng từng bước, lưu tư thế yêu thích, rồi một chạm sang camera để chụp theo.
- **Vì sao làm bây giờ:** app mẫu có quá ít tư thế hoặc danh mục rỗng (bài học 4), tên sai hay thô (bài học 9), thiếu tư thế cho nam (bài học 11), tải lỗi thì màn hình trống mãi (bài học 7).
- **Đo kết quả bằng gì:** tỷ lệ người dùng mở chi tiết rồi chụp với tư thế đó (`pose_selected` có `source` là `detail`, trên `pose_detail_opened`); số tư thế được lưu mỗi người dùng; số lần `content_load_failed` trên mỗi 100 lần mở thư viện, mong đợi dưới 1.

## 2. Phạm vi

**Trong phạm vi:**

- Màn thư viện, mở từ nút thư viện ở camera: thanh trên (quay lại, tiêu đề, nút cài đặt), dòng tổng số tư thế, hàng chip, lưới thẻ tư thế.
- Chip: "Tất cả", "Đã lưu", rồi các nhóm có tư thế theo thứ tự trong catalog (Nữ, Nam, Cặp đôi, Bạn bè, Gia đình, Trẻ em, Du lịch, Cà phê; chốt theo nội dung). Nhóm chưa có tư thế không hiện.
- Thẻ tư thế: ảnh, tên, loại và số bước (ví dụ "Một người · 4 bước"), nút lưu.
- Lưu và bỏ lưu tư thế ở thẻ và ở chi tiết; chip "Đã lưu" lọc các tư thế đã lưu.
- Màn chi tiết: ảnh lớn, loại, nhóm, khung hình, số bước; các bước tạo dáng (3 đến 5 bước); lỗi hay gặp; nút lưu ở thanh trên; nút "Chụp với tư thế này" ghim ở đáy.
- Đang tải, tải lỗi có nút thử lại, chờ có giới hạn.
- Banner quảng cáo ở thư viện và chi tiết (mức B).
- Điện thoại màn hình gập, khi mở ra: thư viện 3 cột; chi tiết chia hai cột (ảnh, hướng dẫn).
- Tên tư thế, các bước, lỗi hay gặp hiện theo ngôn ngữ của máy, đủ 7 ngôn ngữ phát hành.

**Ngoài phạm vi lần này:**

- Tìm kiếm theo chữ và thẻ, nhiều bộ sưu tập, chọn nhiều tư thế để chụp lần lượt, bài mẹo chụp ảnh.
- Làm nội dung tư thế (việc của người làm nội dung, bản đề xuất mục 11).
- Hộp thoại mất mạng (spec `network`).

## 3. Kịch bản sử dụng

### KB-1. Duyệt thư viện

Người dùng ở camera, muốn tìm tư thế khác.

Tiêu chí chấp nhận:

- [ ] Khi bấm nút thư viện ở camera thì màn thư viện mở, chip "Tất cả" đang chọn, lưới 2 cột hiện các tư thế.
- [ ] Khi lưới hiện thì tư thế xếp theo thứ tự: tư thế đã lưu, rồi tư thế đã dùng gần đây (mới dùng nhất trước), rồi các tư thế còn lại theo thứ tự trong catalog; thứ tự này áp dụng cho "Tất cả" và cho từng nhóm (câu hỏi 2). "Tư thế đã dùng" là tư thế người dùng đã chọn ở camera (spec `camera`).
- [ ] Khi vào thư viện thì dòng dưới tiêu đề ghi tổng số tư thế và số nhóm (ví dụ "214 tư thế trong 8 nhóm").
- [ ] Khi vào thư viện thì chỉ các nhóm có ít nhất một tư thế mới có chip.
- [ ] Khi mỗi thẻ hiện thì thẻ có ảnh, tên tư thế, loại và số bước, nút lưu; tên tư thế đọc được, không có chuỗi kỹ thuật kiểu `no_props` (bài học 9).
- [ ] Khi bấm nút quay lại thì về camera, giữ nguyên tư thế đang dùng.
- [ ] Khi bấm nút cài đặt ở thanh trên thì màn cài đặt mở (spec `settings`).

Trường hợp đặc biệt:

- [ ] Khi ảnh của một thẻ chưa tải xong thì thẻ hiện khung xương thay ảnh; tải xong thì hiện ảnh, không làm xô lệch lưới.

### KB-2. Lọc theo nhóm

Tiêu chí chấp nhận:

- [ ] Khi chạm chip một nhóm thì lưới chỉ còn tư thế của nhóm đó; chip đang chọn có nền màu nhấn và dấu ✓.
- [ ] Khi quay lại thư viện từ chi tiết thì chip đang chọn và vị trí cuộn giữ như trước.

### KB-3. Lưu tư thế yêu thích

Tiêu chí chấp nhận:

- [ ] Khi bấm nút lưu trên thẻ hoặc ở chi tiết thì tư thế được lưu, nút đổi sang trạng thái đã lưu; bấm lại thì bỏ lưu. Mỗi lần đổi, app ghi `pose_saved`.
- [ ] Khi chạm chip "Đã lưu" thì lưới hiện các tư thế đã lưu, mới lưu nhất ở đầu.
- [ ] Khi chưa lưu tư thế nào và chạm chip "Đã lưu" thì màn hình hiện tiêu đề "Chưa lưu tư thế nào", lời hướng dẫn "Chạm biểu tượng lưu trên thẻ tư thế để giữ lại ở đây." và nút "Xem tất cả tư thế" đưa về chip "Tất cả".
- [ ] Khi mở lại app thì danh sách đã lưu vẫn còn.

Trường hợp đặc biệt:

- [ ] Khi một tư thế đã lưu không còn trong catalog thì nó không hiện ở "Đã lưu".
- [ ] Khi ghi danh sách đã lưu bị lỗi thì nút lưu trở về trạng thái cũ và một thông báo ngắn báo chưa lưu được.

### KB-4. Xem chi tiết và chụp với tư thế

Tiêu chí chấp nhận:

- [ ] Khi chạm một thẻ thì màn chi tiết mở, có tên tư thế ở thanh trên, ảnh lớn, dòng loại, nhóm, khung hình, số bước, các bước đánh số, khối "Lỗi hay gặp", nút "Chụp với tư thế này" ghim ở đáy; app ghi `pose_detail_opened`.
- [ ] Khi nội dung dài hơn màn hình thì nội dung cuộn được, nút "Chụp với tư thế này" luôn thấy được, không che chữ cuối.
- [ ] Khi bấm "Chụp với tư thế này" thì camera mở với đúng tư thế đó (spec `camera`, KB-1); app ghi `pose_selected` với `source` là `detail`.
- [ ] Khi bấm quay lại ở chi tiết thì về thư viện.

### KB-5. Tải nội dung và khi tải lỗi

Catalog và bộ khởi đầu đã tải ở màn loading (spec `loading`). Ảnh của tư thế ngoài bộ khởi đầu tải khi cần.

Tiêu chí chấp nhận:

- [ ] Khi vào thư viện thì các thẻ của bộ khởi đầu hiện ảnh ngay, không chờ tải.
- [ ] Khi đang tải thêm ảnh thì các thẻ chưa có ảnh hiện khung xương.
- [ ] Khi đọc catalog lỗi (không phải do mất mạng) thì thư viện hiện tiêu đề "Không tải được thư viện", lời hướng dẫn "Đã có lỗi khi tải thư viện. Thử lại sau ít phút." và nút "Thử lại"; app ghi `content_load_failed`.
- [ ] Khi tải ảnh lớn ở chi tiết chưa xong thì vùng ảnh hiện vòng chờ; lỗi thì vùng ảnh hiện "Không tải được ảnh", lời hướng dẫn và nút "Thử lại", các bước vẫn đọc được.
- [ ] Khi một lần tải không có phản hồi sau 15 giây thì app coi là lỗi và hiện như trên, không quay vòng mãi (bài học 7).
- [ ] Khi lỗi do mất mạng thì hiện hộp thoại của spec `network` thay cho thông báo lỗi riêng.

### KB-6. Banner quảng cáo

Tiêu chí chấp nhận:

- [ ] Khi cờ `ads_poses_banner_enabled` bật và người dùng đã đồng ý theo hộp thoại đồng ý thì banner hiện neo ở đáy thư viện; lưới cuộn phía trên banner, không bị che (câu hỏi 1).
- [ ] Khi cờ `ads_poses_detail_banner_enabled` bật thì banner hiện ở đáy chi tiết, dưới nút "Chụp với tư thế này"; nút không bị che hay đẩy ra ngoài màn hình.
- [ ] Khi cờ tắt hoặc banner không tải được thì không chừa khoảng trống.

### KB-7. Điện thoại màn hình gập

Tiêu chí chấp nhận:

- [ ] Khi máy gập mở ra thì thư viện hiện 3 cột; chi tiết chia hai cột: ảnh bên trái, hướng dẫn bên phải, nút "Chụp với tư thế này" ở đáy cột phải; đúng thiết kế `_unfolded`.
- [ ] Khi gập lại thì dùng bố cục điện thoại, giữ chip, vị trí cuộn và tư thế đang xem.

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md) (gói bàn giao lập sau khi spec được xác nhận), dựng trên Figma theo hướng Sticker, section Poses và PoseDetail của trang Screens.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Thư viện tư thế | Duyệt, lọc theo nhóm, lưu tư thế | KB-1, KB-2, KB-3, KB-5, KB-6, KB-7 |
| Chi tiết tư thế | Đọc cách tạo dáng, lưu, chụp với tư thế | KB-3, KB-4, KB-5, KB-6, KB-7 |

Trạng thái trên thiết kế: thư viện `default`, `loading`, `error`, `saved-empty`, `ad` (có banner, câu hỏi 1), `default` khi mở gập; chi tiết `default`, `loading`, `error`, `ad`, `default` khi mở gập. Mỗi trạng thái có bản sáng và tối.

## 5. Quảng cáo

| Vị trí | Định dạng | Khi nào hiển thị | Giới hạn tần suất | Cờ bật tắt |
| --- | --- | --- | --- | --- |
| `poses_banner` | banner | Neo đáy màn thư viện, khi màn hình đang mở | Làm mới theo mặc định của AdMob | `ads_poses_banner_enabled` |
| `poses_detail_banner` | banner | Đáy màn chi tiết tư thế, dưới nút chính | Làm mới theo mặc định của AdMob | `ads_poses_detail_banner_enabled` |

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Danh sách tư thế đã lưu, kèm thời điểm lưu | Chip "Đã lưu" | Đến khi người dùng xóa dữ liệu app |
| Catalog, ảnh nhỏ, ảnh lớn, nét viền đã tải | Hiện ngay ở lần sau (ADR-0015) | Đến khi người dùng xóa dữ liệu app |

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `pose_detail_opened` | Khi mở chi tiết tư thế | `pose_id`, `category` | Tư thế nào được quan tâm |
| `pose_saved` | Khi lưu hoặc bỏ lưu | `pose_id`, `saved`: có hoặc không | Tư thế nào được yêu thích, nên làm thêm loại nào |
| `pose_selected` | Khi bấm "Chụp với tư thế này" | `pose_id`, `category`, `source`: `detail` | Thư viện có dẫn người dùng tới chụp không |
| `content_load_failed` | Khi tải catalog hay ảnh thất bại | `content`: `catalog`, `thumbnail`, `image`; `reason`: `network`, `not_found`, `timeout`, `other` | Lỗi tải nội dung có nhiều không (bài học 7) |

## 7. File tĩnh trên R2

| Nhóm file | Dung lượng ước tính | Khi nào tải | Khi không tải được |
| --- | --- | --- | --- |
| Catalog (`poses/catalog/v<n>/catalog-<sha8>.json`) | 100 đến 300 KB với 200 tư thế, 7 ngôn ngữ | Ở màn loading (spec `loading`) | Màn loading báo lỗi; nếu đọc lỗi ở thư viện thì trạng thái lỗi (KB-5) |
| Ảnh nhỏ (`preview_540.webp`) | 30 đến 60 KB mỗi ảnh | Bộ khởi đầu ở màn loading lần đầu; tư thế khác khi thẻ hiện | Thẻ giữ khung xương, chạm để thử lại |
| Ảnh lớn (`preview_1200.webp`) | 100 đến 200 KB mỗi ảnh | Khi mở chi tiết | Vùng ảnh báo lỗi, nút "Thử lại" (KB-5) |

Đường dẫn, khuôn dữ liệu và cách lưu theo bản đề xuất, mục 11 (ADR-0015). Mỗi lần nội dung đổi là một đường dẫn mới.

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_poses_enabled` | tắt | Bật để hiện nút thư viện ở camera và màn thư viện. Bản staging bật sẵn |
| `ads_poses_banner_enabled` | tắt | Bật banner `poses_banner` (ADR-0014) |
| `ads_poses_detail_banner_enabled` | tắt | Bật banner `poses_detail_banner` (ADR-0014) |

## 9. Câu hỏi còn mở

1. **Thiết kế chưa có chỗ cho banner.** Hai vị trí banner đã được duyệt (câu 9.1), nhưng các frame thư viện và chi tiết chưa vẽ banner.
   - Đề xuất của AI: thêm vào Figma một trạng thái có banner cho mỗi màn (`PosesScreen_ad_light`, `PoseDetailScreen_ad_light` và bản tối): banner 320 x 50 neo đáy; ở chi tiết, banner nằm dưới thanh nút chính. AI dựng trước khi lập gói bàn giao.
   - Trả lời: Đồng ý với đề xuất của AI.
2. **Thứ tự tư thế trong "Tất cả" và trong từng nhóm?**
   - Đề xuất của AI: theo thứ tự người làm nội dung đặt trong catalog; tư thế mới thêm nằm sau cùng. Sau khi có số liệu, xếp theo lượt chọn.
   - Trả lời: Đầu tiên là các tư thế đã được lưu, thứ hai là các tư thế đã được sử dụng gần đây, thứ ba là các tư thế còn lại.
3. **Dòng tổng số tư thế đếm những gì?**
   - Đề xuất của AI: đếm mọi tư thế trong catalog và số nhóm có chip, không đếm "Tất cả" và "Đã lưu".
   - Trả lời: Đồng ý với đề xuất của AI.
4. **Dữ liệu tư thế dùng chung:** như câu hỏi 8 của spec `camera` (module `:core:poses`).
   - Trả lời: Đồng ý với đề xuất của AI.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu (bản đề xuất đã duyệt) |
| 2026-10-21 | AI | Ghi các câu trả lời của anh Nhật vào nội dung: thứ tự tư thế trong lưới (đã lưu, gần đây, còn lại); thêm trạng thái thiết kế `ad` có banner |
| 2026-10-21 | AI | Lập gói bàn giao thiết kế: trạng thái `offline` chuyển sang gói `network` vì hộp thoại do vỏ app hiện, không thuộc `PosesScreen` |
