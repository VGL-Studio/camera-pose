---
id: ADR-0037
title: Thêm AppLovin MAX làm nền tảng quảng cáo thứ hai bên cạnh AdMob; Amazon APS đi kèm MAX khi có tài khoản
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes:                  # thay vế "AdMob là nền tảng điều phối mediation" của ADR-0004 và mục M-01, M-03 của docs/ops/ads-business-rules.md (xem mục Quyết định)
superseded_by:
---

# ADR-0037: THÊM APPLOVIN MAX LÀM NỀN TẢNG QUẢNG CÁO THỨ HAI BÊN CẠNH ADMOB

## Bối cảnh

ADR-0004 chọn AdMob điều phối mediation; ADR-0026 giữ chín mạng con qua adapter của AdMob, AppLovin là một mạng con (M-01). Gói JBase (yêu cầu `features/ads/requests/2026-10-29-hoc-tu-jbase.md`) chạy song song hai nền tảng: AppLovin MAX và AdMob, với bốn chế độ (chỉ AdMob, chỉ MAX, AdMob trước, MAX trước; mặc định MAX trước), và gọi Amazon APS trước mỗi lượt tải của MAX (banner, MREC, interstitial, rewarded). Khi nền tảng thứ nhất chưa có quảng cáo, nền tảng kia lấp chỗ. Người phụ trách sản phẩm chọn thêm MAX và APS ngày 2026-10-29, và chọn nghiệp vụ của JBase làm chuẩn (ADR-0038). Chi tiết chốt ở spec `ads`, câu hỏi 41 tới 47.

## Quyết định

- Thêm SDK AppLovin MAX vào `:core:ads`. Feature vẫn chỉ gọi composable và hàm của `:core:ads` với một vị trí, không biết quảng cáo đến từ nền tảng nào.
- **Định dạng (câu 41):** MAX cho banner, MREC, interstitial, rewarded, app open; native chỉ qua AdMob (phần vẽ và luật native viết trên `NativeAdView` của AdMob; JBase cũng chỉ dùng native của AdMob). Quảng cáo mở app lúc khởi động chỉ dùng app open của AdMob, như JBase; app open của MAX dùng khi quay lại app.
- **Thứ tự (câu 42):** khóa Remote Config `ads_mediation_order` (string, tên do ta đặt): `admob`, `max`, `admob_max`, `max_admob`; mặc định `max_admob` như `MaxPrimary` của JBase. Mỗi lượt hiện hỏi nền tảng thứ nhất, chưa có thì nền tảng thứ hai, trong cùng một lượt quyết định và một cơ hội; nền tảng có trong thứ tự thì được nạp sẵn.
- **Khóa (câu 43):** khóa SDK MAX, mã đơn vị MAX theo khe là cấu hình của từng app, đưa vào lúc build phát hành như `-PadUnits`, không vào repo; staging không có khóa thì MAX không khởi tạo và thứ tự coi như `admob`; dev thử MAX ở staging bằng `local.properties`. Đây thay M-03 cho MAX: khác adapter AppLovin của AdMob, MAX cần khóa trong app.
- **Đồng ý (câu 44):** MAX chỉ khởi tạo khi cổng đồng ý của `:core:ads` mở lần đầu (S-01 tới S-19), đọc chuỗi đồng ý của UMP; không dùng luồng đồng ý riêng của MAX, không viết cứng "đã đồng ý" như JBase. Rút đồng ý thì đóng như AdMob (S-15).
- **AppLovin hai vai (câu 45):** app dùng MAX thì nhóm mediation AdMob không có AppLovin, còn MAX gọi Google qua adapter Google của MAX, như JBase; ghi trong hướng dẫn phát hành.
- **Doanh thu (câu 46):** cùng sự kiện của dự án mẫu (`ad_impression_placement`, `af_ad_revenue`), `ad_platform` là `applovin`, `mediation_network` là `applovinmax`, giá trị MAX theo đô la; phễu `af_inters_*`, `af_rewarded_*` cho cả hai nền tảng.
- **Luật toàn màn hình:** mọi lượt của MAX đi qua cùng cổng không chồng nhau, gỡ kẹt, chặn quảng cáo tự hiện và cùng mốc `ads_interval` với AdMob (F-01 tới F-07, mục 4, mục 6). App open của MAX nạp lại 2 giây sau khi đóng như JBase; thử lại khi lỗi theo F-05 (ADR-0038, quyết định 5).
- **APS (câu 47):** APS chỉ đi kèm MAX, cho banner, MREC, interstitial, rewarded; chế độ thử chỉ ở staging. Nhóm chưa có tài khoản Amazon Publisher Services (2026-10-29): làm MAX trước; APS tách ticket khi có tài khoản, mã ứng dụng và slot.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| MAX cho banner, MREC, toàn màn hình; AdMob giữ native (đã chọn) | Thêm nguồn ở định dạng có doanh thu cao; giữ nguyên phần native và luật đã có; đúng cách JBase dùng | Hai bộ khe, hai đường tải; thứ tự giữa hai nền tảng phải đo |
| Hai nền tảng cho mọi định dạng | Lấp chỗ ở mọi định dạng | Native của MAX cần phần vẽ riêng, gấp đôi pool và test native; JBase không dùng native của MAX |
| MAX là nền tảng điều phối, AdMob thành mạng con của MAX | Một nền tảng | Viết lại gần hết `:core:ads`, UMP và luật PARTNER |
| Giữ AdMob điều phối chín mạng (ADR-0004) | Không đổi gì | Người phụ trách sản phẩm chọn thêm MAX |

## Hệ quả

- **Tích cực:** thêm một nền tảng đấu giá; quảng cáo toàn màn hình, banner, MREC ít khi không có.
- **Phải chấp nhận:** thêm SDK MAX và các adapter của nó, cùng quyền chúng kéo vào manifest (đo ở plan, ghi vào spec mục 6); An toàn dữ liệu trên Play khai thêm; mỗi app cần thêm một bảng mã đơn vị MAX; luật PARTNER (một nguồn mỗi khe) mở rộng thành hai nền tảng; số liệu doanh thu có thêm `ad_platform` khác `admob` (báo nhóm UA trước khi phát hành, HD-07).
- **Việc AI phải tuân thủ:** SDK MAX và APS chỉ nằm trong `:core:ads`; MAX và APS khởi tạo sau cổng đồng ý của `:core:ads`, không bao giờ trước, không viết cứng trạng thái đồng ý; khóa SDK MAX, mã đơn vị MAX và mã APS không vào repo, tài liệu hay resource của module; mọi luồng của MAX đi qua cùng cổng hiện toàn màn hình và cùng mốc `ads_interval` với AdMob; native không đi qua MAX; adapter của MAX ghi phiên bản cụ thể trong `gradle/libs.versions.toml`, không dùng `+`; không tách ticket APS khi chưa có tài khoản Amazon Publisher Services.
