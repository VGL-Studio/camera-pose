---
id: ADR-0045
title: Quảng cáo theo luật nghiệp vụ một bộ (quyết định của người phụ trách sản phẩm, rồi JBase, rồi AdsKit PARTNER), qua một lớp trung gian `:core:ads`, AdMob và AppLovin MAX
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-01
deciders: [Nhật]
supersedes: ADR-0004, ADR-0013, ADR-0014, ADR-0026, ADR-0036, ADR-0037, ADR-0038   # ADR hợp nhất của chuỗi quảng cáo (ADR-0044, mục 4)
superseded_by:
---

# ADR-0045: QUẢNG CÁO THEO LUẬT NGHIỆP VỤ MỘT BỘ, QUA MỘT LỚP TRUNG GIAN `:CORE:ADS`, ADMOB VÀ APPLOVIN MAX

## Bối cảnh

Quyết định về quảng cáo nằm rải ở bảy ADR còn hiệu lực, ADR sau sửa một phần ADR trước. Người đọc phải ghép cả chuỗi mới biết quy tắc hiện tại. ADR-0044, mục 4, yêu cầu hợp nhất: một ADR chép lại mọi quy tắc còn đúng, ADR cũ chuyển `superseded`. ADR này không đổi quyết định nào còn hiệu lực; nó sắp lại theo chủ đề và bỏ phần đã bị thay hay đã xong (đoạn cuối của Quyết định).

Nguồn gốc của từng nhóm quyết định:

- **Lớp trung gian, mã quảng cáo, đồng ý** (ADR-0004, ADR-0013): app kiếm tiền chủ yếu bằng quảng cáo, lỗi ở đây ảnh hưởng thẳng tới doanh thu và tài khoản quảng cáo. Nhóm chọn một module `:core:ads` làm lớp trung gian, hộp thoại đồng ý UMP, mã thử ở staging và mã production đưa vào lúc build phát hành.
- **Giá trị mặc định** (ADR-0014): giá trị app dùng khi chưa nhận được Remote Config là quyết định kinh doanh. Người phụ trách sản phẩm chốt vị trí mặc định tắt.
- **Hành vi theo bộ PARTNER của AdsKit** (ADR-0026, thay phần lớn ADR-0022 và một phần ADR-0004, ADR-0013, ADR-0014, ADR-0023): thư viện AdsKit của nhóm đã có doanh thu tốt ở nhiều app. Người phụ trách sản phẩm chọn hành vi kinh doanh theo bộ PARTNER, viết lại theo cấu trúc của dự án mẫu thay vì dùng thẳng thư viện (repo riêng cần token trong CI, mang hai bộ hành vi, có khóa SDK trong resource).
- **Lệch PARTNER, thêm MAX, JBase làm chuẩn** (ADR-0036, ADR-0037, ADR-0038): đối chiếu với gói JBase (cùng gốc với AdsKit) cho thấy vài cách làm PARTNER không có, và nhóm dùng song song AppLovin MAX với AdMob. Người phụ trách sản phẩm chọn lấy nghiệp vụ JBase làm chuẩn, giữ PARTNER chỗ JBase không có, không theo JBase chỗ là lỗi.

Quy ước khóa Remote Config chung (ADR-0023) hợp nhất riêng ở ADR-0046.

## Quyết định

### 1. Nguồn của nghiệp vụ và thứ tự chuẩn đối chiếu

- **1.1** Hành vi kinh doanh của quảng cáo theo [docs/ops/ads-business-rules.md](../ops/ads-business-rules.md), viết lại từ bộ PARTNER của AdsKit (commit `bec72bd`), mỗi luật dẫn về file và dòng của nguồn. Chỉ một bộ hành vi: không có bộ `CURRENT`, không đổi bộ hành vi qua Remote Config.
- **1.2** Thứ tự phân xử nghiệp vụ: quyết định riêng của người phụ trách sản phẩm (ghi trong spec `ads`) trước; rồi gói JBase; chỗ JBase không có hành vi thì theo luật PARTNER đang có.
- **1.3** Ở tầng PARTNER, chuẩn đối chiếu là bản đặc tả nghiệp vụ độc lập của AdsKit (`docs/business-spec/` của repo AdsKit): khi bản đặc tả khác mã nguồn AdsKit thì theo bản đặc tả, kể cả các mục "Đã chốt"; chỗ bản đặc tả tự mâu thuẫn thì ghi rõ và không theo vế sai.
- **1.4** Không theo JBase ở chỗ trái luật, trái chính sách hay là lỗi rõ ràng; chỗ đó giữ luật hiện tại và ghi lý do. Các chỗ đã biết: MAX báo "đã đồng ý" viết cứng (`setHasUserConsent(true)`); bỏ qua đồng ý khi mở app lúc không có mạng; native không kiểm đồng ý; APS chạy chế độ thử ở bản phát hành; hiện lại native đã ghi lượt hiển thị; tự ghi `ad_impression` (đếm đôi); tiền tệ luôn USD; mã thử của Google cho native ở bản phát hành; khóa thật trong `strings.xml`; adapter MAX phiên bản `+`; thời gian chờ của MAX không chạy lại sau khi app ra nền.
- **1.5** Dẫn chứng JBase bằng đường dẫn và dòng, hoặc lớp và hằng số của bytecode, không theo trí nhớ. Mỗi luật đổi theo JBase ghi vào `ads-business-rules.md` kèm mã ADR và nguồn JBase. Mỗi điểm lệch PARTNER ghi vào mục 12 của tài liệu đó kèm mã ADR.
- **1.6** Mỗi lần AdsKit đổi luật PARTNER, nhóm quyết có theo hay không bằng một lần sửa `ads-business-rules.md`, kèm dẫn chứng commit mới; không tự theo.
- **1.7** `docs/ops/ads-business-rules.md` ghi luật chi tiết theo ADR này, kể cả quyết định đã duyệt mà code chưa làm. Code lệch luật chỉ được sửa bằng ticket có plan đã duyệt; task khác không tự sửa code cho khớp luật ngoài phạm vi plan của nó.

### 2. Nền tảng quảng cáo và mediation

- **2.1** AdMob điều phối mediation, với chín mạng con qua adapter chính thức: AppLovin, Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads.
- **2.2** AppLovin MAX là nền tảng thứ hai, cho banner, MREC, interstitial, rewarded, app open. Native chỉ qua AdMob: phần vẽ và luật native viết trên `NativeAdView` của AdMob. Quảng cáo mở app lúc khởi động chỉ dùng app open của AdMob; app open của MAX dùng khi quay lại app.
- **2.3** Thứ tự hai nền tảng: khóa Remote Config `ads_mediation_order` (string, tên do ta đặt) nhận `admob`, `max`, `admob_max`, `max_admob`; mặc định `max_admob`. Mỗi lượt hiện hỏi nền tảng thứ nhất, chưa có thì nền tảng thứ hai, trong cùng một lượt quyết định và một cơ hội. Nền tảng có trong thứ tự thì được nạp sẵn.
- **2.4** Mọi lượt của MAX đi qua cùng cổng không chồng nhau, gỡ kẹt, chặn quảng cáo tự hiện và cùng mốc `ads_interval` với AdMob. App open của MAX nạp lại 2 giây sau khi đóng; nạp lỗi thì thử lại theo luật thử lại chung (F-05), không theo nhịp cố định 2 giây.
- **2.5** AppLovin hai vai: app dùng MAX thì nhóm mediation AdMob không có AppLovin, còn MAX gọi Google qua adapter Google của MAX. Điều này ghi trong hướng dẫn phát hành.
- **2.6** Amazon APS chỉ đi kèm MAX, cho banner, MREC, interstitial, rewarded; chế độ thử chỉ bật ở staging. APS tách ticket khi nhóm có tài khoản Amazon Publisher Services, mã ứng dụng và slot; trước đó chỉ làm MAX.
- **2.7** SDK quảng cáo (Google Mobile Ads, UMP, MAX, APS) và mọi adapter chỉ nằm trong `:core:ads`. Adapter của MAX ghi phiên bản cụ thể trong `gradle/libs.versions.toml`, không dùng `+`.

### 3. Đồng ý và khởi động SDK

- **3.1** Đồng ý qua hộp thoại được Google chứng nhận (UMP). `MainActivity` gọi `AdsController.gatherConsent` mỗi lần mở app; UMP tự quyết có cần hiện hộp thoại hay không. Khởi tạo SDK theo luật đồng ý của `ads-business-rules.md`, mục 1.
- **3.2** Khởi động sớm bằng đồng ý thật của lần mở trước (`CACHED_EARLY`): khởi tạo SDK ngay khi UMP còn giữ đồng ý dùng được của phiên trước. Luôn bật, không cần cấu hình hay công tắc (mục 7.8).
- **3.3** Quyền tạm khi UMP lỗi mạng (`ERROR_ALLOWED`) chỉ khi app ở foreground, không chốt, hỏi lại khi mạng về. Quyền này chỉ có hiệu lực khi cả cấu hình lúc build của `:app` và công tắc từ xa `is_enable_consent_shortcut` cùng bật; mặc định cả hai tắt.
- **3.4** UMP báo cần hỏi lại hay người dùng rút đồng ý thì đóng cổng ngay. Quảng cáo nạp bằng quyền tạm không hiện khi cổng đã đóng.
- **3.5** Nơi luật yêu cầu, `PrivacyOptionsButton` hiện nút mở lại hộp thoại. Mỗi app đặt nút này trong màn hình cài đặt; app mẫu đặt ở màn hình mẫu.
- **3.6** UMP ghi chuỗi đồng ý theo chuẩn TCF. Firebase Analytics mặc định đọc chuỗi này (tắt bằng `google_analytics_tcf_data_enabled`), nên không cần code riêng để nối Analytics với sự đồng ý. Quảng cáo cá nhân hóa do chuỗi đồng ý của UMP quyết; không có khóa Remote Config cho cá nhân hóa ở vùng có luật.
- **3.7** MAX và APS chỉ khởi tạo khi cổng đồng ý của `:core:ads` mở lần đầu, đọc chuỗi đồng ý của UMP; không bao giờ trước. Không dùng luồng đồng ý riêng của MAX, không viết cứng trạng thái đồng ý. Rút đồng ý thì đóng như AdMob.
- **3.8** Bản staging coi mọi máy là máy test ở châu Âu (giả lập vùng EEA), nên hộp thoại đồng ý hiện ở lần mở đầu.

### 4. Lớp trung gian `:core:ads` và vị trí

- **4.1** `:core:ads` là lớp trung gian duy nhất. Feature chỉ yêu cầu "hiển thị quảng cáo ở vị trí X" qua composable và hàm của `:core:ads` (như `AdSlot`, `NativeAdSlot`, `rememberShowFullScreenAd`, `PrivacyOptionsButton`; danh sách ở `docs/ops/ads-guide.md`), với vị trí trong `AdPlacement`. Feature không gọi `AdsController` hay SDK quảng cáo, không tự dựng `NativeAdView`, không biết quảng cáo đến từ nền tảng nào.
- **4.2** `MainActivity` cấp `AdsController` cho giao diện qua `LocalAdsController`. Test và preview không cấp gì, nên các composable quảng cáo không hiện gì và ảnh screenshot không bao giờ chứa quảng cáo.
- **4.3** Feature chỉ gọi quảng cáo toàn màn hình ở hành động của người dùng. Quảng cáo lúc mở app và khi quay lại app do `:core:ads` và vỏ app làm theo `ads-business-rules.md`.
- **4.4** Cách làm kỹ thuật là của ta: Compose, `:core:ads`, `:core:config`, `:core:analytics`, Hilt, `AppLogger`, test viết trước. Không chép code hay khóa của AdsKit hay JBase. Luật thuần của quảng cáo tách thành hàm có unit test với đồng hồ giả; thời gian đo bằng đồng hồ đơn điệu.
- **4.5** Rào chắn chính sách nằm trong module: giới hạn tần suất quảng cáo toàn màn hình (mục 9), không đặt quảng cáo sát nút bấm.
- **4.6** Vị trí là quyết định kinh doanh: chỉ thêm theo spec đã duyệt. Không thêm vị trí, định dạng hay sự kiện ngoài spec đã duyệt.
- **4.7** Vị trí khai báo tập trung trong `AdPlacement`, theo mô hình chung (`ads-business-rules.md`, mục Mô hình chung): mã, định dạng (danh sách đóng của `:core:ads`), nhóm giãn cách, thuộc tính riêng của định dạng. Khóa suy ra theo mẫu: `is_show_<mã vị trí>`, `ads_interval_<nhóm>`, `<tham số>_<mã vị trí>`. Khóa chung của AdsKit giữ đúng tên; mã vị trí chọn sao cho khóa trùng chuỗi AdsKit khi có thể (`rw_get_item`, `native_onboarding`, nhóm `sounds`).
- **4.8** Rewarded là hành động có điều kiện, có chính sách khi không có quảng cáo (mặc định chặn). Native ở bước của luồng, native trong tập hợp, thanh đáy ghép theo màn là khái niệm chung. Combo và resume là cấu hình cả app; màn native sau inter là lớp phủ của `:core:ads`.
- **4.9** Thêm vị trí cho một tính năng mới không thêm đường code vào `:core:ads`: không thêm cờ, hàm hay composable mang tên tính năng của một app. Cần định dạng hay cơ chế mới thì đề xuất về dự án mẫu.
- **4.10** Vị trí mới mặc định tắt: chỉ hiện quảng cáo khi nhóm bật khóa `is_show_<mã vị trí>` trên Remote Config của từng app. Quảng cáo toàn màn hình chỉ được tải sẵn khi vị trí đã bật, và được tải ngay khi vị trí chuyển sang bật.
- **4.11** Mọi thay đổi trong `:core:ads` là rủi ro cao, cần duyệt chéo.

### 5. Mã quảng cáo và khóa SDK ở staging và production

- **5.1** Bản staging chỉ dùng mã quảng cáo thử nghiệm. Mã thật chỉ được đưa vào khi build bản phát hành, do người thực hiện. Không viết mã quảng cáo thật vào code, tài liệu hay tham số build trong repo.

  | | Staging | Production |
  | --- | --- | --- |
  | Mã ứng dụng AdMob | Mã thử của Google, trong `app/build.gradle.kts` | Tham số `-PadmobAppId` |
  | Mã đơn vị quảng cáo | Mã thử của Google theo định dạng (`GoogleTestAdUnitIds`) | Tham số `-PadUnits="<vị trí>=<mã>;<vị trí>=<mã>"` |
  | Hộp thoại đồng ý | Coi mọi máy là máy test ở châu Âu | Bình thường |

- **5.2** Thiếu `-PadmobAppId` hoặc `-PadUnits` thì build production hỏng ngay ở bước đầu, với thông báo rõ ràng. Build staging và đồng bộ trong Android Studio không cần hai tham số này.
- **5.3** `scripts/release.sh` lấy hai giá trị từ trình quản lý mật khẩu và kiểm mọi vị trí đều có mã, vì một vị trí thiếu mã thì không hiện quảng cáo mà không báo lỗi khi build (`ConfiguredAdUnitIds.missing`).
- **5.4** Khóa SDK AppLovin cho adapter của AdMob là cấu hình của từng app, không vào repo. Adapter chỉ lấy khóa từ ánh xạ trên AdMob, nên khóa cũng không vào app.
- **5.5** Khóa SDK MAX và mã đơn vị MAX theo khe là cấu hình của từng app, đưa vào lúc build phát hành như `-PadUnits`, không vào repo, tài liệu hay resource của module; mã APS cũng vậy. Staging không có khóa thì MAX không khởi tạo và thứ tự coi như `admob`. Dev thử MAX ở staging bằng `local.properties`.

### 6. Khóa Remote Config và giá trị mặc định

- **6.1** Khái niệm có trong AdsKit dùng đúng tên của AdsKit, kể cả `show_resum_ads`. Khóa mới đặt cùng kiểu: `is_show_<thứ>` cho bật tắt, `show_<thứ>_ads`, `<thứ>_type`, `<thứ>_interval`, `time_<việc>_<thứ>`. Đơn vị ghi trong mô tả khóa, không ghi ở cuối tên. Vị trí bật bằng `is_show_<mã vị trí>`.
- **6.2** Khóa không có trong bảng khóa chính thức của AdsKit ghi rõ "tên do ta đặt". Bảng khóa ở `ads-business-rules.md`, mục 10.
- **6.3** Giá trị mặc định của khóa quảng cáo theo bảng khóa đó: mọi định dạng và vị trí mặc định tắt, `ads_interval` 45 giây. Khóa phụ có thể mặc định bật khi hiệu lực của nó còn cần một khóa đang tắt (như `is_show_native_collap` cần `is_show_native_bottom`). Giá trị mặc định chỉ đổi khi người phụ trách sản phẩm quyết.
- **6.4** Công tắc tổng `is_show_ads` tắt mọi quảng cáo.
- **6.5** Các quy tắc chung còn lại của khóa (kiểu, khai báo một chỗ, bảng khóa sinh từ code, giá trị cục bộ của staging, đổi giá trị có ghi lại) theo ADR-0046.

### 7. Các điểm lệch PARTNER có chủ đích

Bốn điểm học từ JBase mà PARTNER không có:

- **7.1** Quảng cáo trong màn hình dừng làm mới và dừng gửi yêu cầu khi một quảng cáo toàn màn hình vẽ trong app đang hiện (native sau inter, native toàn màn hình độc lập). Quảng cáo đã có giữ nguyên, chạy lại khi màn che đóng. Interstitial, app open, rewarded là Activity riêng của SDK nên đã dừng theo vòng đời.
- **7.2** Thanh đáy chừa chỗ ngay lúc khe đang tải, đúng chiều cao của khe (chiều cao khe native, chiều cao khung với banner). Nạp lỗi thì co về 0. Người phụ trách sản phẩm chấp nhận một khoảng trống tạm khi không có quảng cáo, để nội dung không bị đẩy khi quảng cáo về.
- **7.3** Định dạng `banner_mrec` (banner 300 x 250) có trong danh sách đóng. Native medium vẫn là lựa chọn mặc định; MREC dùng khi spec của app cần.
- **7.4** Cổng "không hiện quảng cáo" cho người đã mua gói bỏ quảng cáo: `:core:ads` nhận tín hiệu từ app; khi bật thì không tải, không hiện mọi định dạng, như công tắc tổng `is_show_ads` tắt, và hành động có điều kiện làm thẳng. Tín hiệu mặc định tắt.

Chi tiết từng điểm (giá trị, trường hợp biên) ở spec `ads`, câu hỏi 36 tới 39, và trong `ads-business-rules.md`.

Đổi theo JBase:

- **7.5** Native toàn màn hình sau inter: đếm ngược theo khóa `time_close_native` (tên của JBase), mặc định 5 giây, nhận 1 tới 30, giá trị khác dùng 5. Vòng đếm ngược và nút đóng 48 dp ở góc trên trái. Giữ phần chờ inter rời màn tối đa 2 giây, để không kẹt (JBase không có).
- **7.6** Native thu gọn tự bung lại sau `time_expand_collap_ad`, mặc định 10 giây. Giá trị 0 là không bung lại (JBase coi 0 là bung ngay; đó là lỗi, không theo).
- **7.7** Kích thước native nhỏ ở đáy, native thu gọn và native medium theo layout của JBase (spec `ads`, câu hỏi 48). Màu vẫn theo token.
- **7.8** `CACHED_EARLY` luôn bật, không cần cấu hình hay công tắc. `ERROR_ALLOWED` giữ luật cũ: mặc định tắt (mục 3.3).
- **7.9** Banner cố định dùng cỡ thích ứng neo đáy thường, không dùng cỡ neo đáy lớn.

Giữ luật hiện tại dù JBase khác:

- **7.10** Banner thu gọn nạp lại mỗi 30 giây (JBase không tự nạp lại). Doanh thu AppsFlyer ghi bằng sự kiện trong app `af_ad_revenue` (JBase dùng connector).

### 8. Định dạng và hành vi

- **8.1** Các định dạng: interstitial và combo native toàn màn hình sau inter (mục 7.5); app open lúc mở app (lần đầu sau khi cài và lần thường) và khi quay lại app; banner cố định dùng đơn vị tự làm mới trên AdMob (cỡ ở mục 7.9), banner thu gọn; banner MREC (mục 7.3); thanh đáy gồm native nhỏ ở khe đáy và lớp native thu gọn tự bung lại (mục 7.6); native ở một bước của luồng (giới thiệu và luồng khác); pool native; rewarded với hộp thoại trước một hành động có điều kiện.
- **8.2** Combo vẫn sang native khi inter đã qua cổng mà không lên được màn. Rewarded bấm "Xem" lần hai không bị chặn, chỉ ghi cơ hội `busy`. Pool hủy quảng cáo đã ghi lượt hiển thị khi ô trả về.
- **8.3** Mọi định dạng bật tắt từ xa, mặc định tắt.
- **8.4** Kích thước native theo layout của AdsKit (thẻ ở bước của luồng 250 dp...), trừ native nhỏ ở đáy, native thu gọn và native medium (mục 7.7). Khe đáy dùng cùng một chiều cao cho vẽ và chừa chỗ. Màu theo token của `:core:designsystem`. Thiết kế native trên Figma vẽ theo các kích thước này.

### 9. Mốc `ads_interval`

- **9.1** Chỉ inter đẩy mốc. Mốc sống trong process, không lưu qua phiên, đo bằng đồng hồ đơn điệu của máy.
- **9.2** Không có ân hạn phiên đầu, không có giới hạn số quảng cáo mỗi phiên.

### 10. Số liệu

- **10.1** Doanh thu theo vị trí tự ghi bằng sự kiện `ad_impression_placement`. App không ghi `ad_impression`, vì liên kết AdMob và Firebase đã ghi sự kiện đó.
- **10.2** Số liệu vòng đời (`ad_load_*`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`, rewarded) có công tắc từ xa, mặc định tắt.
- **10.3** AppsFlyer theo ADR-0027; doanh thu gửi bằng sự kiện trong app `af_ad_revenue` (mục 7.10).
- **10.4** MAX dùng cùng sự kiện (`ad_impression_placement`, `af_ad_revenue`): `ad_platform` là `applovin`, `mediation_network` là `applovinmax`, giá trị theo đô la; phễu `af_inters_*`, `af_rewarded_*` gửi cho cả hai nền tảng.

### 11. Quy tắc chính sách luôn giữ

Giữ kể cả khi spec yêu cầu khác; gặp yêu cầu như vậy thì ghi thành câu hỏi:

- **11.1** Không dùng mã quảng cáo thật ở staging, trong repo hay tài liệu.
- **11.2** Nhãn "Ad" luôn thấy được.
- **11.3** Không tự vẽ, che hay đè AdChoices.
- **11.4** Không kéo giãn hay cắt ảnh quảng cáo.
- **11.5** Quảng cáo không giả làm nút hay mục của app; CTA không giống nút chính của app.
- **11.6** Không tự bấm hay nhờ người bấm quảng cáo của mình.
- **11.7** Không phát hành lại app bị đình chỉ bằng tên gói hay khóa ký khác.
- **11.8** Vị trí quảng cáo chỉ thêm theo spec đã duyệt.

### Mục mới ← ADR cũ

| Mục mới | Nguồn |
| --- | --- |
| 1.1 | ADR-0026 quyết định 1 |
| 1.2 | ADR-0038 quyết định 1 |
| 1.3 | ADR-0026 quyết định 1 |
| 1.4 | ADR-0038 quyết định 2 và Bối cảnh |
| 1.5 | ADR-0038 Việc AI phải tuân thủ; ADR-0036 Việc AI phải tuân thủ |
| 1.6 | ADR-0026 Hệ quả |
| 1.7 | Mới: thay câu cuối của ADR-0026 quyết định 1; tài liệu luật đi trước code (dev chọn khi duyệt ADR này) |
| 2.1 | ADR-0004 (AdMob điều phối); ADR-0026 quyết định 8 |
| 2.2 | ADR-0037 (định dạng, câu 41) |
| 2.3 | ADR-0037 (thứ tự, câu 42) |
| 2.4 | ADR-0037 (luật toàn màn hình); ADR-0038 quyết định 5 |
| 2.5 | ADR-0037 (AppLovin hai vai, câu 45) |
| 2.6 | ADR-0037 (APS, câu 47) |
| 2.7 | ADR-0013 (thư viện); ADR-0037 Việc AI phải tuân thủ |
| 3.1 | ADR-0004; ADR-0013 (sự đồng ý) |
| 3.2 | ADR-0026 quyết định 3; ADR-0038 quyết định 3 |
| 3.3, 3.4 | ADR-0026 quyết định 3; ADR-0038 quyết định 3 |
| 3.5, 3.6 | ADR-0013 (sự đồng ý); ADR-0026 bảng "Thay gì" (bỏ khóa cá nhân hóa) |
| 3.7 | ADR-0037 (đồng ý, câu 44) |
| 3.8 | ADR-0013 (mã quảng cáo) |
| 4.1 | ADR-0004; ADR-0013 (composable); ADR-0026 quyết định 2; ADR-0026 bảng "Thay gì" (mục 5 của ADR-0022); ADR-0037 |
| 4.2 | ADR-0013 (`LocalAdsController`) |
| 4.3 | ADR-0013 Việc AI phải tuân thủ |
| 4.4 | ADR-0026 quyết định 2 và Việc AI phải tuân thủ; ADR-0038 Việc AI phải tuân thủ |
| 4.5 | ADR-0004 (rào chắn chính sách, phần còn lại) |
| 4.6 | ADR-0004 Việc AI phải tuân thủ; ADR-0026 Việc AI phải tuân thủ |
| 4.7, 4.8, 4.9 | ADR-0004 (vị trí khai báo tập trung); ADR-0026 quyết định 12 |
| 4.10 | ADR-0014 (vị trí mặc định tắt) |
| 4.11 | ADR-0004 Hệ quả |
| 5.1 | ADR-0004; ADR-0013 (mã quảng cáo) |
| 5.2, 5.3 | ADR-0013 (mã quảng cáo) |
| 5.4 | ADR-0026 quyết định 8 |
| 5.5 | ADR-0037 (khóa, câu 43) |
| 6.1, 6.2 | ADR-0026 quyết định 9 |
| 6.3 | ADR-0014; ADR-0026 bảng "Thay gì" |
| 6.4 | ADR-0026 bảng "Thay gì" (công tắc tổng) |
| 6.5 | ADR-0026 bảng "Thay gì" (mục 2 tới 6 của ADR-0023) |
| 7.1 | ADR-0036 quyết định 1 |
| 7.2 | ADR-0036 quyết định 2 |
| 7.3 | ADR-0036 quyết định 3 |
| 7.4 | ADR-0036 quyết định 4 |
| 7.5 | ADR-0038 quyết định 3 (native sau inter) |
| 7.6 | ADR-0038 quyết định 3 (native thu gọn) |
| 7.7 | ADR-0038 quyết định 3 (kích thước) |
| 7.8 | ADR-0038 quyết định 3 (`CACHED_EARLY`) |
| 7.9 | ADR-0038 quyết định 3 (banner cố định) |
| 7.10 | ADR-0038 quyết định 4 |
| 8.1, 8.2, 8.3 | ADR-0026 quyết định 4 |
| 8.4 | ADR-0026 quyết định 6; ADR-0038 quyết định 3 |
| 9.1, 9.2 | ADR-0026 quyết định 5 |
| 10.1, 10.2 | ADR-0026 quyết định 7 |
| 10.3 | ADR-0026 quyết định 7; ADR-0038 quyết định 4 |
| 10.4 | ADR-0037 (doanh thu, câu 46) |
| 11.1 tới 11.8 | ADR-0026 quyết định 10 |

Không chép lại: phần đã bị ADR sau thay (thời điểm khởi động chỉ sau khi xong đồng ý, bảng khóa `ads_<vị trí>_enabled` và các giá trị mặc định cũ, rào chắn "chờ người dùng ngừng chạm 1 giây", ý "không bao giờ lúc mở hay đóng app", khóa cá nhân hóa ở vùng có luật, đếm ngược 3 giây và nút 36 dp, bung lại sau 30 giây, banner cỡ lớn, công tắc cho `CACHED_EARLY`, AdMob là nền tảng duy nhất), và quyết định 11 của ADR-0026 (code đợt 2 merge rồi sửa bằng ticket sau: việc một lần, đã xong).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Một lớp trung gian `:core:ads`, composable cấp `AdsController` qua `CompositionLocal` (đã chọn) | Đổi mạng, thêm mạng, nâng SDK chỉ sửa một chỗ; test và preview tự động không có quảng cáo | Màn hình ngoài `MainActivity` phải tự cấp; mọi đổi `:core:ads` là rủi ro cao |
| Mỗi feature nhận `AdsController` qua ViewModel | Rõ phụ thuộc | ViewModel không có `Activity` để hiện quảng cáo; mọi test phải có bản giả |
| Mã production trong file `.properties` không commit | Build tay tiện hơn | Dễ lỡ commit; trái ADR-0009 |
| Vị trí mặc định bật | Doanh thu không phụ thuộc Remote Config | Vị trí có vấn đề vẫn hiện trên máy chưa nhận lệnh tắt |
| Hành vi theo bộ PARTNER, viết lại theo cấu trúc của ta (đã chọn) | Doanh thu đã chứng minh; một bộ hành vi cho mọi app; giữ cấu trúc module, test viết trước, log | Viết lại nhiều; rủi ro chính sách cao hơn hướng "chính sách trước doanh thu" |
| Cả hai bộ `CURRENT` và `PARTNER`, đổi qua Remote Config | A/B được | Gấp đôi luật và test |
| Dùng thẳng thư viện AdsKit | Không phải viết lại | Repo riêng cần token trong CI; khóa AppLovin trong resource; không theo cấu trúc, Hilt, log và cổng chất lượng của dự án mẫu |
| JBase làm chuẩn, giữ PARTNER chỗ JBase không có, bỏ chỗ JBase sai (đã chọn) | Theo đúng chuẩn người phụ trách sản phẩm chọn; không mất các luật đã sửa lỗi của AdsKit | Hai nguồn chuẩn; mỗi luật phải ghi theo nguồn nào |
| JBase thay hẳn PARTNER | Một nguồn chuẩn | Mất đồng ý, gỡ kẹt, resume, combo, đo lường; trái GDPR ở chỗ JBase sai |
| MAX cho banner, MREC, toàn màn hình; AdMob giữ native (đã chọn) | Thêm nguồn ở định dạng có doanh thu cao; giữ nguyên phần native | Hai bộ khe, hai đường tải; thứ tự giữa hai nền tảng phải đo |
| MAX là nền tảng điều phối, AdMob thành mạng con của MAX | Một nền tảng | Viết lại gần hết `:core:ads`, UMP và luật PARTNER |

## Hệ quả

- **Tích cực:**
  - Quy tắc quảng cáo hiện tại đọc được từ một ADR.
  - Mọi app mới có hành vi quảng cáo giống các app đang có doanh thu; người vận hành dùng chung tên khóa với AdsKit.
  - Người phụ trách sản phẩm tắt vị trí, đổi tần suất mà không cần phát hành lại; quảng cáo không tải trước khi có đồng ý dùng được.
- **Phải chấp nhận:**
  - **Rủi ro chính sách** (Google và Play có thể coi là quảng cáo gây khó chịu hay gây bấm nhầm): native toàn màn hình liền sau inter, đếm ngược 5 giây trước khi đóng được; app open ngay lần đầu sau khi cài, và `open_ad_type = inter`; quảng cáo khi quay lại app không có khoảng chờ tối thiểu; native thu gọn và banner thu gọn tự bung lại có thể che điều khiển; quảng cáo trên luồng giới thiệu; bấm nhầm nhiều thì AdMob có thể giới hạn phân phát cho cả tài khoản. Đồng ý (GDPR): `ERROR_ALLOWED` cho gửi yêu cầu khi UMP chưa trả lời xong, giảm bằng chỉ khi lỗi mạng, chỉ ở foreground, không chốt, công tắc tắt từ xa; `CACHED_EARLY` luôn bật nên không có công tắc tắt khẩn từ xa cho phần này.
  - Cách giảm chung: mọi định dạng mặc định tắt, bật dần theo `docs/ops/remote-config.md`, mục 5; theo dõi trung tâm chính sách của AdMob và Play; tắt từng khóa khi có cảnh báo.
  - Mỗi app mới phải bật từng vị trí trên Remote Config của project Firebase production trước khi phát hành, nếu không app không có quảng cáo; danh sách việc trước khi phát hành có bước này.
  - Hộp thoại đồng ý trên AdMob của mỗi app phải được kiểm tra trước khi phát hành.
  - SDK quảng cáo kéo thêm quyền (`ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`, `FOREGROUND_SERVICE` qua WorkManager, cùng quyền của MAX và các adapter); khai báo Data safety của từng app khai đủ các mạng, MAX và AppsFlyer.
  - Mỗi app cần thêm một bảng mã đơn vị MAX; số liệu doanh thu có thêm `ad_platform` khác `admob`, báo nhóm UA trước khi phát hành.
  - Bản staging luôn hiện hộp thoại châu Âu ở lần mở đầu, kể cả với người test ở Việt Nam.
  - Khi thanh đáy không có quảng cáo thì có lúc hở khoảng trống tới khi nạp lỗi.
- **Việc AI phải tuân thủ:** hành vi quảng cáo theo `docs/ops/ads-business-rules.md`, không theo trí nhớ về AdsKit hay JBase; phân xử nghiệp vụ quảng cáo theo thứ tự quyết định của người phụ trách sản phẩm trong spec `ads`, rồi JBase, rồi luật PARTNER đang có; không theo JBase ở chỗ trái luật, chính sách hay là lỗi, ghi lý do; dẫn chứng JBase bằng đường dẫn và dòng, hoặc lớp và hằng số của bytecode, không theo trí nhớ; ghi mỗi luật đổi theo JBase vào `ads-business-rules.md` kèm mã ADR và nguồn JBase; ghi mỗi điểm lệch PARTNER vào `ads-business-rules.md` mục 12 kèm mã ADR; không chép code hay khóa của AdsKit hay JBase; luật thuần của quảng cáo tách thành hàm có unit test với đồng hồ giả, đo thời gian bằng đồng hồ đơn điệu; feature chỉ dùng composable và hàm của `:core:ads` và `PrivacyOptionsButton`; không gọi `AdsController` hay SDK quảng cáo từ feature; feature chỉ gọi quảng cáo toàn màn hình ở hành động của người dùng; quảng cáo lúc mở app và khi quay lại app do `:core:ads` và vỏ app làm theo `docs/ops/ads-business-rules.md`; khởi tạo SDK quảng cáo theo luật đồng ý của `docs/ops/ads-business-rules.md`, mục 1; SDK MAX và APS chỉ nằm trong `:core:ads`; MAX và APS khởi tạo sau cổng đồng ý của `:core:ads`, không bao giờ trước, không viết cứng trạng thái đồng ý; mọi luồng của MAX đi qua cùng cổng hiện toàn màn hình và cùng mốc `ads_interval` với AdMob; native không đi qua MAX; adapter của MAX ghi phiên bản cụ thể trong `gradle/libs.versions.toml`, không dùng `+`; không tách ticket APS khi chưa có tài khoản Amazon Publisher Services; không viết mã quảng cáo thật vào code, tài liệu hay tham số build trong repo; khóa SDK MAX, mã đơn vị MAX và mã APS không vào repo, tài liệu hay resource của module; không tự thêm vị trí, định dạng hay sự kiện ngoài spec đã duyệt; vị trí mới mặc định tắt; vị trí mới khai báo theo mô hình chung (mã, định dạng, nhóm giãn cách) với khóa suy ra theo mẫu, không thêm cờ, hàm hay composable mang tên tính năng của một app vào `:core:ads`; khóa Remote Config quảng cáo dùng đúng tên trong bảng khóa của `ads-business-rules.md`, khóa mới cùng kiểu đặt tên và ghi rõ là tên do ta đặt; giá trị mặc định của khóa quảng cáo theo bảng khóa của `docs/ops/ads-business-rules.md`, mục 10, chỉ đổi khi người phụ trách sản phẩm quyết; không ghi `ad_impression` từ app, doanh thu theo vị trí ghi bằng `ad_impression_placement`; quảng cáo trong màn hình không gửi yêu cầu và không làm mới khi quảng cáo toàn màn hình vẽ trong app đang hiện; thanh đáy chừa chỗ đúng chiều cao của khe từ lúc khe đang tải, nạp lỗi thì co về 0; `banner_mrec` là một định dạng của danh sách đóng, chỉ dùng khi spec của app khai báo; cổng không hiện quảng cáo bật thì mọi định dạng như công tắc tổng tắt và hành động có điều kiện làm thẳng; giữ các quy tắc ở mục 11 của ADR-0045 kể cả khi spec yêu cầu khác, gặp yêu cầu như vậy thì ghi thành câu hỏi; code lệch luật của `ads-business-rules.md` chỉ sửa bằng ticket có plan đã duyệt, không sửa ngoài phạm vi plan.
