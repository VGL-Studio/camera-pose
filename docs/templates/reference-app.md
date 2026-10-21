---
package: <tên gói của app mẫu>
name: <tên hiển thị>
version: <versionName>
surveyed_at: YYYY-MM-DD
surveyed_on: <máy thật hay emulator, Android, độ phân giải, mật độ>
network: <có mạng | không có mạng>   # không có mạng thì quảng cáo và nội dung tải về không hiện
status: draft                          # draft | reviewed. Chỉ người ghi reviewed
---

# APP MẪU: <TÊN HIỂN THỊ>

<!--
Báo cáo khảo sát một app mẫu (ADR-0021, docs/ops/clone-study.md). AI viết, dev xem lại.
Mỗi nhận xét dẫn về ảnh (`NN-ten`, nằm trong captures/<package>/ của thư mục làm việc, không vào repo) hay dòng cây giao diện. Không đoán: chưa xem được thì ghi ở mục 9.
Không chép mã quảng cáo, khóa, token, mã định danh máy hay tên tài khoản; trích log thì che (ca-app-pub-<đã che>).
-->

## 1. Thông tin

Tên, nhà phát triển, lượt cài và điểm trên Play (lấy từ báo cáo web nếu có), cùng mã nguồn với app nào (lấy từ phân tích APK).

## 2. Lần mở đầu

Từng bước theo thứ tự, kèm ảnh: splash bao lâu, chọn ngôn ngữ, giới thiệu mấy trang, xin quyền lúc nào, hộp thoại đồng ý quảng cáo, quảng cáo nào hiện ở đâu. Ghi số bước và số quảng cáo từ lúc mở app tới màn hình chính và tới chức năng chính.

## 3. Bản đồ màn hình

| Màn hình | Mục đích | Vào từ đâu | Ảnh |
| --- | --- | --- | --- |

Luồng chính dạng chữ: `A > B > C`.

## 4. Tính năng

Từng tính năng: cách hoạt động, độ sâu (số lượng nội dung, tùy chọn), chỗ làm tốt, chỗ làm dở.

## 5. Chức năng chính

Chức năng mà người dùng cài app để dùng (ví dụ camera có lớp phủ): mọi điều khiển, giới hạn, luồng sau khi dùng xong.

## 6. Kiếm tiền

| Loại quảng cáo | Vị trí | Thời điểm | Tần suất | Thời gian chờ đóng | Ảnh |
| --- | --- | --- | --- | --- | --- |

Gói trả phí: giá, chu kỳ, dùng thử, quyền lợi, những gì bị khóa. Chỉ xem, không mua.

## 7. Giao diện

Theme, màu chính (mã hex lấy mẫu từ ảnh), phông chữ, cỡ chữ ước lượng, bộ icon. Bố cục các màn hình chính với số đo dp từ cây giao diện: thanh trên, lưới, thẻ, nút chính, thanh điều hướng. Đủ để dựng lại trên Figma.

## 8. Điểm mạnh, điểm yếu

Quan sát được trong lúc dùng: dễ dùng, chậm, quảng cáo dày, lỗi, chỗ gây lúng túng.

## 9. Chưa xem được

Màn hình hay tính năng chưa vào được, lý do, và cần người làm gì.

## 10. Sự cố trong lúc khảo sát

Dòng mới trong `incidents.log` (mở nhầm quảng cáo, Play, app khác), app văng, mất kết nối. File để lại trên máy cần người dọn: danh sách từ `ax.sh newfiles`.
