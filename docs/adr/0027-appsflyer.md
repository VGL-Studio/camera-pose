---
id: ADR-0027
title: Dùng AppsFlyer trong :core:analytics cho doanh thu quảng cáo và phễu quảng cáo, khóa dev đưa vào lúc build, staging không gửi
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-23
deciders: [Người phụ trách sản phẩm]
supersedes:
superseded_by:
---

# ADR-0027: DÙNG APPSFLYER TRONG :CORE:ANALYTICS CHO DOANH THU QUẢNG CÁO VÀ PHỄU QUẢNG CÁO, KHÓA DEV ĐƯA VÀO LÚC BUILD, STAGING KHÔNG GỬI

## Bối cảnh

ADR-0026 cho hành vi quảng cáo theo bộ PARTNER của AdsKit. Các app dùng AdsKit gửi doanh thu quảng cáo và phễu quảng cáo sang AppsFlyer: nhóm đo hiệu quả chiến dịch thu hút người dùng (doanh thu trên chi phí) và đối tác kiểm SDK bằng các sự kiện này. Dự án mẫu chưa có AppsFlyer; số liệu chỉ ở Firebase (ADR-0012).

AdsKit đã gặp hai lỗi ở phần này (`docs/ops/ads-business-rules.md`, mục 12, L-A20): gửi cả `af_ad_revenue` và `af_revenue` làm doanh thu nhân đôi; dùng hàm ghi doanh thu quảng cáo riêng của AppsFlyer thì công cụ kiểm của đối tác không nhận. AdsKit để app tự khởi tạo AppsFlyer với khóa dev.

Khóa dev của AppsFlyer là bí mật của từng app. Repo không chứa bí mật (ADR-0007, ADR-0009); CI và phiên tự chạy build không cần bí mật (ADR-0012).

## Quyết định

1. **Vị trí:** SDK AppsFlyer chỉ nằm trong `:core:analytics`, cạnh Firebase Analytics, sau một interface của module. `:core:ads` gửi doanh thu và phễu quảng cáo qua interface đó (core phụ thuộc core, ADR-0001). Feature và vỏ app không gọi SDK AppsFlyer; `:app` chỉ gọi hàm khởi tạo của `:core:analytics` trong `Application`.
2. **Khóa dev:**
   - Production: tham số build `-PappsFlyerDevKey`, như mã quảng cáo (ADR-0013). Thiếu tham số thì build production hỏng ngay với thông báo rõ. Người phát hành lấy khóa từ trình quản lý mật khẩu; thêm bước này vào `scripts/release.sh` là việc của người hoặc của một plan rủi ro cao được duyệt chéo.
   - Staging và CI: không có khóa trong repo. Build không có khóa thì AppsFlyer không khởi động, `:core:analytics` dùng bản không gửi gì và ghi một dòng log, giống cách file Firebase giả không gửi số liệu (ADR-0012). Muốn thử AppsFlyer trên staging thì dùng khóa của một app AppsFlyer riêng cho staging, đưa vào qua bí mật của `staging.yml` (sửa `.github/` là plan rủi ro cao).
3. **Đồng ý và quyền riêng tư:**
   - Bật chế độ SDK tự đọc chuỗi đồng ý TCF mà UMP ghi (tên hàm và phiên bản SDK tối thiểu chốt ở plan).
   - Chỉ khởi động AppsFlyer sau khi có kết quả hỏi đồng ý của lượt mở app, hoặc ngay khi UMP còn giữ đồng ý của phiên trước, cùng lúc với SDK quảng cáo (`ads-business-rules.md`, S-03). Hết thời gian chờ đồng ý của màn loading (10 giây) thì khởi động với trạng thái đồng ý đang có.
   - Không gửi dữ liệu cá nhân, chữ người dùng nhập hay mã người dùng; app không có tài khoản nên không đặt mã khách hàng.
   - Data safety của mỗi app khai AppsFlyer: mã quảng cáo của máy, dữ liệu tương tác với app và quảng cáo, chẩn đoán.
4. **Sự kiện:** đúng danh sách ở `ads-business-rules.md`, mục 8.3: `af_ad_revenue` cho mọi lượt hiển thị quảng cáo (kể cả lượt dự phòng giá trị 0), phễu `af_inters_*`, `af_rewarded_*`, `af_native_*`, `af_inters_displayed_{N}_times`. Gửi bằng hàm ghi sự kiện thường. Không bao giờ gửi `af_revenue`. Không gửi số liệu vòng đời quảng cáo của Firebase sang AppsFlyer. Mỗi sự kiện có công tắc từ xa riêng (khóa `is_log_af_*`, mặc định bật như AdsKit, `ads-business-rules.md` mục 10), đổi giữa phiên có hiệu lực từ sự kiện kế tiếp, để tắt khẩn một sự kiện làm hỏng số liệu. Sự kiện khác (mua hàng, sự kiện của feature) chỉ thêm theo spec đã duyệt.
5. **Test:** test không gọi AppsFlyer; `:core:analytics` có bản giả ghi lại sự kiện để `:core:ads` kiểm tên và tham số.
6. **Phát hành:** trước bản đầu tiên gửi `ad_placement` sang AppsFlyer hay gửi sự kiện đếm inter, báo nhóm thu hút người dùng (UA) và ghi ngày đổi lên dashboard (`ads-business-rules.md`, HD-07).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| AppsFlyer trong `:core:analytics` (đã chọn) | Cùng cách với các app AdsKit và đối tác; một chỗ cho mọi SDK số liệu | Thêm một SDK, thêm khai báo Data safety, thêm một bí mật khi phát hành |
| Module core riêng cho AppsFlyer | Tách hẳn | Thêm module cho một SDK; `:core:analytics` đã là chỗ của số liệu |
| Chỉ Firebase, không AppsFlyer | Không thêm SDK | Không đo được chiến dịch thu hút người dùng như các app khác; đối tác không kiểm được |
| Adjust hay mạng đo khác | | Khác các app đang chạy; đối tác dùng AppsFlyer |
| Hàm ghi doanh thu quảng cáo riêng của AppsFlyer | Đúng loại sự kiện doanh thu | Công cụ kiểm của đối tác không nhận (AdsKit commit `0088661`) |

## Hệ quả

- **Tích cực:** doanh thu quảng cáo theo vị trí và phễu quảng cáo có ở AppsFlyer như các app AdsKit; CI, phiên tự chạy và máy của dev build được mà không cần khóa.
- **Phải chấp nhận:**
  - Thêm SDK và thư viện (`gradle/libs.versions.toml`, file được bảo vệ): plan rủi ro cao, ghi quyền SDK kéo vào manifest gộp.
  - Bản staging mặc định không gửi gì sang AppsFlyer; kiểm sự kiện trên staging cần khóa staging riêng hoặc dựa vào log của bản không gửi.
  - Mỗi app có thêm một bí mật phải giữ và một bước phát hành.
- **Việc AI phải tuân thủ:** chỉ gọi SDK AppsFlyer trong `:core:analytics`; không viết khóa dev AppsFlyer vào code, tài liệu, tham số build hay file trong repo; không gửi `af_revenue`, doanh thu quảng cáo chỉ qua `af_ad_revenue` bằng hàm ghi sự kiện thường; build không có khóa thì không khởi động AppsFlyer; không thêm sự kiện AppsFlyer ngoài `ads-business-rules.md` mục 8.3 và spec đã duyệt; mỗi sự kiện AppsFlyer đi qua công tắc từ xa của nó; không gửi dữ liệu cá nhân hay chữ người dùng nhập sang AppsFlyer.
