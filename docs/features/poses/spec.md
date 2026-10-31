---
feature: poses
status: confirmed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-thu-vien-tu-the.md
design: design/README.md
created: 2026-10-21
confirmed: 2026-10-30
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
- Quảng cáo ở thư viện và chi tiết: thanh đáy native ở cả hai màn, ô quảng cáo xen trong lưới, quảng cáo toàn màn hình khi bấm "Chụp với tư thế này"; hai vị trí sau tắt lúc phát hành (`features/ads/spec.md` câu 8, 9).
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
- [ ] Khi khóa `is_show_inter_pose_detail` bật và có quảng cáo toàn màn hình sẵn sàng, qua nhịp chung `ads_interval`, thì quảng cáo hiện sau khi bấm "Chụp với tư thế này"; đóng quảng cáo thì camera mở với đúng tư thế đó. Không có quảng cáo, chưa đủ nhịp hay khóa tắt thì camera mở ngay, không chờ (`features/ads/spec.md` câu 9).
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

### KB-6. Quảng cáo ở thư viện và chi tiết

Theo `features/ads/spec.md` câu 8, 9, 10: thanh đáy native thay banner từ T-079.

Tiêu chí chấp nhận:

- [ ] Khi khóa `is_show_poses_bottom` bật, `is_show_native_bottom` bật và người dùng đã đồng ý theo hộp thoại đồng ý thì thanh đáy native nhỏ (80 dp) hiện ở đáy thư viện; lưới cuộn phía trên thanh, thẻ cuối không bị che (câu hỏi 1).
- [ ] Khi khóa `is_show_pose_detail_bottom` bật thì thanh đáy native hiện ở đáy chi tiết, dưới nút "Chụp với tư thế này"; nút không bị che hay đẩy ra ngoài màn hình.
- [ ] Khi khóa tắt, quảng cáo đang tải hay không tải được thì không chừa khoảng trống; nội dung chỉ được chừa chỗ khi khe đã có quảng cáo.
- [ ] Khi `is_show_native_collap` tắt (giá trị lúc phát hành) thì không có lớp quảng cáo nổi trên lưới hay trên nút chính.
- [ ] Khi khóa `is_show_poses_in_collection` bật thì lưới có một ô quảng cáo sau mỗi `native_every_poses_in_collection` tư thế (mặc định 8), không ở đầu, không ở cuối lưới; thẻ quảng cáo có nhãn quảng cáo và khác rõ thẻ tư thế; hết quảng cáo thì ô ẩn, lưới không có ô trống. Áp dụng cho "Tất cả" và từng nhóm, cả khi mở gập (3 cột).
- [ ] Khi khóa tắt thì lưới không có ô quảng cáo, thứ tự tư thế giữ như KB-1.

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

Trạng thái trên thiết kế: thư viện `default`, `loading`, `error`, `saved-empty`, `ad` (có quảng cáo ở đáy, câu hỏi 1 và 5), `default` khi mở gập; chi tiết `default`, `loading`, `error`, `ad`, `default` khi mở gập. Mỗi trạng thái có bản sáng và tối. Trạng thái `ad` vẽ banner 320 x 50 theo câu hỏi 1; từ T-079 đáy là thanh native (câu hỏi 5).

## 5. Quảng cáo

| Vị trí | Định dạng | Khi nào hiển thị | Giới hạn tần suất | Cờ bật tắt |
| --- | --- | --- | --- | --- |
| `poses_bottom` | native_small (thanh đáy) | Đáy màn thư viện, khi màn hình đang mở | Thay nội dung tại chỗ mỗi 60 giây khi app ở foreground; lớp thu gọn tắt lúc phát hành | `is_show_poses_bottom`, cùng `is_show_native_bottom` |
| `pose_detail_bottom` | native_small (thanh đáy) | Đáy màn chi tiết tư thế, dưới nút chính | Như trên | `is_show_pose_detail_bottom`, cùng `is_show_native_bottom` |
| `poses_in_collection` | native_in_collection | Ô xen trong lưới thư viện, sau mỗi 8 tư thế; không ở đầu, không ở cuối | Pool 5 quảng cáo, hết hạn 55 phút; tắt lúc phát hành | `is_show_poses_in_collection`, `native_every_poses_in_collection` |
| `inter_pose_detail` | interstitial | Sau khi bấm "Chụp với tư thế này", trước khi camera mở | Nhịp chung `ads_interval` (mặc định 45 giây), cùng nhóm với `review_interstitial`; tắt lúc phát hành | `is_show_inter_pose_detail` |

`poses_banner`, `poses_detail_banner` (banner, T-074) bỏ khỏi app ở T-079 (`features/ads/spec.md` câu 8); khóa cũ giữ trên Firebase tới khi bản cũ không còn người dùng.

Quảng cáo toàn app khi quay lại app (`resume`, `features/ads/spec.md` câu 6) có thể hiện khi người dùng quay lại app lúc đang ở thư viện hay chi tiết tư thế.

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Danh sách tư thế đã lưu, kèm thời điểm lưu | Chip "Đã lưu" | Đến khi người dùng xóa dữ liệu app |
| Catalog, ảnh nhỏ, ảnh lớn, nét viền đã tải | Hiện ngay ở lần sau (ADR-0015) | Đến khi người dùng xóa dữ liệu app |
| Đã mở app lần đầu (`ads_first_open_done`) | Không có quảng cáo mở app ở lần mở đầu sau khi cài | Đến khi người dùng xóa dữ liệu app |
| Số quảng cáo toàn màn hình đã hiện từ khi cài (`ads_inter_display_count`) | Luật đếm của khung | Đến khi người dùng xóa dữ liệu app |

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `pose_detail_opened` | Khi mở chi tiết tư thế | `pose_id`, `category` | Tư thế nào được quan tâm |
| `pose_saved` | Khi lưu hoặc bỏ lưu | `pose_id`, `saved`: có hoặc không | Tư thế nào được yêu thích, nên làm thêm loại nào |
| `pose_selected` | Khi bấm "Chụp với tư thế này" | `pose_id`, `category`, `source`: `detail` | Thư viện có dẫn người dùng tới chụp không |
| `content_load_failed` | Khi tải catalog hay ảnh thất bại | `content`: `catalog`, `thumbnail`, `image`; `reason`: `network`, `not_found`, `timeout`, `other` | Lỗi tải nội dung có nhiều không (bài học 7) |

**Sự kiện quảng cáo do khung ghi** (`:core:analytics`, `FirebaseAdMetrics`; chung cho mọi vị trí, không riêng màn này; mỗi sự kiện có `ad_format`, `ad_placement`): `ad_impression_placement` (luôn ghi; thêm `value`, `currency`, `ad_source`, `ad_unit_name`, `ad_platform`), `ad_opportunity` (luôn ghi; `opportunity_result`, `load_state`), `ad_load_success`, `ad_load_fail`, `ad_show_fail`, `ad_clicked` (chỉ ghi khi khóa `is_log_ad_fullscreen_events` hay `is_log_ad_slot_events` bật; mặc định tắt, bản staging bật). Dùng để đo doanh thu và tỷ lệ hiện của từng vị trí. Không gửi gì tới AppsFlyer (`features/ads/spec.md` câu 3).

## 7. File tĩnh trên R2

| Nhóm file | Dung lượng ước tính | Khi nào tải | Khi không tải được |
| --- | --- | --- | --- |
| Catalog (`poses/catalog/v<n>/catalog-<sha8>.json`) | Khoảng 1,6 MB với 350 tư thế, 7 ngôn ngữ (catalog v3, đo ngày 2026-10-28; ước tính ban đầu 100 đến 300 KB với 200 tư thế) | Ở màn loading (spec `loading`) | Màn loading báo lỗi; nếu đọc lỗi ở thư viện thì trạng thái lỗi (KB-5) |
| Ảnh nhỏ (`preview_540.webp`) | 30 đến 60 KB mỗi ảnh | Bộ khởi đầu ở màn loading lần đầu; tư thế khác khi thẻ hiện | Thẻ giữ khung xương, chạm để thử lại |
| Ảnh lớn (`preview_1200.webp`) | 100 đến 200 KB mỗi ảnh | Khi mở chi tiết | Vùng ảnh báo lỗi, nút "Thử lại" (KB-5) |

Đường dẫn, khuôn dữ liệu và cách lưu theo bản đề xuất, mục 11 (ADR-0015). Mỗi lần nội dung đổi là một đường dẫn mới.

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_poses_enabled` | tắt | Bật để hiện nút thư viện ở camera và màn thư viện. Bản staging bật sẵn |
| `is_show_poses_bottom` | tắt | Bật thanh đáy `poses_bottom` (`features/ads/spec.md` câu 8; thay `is_show_poses_banner`). Cần cả `is_show_native_bottom` bật. Khóa chung `is_show_ads` (mặc định bật) tắt thì mọi quảng cáo tắt |
| `is_show_pose_detail_bottom` | tắt | Bật thanh đáy `pose_detail_bottom` (như trên; thay `is_show_poses_detail_banner`) |
| `is_show_poses_in_collection` | tắt | Bật ô quảng cáo trong lưới (câu 9). `native_every_poses_in_collection`: số tư thế giữa hai ô, mặc định 8 |
| `is_show_inter_pose_detail` | tắt | Bật quảng cáo toàn màn hình khi chụp với tư thế từ chi tiết (câu 9) |

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
5. **Trạng thái `ad` của thiết kế khi đáy là thanh native (T-079).** Bốn frame `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark` vẽ banner 320 x 50 (câu hỏi 1), và mỗi ảnh thiết kế có một screenshot test cùng tên (ADR-0020).
   - Đề xuất của AI: AI vẽ lại bốn frame trên Figma với thanh đáy native 80 dp của khung (khe 80 dp, lề đáy 16 dp), xuất lại ảnh vào `design/screens/` trước khi soạn plan T-079; thêm một frame `PosesScreen_ad-collection_light` có ô quảng cáo trong lưới để kiểm thẻ quảng cáo khác rõ thẻ tư thế. Không có thiết kế riêng cho thẻ quảng cáo: dùng giao diện của khung.
   - Trả lời: đồng ý với đề xuất của AI. Nhật, 2026-10-30.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu (bản đề xuất đã duyệt) |
| 2026-10-21 | AI | Ghi các câu trả lời của anh Nhật vào nội dung: thứ tự tư thế trong lưới (đã lưu, gần đây, còn lại); thêm trạng thái thiết kế `ad` có banner |
| 2026-10-21 | AI | Lập gói bàn giao thiết kế: trạng thái `offline` chuyển sang gói `network` vì hộp thoại do vỏ app hiện, không thuộc `PosesScreen` |
| 2026-10-28 | AI | Mục 7: cỡ catalog theo bản thật (v3, 350 tư thế, khoảng 1,6 MB), theo câu 4 của plan T-071 mà Nhật duyệt. Không đổi hành vi. Chuyển `changed`, chờ xác nhận lại |
| 2026-10-28 | Nhật | Xác nhận lại mục 7 (cỡ catalog thật). Chuyển lại `confirmed` |
| 2026-10-28 | AI | Mục 5: tên khóa bật tắt đổi theo `features/ads/spec.md` câu 2 (từ T-074). Màn thư viện và chi tiết có thể bị phủ bởi quảng cáo toàn app khi quay lại app (câu 6) |
| 2026-10-28 | Nhật | Xác nhận phần đổi của mục 5, cùng lúc xác nhận spec ads. Giữ `confirmed` |
| 2026-10-29 | AI | Sau T-074 (lượt 3, plan mục 3.1): bỏ chữ "từ T-074; trước đó ..."; tên khóa mới ở KB-6 và mục 8; mục 6 thêm dữ liệu trên máy và sự kiện quảng cáo do khung ghi. Chuyển `changed`, chờ Nhật xác nhận lại |
| 2026-10-29 | Nhật | Xác nhận phần đổi sau T-074 lượt 3 (lời của Nhật: chọn "Xác nhận cả ba" khi trả lời câu hỏi cuối phiên RUN-20261029-01). Chuyển lại `confirmed` |
| 2026-10-30 | AI | Theo `features/ads/spec.md` câu 8 tới 10 (Nhật, 2026-10-30): thanh đáy native `poses_bottom`, `pose_detail_bottom` thay hai banner; thêm `poses_in_collection`, `inter_pose_detail`, tắt lúc phát hành. Sửa mục 2, 4, 5, 8, KB-4, KB-6; thêm câu hỏi 5 (thiết kế trạng thái `ad`). Chuyển `changed` |
| 2026-10-30 | Nhật | Trả lời câu hỏi 5 (AI vẽ lại trạng thái `ad` trên Figma; AI ghi). Xác nhận spec. Lời xác nhận của Nhật: chọn "Chuyển confirmed" khi được hỏi, sau khi trả lời câu cuối. Chuyển lại `confirmed` |
