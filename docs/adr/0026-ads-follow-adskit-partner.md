---
id: ADR-0026
title: Hành vi kinh doanh của quảng cáo theo bộ PARTNER của AdsKit, cách làm kỹ thuật theo cấu trúc của dự án mẫu
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-23
deciders: [Người phụ trách sản phẩm]
supersedes: ADR-0022       # thay phần lớn ADR-0022 và một phần ADR-0004, ADR-0013, ADR-0014, ADR-0023; xem mục "Thay gì"
superseded_by:
---

# ADR-0026: HÀNH VI KINH DOANH CỦA QUẢNG CÁO THEO BỘ PARTNER CỦA ADSKIT, CÁCH LÀM KỸ THUẬT THEO CẤU TRÚC CỦA DỰ ÁN MẪU

## Bối cảnh

Từ ADR-0004 tới ADR-0022, dự án mẫu chọn hướng "chính sách trước doanh thu": không quảng cáo lúc mở app, không ép chờ, native thu gọn không tự bung lại, khoảng cách lưu qua phiên, ân hạn phiên đầu, chỉ AdMob và AppLovin, không tự ghi sự kiện hiển thị. Đợt 2 (T-007 tới T-018, phiên RUN-20261023-01) đang code theo hướng đó.

Nhóm có thư viện AdsKit (repo riêng `VGL-Studio/AdsKit`) đã chạy ở nhiều app có doanh thu tốt. AdsKit có hai bộ hành vi: `CURRENT` và `PARTNER` (bộ của đối tác, dùng cho A/B). Ngày 2026-10-23, người phụ trách sản phẩm (anh Nhật) quyết định: hành vi kinh doanh của quảng cáo theo AdsKit, chỉ một bộ là PARTNER. Lý do:

- Doanh thu của bộ này đã được chứng minh trên nhiều app; hướng của dự án mẫu chưa có số liệu nào.
- Một bộ hành vi chung cho mọi app của nhóm giúp so doanh thu giữa các app và với đối tác, và người vận hành dùng chung một bộ khóa Remote Config.
- Đối tác kiểm SDK và sự kiện theo cách AdsKit gửi (ví dụ `af_ad_revenue` bằng sự kiện trong app).

Dùng thẳng thư viện AdsKit thì không hợp với dự án mẫu: repo riêng cần token trong CI (trái ADR-0009), mang theo hai bộ hành vi và các API giữ tương thích, có khóa SDK AppLovin trong resource, và không theo cấu trúc module, Hilt, `AppLogger`, test viết trước của dự án mẫu.

## Quyết định

1. **Hành vi kinh doanh theo [docs/ops/ads-business-rules.md](../ops/ads-business-rules.md)**, viết lại từ bộ PARTNER của AdsKit (commit `bec72bd`), mỗi luật dẫn về file và dòng của AdsKit. Chuẩn đối chiếu là bản đặc tả nghiệp vụ độc lập của AdsKit (`docs/business-spec/`): khi bản đặc tả khác mã nguồn AdsKit, theo bản đặc tả, kể cả các mục "Đã chốt" (anh Nhật, 2026-10-23); chỗ bản đặc tả tự mâu thuẫn thì ghi rõ và không theo vế sai. Chỉ một bộ hành vi: không có `CURRENT`, không đổi bộ hành vi qua Remote Config. Khi tài liệu đó khác ADR cũ, theo tài liệu đó.
2. **Cách làm kỹ thuật là của ta:** Compose, `:core:ads`, `:core:config`, `:core:analytics`, Hilt, `AppLogger`, test viết trước. Không chép code AdsKit. Feature vẫn chỉ đi qua composable và hàm của `:core:ads` với vị trí trong `AdPlacement` (ADR-0013).
3. **Đồng ý và khởi động theo PARTNER:** khởi tạo SDK ngay khi UMP còn giữ đồng ý dùng được của phiên trước (`CACHED_EARLY`); lỗi mạng của UMP coi như được phép (`ERROR_ALLOWED`) chỉ khi app ở foreground, không chốt, hỏi lại khi mạng về; UMP báo cần hỏi lại hay người dùng rút đồng ý thì đóng cổng ngay; quảng cáo nạp bằng quyền tạm không hiện khi cổng đã đóng. Hai quyền tạm (nhánh rút gọn) chỉ có hiệu lực khi cả cấu hình lúc build của `:app` và công tắc từ xa `is_enable_consent_shortcut` cùng bật; **mặc định cả hai tắt**, như AdsKit.
4. **Định dạng và hành vi:** interstitial và combo native toàn màn hình sau inter (đếm ngược 3 giây, nút đóng 36 dp ở góc trên phải); app open lúc mở app (lần đầu sau khi cài và lần thường) và khi quay lại app; banner cố định dùng đơn vị tự làm mới trên AdMob, banner thu gọn; thanh đáy gồm native nhỏ 80 dp và lớp native thu gọn 250 dp tự bung lại sau `time_expand_collap_ad`; native ở một bước của luồng (giới thiệu và luồng khác); pool native; rewarded với hộp thoại trước một hành động có điều kiện. Combo vẫn sang native khi inter đã qua cổng mà không lên được màn; rewarded bấm "Xem" lần hai không bị chặn, chỉ ghi cơ hội `busy`; pool hủy quảng cáo đã ghi lượt hiển thị khi ô trả về. Mọi định dạng bật tắt từ xa, mặc định tắt.
5. **Mốc `ads_interval`:** chỉ inter đẩy mốc, mốc sống trong process (không lưu qua phiên), đo bằng đồng hồ đơn điệu của máy. Không có ân hạn phiên đầu, không có giới hạn số quảng cáo mỗi phiên.
6. **Kích thước native theo layout của AdsKit** (khe đáy 80 dp cho cả vẽ và chừa chỗ theo bản đặc tả, thu gọn 250 dp, thẻ ở bước của luồng 250 dp...), màu theo token của `:core:designsystem`. Thiết kế native trên Figma vẽ lại theo kích thước này (ticket riêng).
7. **Số liệu:** tự ghi doanh thu theo vị trí bằng sự kiện `ad_impression_placement` (không dùng `ad_impression`, vì liên kết AdMob và Firebase đã ghi sự kiện đó); số liệu vòng đời (`ad_load_*`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`, rewarded) có công tắc từ xa, mặc định tắt; AppsFlyer theo ADR-0027.
8. **Mediation:** AdMob điều phối chín mạng con qua adapter chính thức: AppLovin, Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads. Khóa SDK AppLovin là cấu hình của từng app, đưa vào lúc build, không vào repo. (Ghi chú 2026-10-24, plan T-034 câu hỏi 2: adapter AppLovin chỉ lấy khóa từ ánh xạ trên AdMob, nên khóa không vào app; vẫn là cấu hình của từng app, không vào repo.)
9. **Khóa Remote Config:** khái niệm có trong AdsKit dùng đúng tên của AdsKit, kể cả `show_resum_ads`. Khóa mới đặt cùng kiểu: `is_show_<thứ>` cho bật tắt, `show_<thứ>_ads`, `<thứ>_type`, `<thứ>_interval`, `time_<việc>_<thứ>`; đơn vị ghi trong mô tả khóa, không ghi ở cuối tên. Vị trí bật bằng `is_show_<mã vị trí>`. Khóa không có trong bảng khóa chính thức của AdsKit ghi rõ "tên do ta đặt". Bảng khóa ở `ads-business-rules.md`, mục 10.
10. **Những quy tắc cũ vẫn giữ:** không dùng mã quảng cáo thật ở staging, trong repo hay tài liệu; nhãn "Ad" luôn thấy được; không tự vẽ, che hay đè AdChoices; không kéo giãn hay cắt ảnh quảng cáo; quảng cáo không giả làm nút hay mục của app, CTA không giống nút chính của app; không tự bấm hay nhờ người bấm quảng cáo của mình; không phát hành lại app bị đình chỉ bằng tên gói hay khóa ký khác; vị trí quảng cáo chỉ thêm theo spec đã duyệt.
11. **Code đợt 2 (T-007 tới T-018) vẫn merge như đã duyệt.** Phần lệch với ADR này được sửa bằng ticket T-019 tới T-036 của `docs/features/ads/tickets/`, viết plan sau khi đợt 2 merge.
12. **Mô hình chung thay cho tên tính năng của một app** (`ads-business-rules.md`, mục Mô hình chung). AdsKit sinh ra cho một app hình nền nên nhiều tên gắn với tính năng của app đó (`is_show_rw_get_item`, `isShowRewardedOnExport`, `ads_interval_sounds`, native trang 3 của giới thiệu, ô lưới gallery). Dự án mẫu giữ cơ chế, bỏ tên tính năng:
    - Mỗi app khai báo vị trí trong spec và `AdPlacement`: mã, định dạng (danh sách đóng của `:core:ads`), nhóm giãn cách, thuộc tính của định dạng.
    - Khóa suy ra theo mẫu: `is_show_<mã vị trí>`, `ads_interval_<nhóm>`, `<tham số>_<mã vị trí>`. Khóa chung của AdsKit giữ đúng tên; mã vị trí chọn sao cho khóa trùng chuỗi AdsKit khi có thể (`rw_get_item`, `native_onboarding`, nhóm `sounds`).
    - Rewarded là hành động có điều kiện, có chính sách khi không có quảng cáo (mặc định chặn). Native ở bước của luồng, native trong tập hợp, thanh đáy ghép theo màn là khái niệm chung. Combo và resume là cấu hình cả app; màn native sau inter là lớp phủ của `:core:ads`.
    - Thêm vị trí cho một tính năng mới không thêm đường code vào `:core:ads`. Cần định dạng hay cơ chế mới thì đề xuất về dự án mẫu.

**Thay gì** (các ADR cũ có ghi chú ở đầu và ở vế quy tắc bị thay):

| ADR | Phần bị thay | Phần còn hiệu lực |
| --- | --- | --- |
| ADR-0004 | Khởi tạo SDK chỉ sau khi xong đồng ý; danh sách mạng con; rào chắn "không hiện khi người dùng đang thao tác" | AdMob điều phối; `:core:ads` là lớp trung gian; vị trí khai báo tập trung; staging chỉ dùng mã thử; mọi đổi `:core:ads` là rủi ro cao |
| ADR-0013 | SDK chỉ khởi động khi UMP cho phép; bảng khóa Remote Config; rào chắn "chờ người dùng ngừng chạm 1 giây"; "không bao giờ lúc mở hay đóng app" | Thư viện Google Mobile Ads và UMP chỉ trong `:core:ads`; `MainActivity` gọi hỏi đồng ý mỗi lần mở; `PrivacyOptionsButton`; composable cấp qua `LocalAdsController`; mã thử ở staging, mã production lúc build phát hành; staging giả lập vùng EEA |
| ADR-0014 | Bảng giá trị mặc định (thay bằng bảng khóa AdsKit); khóa cá nhân hóa ở vùng có luật (bỏ, spec ads câu hỏi 8) | Vị trí mới mặc định tắt; giá trị mặc định chỉ đổi khi người phụ trách sản phẩm quyết |
| ADR-0022 | Mục 1 (bảng "không bao giờ làm" và nguyên tắc chính sách trước doanh thu), mục 2, 3, 6, 7 (tên khóa; công tắc tổng giữ với tên `is_show_ads`, spec ads câu hỏi 7), 8, 9; số đo trong mục 4 | Mục 5 (feature chỉ qua composable của `:core:ads`); quy tắc chung của mục 4 còn trong mục 10 ở trên |
| ADR-0023 | Mục 1 (cách đặt tên khóa) | Mục 2 tới 6 (bốn kiểu, khai báo một chỗ, bảng khóa sinh tự động, giá trị cục bộ cho staging, đổi giá trị có ghi lại) |

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Viết lại theo bộ PARTNER, cấu trúc của ta (đã chọn) | Doanh thu đã chứng minh; một bộ hành vi cho mọi app; giữ cấu trúc module, test viết trước, log | Viết lại nhiều; rủi ro chính sách cao hơn hướng cũ (xem Hệ quả); đợt 2 phải sửa |
| Viết lại theo bộ CURRENT của AdsKit | Là mặc định của AdsKit | Người phụ trách sản phẩm chọn PARTNER; CURRENT không có init sớm, không bỏ lượt resume sau khi bấm quảng cáo |
| Cả hai bộ, đổi qua Remote Config như AdsKit | A/B được | Gấp đôi luật và test; dự án mẫu cần một cách làm rõ ràng |
| Dùng thẳng thư viện AdsKit | Không phải viết lại | Repo riêng cần token trong CI; khóa AppLovin trong resource; không theo cấu trúc, Hilt, log và cổng chất lượng của dự án mẫu |
| Giữ hướng chính sách trước doanh thu (ADR-0022) | Rủi ro chính sách thấp nhất | Chưa có số liệu doanh thu; khác mọi app đang chạy AdsKit; người phụ trách sản phẩm không chọn |

## Hệ quả

- **Tích cực:** mọi app mới có hành vi quảng cáo giống các app đang có doanh thu; người vận hành dùng chung tên khóa với AdsKit; có doanh thu theo vị trí ở Firebase và AppsFlyer; mỗi luật có dẫn chứng để người duyệt kiểm.
- **Phải chấp nhận:**
  - **Rủi ro chính sách** (Google và Play có thể coi là quảng cáo gây khó chịu hay gây bấm nhầm):
    - Native toàn màn hình liền sau inter là hai quảng cáo toàn màn hình liền nhau, và đếm ngược 3 giây ép người dùng chờ trước khi đóng. Hướng dẫn native toàn màn hình của Google yêu cầu đóng được; chính sách Play về quảng cáo gây khó chịu nêu quảng cáo toàn màn hình không đóng được.
    - App open lúc mở app ngay lần đầu sau khi cài, và `open_ad_type = inter` (interstitial lúc mở app là dạng AdMob ghi là không được phép). Quảng cáo khi quay lại app không có khoảng chờ tối thiểu nào.
    - Native thu gọn tự bung lại và banner thu gọn bung lại ở mỗi lần làm mới có thể che điều khiển và gây bấm nhầm (đã thấy ở app mẫu khảo sát, ADR-0021).
    - Quảng cáo trên luồng giới thiệu, trước khi người dùng thấy giá trị của app; trang native toàn màn hình không có nút điều khiển dễ bị nhầm là trang giới thiệu.
    - Bấm nhầm nhiều thì AdMob có thể giới hạn phân phát quảng cáo (limited ad serving) cho cả tài khoản, ảnh hưởng mọi app.
    - Đồng ý (GDPR): quyền tạm cho phép gửi yêu cầu khi UMP chưa trả lời xong. Giảm rủi ro bằng cách của PARTNER: `CACHED_EARLY` chỉ dùng đồng ý thật của phiên trước; `ERROR_ALLOWED` chỉ khi lỗi mạng, chỉ ở foreground, không chốt, hỏi lại khi mạng về; đóng cổng ngay khi UMP báo cần hỏi lại; có công tắc tắt từ xa. Còn lại một khoảng nhỏ: người dùng mới ở vùng có luật, UMP lỗi mạng mà yêu cầu quảng cáo vẫn đi được.
    - Cách giảm chung: mọi định dạng mặc định tắt, bật dần theo `docs/ops/remote-config.md`, mục 5; theo dõi trung tâm chính sách của AdMob và Play; tắt từng khóa khi có cảnh báo.
  - **Chi phí:** code đợt 2 phải sửa bằng ticket tiếp theo: T-009 (công tắc tổng, ân hạn, giới hạn mỗi phiên, khoảng cách lưu qua phiên), T-010 tới T-014 (kích thước native, thu gọn không bung lại, native toàn màn hình đóng được ngay, màn hình xem thử), T-015 (chỉ AppLovin, gỡ quyền), tên `RemoteKey` ở T-007 và T-008; thiết kế native trên Figma vẽ lại; thêm SDK AppsFlyer và tám adapter mediation; Data safety của từng app phải khai đủ chín mạng và AppsFlyer.
  - Mỗi lần AdsKit đổi luật PARTNER, nhóm quyết có theo không bằng một lần sửa `ads-business-rules.md` (kèm dẫn chứng commit mới), không tự theo.
- **Việc AI phải tuân thủ:** hành vi quảng cáo theo `docs/ops/ads-business-rules.md` (bộ PARTNER của AdsKit), không theo trí nhớ về AdsKit hay về ADR-0022; không chép code AdsKit và không chép khóa nào từ AdsKit; luật thuần của quảng cáo tách thành hàm có unit test với đồng hồ giả, đo thời gian bằng đồng hồ đơn điệu; khóa Remote Config quảng cáo dùng đúng tên trong bảng khóa của `ads-business-rules.md`, khóa mới cùng kiểu đặt tên và ghi rõ là tên do ta đặt; vị trí mới khai báo theo mô hình chung (mã, định dạng, nhóm giãn cách) với khóa suy ra theo mẫu, không thêm cờ, hàm hay composable mang tên tính năng của một app vào `:core:ads`; không ghi `ad_impression` từ app, doanh thu theo vị trí ghi bằng `ad_impression_placement`; giữ các quy tắc ở mục 10 của ADR-0026 kể cả khi spec yêu cầu khác, gặp yêu cầu như vậy thì ghi thành câu hỏi; không thêm vị trí, định dạng hay sự kiện ngoài spec đã duyệt.
