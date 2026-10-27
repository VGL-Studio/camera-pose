---
id: ADR-0022
title: Quảng cáo theo nguyên tắc chính sách trước doanh thu, native thay banner và MREC theo quy chuẩn dp của nhóm, mediation AdMob và AppLovin
status: superseded
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-21
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
supersedes:                  # thay một phần: mạng mediation của ADR-0004, danh sách composable của ADR-0013
superseded_by: ADR-0026         # thay phần lớn, xem ghi chú dưới tiêu đề
---

# ADR-0022: QUẢNG CÁO THEO NGUYÊN TẮC CHÍNH SÁCH TRƯỚC DOANH THU, NATIVE THAY BANNER VÀ MREC THEO QUY CHUẨN DP CỦA NHÓM, MEDIATION ADMOB VÀ APPLOVIN

> **Ghi chú 2026-10-23:** người phụ trách sản phẩm quyết định hành vi quảng cáo theo bộ PARTNER của AdsKit. ADR-0026 (duyệt 2026-10-24) thay phần lớn ADR này: mục 1 (bảng "không bao giờ làm" và nguyên tắc chính sách trước doanh thu), mục 2, 3, 6, 7, 8, 9 và số đo của mục 4. Còn hiệu lực: mục 5 và các quy tắc chung của mục 4 được giữ ở ADR-0026, mục 10. Code đợt 2 theo ADR này vẫn merge, rồi sửa bằng ticket T-019 tới T-036.

## Bối cảnh

`:core:ads` hiện có banner và interstitial (ADR-0013, ADR-0014). Nhóm dùng native để thay một số loại thường: native small thay banner, native medium thay MREC, native trong danh sách, native thu gọn được. Hai nguồn cũ có quy chuẩn cho việc này nhưng không dùng thẳng được (đọc ngày 2026-10-21):

- Repo `vgl-docs`: viết cho game Unity, tự mâu thuẫn ở nhiều chỗ (ít nhất 5 bảng khóa Remote Config lệch tên, chu kỳ làm mới banner ghi 10, 30 và 60 giây), có cách làm rủi ro chính sách: native toàn màn hình ép xem 3 giây ngay sau interstitial, biểu tượng AdChoices tự vẽ, quảng cáo lúc mở app, banner làm mới mỗi 10 giây, native thu gọn tự bung lại, phát hành lại bằng tên gói mới sau khi bị đình chỉ.
- File "Quy chuẩn Native": 5 loại native (small, medium, inlist, collap, fullscreen), nhãn Ad bắt buộc, không méo ảnh; nhưng tính bằng px, thiếu trạng thái đang tải và không có quảng cáo, và có chỗ khen đặt nút để "dẫn đến misclick và tăng CTR".

Khảo sát app mẫu cho app clone (ADR-0021) cho thấy hậu quả của cách làm đó: quảng cáo native hiện lên đúng chỗ nút của app, banner thu gọn tự bung lại che điều khiển, công cụ khảo sát chạm nhầm hai lần; phàn nàn số một trên Play của các app đó là quảng cáo.

Tài liệu của Google (tra ngày 2026-10-21): khi liên kết AdMob với Firebase, sự kiện `ad_impression` được ghi tự động; MediaView nhỏ hơn 120 dp ở một chiều thì không nhận quảng cáo video; native phải hiện nhãn quảng cáo và AdChoices.

## Quyết định

1. **Chính sách trước doanh thu.** `docs/ops/ads-guide.md` có bảng "không bao giờ làm", mỗi dòng dẫn tới chính sách AdMob hay Play. Không có ngoại lệ vì doanh thu. Gồm: đặt quảng cáo để gây bấm nhầm (sát nút của app, hiện lên đúng chỗ người dùng sắp chạm, CTA giống nút của app); quảng cáo toàn màn hình lúc mở app hay trước khi người dùng làm xong hành động chính lần đầu; ép chờ trước khi đóng được quảng cáo; tự vẽ hay che AdChoices và nhãn quảng cáo; native thu gọn tự bung lại; làm mới quảng cáo nhanh hơn mức Google cho phép.
2. **Vùng không quảng cáo:** màn hình chức năng chính của app (ghi trong spec) và luồng mở đầu lần đầu không có quảng cáo.
3. **Định dạng.** `:core:ads` hỗ trợ: banner thích ứng (đã có), interstitial (đã có), **native small** (thay banner), **native medium** (thay MREC), **native in-list** (small hay medium chèn trong danh sách), **native collapsible** (medium thu gọn thành small khi người dùng chạm, không tự bung lại), **native toàn màn hình** (thay interstitial ở điểm dừng tự nhiên, theo hướng dẫn full-screen native của Google: nút đóng thấy và bấm được ngay từ đầu, nút quay lại của máy cũng đóng, không ép chờ, tính chung giới hạn tần suất với interstitial, không liền sau một quảng cáo toàn màn hình khác, không trong luồng mở đầu). App open và rewarded chưa làm; thêm sau bằng ADR mới khi một app cần.
4. **Quy chuẩn native tính bằng dp, do thiết kế của nhóm quyết.** File Excel chỉ để tham khảo. Native dựng bằng Compose bọc `NativeAdView`, dùng màu, chữ, bo góc của `:core:designsystem` để giống giao diện app; các cỡ native là component trong hệ thống thiết kế (Figma và code). Quy tắc chung: nhãn "Ad" và AdChoices luôn thấy được; MediaView ít nhất 120 x 120 dp; giữ chỗ khi đang tải để bố cục không nhảy; không có quảng cáo thì thu chỗ lại sau thời gian chờ, trừ khi vị trí nằm cạnh nút của app (khi đó giữ chỗ để nút không dịch); cách phần tử bấm được của app ít nhất 16 dp và có ranh giới nhìn thấy được.
5. **Feature dùng quảng cáo qua composable của `:core:ads`.** Ngoài `AdSlot`, `rememberShowInterstitial`, `PrivacyOptionsButton` (ADR-0013), thêm composable cho native (tên chốt ở plan của đợt code). Feature không gọi SDK quảng cáo, không tự dựng `NativeAdView`.
6. **Tần suất lưu qua các phiên.** Khoảng cách giữa hai quảng cáo toàn màn hình và thời gian ân hạn của phiên đầu lưu trên máy, không chỉ trong bộ nhớ. Mọi giới hạn là khóa Remote Config (ADR-0023).
7. **Công tắc tổng** `ads_enabled` (mặc định bật) tắt mọi quảng cáo ngay khi có sự cố chính sách; từng vị trí vẫn có `ads_<vị trí>_enabled` mặc định tắt (ADR-0014).
8. **Mediation: AdMob và AppLovin**, qua adapter của AdMob, bidding. Thay phần mạng mediation của ADR-0004 (bỏ Unity). Mỗi app có `app-ads.txt` đủ dòng cho cả hai mạng trước khi phát hành.
9. **Đo lường:** dựa vào liên kết AdMob và Firebase cho `ad_impression` và doanh thu; app không tự ghi sự kiện hiển thị quảng cáo (tránh đếm trùng).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Quy chuẩn mới, chính sách trước doanh thu (đã chọn) | Giảm rủi ro khóa tài khoản; giữ điểm Play; một nguồn sự thật | Doanh thu mỗi người dùng có thể thấp hơn cách làm dày đặc |
| Dùng nguyên `vgl-docs` và file Excel | Có sẵn | Viết cho game; tự mâu thuẫn; có cách làm bị cấm |
| Chỉ dùng banner và interstitial | Đơn giản | eCPM native thường cao hơn banner; không khớp cách nhóm đang làm |
| Tự ghi `ad_impression` bằng `OnPaidEventListener` | Có giá trị doanh thu theo từng lần | Đếm trùng khi đã liên kết AdMob và Firebase |

## Hệ quả

- **Tích cực:** quy chuẩn native và quảng cáo nằm cùng repo với code, AI đọc được; các app clone có lý do để được đánh giá tốt hơn app mẫu.
- **Phải chấp nhận:** đợt code (native, tần suất lưu qua phiên, AppLovin) là thay đổi module core và thư viện, cần plan rủi ro cao duyệt chéo; khi chưa xong đợt đó, ADR này ở trạng thái `proposed`.
- **Việc AI phải tuân thủ:** native toàn màn hình chỉ ở điểm dừng tự nhiên, có nút đóng dùng được ngay (ADR-0026 đề xuất thay vế này: native sau inter đếm ngược 3 giây); không đặt quảng cáo theo bất kỳ dòng nào trong bảng "không bao giờ làm" của `docs/ops/ads-guide.md`, kể cả khi spec yêu cầu; gặp yêu cầu như vậy thì ghi thành câu hỏi; không đặt quảng cáo ở màn hình chức năng chính và luồng mở đầu lần đầu (ADR-0026 đề xuất bỏ vế này: vị trí do spec quyết, native giới thiệu bật từ xa); native chỉ qua composable của `:core:ads`, kích thước tính bằng dp theo hệ thống thiết kế (ADR-0026 đề xuất kích thước theo AdsKit, màu theo hệ thống thiết kế); không tự ghi sự kiện hiển thị quảng cáo (ADR-0026 đề xuất thay vế này: ghi `ad_impression_placement`, không ghi `ad_impression`).
