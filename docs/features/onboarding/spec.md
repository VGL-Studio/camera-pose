---
feature: onboarding
status: draft
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-man-chao-xin-quyen-camera.md
design: design/README.md
created: 2026-10-21
confirmed:
---

# SPEC: MÀN CHÀO VÀ XIN QUYỀN CAMERA

## 1. Mục tiêu

- **Người dùng được gì:** lần đầu mở app, người dùng hiểu ngay app làm gì và vào camera chỉ sau 2 chạm, không gặp quảng cáo hay màn hình chen giữa.
- **Vì sao làm bây giờ:** phàn nàn số một về app mẫu là chuỗi quảng cáo và màn hình trước khi tới camera (bản đề xuất, bài học 1, 2). Giải thích trước khi xin quyền giúp người dùng ít từ chối quyền camera hơn (bài học 16).
- **Đo kết quả bằng gì:** tỷ lệ người dùng mới cho phép camera (`camera_permission_result` có kết quả cho phép, trên số `onboarding_complete`), mong đợi ít nhất 80%. Số chạm từ lúc màn loading xong tới camera có tư thế: đúng 2. Số quảng cáo toàn màn hình trước ảnh đầu tiên: 0.

## 2. Phạm vi

**Trong phạm vi:**

- Một màn chào duy nhất, hiện sau màn loading ở lần mở đầu tiên: hình minh họa, tiêu đề, một đoạn giới thiệu, ba điểm chính, nút "Bật camera", dòng ghi chú về quyền camera và nơi lưu ảnh.
- Bấm "Bật camera" thì máy hỏi quyền camera; dù người dùng cho phép hay từ chối, app đều sang camera.
- Màn chào chỉ hiện một lần. Từ lần mở sau, app vào thẳng camera.
- Chữ trên màn chào theo ngôn ngữ của máy, có đủ các ngôn ngữ phát hành (bản đề xuất, câu 12.1).

**Ngoài phạm vi lần này:**

- Nhiều trang giới thiệu, màn chọn ngôn ngữ, đăng nhập, quảng cáo.
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
- [ ] Khi bấm "Bật camera" thì app ghi `onboarding_complete` một lần; khi hệ thống trả kết quả quyền thì app ghi `camera_permission_result`.

Trường hợp đặc biệt:

- [ ] Khi bấm nút quay lại của máy ở màn chào thì app đóng.
- [ ] Khi xoay máy ở màn chào thì màn hình không bị cắt chữ hay nút; nội dung dài hơn màn hình thấp thì cuộn được, nút "Bật camera" luôn thấy được.
- [ ] Khi máy đặt ngôn ngữ chưa có trong app thì màn chào hiện tiếng Anh.

### KB-2. Mở app lần đầu, từ chối camera

Người dùng không muốn cho phép camera ngay.

Tiêu chí chấp nhận:

- [ ] Khi người dùng từ chối quyền thì app vẫn sang camera, ở trạng thái chưa có quyền (spec `camera`), không quay lại màn chào.
- [ ] Khi người dùng đã chọn "không hỏi lại" từ trước (máy không hiện hộp hỏi quyền nữa) và bấm "Bật camera" thì app sang thẳng camera ở trạng thái chưa có quyền.

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

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md) (gói bàn giao lập sau khi spec được xác nhận), dựng trên Figma theo hướng Sticker, frame `OnboardingScreen_default_light`, `OnboardingScreen_default_dark`.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Chào mừng | Giới thiệu app trong một màn, mở đường xin quyền camera | KB-1, KB-2, KB-3 |

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

Không có. Màn chào không hiện quảng cáo (bài học 1, câu 9.1).

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Đã qua màn chào (có hoặc không) | Lần sau vào thẳng camera | Đến khi người dùng xóa dữ liệu app |

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `onboarding_complete` | Khi bấm "Bật camera" ở màn chào | Không có | Bao nhiêu người dùng mới qua được màn chào |
| `camera_permission_result` | Khi hộp hỏi quyền của hệ thống trả kết quả, ở màn chào hoặc ở camera | `result`: `granted` hoặc `denied`; `source`: `onboarding` hoặc `camera` | Tỷ lệ cho phép camera, màn giải thích có giúp không |

## 7. File tĩnh trên R2

Không có. Hình minh họa vẽ bằng thành phần của hệ thống thiết kế, không tải file.

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_onboarding_enabled` | tắt | Bật để hiện màn chào ở lần mở đầu tiên. Bản staging bật sẵn |

## 9. Câu hỏi còn mở

1. **Ai dịch chữ sang 7 ngôn ngữ phát hành (Anh, Hindi, Bồ Đào Nha Brazil, Tây Ban Nha Mỹ Latinh, Indonesia, Thái, Việt)?** Câu này dùng chung cho mọi spec của app.
   - Đề xuất của AI: AI dịch trong cùng ticket với chữ tiếng Anh và tiếng Việt; một người biết tiếng duyệt trước khi phát hành. Ngôn ngữ gốc trong `strings.xml` là tiếng Anh, máy đặt ngôn ngữ chưa có thì hiện tiếng Anh.
   - Trả lời:
2. **Tư thế mặc định ở lần mở camera đầu tiên là tư thế nào?** Bản đề xuất ghi "tư thế đầu của nhóm Nữ hoặc nhóm mặc định đã chọn sẵn".
   - Đề xuất của AI: tư thế đầu tiên của nhóm "Tất cả" theo thứ tự trong catalog; người làm nội dung đặt một tư thế dễ, toàn thân, một người lên đầu. Chi tiết ở spec `camera`.
   - Trả lời:
3. **Điểm "Hơn 200 tư thế" chỉ đúng khi catalog đã đủ 200 tư thế (bài học 8: chữ chỉ nói điều app đã có).**
   - Đề xuất của AI: giữ chữ này; script kiểm catalog chặn phát hành khi chưa đủ 200. Nếu phát hành sớm hơn thì đổi chữ trước.
   - Trả lời:

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu (bản đề xuất đã duyệt) |
