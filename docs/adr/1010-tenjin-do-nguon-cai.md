---
id: ADR-1010
title: Dùng Tenjin trong :core:analytics để đo nguồn cài, doanh thu quảng cáo và phễu quảng cáo; khóa đưa vào lúc build, không khóa thì không gửi
status: accepted             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-11-17
deciders: [Nhật]
supersedes:                  # không thay ADR nào: ADR-1007 vẫn giữ AppsFlyer tắt; ADR-0027 chỉ áp dụng phần không cần SDK AppsFlyer
superseded_by:
---

# ADR-1010: DÙNG TENJIN TRONG :CORE:ANALYTICS ĐỂ ĐO NGUỒN CÀI, DOANH THU QUẢNG CÁO VÀ PHỄU QUẢNG CÁO

## Bối cảnh

App chưa có công cụ đo nguồn cài nào. Khung của dự án mẫu dùng AppsFlyer (ADR-0027), nhưng app tắt AppsFlyer: không có SDK, `NoOpAppsFlyerSink`, `AppsFlyerConfig("")` (ADR-1007). Nhóm thu hút người dùng (UA) cần biết chiến dịch nào mang lại người dùng có doanh thu quảng cáo.

Nhật chọn Tenjin, thay chỗ AppsFlyer, là quyết định riêng của app (spec ads, câu 21; yêu cầu 2026-11-17). Tenjin khác AppsFlyer ở ba điểm ảnh hưởng tới code:

- Tenjin chỉ tính doanh thu quảng cáo từ hàm doanh thu quảng cáo riêng của nó (với AdMob: hàm nhận giá trị lượt hiện mà AdMob báo), không từ sự kiện thường. Luật của AppsFlyer ngược lại (`docs/ops/ads-business-rules.md`, mục 8.3).
- Sự kiện thường của Tenjin chỉ có tên, kèm tùy chọn một số nguyên; không có tham số.
- Khóa SDK Tenjin là cấu hình của từng app. Repo không chứa bí mật (ADR-0007), CI và phiên tự chạy build không cần bí mật.

## Quyết định

1. **Vị trí:** SDK Tenjin chỉ nằm trong `:core:analytics`, sau một interface nội bộ của module (như `AppsFlyerSink`), cạnh Firebase Analytics. Một lớp theo dõi Tenjin nhận `AdMetrics` và `AdFunnel` của module và được thêm vào luồng chia sự kiện có sẵn (`FanOutAdMetrics`). `:core:ads` chỉ thêm một tín hiệu "đồng ý đã rõ" gửi qua một interface công khai của `:core:analytics` (mục 3); không gọi Tenjin. Feature và `:app` không gọi SDK Tenjin; `:app` chỉ cấp cấu hình (khóa) và gọi khởi động qua interface của `:core:analytics`, như AppsFlyer.
2. **Khóa SDK:** production nhận khóa lúc build bằng tham số `-PtenjinSdkKey`, tùy chọn, như khóa MAX (`ads-business-rules.md`, M-09). Không có khóa thì Tenjin không khởi động, không gửi gì, ghi một dòng log. Staging của CI không có khóa; dev thử trên máy thì đặt `tenjin.sdkKey` trong `local.properties` (không vào git), dùng một app Tenjin riêng cho thử nghiệm. Đưa khóa vào `scripts/release.sh` là việc của người.
3. **Đồng ý:** chỉ khởi động Tenjin khi khung khởi động SDK số liệu (cùng chỗ khung gọi khởi động AppsFlyer: sau kết quả hỏi đồng ý của lượt mở app, hay ngay khi đồng ý của phiên trước còn giữ; hết thời gian chờ đồng ý của màn loading thì khởi động với đồng ý đang có). Trước mỗi lần kết nối và mỗi lượt gửi, gọi hàm của Tenjin đọc đồng ý từ chuỗi chuẩn mà UMP ghi; không đồng ý thì app không gọi kết nối và không gửi lượt nào. Không dựa vào chế độ opt out của SDK: ở chế độ đó SDK vẫn gửi lượt kết nối (mã cài đặt, nguồn cài) kèm cờ `opt_out` (đọc mã SDK 2.0.2). Tenjin chỉ kết nối và gửi khi đồng ý đã rõ (spec ads câu 21 ý i): vòng hỏi đồng ý của lượt mở đã có câu trả lời (xong và chốt, kể cả từ chối và "không cần hỏi"), hoặc xong vì cập nhật lỗi mà UMP vẫn giữ đồng ý của phiên trước. `:core:ads` báo tín hiệu này; hết thời gian chờ của màn loading hay vòng hỏi lỗi mạng không phải đồng ý đã rõ. Không gửi dữ liệu cá nhân, chữ người dùng nhập hay mã người dùng.
4. **Doanh thu quảng cáo:** mỗi lượt hiện có doanh thu mạng báo gửi một lần qua hàm doanh thu quảng cáo của Tenjin đúng với nền tảng của lượt (AdMob; MAX khi MAX bật). Không gửi lượt dự phòng giá trị 0. Không gửi doanh thu dưới dạng sự kiện thường.
5. **Phễu quảng cáo:** cùng bộ sự kiện và điều kiện gửi như `ads-business-rules.md` mục 8.3, bỏ tiền tố `af_`, không tham số: `inters_ad_eligible`, `inters_api_called`, `inters_displayed`, `rewarded_ad_eligible`, `rewarded_api_called`, `rewarded_ad_displayed`, `native_ad_eligible`, `native_api_called`, `native_ad_displayed`, `inters_displayed_{N}_times` (N từ 1 tới 20).
6. **Công tắc từ xa:** mỗi sự kiện Tenjin, kể cả doanh thu, có khóa riêng `is_log_tenjin_<tên sự kiện>`, boolean, mặc định bật, đọc ở mỗi sự kiện. Không dùng lại khóa `is_log_af_*`.
7. **Lỗi:** lỗi của SDK Tenjin bị bắt và ghi log theo tên lớp, quảng cáo và Firebase vẫn chạy.
8. **Test:** test không gọi SDK Tenjin; có bản giả của interface để kiểm tên sự kiện, doanh thu và cổng khóa, đồng ý, công tắc. Manifest gộp kiểm bằng `MergedManifestTest`: không có quyền mới.
9. **Không làm:** không đo mua hàng (app không bán gì), không bật đo nguồn cài của Meta (cần khai báo `queries` và mã app Facebook), không bật AppsFlyer.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tenjin trong `:core:analytics`, sau interface, khóa tùy chọn (đã chọn) | Đúng lựa chọn của nhóm UA; `:core:ads` chỉ thêm một tín hiệu đồng ý; CI build không cần khóa | Lệch dự án mẫu thêm một phần riêng ở `:core:analytics` mà đợt đồng bộ phải giữ |
| Bật AppsFlyer như dự án mẫu (ADR-0027) | Không lệch mẫu | Nhóm chọn Tenjin; spec ads câu 3 giữ AppsFlyer tắt |
| Module core riêng cho Tenjin | Tách hẳn khỏi phần lấy từ mẫu | Thêm module cho một SDK; vẫn cần `:core:ads` báo tín hiệu đồng ý đã rõ, nên không bớt lệch mẫu ở `:core:ads` |
| Khóa bắt buộc ở production | Không thể phát hành quên khóa | Chặn phát hành khi chưa có app Tenjin; khác cách của khóa MAX |
| Doanh thu qua sự kiện thường như AppsFlyer | Giống luật mục 8.3 | Tenjin không tính vào ROAS |

## Hệ quả

- **Tích cực:** nhóm UA có nguồn cài và ROAS theo chiến dịch; staging và CI không gửi gì, không cần bí mật; tắt khẩn từng sự kiện từ xa.
- **Phải chấp nhận:**
  - Thêm SDK Tenjin (và thư viện Install Referrer, App Set nếu chưa có) vào `gradle/libs.versions.toml`, file được bảo vệ: plan rủi ro cao, duyệt chéo.
  - Mất tham số `ad_format`, `ad_unit_id`, `ad_source` của phễu trên Tenjin.
  - Data safety và chính sách quyền riêng tư phải có Tenjin trước bản production đầu tiên có khóa; báo nhóm UA ngày bật.
  - Đợt đồng bộ dự án mẫu phải giữ phần Tenjin của `:core:analytics` và phần riêng của `:core:ads` (tín hiệu đồng ý đã rõ trong `AdsController`, hàm của `AttributionStart.kt`).
- **Việc AI phải tuân thủ:** chỉ gọi SDK Tenjin trong `:core:analytics`, sau interface nội bộ của module; không viết khóa SDK Tenjin vào code, tài liệu, lệnh build ghi sẵn hay file trong repo; build không có khóa Tenjin thì không khởi động Tenjin và không gửi gì; chỉ khởi động Tenjin cùng lúc khung khởi động SDK số liệu sau đồng ý, đọc đồng ý của UMP qua Tenjin ngay trước mỗi lần kết nối và mỗi lượt gửi, không đồng ý thì không gọi kết nối hay gửi, không dựa vào chế độ opt out của SDK; chỉ kết nối và gửi sang Tenjin khi `:core:ads` đã báo đồng ý đã rõ, không kết nối theo đường hết thời gian chờ; doanh thu quảng cáo sang Tenjin chỉ qua hàm doanh thu quảng cáo của Tenjin, một lần mỗi lượt hiện có doanh thu, không gửi lượt giá trị 0; sự kiện phễu sang Tenjin đúng danh sách của ADR này, không tham số; mỗi sự kiện Tenjin đi qua khóa `is_log_tenjin_*` của nó; không gửi sự kiện Tenjin ngoài ADR này và spec đã duyệt; không gửi dữ liệu cá nhân hay chữ người dùng nhập sang Tenjin; không chạm `scripts/release.sh` để thêm khóa Tenjin.
