---
feature: <ten-feature>
status: draft            # draft | confirmed | changed
owner: <chủ feature>
product_owner: <người phụ trách sản phẩm>
requests:
  - requests/YYYY-MM-DD-<tu-khoa>.md   # đường dẫn tính từ thư mục feature
design: design/README.md   # để trống nếu không cần thiết kế
created: YYYY-MM-DD
confirmed: YYYY-MM-DD    # người phụ trách sản phẩm điền khi xác nhận
---

# SPEC: <TÊN FEATURE>

<!--
Spec dành cho người phụ trách sản phẩm đọc và xác nhận.
Mục 1 đến 3 viết bằng ngôn ngữ nghiệp vụ, không dùng thuật ngữ kỹ thuật.
Mục nào không áp dụng thì ghi "Không có", không xóa mục.
-->

## 1. Mục tiêu

- **Người dùng được gì:** <một đến hai câu>
- **Vì sao làm bây giờ:** <lý do kinh doanh hoặc vấn đề của người dùng>
- **Đo kết quả bằng gì:** <chỉ số và mức mong đợi, ví dụ tỷ lệ người dùng quay lại ngày thứ 7>

## 2. Phạm vi

**Trong phạm vi:**

- <việc 1>
- <việc 2>

**Ngoài phạm vi lần này:**

- <việc không làm, để tránh hiểu nhầm>

## 3. Kịch bản sử dụng

### KB-1. <Tên kịch bản>

<Mô tả ngắn: ai, trong hoàn cảnh nào, làm gì.>

Tiêu chí chấp nhận:

- [ ] Khi <hành động hoặc tình huống> thì <kết quả nhìn thấy được>.
- [ ] Khi <...> thì <...>.

Trường hợp đặc biệt:

- [ ] Khi không có mạng thì <...>.
- [ ] Khi mở app lần đầu, chưa có dữ liệu thì <...>.

### KB-2. <Tên kịch bản>

Tiêu chí chấp nhận:

- [ ] Khi <...> thì <...>.

## 4. Giao diện

- **Thiết kế:** <đường dẫn tới design/README.md, hoặc "Dùng thành phần có sẵn, không cần thiết kế mới">
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| <tên> | <một câu> | KB-1 |

## 5. Quảng cáo

<!-- Vị trí quảng cáo là quyết định kinh doanh. AI không tự thêm vị trí ở đây. Theo docs/ops/ads-guide.md: định dạng ở mục 3, bảng "luôn giữ" ở mục 2; hành vi và tên khóa ở docs/ops/ads-business-rules.md (ADR-0026). -->

**Màn hình không có quảng cáo (nếu spec muốn giữ):** <tên màn hình, hoặc "Không có">

| Vị trí | Định dạng | Màn hình | Khi nào hiển thị | Giới hạn tần suất | Khóa bật tắt |
| --- | --- | --- | --- | --- | --- |
| <tên vị trí> | interstitial / native sau inter / banner / thanh đáy / native medium / native ô lưới / native giới thiệu / rewarded | <màn hình> | <thời điểm; interstitial ở hành động của người dùng> | <ví dụ: theo `ads_interval`> | `is_show_<vị trí>` hoặc khóa AdsKit của khe |

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| <tên> | <vì sao cần> | <ví dụ: đến khi người dùng xóa> |

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| <ten_su_kien> | <thời điểm> | <tham số> | <câu hỏi kinh doanh> |

**Quyền của app và dịch vụ bên ngoài:** <quyền mà feature cần hỏi người dùng (ví dụ camera, lưu ảnh), khi nào hỏi, trên phiên bản Android nào, khi bị từ chối thì sao; dịch vụ hay SDK bên ngoài mới mà feature cần. Hoặc "Không có". Đây là quyết định của người phụ trách sản phẩm, vì nó đổi khai báo trên Play Console (quyền, An toàn dữ liệu).>

## 7. File tĩnh trên R2

| Nhóm file | Dung lượng ước tính | Khi nào tải | Khi không tải được |
| --- | --- | --- | --- |
| <ví dụ: ảnh nền> | <...> | <lần đầu mở màn hình> | <hiển thị ảnh mặc định> |

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| <feature_xxx_enabled> | tắt | Bật để mở feature cho người dùng |

## 9. Câu hỏi còn mở

<!-- AI ghi những chỗ chưa rõ kèm phương án đề xuất. Người phụ trách sản phẩm trả lời ngay dưới câu hỏi. -->

1. **<Câu hỏi>**
   - Đề xuất của AI: <...>
   - Trả lời: <...>

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| YYYY-MM-DD | AI | Tạo bản đầu từ yêu cầu |
| YYYY-MM-DD | <tên> | Xác nhận |
