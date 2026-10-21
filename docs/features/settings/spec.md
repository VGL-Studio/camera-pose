---
feature: settings
status: draft
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-cai-dat.md
design: design/README.md
created: 2026-10-21
confirmed:
---

# SPEC: CÀI ĐẶT

## 1. Mục tiêu

- **Người dùng được gì:** chỉnh vài lựa chọn chung của app ở một chỗ: giao diện sáng tối, âm khi chụp, ngôn ngữ, quyền riêng tư và đồng ý quảng cáo; xem chính sách quyền riêng tư và phiên bản app.
- **Vì sao làm bây giờ:** app không có màn chọn ngôn ngữ lúc mở (bài học 2), nên phải đổi được ngôn ngữ ở đây. Người dùng phải đổi được lựa chọn đồng ý quảng cáo (ADR-0013). Google Play yêu cầu có đường dẫn tới chính sách quyền riêng tư.
- **Đo kết quả bằng gì:** không có chỉ số riêng; không có phản hồi người dùng phàn nàn thiếu lựa chọn.

## 2. Phạm vi

**Trong phạm vi:**

- Màn cài đặt mở từ nút bánh răng ở camera và ở thư viện.
- Nhóm "Chung": Giao diện (Theo hệ thống, Sáng, Tối), Âm khi chụp (bật tắt), Ngôn ngữ.
- Nhóm "Quyền riêng tư": Tùy chọn quyền riêng tư (mở lại hộp thoại đồng ý quảng cáo), Chính sách quyền riêng tư (mở trang web).
- Nhóm "Thông tin": Phiên bản app.
- Giao diện sáng tối áp dụng cho mọi màn trừ camera và màn xem lại, hai màn này luôn tối.

**Ngoài phạm vi lần này:**

- Tài khoản, gói trả phí, gửi góp ý, đánh giá app, giới thiệu app khác, điều khoản sử dụng.

## 3. Kịch bản sử dụng

### KB-1. Mở cài đặt

Tiêu chí chấp nhận:

- [ ] Khi bấm nút bánh răng ở camera hoặc ở thư viện thì màn cài đặt mở, đúng thiết kế, không có quảng cáo.
- [ ] Khi bấm quay lại thì về màn trước đó.

### KB-2. Đổi giao diện

Tiêu chí chấp nhận:

- [ ] Khi bấm "Giao diện" thì một hộp chọn hiện ba lựa chọn: Theo hệ thống, Sáng, Tối; lựa chọn hiện tại được đánh dấu.
- [ ] Khi chọn một lựa chọn thì app đổi giao diện ngay, dòng "Giao diện" ghi lựa chọn mới.
- [ ] Khi chọn "Theo hệ thống" thì app đổi theo chế độ sáng tối của máy, kể cả khi máy đổi lúc app đang mở.
- [ ] Khi đổi giao diện thì camera và màn xem lại vẫn nền tối.
- [ ] Khi mở lại app thì lựa chọn vẫn giữ. Mặc định là "Theo hệ thống".

### KB-3. Âm khi chụp

Tiêu chí chấp nhận:

- [ ] Khi tắt "Âm khi chụp" thì chụp ảnh không phát âm thanh của app (câu hỏi 1); bật thì có.
- [ ] Khi mở lại app thì lựa chọn vẫn giữ. Mặc định là bật.

### KB-4. Đổi ngôn ngữ

Tiêu chí chấp nhận:

- [ ] Khi bấm "Ngôn ngữ" trên Android 13 trở lên thì trang ngôn ngữ của app trong cài đặt máy mở ra; chọn xong quay lại thì app hiện ngôn ngữ mới.
- [ ] Khi bấm "Ngôn ngữ" trên máy dưới Android 13 thì một hộp chọn trong app hiện "Theo hệ thống" và 7 ngôn ngữ phát hành; chọn xong app đổi ngôn ngữ ngay (câu hỏi 2).
- [ ] Khi dòng "Ngôn ngữ" hiện thì nó ghi ngôn ngữ đang dùng, viết bằng chính ngôn ngữ đó (ví dụ "Tiếng Việt", "हिन्दी").

### KB-5. Quyền riêng tư

Tiêu chí chấp nhận:

- [ ] Khi bấm "Tùy chọn quyền riêng tư" thì hộp thoại đồng ý quảng cáo của `:core:ads` hiện lại để người dùng đổi lựa chọn (ADR-0013).
- [ ] Khi người dùng ở nơi không cần hỏi đồng ý thì dòng "Tùy chọn quyền riêng tư" không hiện (câu hỏi 3).
- [ ] Khi bấm "Chính sách quyền riêng tư" thì trang chính sách mở trong trình duyệt (câu hỏi 4).

### KB-6. Phiên bản

Tiêu chí chấp nhận:

- [ ] Khi vào cài đặt thì dòng "Phiên bản" ghi số phiên bản của app (ví dụ "1.0.0"); dòng này không bấm được và không có mũi tên.

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md) (gói bàn giao lập sau khi spec được xác nhận), dựng trên Figma theo hướng Sticker, frame `SettingsScreen_default_light`, `SettingsScreen_default_dark`. Trên thiết kế, dòng "Phiên bản" còn mũi tên; sửa theo KB-6 trước khi lập gói bàn giao.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Cài đặt | Các lựa chọn chung của app | KB-1 đến KB-6 |
| Hộp chọn giao diện, hộp chọn ngôn ngữ | Chọn một trong vài lựa chọn | KB-2, KB-4 |

Hộp chọn dùng thành phần có sẵn của hệ thống thiết kế; chưa có trên Figma (câu hỏi 5).

## 5. Quảng cáo

Không có.

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Lựa chọn giao diện | Giữ giữa các lần dùng | Đến khi người dùng xóa dữ liệu app |
| Âm khi chụp bật hay tắt | Giữ giữa các lần dùng | Đến khi người dùng xóa dữ liệu app |
| Ngôn ngữ đã chọn (máy dưới Android 13) | Giữ giữa các lần dùng | Đến khi người dùng xóa dữ liệu app |

**Sự kiện phân tích:** Không có (bản đề xuất, câu 9.2 chỉ duyệt 9 sự kiện, không có sự kiện cài đặt).

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_settings_enabled` | tắt | Bật để hiện nút bánh răng và màn cài đặt. Bản staging bật sẵn |

## 9. Câu hỏi còn mở

1. **Tắt âm khi chụp ở những nước bắt buộc có âm (ví dụ Nhật, Hàn)?** Không nước nào trong danh sách phát hành bắt buộc.
   - Đề xuất của AI: cho tắt ở mọi nơi; nếu sau này phát hành ở nước bắt buộc thì ẩn lựa chọn ở nước đó.
   - Trả lời:
2. **Máy dưới Android 13 đổi ngôn ngữ trong app?**
   - Đề xuất của AI: có, dùng hộp chọn trong app như KB-4, vì minSdk là 26 và nhiều máy ở các thị trường phát hành còn dưới Android 13.
   - Trả lời:
3. **Ẩn "Tùy chọn quyền riêng tư" khi không cần hỏi đồng ý?**
   - Đề xuất của AI: ẩn, theo cách `:core:ads` cho biết có cần hiện nút này không.
   - Trả lời:
4. **Đường dẫn trang chính sách quyền riêng tư?**
   - Đề xuất của AI: dev cung cấp đường dẫn; đặt trong `RemoteSettings` để đổi được mà không cần bản mới.
   - Trả lời:
5. **Hộp chọn giao diện và ngôn ngữ chưa có trên thiết kế.**
   - Đề xuất của AI: AI dựng thêm frame `SettingsScreen_theme-picker_light` và bản tối trên Figma trước khi lập gói bàn giao.
   - Trả lời:

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu (bản đề xuất đã duyệt) |
