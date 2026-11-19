---
id: T-101
title: Chuẩn bị phát hành có Tenjin: khóa trong release.sh, Data safety, chính sách quyền riêng tư, báo UA
feature: ads
spec: features/ads/spec.md   # câu 21, ý e, h
scenarios: [KB-T1]           # tiêu chí cài từ chiến dịch với bản phát hành có khóa
owner: Nhật
status: backlog
risk: high
size: S
depends_on: [T-099, T-100]
sprint:
plan:
auto_run: false              # chạm scripts/release.sh và Play Console: người làm
created: 2026-11-17
---

# T-101: CHUẨN BỊ PHÁT HÀNH CÓ TENJIN

## Mô tả

Việc của người trước bản production đầu tiên có khóa Tenjin. Sau ticket này, bản phát hành gửi số liệu sang Tenjin và các khai báo trên Play khớp với dữ liệu app gửi.

## Phạm vi

- Tạo app trên Tenjin (production và một app thử nghiệm riêng), giữ khóa trong trình quản lý mật khẩu.
- Thêm bước nhận `-PtenjinSdkKey` vào `scripts/release.sh` (tùy chọn, như khóa MAX), đọc khóa `tenjinSdkKey` của `release.properties` trong thư mục phát hành; cùng lúc cập nhật khối hợp đồng `release.sh` và build của `scripts/tests/run.sh` (đang khẳng định `release.sh` xuất đúng 10 tham số Gradle).
- Khai Tenjin trong An toàn dữ liệu của Play Console (mã quảng cáo, mã thiết bị hay mã cài đặt, tương tác với app và quảng cáo, chẩn đoán; đủ danh sách ở spec ads mục 6) và cập nhật chính sách quyền riêng tư.
- Báo nhóm UA ngày bật Tenjin.
- Thử trên máy thật bản có khóa thử nghiệm: Tenjin ghi lượt cài và doanh thu.

**Không bao gồm:**

- Code của app (T-099, T-100).

## Ghi chú thử trên máy

Lần thử ngày 2026-11-19 sau T-100. Máy thật: Xiaomi 23021RAAEG. Bản staging debug build trên máy dev, khóa thử trong `local.properties`.

- **Lần 1, máy chủ không nhận app.** Tenjin trả `{"code":404,"message":"no such app"}` cho mọi lần kết nối, nên không sự kiện nào được gửi. Yêu cầu gửi đi có `bundle_id=com.vglstudio.camera.pose.line.guide.staging`. Nguyên nhân: app thử trên Tenjin chưa mang tên gói của bản staging. Đã sửa: app thử trên Tenjin đổi sang đúng tên gói đó, dùng khóa mới.
- **Lần 2, kết nối và phễu đều qua.** Kết nối trả `{"code":200,"success":true}`. Có 28 sự kiện phễu, tất cả trả 200, tên không có `af_`, không tham số:
  - native: `native_ad_eligible`, `native_api_called`, `native_ad_displayed`;
  - interstitial: `inters_api_called`, và lúc hiện `inter_pose_detail` thì có `inters_ad_eligible`, `inters_displayed`, `inters_displayed_1_times`;
  - rewarded: `rewarded_api_called`.
- **Doanh thu chưa kiểm được.** App ghi 9 lượt có doanh thu, nhưng Tenjin không nhận yêu cầu doanh thu nào. Không có dòng chặn hay lỗi của Tenjin. Khả năng cao nhất: bản staging dùng quảng cáo thử của Google, thường báo giá trị 0, và T-099 cố ý không gửi lượt giá trị 0, không ghi log. Chưa xác nhận được, vì log không in giá trị. Cần kiểm bằng bản có mã quảng cáo thật (mục "Bản phát hành có khóa ghi lượt cài và doanh thu").
- **Log `Tenjin connected` dễ hiểu nhầm.** Dòng này chỉ có nghĩa là app đã gọi `connect()`, kể cả khi máy chủ trả 404. Muốn biết máy chủ có nhận hay không thì xem dòng `Tenjin::connect response` của SDK.
- **SDK gửi nhiều yêu cầu khi kết nối thất bại.** Khi chưa kết nối được, mỗi sự kiện phễu khiến SDK thử kết nối lại: khoảng 20 yêu cầu HTTP trong 40 giây đầu, không làm hỏng app. Có thể xét một ticket riêng nếu muốn giảm số yêu cầu này. SDK cũng ghi mọi yêu cầu gửi sự kiện (`v0/event`) dưới nhãn `Tenjin::connect`.
- **Cài qua USB trên Xiaomi.** Bản staging đã cài từ App Distribution (versionCode theo số lần chạy CI) không cài đè được bằng bản build trên máy dev (versionCode 2), nên phải gỡ trước. MIUI còn đòi bấm "Cài đặt" trên máy ở mỗi lần cài qua USB, nếu không bấm thì báo `INSTALL_FAILED_USER_RESTRICTED`.

## Rủi ro sơ bộ

Cao: chạm `scripts/release.sh` và cấu hình phát hành, chỉ người làm (ADR-0049).

## Định nghĩa hoàn thành

- [ ] Bản phát hành có khóa ghi lượt cài và doanh thu trên Tenjin.
- [ ] Data safety và chính sách quyền riêng tư đã có Tenjin trước khi bản lên Play.
- [ ] Nhóm UA đã được báo ngày bật.
