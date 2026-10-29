---
feature: network
status: changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-hop-thoai-mat-mang.md
design: design/README.md
created: 2026-10-21
confirmed: 2026-10-21
---

# SPEC: BÁO MẤT MẠNG KHI ĐANG DÙNG

## 1. Mục tiêu

- **Người dùng được gì:** luôn biết khi nào app mất mạng và biết phải làm gì, thay vì thấy màn hình trống, quay vòng mãi hay lỗi khó hiểu.
- **Vì sao làm bây giờ:** quyết định chung cho mọi app của nhóm (2026-10-21): app luôn cần mạng. Lúc mở app, màn loading đã chặn khi không có mạng (spec `loading`, ADR-0018). Khi đang dùng, chưa có gì báo.
- **Đo kết quả bằng gì:** không có sự kiện riêng (bản đề xuất, câu 9.2). Theo dõi qua phản hồi và qua `content_load_failed` có `reason` là `network`.

## 2. Phạm vi

**Trong phạm vi:**

- Hộp thoại "Mất kết nối mạng" hiện phủ lên màn hình đang mở, ở mọi màn sau màn loading: màn chào, camera, xem lại, thư viện, chi tiết, cài đặt.
- Nhận ra mất mạng bằng hai cách: máy báo không còn kết nối, và một lần tải của app thất bại vì mạng (cấu hình, catalog, ảnh tư thế).
- Nút "Thử lại": kiểm tra lại mạng; có mạng thì hộp thoại đóng và màn hình tải lại phần còn dở.

**Ngoài phạm vi lần này:**

- Màn loading: đã có cách báo lỗi và thử lại riêng (spec `loading`).
- Dùng app khi không có mạng.
- Quảng cáo không tải được: không coi là mất mạng, không hiện hộp thoại.

## 3. Kịch bản sử dụng

### KB-1. Mất mạng khi đang dùng

Người dùng đang ở một màn bất kỳ sau màn loading thì máy mất kết nối.

Tiêu chí chấp nhận:

- [ ] Khi máy báo không còn kết nối mạng và tình trạng này kéo dài quá thời gian chờ (câu hỏi 2) thì hộp thoại "Mất kết nối mạng" hiện phủ lên màn hình, kèm lời hướng dẫn "App cần mạng để hoạt động. Kiểm tra kết nối Internet rồi thử lại." và nút "Thử lại", đúng thiết kế.
- [ ] Khi một lần tải của app thất bại vì mạng (dù máy chưa báo mất kết nối) thì hộp thoại hiện như trên.
- [ ] Khi hộp thoại đang hiện thì người dùng không bấm được gì phía sau; chạm ra ngoài hộp thoại không đóng nó.
- [ ] Khi hộp thoại đang hiện thì chỉ có một hộp thoại, kể cả khi nhiều lần tải cùng thất bại.

Trường hợp đặc biệt:

- [ ] Khi đang đếm ngược hẹn giờ ở camera thì việc đếm dừng, không chụp.
- [ ] Khi đang lưu ảnh ở màn xem lại thì việc lưu vẫn chạy tới xong (lưu trên máy không cần mạng), hộp thoại hiện sau đó.
- [ ] Khi mạng chập chờn mất rồi có lại trong thời gian chờ thì hộp thoại không hiện.

### KB-2. Thử lại

Tiêu chí chấp nhận:

- [ ] Khi bấm "Thử lại" mà đã có mạng thì hộp thoại đóng; màn hình tải lại phần đã thất bại (ví dụ ảnh, catalog) và người dùng dùng tiếp từ chỗ cũ.
- [ ] Khi bấm "Thử lại" mà vẫn chưa có mạng thì hộp thoại vẫn hiện; nút cho thấy đang kiểm tra rồi trở lại như cũ (câu hỏi 3).
- [ ] Khi mạng có lại mà người dùng chưa bấm "Thử lại" thì hộp thoại vẫn hiện cho tới khi người dùng bấm (bản đề xuất, mục 6).

### KB-3. Nút quay lại khi đang có hộp thoại

Tiêu chí chấp nhận:

- [ ] Khi bấm nút quay lại của máy lúc hộp thoại đang hiện thì app ra nền như khi bấm nút Home (câu hỏi 4); mở lại app thì nếu vẫn chưa có mạng, hộp thoại hiện lại.

### KB-4. Tắt từ xa

Tiêu chí chấp nhận:

- [ ] Khi cờ `feature_network_dialog_enabled` tắt thì không hiện hộp thoại; các màn hình báo lỗi tải theo cách riêng của mình.

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md): component `NetworkDialog`, ảnh `NetworkDialogScreen_default_light`, `NetworkDialogScreen_default_dark` (hộp thoại trên lớp mờ). Section Network trên Figma có thêm ba frame ví dụ hộp thoại phủ lên camera và thư viện, chỉ để minh họa, không xuất.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Hộp thoại mất mạng | Báo mất mạng, cho thử lại | KB-1, KB-2, KB-3 |

## 5. Quảng cáo

Không có vị trí quảng cáo. Khi mất mạng thì khung quảng cáo không nạp quảng cáo mới (`InternetConnectivity`), nhưng quảng cáo toàn màn hình đã nạp sẵn vẫn có thể hiện. Khi một quảng cáo toàn màn hình đang hiện thì hộp thoại chờ quảng cáo đóng rồi mới hiện, không phủ lên quảng cáo (câu 11 của plan T-074, Nhật chọn theo khung ngày 2026-10-28).

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:** Không có.

**Sự kiện phân tích:** Không có (câu hỏi 5).

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_network_dialog_enabled` | tắt | Bật để hiện hộp thoại mất mạng. Bản staging bật sẵn |

## 9. Câu hỏi còn mở

1. **Đặt phần này ở đâu trong code, và có đưa về dự án mẫu không?** Hộp thoại không thuộc một màn nào, và mọi app của nhóm đều cần.
   - Đề xuất của AI: phần nhận ra mất mạng đặt trong một module core mới `:core:network`; hộp thoại là thành phần của `:core:designsystem`; vỏ app (`:app`) hiện hộp thoại phủ lên mọi màn. Không có module `:feature:network`; thư mục tài liệu vẫn tên `network`. Theo dõi trạng thái mạng của máy cần quyền `ACCESS_NETWORK_STATE`; thêm module core và quyền đều là thay đổi rủi ro cao, cần plan duyệt chéo. Sau khi làm ở app này, mở issue đưa về dự án mẫu (`docs/ops/from-app.md`).
   - Trả lời: Đồng ý với đề xuất của AI. Không cần đưa về app mẫu.
2. **Mất kết nối bao lâu thì mới hiện hộp thoại?**
   - Đề xuất của AI: 3 giây, đặt trong `RemoteSettings`. Lỗi tải vì mạng thì hiện ngay.
   - Trả lời: Đồng ý với đề xuất của AI.
3. **Bấm "Thử lại" lúc vẫn chưa có mạng thì báo thế nào?**
   - Đề xuất của AI: nút hiện vòng chờ tối đa 2 giây rồi trở lại; dưới nút hiện một dòng nhỏ "Vẫn chưa có mạng".
   - Trả lời: Đồng ý với đề xuất của AI.
4. **Nút quay lại của máy khi hộp thoại đang hiện?**
   - Đề xuất của AI: đưa app ra nền, không đóng hộp thoại, không quay lại màn trước.
   - Trả lời: Đồng ý với đề xuất của AI.
5. **Có cần sự kiện phân tích cho mất mạng không?** Danh sách 9 sự kiện đã duyệt không có sự kiện này.
   - Đề xuất của AI: chưa cần; nếu muốn biết người dùng mất mạng bao nhiêu thì thêm `network_lost` ở spec sau.
   - Trả lời: Đồng ý với đề xuất của AI.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu của người phụ trách sản phẩm |
| 2026-10-21 | AI | Ghi các câu trả lời của anh Nhật vào nội dung: không đưa phần này về dự án mẫu (câu hỏi 1) |
| 2026-10-21 | AI | Lập gói bàn giao thiết kế: ảnh `NetworkDialogScreen_*` thay cho các frame `offline` của camera và thư viện |
| 2026-10-29 | AI | Mục 5 theo hành vi của khung quảng cáo (T-074 lượt 2, câu 11 của plan): bỏ luật "khi hộp thoại đang hiện thì không hiện quảng cáo toàn màn hình"; hộp thoại chờ quảng cáo toàn màn hình đóng. Chuyển `changed`, chờ Nhật xác nhận lại |
