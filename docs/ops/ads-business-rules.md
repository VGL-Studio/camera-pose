# LUẬT NGHIỆP VỤ QUẢNG CÁO (THEO ADSKIT PARTNER)

Tài liệu này là đặc tả nghiệp vụ cho cơ chế quảng cáo của dự án mẫu. Nó dành cho dev, AI viết plan và người duyệt. Quyết định nằm ở ADR-0026 (hành vi theo AdsKit PARTNER) và ADR-0027 (AppsFlyer). Cách thêm vị trí, thử trên staging và phát hành nằm ở [ads-guide.md](ads-guide.md); cách đặt tên khóa ở [remote-config.md](remote-config.md).

## 0. Cách đọc

- **Nguồn:** thư viện AdsKit (repo riêng `VGL-Studio/AdsKit`, nhánh `main`, commit `bec72bd`, ngày 2026-10-22), bộ hành vi `PARTNER`. AdsKit đã chạy ở nhiều app có doanh thu tốt. Ta chỉ lấy **một** bộ hành vi là PARTNER: không có bộ `CURRENT`, không đổi bộ hành vi qua Remote Config.
- **Ta tự viết lại:** Compose, `:core:ads`, `:core:config`, `:core:analytics`, Hilt, `AppLogger`, test viết trước. Không chép code của AdsKit. Không chép bất kỳ khóa thật nào (AdsKit có khóa SDK AppLovin trong resource; bản của ta không có, mục 9).
- **Chuẩn đối chiếu:** bản đặc tả nghiệp vụ độc lập của AdsKit (`docs/business-spec/` của repo AdsKit, commit `bec72bd`). Anh Nhật quyết ngày 2026-10-23: khi bản đặc tả khác mã nguồn AdsKit, theo bản đặc tả, kể cả các mục "Đã chốt" của nó. Mã nguồn chỉ dùng để phân xử chỗ bản đặc tả không nói. Chỗ bản đặc tả tự mâu thuẫn thì không theo vế sai và ghi rõ (mục 12, AS-01).
- **Dẫn chứng:** mỗi luật có cột AdsKit ghi `đường dẫn:dòng` để người duyệt kiểm lại. Quy ước: `M/` là `src/main/java/com/vgl/adskit/`, `T/` là `src/test/java/com/vgl/adskit/`, `R.md` là `README.md`, `DG` là `docs/deployment-guide.md` của AdsKit. `A 05:68` là dòng 68 của file `05-...md` trong bản đặc tả độc lập (`A 07` là file `07-hop-dong-app-chu.md`).
- **Ai làm:** cột "Chủ" ghi phần nào của app sở hữu luật.
  - `ads`: `:core:ads`. Mọi cổng, bộ đếm, nạp và hiển thị.
  - `app`: vỏ app `:app` (màn loading đóng vai splash, `MainActivity`, `AppNavHost`, luồng giới thiệu nếu app có). AdsKit cũng để phần này cho app chủ.
  - `feature`: module feature, chỉ gọi composable và hàm của `:core:ads`.
  - `analytics`: `:core:analytics` (Firebase Analytics, AppsFlyer).
- **Thời gian:** mọi khoảng chờ và mốc trong tài liệu này đo bằng đồng hồ đơn điệu của máy (thời gian kể từ lúc máy khởi động), không đo bằng giờ hệ thống. Người dùng đổi giờ máy thì luật vẫn đúng (mục 12, AR-01).

Từ ngữ:

| Từ | Nghĩa |
| --- | --- |
| Quảng cáo toàn màn hình | App open (AOA), interstitial (inter), rewarded. Native toàn màn hình sau inter tính là một phần của cụm inter |
| Cổng đồng ý | Được phép gửi yêu cầu quảng cáo hay không, theo UMP và quyền tạm của PARTNER |
| Quyền tạm | Quyền gửi yêu cầu khi UMP chưa trả lời xong: `CACHED_EARLY` hoặc `ERROR_ALLOWED` (mục 1) |
| Vị trí (placement) | Tên màn hình hay hành động đặt quảng cáo, dùng để bật tắt và để ghi doanh thu theo vị trí |
| Khe (slot) | Một đơn vị quảng cáo theo định dạng (inter, app open, banner, native đáy...). Nhiều vị trí dùng chung một khe |
| Chặn quảng cáo tự hiện | Tạm không cho quảng cáo khi quay lại app (resume) trong lúc app tự mở màn khác (mục 6) |
| Nhóm giãn cách | Một mốc "inter gần nhất rời màn" và một khóa khoảng cách. Nhóm mặc định dùng `ads_interval` (mục Mô hình chung, MC-04) |
| Hành động có điều kiện | Một hành động của người dùng (nhận vật phẩm, xuất file...) mà app chỉ cho làm sau khi xem rewarded (MC-05) |
| Tên do ta đặt | Khóa Remote Config không có trong bảng khóa chính thức của AdsKit (`DG:171-181`). Ta tự đặt tên theo kiểu AdsKit |

## Mô hình chung: vị trí, định dạng, nhóm giãn cách

AdsKit sinh ra cho một app cụ thể (app hình nền `diy-wallpaper-maker-gallery`: lưới ảnh, trang xem trước, giới thiệu, xuất ảnh, màn âm thanh). Nhiều tên cờ, khe và composable của nó mang tên màn hình hay tính năng của app đó, dù cơ chế bên dưới là chung. Dự án mẫu phục vụ nhiều app, nên ta giữ **cơ chế** của AdsKit và bỏ **tên tính năng**: mỗi app khai báo vị trí của mình theo một mô hình chung, khóa suy ra theo mẫu, `:core:ads` không có đường code riêng cho tính năng của một app.

### Luật của mô hình

| # | Luật | Chủ |
| --- | --- | --- |
| MC-01 | **Danh sách vị trí.** Mỗi app khai báo vị trí trong spec đã duyệt rồi thêm vào `AdPlacement`. Mỗi vị trí có: mã (chữ thường, gạch dưới), định dạng (MC-02), nhóm giãn cách (chỉ inter, MC-04), và thuộc tính riêng của định dạng (MC-05 tới MC-08). Mã vị trí cũng là giá trị `ad_placement` trong số liệu. Dự án mẫu chỉ có vị trí của màn mẫu và màn xem thử | app khai báo, ads đọc |
| MC-02 | **Định dạng là danh sách đóng** do `:core:ads` cung cấp: `app_open`, `interstitial`, `rewarded`, `banner` (tự làm mới), `banner_collapsible`, `native_small` (khe đáy), `native_collap` (lớp nổi trên đáy), `native_fullscreen` (toàn màn hình: sau inter hay một bước của luồng), `native_step` (thẻ 250 dp ở một bước của luồng), `native_medium`, `native_in_collection` (ô lưới, danh sách, trang của pager). Mỗi định dạng dùng một khe (mục 9, M-05). App cần định dạng mới thì đề xuất về dự án mẫu, không tự thêm | ads |
| MC-03 | **Khóa suy ra theo mẫu.** Mỗi vị trí có một khóa bật tắt `is_show_<mã vị trí>`, boolean, mặc định tắt. Tham số riêng của một vị trí đặt tên `<tham số>_<mã vị trí>` (câu hỏi 28 của spec ads). Khóa chung của AdsKit giữ đúng tên (mục 10). Chọn mã vị trí khéo thì khóa suy ra trùng đúng chuỗi của AdsKit: vị trí `rw_get_item` cho khóa `is_show_rw_get_item`, vị trí `native_onboarding` cho khóa `is_show_native_onboarding` | ads |
| MC-04 | **Nhóm giãn cách** thay cho khóa riêng kiểu `ads_interval_sounds`. Mỗi inter thuộc một nhóm. Nhóm mặc định dùng mốc chung và khóa `ads_interval`. Vị trí khai báo nhóm riêng `<nhóm>` thì dùng khóa `ads_interval_<nhóm>`, mốc riêng của nhóm, cùng luật I-01 tới I-05. Thuộc tính của vị trí: `ignoreInterval` (bỏ cổng khi đọc) và `recordInterval` (ghi mốc hay không) thay cho tham số của từng lượt gọi (I-04). Vị trí thuộc nhóm riêng đọc và ghi mốc của nhóm mình, không đọc và không ghi mốc chung (câu hỏi 30). App open, resume và combo luôn ở nhóm mặc định | ads |
| MC-05 | **Rewarded là một hành động có điều kiện.** Vị trí rewarded khai báo: hành động nó giữ (nhận vật phẩm, xuất file, mở khóa...), và chính sách khi không có quảng cáo: `block` (chặn, người dùng thử lại được), `allow` (cho làm luôn), hay `allow_after_<n>` (cho làm sau n lần thử không có quảng cáo liên tiếp). Mặc định `block`, theo AdsKit (A 03:188-190; câu hỏi 29). Người dùng đóng quảng cáo trước khi được thưởng thì luôn chặn hành động, không khai báo khác được (R-02, R-04). Khóa bật `is_show_<mã vị trí>` tắt thì hành động làm thẳng, không hộp thoại. Cổng đồng ý đóng (chưa có đồng ý dùng được) mà khóa bật thì cũng coi như vị trí tắt: hành động làm thẳng, không hộp thoại, không ghi cơ hội (T-031, anh Nhật chốt 2026-10-25) | feature gọi, ads làm |
| MC-06 | **Native ở một bước của luồng** (giới thiệu, sau màn tải xong, màn kết quả, hướng dẫn) là một khái niệm, không phải "trang 3 của giới thiệu". Vị trí định dạng `native_step` (thẻ 250 dp từ pool, đặt ngoài pager) hoặc `native_fullscreen` (cả trang, không có nút của quảng cáo, app tự đặt nút Tiếp). Một vị trí cho cả luồng; bước nào có quảng cáo là thông số của spec app, không thành khóa (câu hỏi 32). Trang native toàn trang giữ chặn quảng cáo tự hiện khi đang hiện (G-03). Khe của bước đầu tiên xếp ở nhóm nạp sẵn đầu (HD-02) | app, ads |
| MC-07 | **Native trong tập hợp** (lưới, danh sách, pager) thay cho "ô lưới gallery" và "trang xem trước". Vị trí định dạng `native_in_collection` (thẻ dọc) hoặc `native_medium`, lấy từ pool (mục 5.9). Thuộc tính: giữ chỗ khi pool cạn hay ẩn ô (P-06; mặc định ẩn, câu hỏi 33); trong pager thì ô báo "đang được xem" (P-07). Chèn mỗi N mục là tham số `native_every_<mã vị trí>` khi spec cần | feature, ads |
| MC-08 | **Thanh đáy là một bộ ghép theo màn:** khe đáy (native nhỏ 80 dp hay banner, theo `is_show_native_bottom`) cộng lớp native thu gọn tùy chọn (theo `is_show_native_collap`). Mỗi màn có thanh đáy là một vị trí, bật bằng `is_show_<mã vị trí>` (quyết định "được phép hiện quảng cáo" của app, A 05:46-47). Vị trí đọc lúc có sự kiện (D-05) | feature, ads |
| MC-09 | **App open, resume, combo là cấu hình toàn app**, không theo vị trí: khóa chung ở mục 10. **Đổi 2026-10-26 (spec mục 5):** mã đơn vị đi theo vị trí, nên mỗi loại có một vị trí toàn app không có `is_show_<mã>`: `open_app`, `open_app_inter`, `resume`, `resume_inter`, `native_combo`. App nào không dùng combo thì để `is_show_native_after_inter` tắt. Màn native sau inter là lớp phủ do `:core:ads` vẽ, không phải đích điều hướng của `:app` (HD-05, câu hỏi 31) | ads |
| MC-10 | **Không có đường code theo tính năng.** Thêm vị trí cho một tính năng mới của app chỉ gồm: dòng trong spec, dòng trong `AdPlacement`, lời gọi composable hay hàm của `:core:ads`. Không thêm cờ, hàm hay composable mang tên tính năng vào `:core:ads` | ads |

### Từ AdsKit sang mô hình chung

Bằng chứng ở repo AdsKit, đường dẫn tính từ `M/`.

| AdsKit (riêng một app) | Bằng chứng | Mô hình chung | Khóa của ta | Giữ chuỗi của AdsKit |
| --- | --- | --- | --- | --- |
| Khóa `is_show_rw_get_item` (rewarded trước khi nhận vật phẩm, trong một game) | `DG:180` | Vị trí `rw_get_item`, `rewarded`, hành động "nhận vật phẩm" | `is_show_rw_get_item` | Có |
| Trường `isShowRewardedOnExport` (rewarded trước khi xuất ảnh) | `M/AdFeatureFlags.kt:24`, A 03:188-190 | Vị trí `rw_export`, `rewarded`, hành động "xuất file", chính sách `block` | `is_show_rw_export` (tên do ta đặt) | Không: AdsKit gán chuỗi `is_show_rw_get_item` cho trường này (`DG:180`); app đang dùng chuỗi cũ cho việc xuất thì đặt mã vị trí `rw_get_item` |
| `ads_interval_sounds` (inter ở màn âm thanh, bộ đếm riêng ở app) | `M/AdsKit.kt:124-127`, `M/controller/AdsIntervalLogFormat.kt:30-31` | Nhóm giãn cách `sounds` | `ads_interval_sounds` | Có |
| Trường `isShowNativeOnboarding`, khóa `is_show_native_onboarding` | `M/AdFeatureFlags.kt:25`, `DG:181` | Vị trí `native_onboarding`, `native_step` hay `native_fullscreen` | `is_show_native_onboarding` | Có |
| "Native sau inter, thường là trang 3 của giới thiệu" | `M/compose/NativeAfterInterScreen.kt:88-95` | Bước của luồng chọn trong spec app (MC-06) | Không có khóa riêng | Không áp dụng |
| Khe `nativeOnboarding`, pool 3, `NativeAdOnboarding`, layout `adskit_native_onboarding` | `M/AdUnitIds.kt:12`, `M/AdsKit.kt:452-460`, `M/compose/NativeAdOnboarding.kt:24-36` | Định dạng `native_step`, khe `native_step` | Không có | Không áp dụng |
| Khe `nativeContent`, `NativeAdGridCell`, `NativeAdPreviewPage`, layout thẻ dọc | `M/AdUnitIds.kt:13-14`, `M/compose/PooledNativeAd.kt:137-173` | Định dạng `native_in_collection`, vị trí theo màn | `is_show_<mã vị trí>` | Không áp dụng |
| Biến thể CTA lớn của native medium "cho giới thiệu" | `M/compose/NativeAdLayouts.kt:13-15`, `M/compose/NativeAdMedium.kt:43-48` | Thuộc tính `cta_large` của vị trí `native_medium` | Không có | Không áp dụng |
| `RewardedAdDialog` có chỗ cho nội dung "mở khóa premium" | `M/compose/RewardedAdDialog.kt:29-56` | Hộp thoại chung của hành động có điều kiện | Không có | Không áp dụng |
| `AdInterstitialScreen`, nguồn cứng `"ad_interstitial_screen"` | `M/compose/AdInterstitialScreen.kt:29-38,68-72` | Màn loading của `:app` gọi hàm của `:core:ads` (A-02); lượt dò trạng thái không đo (A-04) | `show_open_ads`, `open_ad_type` | Có |
| `BottomAdBar` "giống app gallery" | `M/compose/BottomAdDecision.kt:4` | Bộ ghép thanh đáy theo màn (MC-08) | `is_show_native_bottom`, `is_show_native_collap` (tên do ta đặt), `is_show_<mã vị trí>` | Có cho `is_show_native_bottom` |

## 1. Đồng ý và khởi động SDK

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| S-01 | Khởi tạo `:core:ads` đúng một lần mỗi process, trong `Application` trước mọi màn hình (HD-01). Gọi lại không làm gì | ads | `M/AdsKit.kt:312-538`, A 07:11-13 |
| S-02 | Mỗi lần mở app, `MainActivity` gọi một lượt hỏi đồng ý: cập nhật thông tin đồng ý của UMP, hiện form nếu UMP báo cần, rồi đọc "được phép gửi yêu cầu". Ngoài vùng có luật, UMP trả gần như ngay, không có form | app gọi, ads làm | `M/consent/ConsentManager.kt:57-101` |
| S-03 | **Init sớm (`CACHED_EARLY`):** ngay sau khi gửi lượt hỏi, nếu UMP đã có đồng ý dùng được từ phiên trước thì khởi tạo SDK quảng cáo ngay, không chờ UMP trả lời | ads | `M/consent/AdsStartupState.kt:336-344,91-96` |
| S-04 | **Lỗi mạng của UMP (`ERROR_ALLOWED`):** lượt hỏi lỗi vì mạng (mã `INTERNET_ERROR`, `TIME_OUT`, ở bước cập nhật hay bước form) thì coi như được phép, nhưng không chốt kết quả | ads | `M/consent/ConsentGate.kt:126-131` |
| S-05 | Lỗi khác của UMP (`INTERNAL_ERROR`, `INVALID_OPERATION`, mã lạ) là lỗi cấu hình: không cấp quyền tạm. Form lỗi vì Activity bị hủy, đang đóng hay đổi cấu hình là "bị ngắt" (`FORM_INTERRUPTED`): người dùng chưa trả lời, lượt sau hỏi lại | ads | `M/consent/ConsentGate.kt:86-103` |
| S-06 | **Chốt kết quả.** Nhánh rút gọn bật: chỉ chốt kết quả **sạch** (cập nhật thành công và form đóng không lỗi). Nhánh rút gọn tắt: chốt khi kết quả sạch; bước cập nhật lỗi thì không chốt; form lỗi thì chỉ chốt khi quyền của UMP đã là "cho phép". Trạng thái nhánh đọc lúc có kết quả. Đã chốt thì các lượt hỏi sau trong process trả ngay, không gọi UMP | ads | A 02:135-139, `M/consent/ConsentGate.kt:137-139`, `M/consent/AdsStartupState.kt:288-293` |
| S-07 | Lượt hỏi thứ hai đến khi lượt đầu chưa xong thì chờ chung kết quả, không mở form thứ hai | ads | `M/consent/AdsStartupState.kt:294-301` |
| S-08 | `ERROR_ALLOWED` chỉ dùng khi app ở foreground. App ở nền thì không gửi yêu cầu quảng cáo nào bằng quyền này | ads | `M/consent/AdsStartupState.kt:207-208` |
| S-09 | Đang có `ERROR_ALLOWED` thì hỏi lại UMP khi mạng về, khi app lên foreground, hoặc khi nạp sẵn lại. Chỉ hỏi trên đúng Activity đã hỏi lần trước, khi nó đang hiện và không có quảng cáo toàn màn hình che. Hỏi xong mới nạp | ads | `M/consent/AdsStartupState.kt:357-426`, `M/AdsKit.kt:769-772` |
| S-10 | Đang có quyền tạm mà UMP báo cần hỏi lại (không còn được phép): đóng cổng **ngay**, kể cả lúc form đang mở. Giữ lại một lượt nạp sẵn để chạy khi cổng mở lại | ads | `M/consent/AdsStartupState.kt:109-116` |
| S-11 | Quảng cáo đã nạp bằng quyền tạm **không lên màn hình** khi cổng đã đóng: app open, rewarded, native (lấy ra từ khe hay pool), lượt đổi quảng cáo của native thu gọn | ads | `M/consent/ConsentGate.kt:150`, `T/manager/PartnerLoadedAdShowGuardTest.kt` |
| S-12 | **Nhánh rút gọn** (`CACHED_EARLY`, `ERROR_ALLOWED`) chỉ có hiệu lực khi **cả** cấu hình lúc build của `:app` **và** công tắc từ xa `is_enable_consent_shortcut` (mục 10) cùng bật; **mặc định cả hai tắt**. Tắt thì đi theo UMP. Tắt công tắc giữa phiên thì thu hồi **ngay** quyền chỉ có nhờ `ERROR_ALLOWED`, không chốt kết quả (lượt hỏi sau theo luật nhánh tắt); quyền thật của UMP, kể cả `CACHED_EARLY`, giữ nguyên. Bật công tắc không bao giờ thu hồi gì | ads | A 02:24-26, 02:129, 02:165, `M/AdsKitConfig.kt:55`, `M/AdsKit.kt:876-892`, `M/consent/AdsStartupState.kt:146-152`, `T/PartnerConsentRevocationTest.kt` |
| S-13 | Đường đi của đồng ý ghi được để đo: `CACHED_EARLY`, `GRANTED`, `DENIED_OR_REQUIRED`, `ERROR_ALLOWED`, `INTERRUPTED_REGATHER` | ads | `M/consent/AdsStartupState.kt:27`, `M/consent/ConsentGate.kt:153-161` |
| S-14 | **Rút đồng ý:** nút quyền riêng tư (`PrivacyOptionsButton`) chỉ hiện khi UMP báo form này là bắt buộc (gọi sai lúc thì UMP trả `INVALID_OPERATION`, A 02:171), và mở form của UMP. Form đóng thì đọc lại quyền: rút thì đóng mọi cổng; cấp lại khi SDK chưa khởi tạo thì khởi tạo; cấp lại khi đã khởi tạo thì nạp sẵn | ads | `M/AdsKit.kt:779-804`, `M/consent/ConsentGate.kt:44-53` |
| S-15 | Sau khi rút: cổng nạp đóng ở **mọi** lượt nạp, kể cả các vòng tự nạp lại; cổng hiển thị đóng; quảng cáo đã nạp không hiện. Chuẩn bị yêu cầu mất thời gian thì hỏi lại cổng **ngay trước khi gửi**; cổng đã đóng thì kết thúc lượt, không gửi, không tính là lỗi, không hẹn thử lại (A 02:54-59, A 05:171-174) | ads | `M/AdsKit.kt:574-581,1142,1195`, commit `0a5c65b` |
| S-16 | Form đồng ý (chỉ khi UMP báo trạng thái `REQUIRED`, tức form thật sự hiện) và form quyền riêng tư (luôn) chặn quảng cáo khi quay lại app suốt lúc form mở (A 04:143-146); nhả đúng một lần khi form đóng, kể cả khi đóng có lỗi | ads | `M/consent/ConsentManager.kt:171-191`, `T/consent/ConsentFormAdSuppressTest.kt` |
| S-17 | Cài đặt gỡ lỗi của UMP (vùng địa lý giả, máy test) chỉ dùng ở staging; lượt hỏi lại dùng lại cài đặt của lượt trước. Staging giả lập vùng EEA (ADR-0013) | ads | `M/AdsKit.kt:709-713`, `M/consent/ConsentManager.kt:137-141` |
| S-18 | Đặt lại đồng ý (xóa đồng ý đã lưu, bỏ chốt) chỉ có ở bản staging để thử lại form; bản production không có đường này | ads | A 02:198 |
| S-19 | Trước khi `:core:ads` khởi tạo: mọi cổng đóng, lệnh nạp sẵn không làm gì, nhưng tín hiệu khởi động vẫn đọc được ("SDK sẵn sàng" là `false`, đường đồng ý rỗng) | ads | A 02:50 |

## 2. Thời điểm khởi tạo, nạp sẵn, mạng

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| S-20 | SDK quảng cáo khởi tạo đúng một lần. Không khe nào tự nạp khi được tạo; một kế hoạch nạp sẵn điều phối việc nạp | ads | `M/AdsKit.kt:544-564` |
| S-21 | "SDK sẵn sàng" khi callback khởi tạo về, **hoặc** sau tối đa 35 giây kể từ lúc khởi tạo. Chỉ nạp sẵn khi SDK sẵn sàng | ads | `M/AdsKit.kt:560-572`, `M/behavior/PartnerAdsConstants.kt:19`, `M/consent/AdsStartupState.kt:214-218` |
| S-22 | **Kế hoạch nạp sẵn:** danh sách nhóm khe chạy lần lượt. Nhóm sau bắt đầu khi mọi khe của nhóm trước xong (có quảng cáo hay lỗi đều tính), hoặc sau tối đa 6 giây. Kiểm mỗi 200 ms. Khe không có trong kế hoạch thì không nạp sẵn. Mặc định một nhóm gồm mọi khe; `:app` đặt kế hoạch **trước** khi đồng ý có kết quả, xếp khe người dùng thấy đầu tiên (app open, native của bước đầu luồng) ở nhóm đầu (HD-02) | ads | `M/AdPreloadPlan.kt:9-48`, `M/AdsKit.kt:91-98,639-677` |
| S-23 | Lượt nạp sẵn bị chặn vì cổng đóng được giữ lại, chạy đúng một lần khi cổng mở; không chạy chồng lên kế hoạch đang chạy. Không giữ lượt nào khi SDK chưa khởi tạo, khi app chưa từng xin nạp sẵn, hay khi quyền thật của UMP bị đóng. Lệnh nạp sẵn khi quyền là "không" thì không làm gì; mọi lượt nạp sẵn, nạp lại, nạp bù đều đi qua kế hoạch, không gọi tay từng khe (A 02:68-75) | ads | `M/consent/AdsStartupState.kt:159-193,462-492` |
| S-24 | **Mạng về:** chỉ tính mạng có Internet đã được xác thực; gộp các lần đổi mạng trong 3 giây. Mạng về thì nạp sẵn lại và đặt lại thời gian chờ thử lại của native. Dùng theo dõi kết nối chung của app (T-016), không viết bộ theo dõi thứ hai | ads | `M/net/AdsNetworkMonitor.kt:37-92`, `M/AdsKit.kt:590-605` |
| S-25 | Khi Activity chính quay lại (resume), nạp bù các khe native theo thứ tự kế hoạch, chỉ khi kế hoạch đã chạy xong ít nhất một lần, không có kế hoạch đang chạy và không có quảng cáo toàn màn hình | ads | `M/manager/NativePolicyDecisions.kt:115-132`, `M/AdsKit.kt:613-622` |

## 3. Quảng cáo toàn màn hình: cổng chung

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| F-01 | Không bao giờ có hai quảng cáo toàn màn hình chồng nhau. Lượt bị chặn vì đang có một cái hiện thì **bỏ hẳn**, không xếp hàng (kết quả `busy`) | ads | `M/AdsKit.kt:946-949,1186-1195`, commit `e3c1551` |
| F-02 | Cờ "đang hiện" chỉ hạ ở callback thật của SDK (đóng, hiện lỗi). Có trần 10 phút, vì người dùng bấm quảng cáo rồi đọc trong trình duyệt vẫn hợp lệ | ads | `M/manager/FullscreenShowState.kt:21-66` |
| F-03 | **Hồi phục khi kẹt:** đúng Activity đã dùng để hiện quảng cáo bị che (pause) rồi hiện lại (resume) mà cờ vẫn bật thì coi là kẹt: hạ cờ của mọi loại toàn màn hình, ghi mốc `ads_interval` lúc phát hiện, chạy hàm hoàn tất đang chờ đúng một lần. Phải có "che trước rồi mới hiện lại"; tín hiệu "đã rời màn" bị tiêu sau lần dùng đầu (A 04:165). Không tự đóng màn hình quảng cáo đang treo | ads | `M/controller/StuckFullscreenAdRecovery.kt:34-74`, `M/controller/ShowHostResumeSignal.kt:18-40`, `M/AdsKit.kt:991-1019,1307-1314` |
| F-04 | Hỏi "đã sẵn sàng chưa" chỉ đọc trạng thái, không bao giờ nạp. Hỏi "hiện được không" mà chưa có quảng cáo thì kích "nạp nếu cần": không nạp khi đang nạp, đã có, hay đang chờ thử lại. Lượt hẹn giờ tới hạn lúc cổng đồng ý đóng thì khe thôi bị coi là "đang hẹn giờ" (A 03:30-32) | ads | `M/AdsKit.kt:1120-1133`, `M/manager/FullscreenPolicyDecisions.kt:35-36` |
| F-05 | **Thử lại sau lỗi nạp** (inter, app open, rewarded): chờ `min(n x 2 giây, 10 giây)` với lần lỗi thứ n, mọi mã lỗi, không giới hạn số lần, không ngẫu nhiên. Bộ đếm lỗi về 0 khi nạp thành công | ads | `M/retry/AdRetryPolicy.kt:54-200`, `M/behavior/PartnerAdsConstants.kt:42-43` |
| F-06 | **Nạp lại sau khi đóng hay hiện lỗi:** inter và rewarded chờ 2 giây; app open nạp ngay | ads | `M/manager/InterstitialAdManagerImpl.kt:270-272`, `M/manager/AppOpenAdManagerImpl.kt:253-266`, `M/behavior/PartnerAdsConstants.kt:38` |
| F-07 | Gọi hiện khi chưa có quảng cáo: chạy hàm hoàn tất ngay, nạp lại, không hiện thông báo nào | ads | `M/manager/InterstitialAdManagerImpl.kt:257-266` |

## 4. Mốc `ads_interval`

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| I-01 | Một mốc chung: "lần gần nhất một inter rời màn hình". Cổng mở khi thời gian từ mốc tới nay lớn hơn hoặc bằng `ads_interval` giây. Chưa có mốc thì không chặn. Đúng biên thì cho qua | ads | `M/controller/InterstitialThrottleDecision.kt:9-29` |
| I-02 | Chỉ inter đẩy mốc, ở ba điểm: khi inter đóng, khi hạ cờ kẹt (F-03), và khi đóng cả cụm inter rồi native (K-05). App open và rewarded **không** đẩy mốc | ads | `M/manager/FullscreenPolicyDecisions.kt:78-79` |
| I-03 | Mốc chỉ giữ trong bộ nhớ của process. Mở lại app (process mới) thì bắt đầu từ chưa có mốc. Không lưu qua phiên | ads | `M/controller/InterstitialThrottle.kt:51-88` |
| I-04 | Bỏ cổng khi đọc (`ignoreInterval`) và ghi mốc hay không (`recordInterval`) là hai lựa chọn độc lập của từng lượt gọi. Vị trí có bộ đếm riêng (ví dụ `ads_interval_<vị trí>`) gọi với `recordInterval=false`, áp ở mọi điểm ghi | ads | `M/AdsKit.kt:121-134,1150-1166`, commit `61981bb` |
| I-05 | Ghi mốc trước khi đếm lượt hiện hay ghi sự kiện: ghi sự kiện lỗi thì mốc vẫn còn. Ghi mốc ở callback thật của SDK, không ở hàm hoàn tất chung | ads | `M/AdsKit.kt:358-359`, commit `32afa80`, `f2dab3d` |

## 5. Từng định dạng

### 5.1 Interstitial

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| N-01 | Hiện được khi: cổng đồng ý mở, có quảng cáo, không có quảng cáo toàn màn hình khác, và qua mốc `ads_interval` (hoặc lượt gọi bỏ cổng) | ads | `M/AdsKit.kt:1117-1148` |
| N-02 | Feature gọi inter qua `rememberShowInterstitial(placement)` ở một hành động của người dùng (bấm nút sang màn sau, xong một việc). Bị chặn thì app đi tiếp ngay. Inter đóng thì hàm hoàn tất chạy **một lần**, sau khi màn quảng cáo đã đóng | feature gọi, ads làm | `M/AdsKit.kt:1157-1244`, `M/manager/InterstitialAdManagerImpl.kt:236-249` |
| N-03 | App đọc được inter vừa rồi có thật sự lên màn không (cờ "lần gần nhất đã hiện"), dùng cho luồng sau quảng cáo **khác** của app. Combo không đọc cờ này (K-01). Hàm hoàn tất cũng chạy khi chưa nạp, hiện lỗi hay hồi phục kẹt | ads | `M/AdsKit.kt:146-154,1225` |
| N-04 | Trong lúc inter hiện, `:core:ads` cho biết trạng thái "đang hiện" để vỏ app khóa nút quay lại. Có thao tác **hoàn tất cưỡng bức**: Activity đã giao cho inter bị hủy khi inter chưa báo đóng thì tắt trạng thái và chạy hàm hoàn tất đang chờ; hàm hoàn tất chạy đúng một lần dù đến từ SDK, hồi phục kẹt hay hoàn tất cưỡng bức (HD-03) | ads, app | `R.md:153-157`, A 03:99, A 07:86-88 |
| N-05 | Đếm số lần inter đã hiện từ lúc cài app, lưu trên máy, tăng khi inter hiện. Dùng cho sự kiện `af_inters_displayed_{N}_times` (mục 8) | ads | `M/AdsKit.kt:1348-1377`, `T/InterDisplayCounterTest.kt` |

### 5.2 Native sau inter (combo)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| K-01 | Combo chạy khi `is_show_native_after_inter` bật. Giá trị đọc **lúc inter hoàn tất**. Inter đã qua cổng thì khi hoàn tất mở màn native toàn màn hình, **kể cả khi inter không lên được màn** (mất quảng cáo giữa chừng, hiện lỗi) (A 03:113, đã chốt). Inter bị chặn ngay ở cổng (chưa sẵn sàng, giãn cách, bận, đồng ý) thì không có native. Khóa tắt thì đi tiếp như inter thường | ads | `M/AdsKit.kt:1084-1115`, `T/controller/ResumeAdShowPlanTest.kt:154` |
| K-02 | Đếm ngược **3 giây**, bắt đầu khi inter đã rời màn hẳn; chờ inter rời màn tối đa 2 giây (kiểm mỗi 200 ms) để không kẹt khi cờ inter kẹt. Mốc kết thúc là thời điểm tuyệt đối, giữ qua lúc Activity bị tạo lại | ads | `M/compose/NativeAfterInterScreen.kt:34-48,99-106,142-158` |
| K-03 | Nút đóng ở góc ~~**trên phải**, cách mép 12 dp~~ **đổi 2026-10-26:** **trên trái**, tại (16, 24) dp theo thiết kế T-035 (góc trên phải dành cho AdChoices; KB-16 của spec), vòng tròn **36 dp**, biểu tượng X 20 dp. Trong lúc đếm, chỗ đó là vòng 36 dp chứa số giây (16 sp, đậm). Nút dùng được khi đếm xong **hoặc** khi nạp lỗi | ads | `M/compose/NativeAfterInterScreen.kt:116-121,183-195` |
| K-04 | Nạp lỗi thì tự đóng khi hết đếm ngược. Quảng cáo chưa về thì hiện khung chờ toàn màn hình và vẫn đóng được sau 3 giây. Bấm đóng chỉ ăn một lần | ads | `M/compose/NativeAfterInterScreen.kt:115,133-140,168-170`, commit `4362a78` |
| K-05 | Đóng màn native thì đẩy mốc `ads_interval` về lúc đóng cả cụm. Trong lúc màn native hiện, quảng cáo khi quay lại app bị chặn (mục 6). Màn native không tự bật chặn: bộ điều phối combo của `:core:ads` (bên mở màn) giữ một suất của bộ đếm chặn khi màn đang hiện và nhả khi đóng (A 03:124, A 04:151) | ads | `M/AdsKit.kt:1021-1052`, `M/compose/NativeAfterInterScreen.kt:78-84` |
| K-06 | Mỗi lần hiện sở hữu một quảng cáo native riêng, hủy khi rời màn, rồi nạp sẵn cho lần sau | ads | `M/compose/NativeAfterInterScreen.kt:92-95,159-167` |
| K-07 | Mỗi lần hiện ghi đúng một kết quả cơ hội: `shown` nếu có quảng cáo; `not_ready` nếu nạp lỗi hay người dùng đóng khi quảng cáo chưa về | ads | `M/compose/NativeAfterInterScreen.kt:55-65`, commit `80e82a8` |
| K-08 | Nút quay lại của máy trên màn native: AdsKit không xử lý; ta chặn trong lúc đếm ngược và cho đóng khi nút đóng đã dùng được (câu hỏi 17 của spec ads) | ads | `R.md:153-154` |

### 5.3 App open lúc mở app

AdsKit không có splash; app chủ tự làm. Ở dự án mẫu, màn loading (`:feature:loading`, ADR-0018) đóng vai splash.

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| A-01 | Lần mở đầu sau khi cài (hoặc sau khi xóa dữ liệu) dùng khóa `show_open_ads_first_open`; từ lần mở thứ hai dùng `show_open_ads`. App lưu dấu "đã mở lần đầu" trên máy | app | `M/AdFeatureFlags.kt:4-6`, `R.md:113-115` |
| A-02 | `open_ad_type` là `"aoa"` hay `"inter"`. Với `"inter"`: hiện inter, bỏ cổng `ads_interval` khi đọc, và lượt hiện vẫn đẩy mốc. Với `"aoa"`: hiện app open nếu sẵn sàng, không thì đi tiếp. Hàm của `:core:ads` nhận ba tham số (có hiện không, loại, có bỏ giãn cách không) và trả "đã hiện inter hay không" (A 03:162-165) | app gọi, ads làm | `M/compose/AdInterstitialScreen.kt:53-94` |
| A-03 | Màn loading chờ có giới hạn: đồng ý tối đa 10 giây, SDK tối đa 35 giây, app open nạp xong tối đa 40 giây, kiểm mỗi 250 ms. Hết thời gian chờ đồng ý thì không cấp quyền: form còn mở sau 10 giây thì SDK vẫn chưa khởi tạo | app | `M/behavior/PartnerAdsConstants.kt:16-25` |
| A-04 | Trong lúc chờ, chỉ hỏi "đã sẵn sàng chưa" (không tự nạp) và "đảm bảo đang nạp" (không gửi thêm yêu cầu khi đang nạp, đã có hay đang chờ thử lại). Kiểm 250 ms suốt 40 giây mà không có quảng cáo thì chỉ gửi yêu cầu theo lịch thử lại. Lượt dò trạng thái của màn loading **không đo**: không ghi cơ hội, không gắn vị trí (A 06:212, A 07:76) | ads | `R.md:171-173`, `M/manager/FullscreenPolicyDecisions.kt:35-36` |
| A-05 | Luôn hiện qua một hàm của `:core:ads` có cổng đồng ý, cổng chồng nhau và hồi phục kẹt; vỏ app không gọi SDK | app gọi, ads làm | commit `5d814c2`, `ee7d875` |
| A-06 | Vỏ app tự bỏ app open (hết giờ chờ mà chưa có quảng cáo) thì vẫn ghi cơ hội `not_ready` cho vị trí đó | app | `M/AdsKit.kt:1273-1292`, commit `ff59e7a` |
| A-07 | Quảng cáo app open hết hạn từ đủ **1 giờ** sau khi nạp; lượt nạp bỏ quảng cáo hết hạn (trừ cái đang hiện) | ads | `M/manager/FullscreenPolicyDecisions.kt:12-22`, `M/manager/AppOpenAdManagerImpl.kt:122-130` |
| A-08 | Không hiện app open trong **1 giây** sau lần app open trước đóng | ads | `M/manager/FullscreenPolicyDecisions.kt:28-29` |
| A-09 | App open đóng hay hiện lỗi thì nạp lại ngay, nhưng chỉ bỏ quảng cáo trong khe nếu đó đúng là quảng cáo vừa chiếu; quảng cáo mới nạp trong lúc chiếu được giữ | ads | A 03:144 |

### 5.4 Quảng cáo khi quay lại app (resume)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| A-10 | Theo dõi vòng đời của cả process: app lên foreground thì xét hiện; app xuống nền thì ghi mốc. Lần lên foreground đầu tiên của process là lúc mở app (màn loading lo), bỏ qua, kể cả khi lúc đó cổng đồng ý chưa mở. SDK chưa khởi tạo hay đồng ý chưa cho thì lượt resume im lặng (không ghi cơ hội, không kích nạp) nhưng **vẫn tiêu** cờ lần đầu (A 04:25-30) | ads | `M/controller/AppOpenAdController.kt:53-73,98-100` |
| A-11 | **Thứ tự cổng:** lần đầu của process, rồi đang bị chặn (rời app có chủ đích, màn tự mở, form UMP), rồi `show_resum_ads` tắt, rồi đang có quảng cáo toàn màn hình, rồi khóa 1 giây sau app open trước (chỉ khi resume là app open), rồi chưa có quảng cáo. Không có cổng thời gian nào: không có thời gian tối thiểu ở nền, không bị `ads_interval` chặn | ads | `M/controller/AppOpenAdPolicy.kt:127-162`, `T/controller/ResumeAdPartnerRulesTest.kt:130,165` |
| A-12 | `resume_ad_type` đúng bằng `"inter"` thì resume là inter (bỏ cổng `ads_interval`, rồi nối combo nếu `is_show_native_after_inter` bật). Mọi giá trị khác là app open | ads | `M/controller/AppOpenAdPolicy.kt:165-166,203-208` |
| A-13 | Bị chặn vì chưa có quảng cáo thì kích nạp lặng lẽ; lượt này bỏ, lượt sau có quảng cáo. Bị chặn ở các cổng trước đó thì không kích nạp (A 04:69) | ads | `M/controller/AppOpenAdController.kt:182-235` |
| A-14 | **Bấm quảng cáo thì bỏ lượt resume kế tiếp:** mọi quảng cáo (toàn màn hình, native, banner) khi được bấm đánh dấu "rời app"; dấu này nhả ở lần Activity chính hiện lại. Bấm nhiều lần chỉ giữ một dấu | ads | `M/manager/AdClickResumeSkip.kt:12-14`, `T/manager/AdClickSkipsResumeTest.kt` |
| A-15 | Vị trí của resume ~~luôn là `resume`~~ **đổi 2026-10-26:** là `resume` khi loại app open, `resume_inter` khi loại inter (spec mục 5). Resume cũng đi qua hàm hiện có hồi phục kẹt | ads | `M/controller/AppOpenAdController.kt:126-153` |

### 5.5 Banner

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| B-01 | Banner cố định: thích ứng theo chiều rộng màn hình, neo ở đáy. Lần nạp đầu chờ cổng đồng ý, không nạp ngay lúc dựng giao diện | ads | `M/compose/BannerAdFixed.kt:39-157`, commit `0a5c65b` |
| B-02 | **Làm mới:** dùng đơn vị của khe `banner`, đã bật tự làm mới trên AdMob (mã nguồn AdsKit gọi khe này là `bannerPartner`, M-05); app không tự nạp lại. Banner tạm dừng và chạy lại theo Activity chính. Lỗi thì thử lại `min(n x 2 giây, 10 giây)`; tới hạn thử lại khi Activity không hiện thì dời tới lần hiện lại kế tiếp | ads | `M/manager/NativePolicyDecisions.kt:84-92`, `M/compose/BannerAdFixed.kt:58-141` |
| B-03 | Banner thu gọn được (collapsible, bung từ đáy): khe `banner_collapsible`, đơn vị **không** bật tự làm mới; `:core:ads` nạp lại tay theo nhịp (mặc định 30 giây), mỗi lượt qua cổng đồng ý và có mã yêu cầu ngẫu nhiên mới, nên mỗi lần làm mới là một banner bung ra mới. Không nằm trong thanh đáy | ads | A 05:108, A 08:40, `M/compose/BannerAd.kt:23-65` |
| B-04 | Mã đơn vị rỗng thì không gửi yêu cầu, không chiếm chỗ | ads | `T/compose/BannerAdRequestDecisionTest.kt` |
| B-05 | Mọi lượt nạp lại hay thử lại của banner chỉ chạy khi app ở foreground. AdsKit đã dời lượt thử lại tới lần hiện lại; vòng nạp lại tay khi thiếu đơn vị tự làm mới thì chưa kiểm (mục 12, AR-03) | ads | `M/compose/BannerAdFixed.kt:61-73,130-131,143-147` |
| B-06 | **Rút đồng ý giữa phiên khi banner đã có quảng cáo:** dừng banner (không gửi yêu cầu mới, tạm dừng tự làm mới), nhưng **giữ khung** và chỗ chừa cho nội dung, không co về 0; cổng mở lại thì nạp lại trong khung đó | ads | A 05:110-115 |

### 5.6 Thanh quảng cáo ở đáy màn hình

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| D-01 | Hai lớp. Khe đáy luôn có khi màn được phép hiện quảng cáo: native nhỏ nếu `is_show_native_bottom` bật, không thì banner cố định. Lớp native thu gọn nổi đè lên khe đáy khi native bật **và** công tắc riêng `is_show_native_collap` bật (mặc định bật, nên mặc định đi theo `is_show_native_bottom`); tắt lớp collap không làm mất khe đáy (A 05:56). Người dùng thu gọn lớp nổi thì lộ khe đáy bên dưới | ads | `M/compose/BottomAdBar.kt:20-66` |
| D-02 | Khe đáy native cao **80 dp** (A 05:68, đã chốt; mã nguồn AdsKit vẽ 56 dp); banner cao theo nội dung. Lúc chờ lượt nạp đầu, khe native hiện khung chờ (shimmer). Nạp lỗi thì khe co về 0 dp và báo app "không có quảng cáo"; sau đó có quảng cáo thì hiện lại và báo "có" (A 05:72) | ads | A 05:53-72, `M/compose/BottomAdSlot.kt:42-53`, commit `d7f0a6d` |
| D-03 | Lớp nổi chỉ lấp chiều rộng, không lấp cả khoảng trống, để không đẩy thanh điều hướng của app ra ngoài và không nhận chạm thay app | ads | `M/compose/BottomAdBar.kt:50-57`, commit `b4781fa` |
| D-04 | Khoảng chừa dưới nội dung chỉ tính khe đáy và chỉ khi khe đã có quảng cáo, đúng bằng chiều cao thật của khe (80 dp với native, cùng một hằng số với chiều cao vẽ). Lớp nổi không tính. Không được phép hiện quảng cáo thì không dựng thanh đáy, chừa 0 | ads | `M/compose/BottomAdDecision.kt:26-45` |
| D-05 | Vị trí của thanh đáy đọc **lúc có sự kiện** (hiện, bấm, doanh thu), không chụp lúc dựng, vì thanh đáy sống qua nhiều màn hình | ads | `M/compose/BannerAdFixed.kt:55-57`, `M/compose/NativeAdSmall.kt:64-66` |

### 5.7 Native: các biến thể và kích thước

Kích thước theo layout của AdsKit (`src/main/res/layout/adskit_native_*.xml`). Màu lấy từ token của `:core:designsystem`, không lấy màu của AdsKit. Màu chữ native không đổi theo chế độ tối của máy khi nền quảng cáo không đổi (AdsKit commit `9994dbc`).

| Biến thể | Nguồn quảng cáo | Kích thước chính | Làm mới | AdsKit |
| --- | --- | --- | --- | --- |
| Native nhỏ ở khe đáy | Khe một quảng cáo `native_small` | Cao 80 dp (A 05:248); icon 36 dp; tiêu đề 14 sp; mô tả 12 sp; CTA cao 34 dp, chữ 13 sp; nhãn "Ad" 10 sp; không có media | Thay nội dung tại chỗ mỗi 60 giây, bắt đầu trễ 500 ms sau lớp thu gọn; chỉ ở foreground; qua cổng đồng ý; lỗi thì giữ quảng cáo cũ | `M/compose/NativeAdSmall.kt:41-149`, `res/layout/adskit_native_small.xml` |
| Native thu gọn (lớp nổi) | Khe `native_collap` (một quảng cáo và một quảng cáo chờ) | Cao 250 dp; media lấp phần còn lại; icon 32 dp; tiêu đề 13 sp; CTA cao 36 dp; nút thu gọn 32 dp (biểu tượng 20 dp, cách mép 4 dp) ở góc trên phải, nền đen 40% | Mỗi 60 giây (mục 5.8) | `M/compose/NativeAdCollap.kt:33-247`, `res/layout/adskit_native_collap.xml` |
| Native đầy đủ | Khe `native_full` | Media cao 200 dp; icon 40 dp; tiêu đề 14 sp; cao theo nội dung | Không | `M/compose/NativeAdFull.kt`, `res/layout/adskit_native_full.xml` |
| Native toàn màn hình (bước của luồng, sau inter) | Khe `native_fullscreen` | Lấp đầy màn; icon 48 dp; tiêu đề 16 sp; CTA cao 52 dp. Bản ở bước của luồng không có nút điều khiển, app tự đặt nút Tiếp; lỗi thì không vẽ gì | Không | `M/compose/NativeAdFullscreen.kt:19-56`, `res/layout/adskit_native_fullscreen.xml` |
| Native ở bước của luồng (`native_step`; AdsKit gọi là onboarding) | Pool `native_step`, 3 quảng cáo | Cao 250 dp; icon 32 dp; CTA cao 36 dp | Không | `M/compose/NativeAdOnboarding.kt:35-81`, `res/layout/adskit_native_onboarding.xml` |
| Native medium | Pool `native_medium`, 3 quảng cáo | Icon 40 dp; có nền mờ sau media và hàng sao; CTA cao 44 dp. Biến thể CTA lớn (thuộc tính `cta_large` của vị trí): CTA cao 56 dp, chữ 17 sp, không viết hoa toàn bộ. Chiều cao do nơi gọi quyết | Không | `M/compose/NativeAdMedium.kt`, `M/compose/NativeAdLayouts.kt:5-16`, `res/layout/adskit_native_medium*.xml` |
| Native trong tập hợp (`native_in_collection`: ô lưới, danh sách, trang của pager) | Pool `native_content`, 5 quảng cáo, xin media dọc | Thẻ dọc: media lấp phần trên; icon 32 dp; tiêu đề 13 sp; khung chờ dạng thẻ dọc | Không | `M/compose/PooledNativeAd.kt:78-186`, `res/layout/adskit_native_card_portrait.xml` |

Luật chung của native:

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| O-01 | Mỗi lượt hiện sở hữu riêng một quảng cáo native: lấy ra thì khe tự nạp cái mới; rời màn thì hủy. Một quảng cáo chỉ ghi được một lượt hiển thị; dựng lại cùng một quảng cáo thì không có doanh thu | ads | `M/manager/NativeAdProvider.kt:31-61`, commit `51897f2` |
| O-02 | Hai chỗ cùng chờ một lượt nạp: chỗ thua thử lại một lần, không coi là hết quảng cáo | ads | `M/manager/NativeAdProvider.kt:12-29`, `T/manager/NativeAdClaimTest.kt` |
| O-03 | Nguồn bị hủy hay nơi hiện rời màn khi yêu cầu đang bay: quảng cáo về sau đó thì hủy ngay; lỗi về sau đó không hẹn thử lại; chỗ đang chờ được trả "không có quảng cáo" ngay và nguồn không còn ở trạng thái "đang nạp" (A 05:165-169) | ads | `M/compose/NativeAdSmall.kt:264-307`, `M/manager/NativeAdManagerImpl.kt:336-348` |
| O-04 | Native hết hạn từ đủ 55 phút sau khi nạp; quảng cáo chờ đã hết hạn thì bỏ, không đổi vào | ads | `M/manager/NativePolicyDecisions.kt:39-46`, `M/behavior/PartnerAdsConstants.kt:48` |
| O-05 | **Thử lại native:** còn giữ quảng cáo thì chờ 2, 4, 8, 16, 32 rồi 60 giây với mọi mã lỗi; chưa giữ quảng cáo nào thì thử lại cố định sau 5 giây. Bộ đếm về 0 khi thành công. Không gửi yêu cầu khi app ở nền; kiểm lại cổng đồng ý ngay trước khi gửi | ads | `M/manager/NativePolicyDecisions.kt:21-33,100-101`, `M/manager/NativeAdRequester.kt:33-58`, `M/behavior/PartnerAdsConstants.kt:50-56` |
| O-06 | Tùy chọn yêu cầu: video tắt tiếng, không có điều khiển riêng, một ảnh, media ngang (ô lưới xin media dọc) | ads | `M/manager/NativePolicyDecisions.kt:57-73` |
| O-07 | Đặt cách co giãn ảnh **trước** khi gắn quảng cáo vào khung, để ảnh không bị cắt | ads | commit `f3947af` |
| O-08 | Nhãn "Ad" luôn thấy được, ~~đặt ở góc trên trái~~ **đổi 2026-10-26:** đặt theo thiết kế T-035, ở đầu dòng mô tả dưới tiêu đề (câu hỏi 6 của plan T-027); biểu tượng AdChoices do SDK ghim ở góc trên phải (**thêm 2026-10-26:** trừ lớp nổi của native thu gọn, nơi nút thu gọn chiếm góc trên phải nên AdChoices ở góc trên trái, theo thiết kế T-035 và plan T-030), không bị đè hay che. Hàng sao chỉ hiện khi quảng cáo có điểm thật lớn hơn 0: sao đặc làm tròn theo điểm, tối đa 5, không vẽ nửa sao | ads | A 05:240-242, `src/main/res/values/strings.xml` (`adskit_native_ad_label`) |
| O-09 | Chuẩn bị yêu cầu native bị lỗi (kể cả SDK tự hủy giữa chừng) thì tính là một lần nạp lỗi mã 0 và thử lại theo O-05 | ads | A 05:174, `M/manager/NativeAdRequester.kt:100` |

### 5.8 Native thu gọn

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| CL-01 | Quảng cáo đầu tiên mở rộng, trừ khi người dùng đã thu gọn. Lần nạp đầu chờ tối đa 10 giây (kiểm mỗi 200 ms); quá thời gian thì không vẽ | ads | `M/compose/NativeAdCollap.kt:69-93`, `M/compose/NativeAdCollapReloadPolicy.kt:29` |
| CL-02 | Làm mới mỗi 60 giây: nạp quảng cáo chờ, đợi tối đa 10 giây, rồi đổi vào và hủy cái cũ. Làm mới **chỉ đổi nội dung**, giữ nguyên trạng thái thu gọn. Lỗi thì giữ quảng cáo và trạng thái cũ. Ở nền thì bỏ nhịp | ads | `M/compose/NativeAdCollap.kt:97-188`, `M/manager/NativePolicyDecisions.kt:76-81` |
| CL-03 | **Bung lại:** hẹn giờ riêng, đếm từ lúc người dùng bấm thu gọn, kéo dài `time_expand_collap_ad` giây (mặc định 30). Giá trị 0 hay âm là không bao giờ bung lại. Làm mới không đặt lại hẹn giờ. Chỉ bung khi đang có quảng cáo | ads | `M/compose/NativeAdCollap.kt:190-217`, commit `28f6941`, `5e5deba` |
| CL-04 | Vòng làm mới đọc trạng thái thu gọn **hiện tại**, không dùng giá trị chụp lúc bắt đầu | ads | `M/compose/NativeAdCollap.kt:55-58`, commit `70d52db` |

### 5.9 Pool native (ô lưới, medium, giới thiệu)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| P-01 | Pool nạp bằng nhiều yêu cầu một quảng cáo riêng (yêu cầu nhiều quảng cáo một lần bỏ qua mediation). Mỗi lần lấy trả một quảng cáo rảnh, theo vòng tròn; không bao giờ trả cái đang hiện. Ô rời màn thì trả quảng cáo về pool theo P-01a | ads | `M/manager/NativeAdPoolManager.kt:28-41`, `M/manager/RoundRobinPool.kt:12-77` |
| P-01a | **Quảng cáo đã ghi lượt hiển thị thì không cấp lại.** Ô trả về một quảng cáo chưa ghi lượt hiển thị thì nó trở lại vòng xoay; đã ghi thì hủy ngay và pool nạp bù. Khác mã nguồn AdsKit (mọi quảng cáo trả về đều quay lại vòng xoay) | ads | A 05:197-202 (luật mới của bản đặc tả); mã nguồn `M/manager/RoundRobinPool.kt:53-55` |
| P-02 | Cỡ pool: ô lưới 5, giới thiệu 3, medium 3. Pool đếm cả quảng cáo đang dùng; dư một so với số ô cùng lúc | ads | `M/manager/NativeAdPoolManager.kt:425`, `M/AdsKit.kt:452-487` |
| P-03 | Nạp bù tuần tự, mỗi lúc một yêu cầu. Quảng cáo trong pool hết hạn từ đủ 55 phút | ads | `M/manager/NativeAdPoolManager.kt:113-141,354-359,427` |
| P-04 | Vị trí gán lúc **lấy** quảng cáo ra; bấm và doanh thu tính cho màn đang giữ quảng cáo | ads | `M/manager/NativeAdPoolManager.kt:363-381` |
| P-05 | **Pool cạn** khi không có quảng cáo rảnh, không có yêu cầu đang chạy, và không nạp được (chưa khởi tạo, mã rỗng, đang chờ thử lại, ở nền, cổng đồng ý đóng). Pool đầy mà mọi quảng cáo đang dùng thì không tính là cạn | ads | `M/manager/NativeAdPoolAvailability.kt:14-68` |
| P-06 | Ô chờ quảng cáo thử lấy mỗi 500 ms; khi pool cạn thì mỗi 2 giây, vẫn tiếp tục nên quảng cáo về muộn vẫn hiện. Ô cạn thì không vẽ gì, không giữ chỗ; nơi gọi chọn được hiện khung chờ thay vì ẩn | ads | `M/compose/PooledNativeAd.kt:26-29,62-71,101-133`, commit `a4b33ca` |
| P-07 | Ô trong pager chỉ tính là "đang hiện" khi trang đó đã dừng hẳn. Ô chưa từng hiện thì không ghi gì. Ghi `shown` khi ô vừa có quảng cáo vừa đang hiện; ghi `not_ready` khi ô rời màn mà chưa có quảng cáo. Native ở bước của luồng không có trạng thái này nên chỉ đặt **ngoài** pager | ads | `R.md:294`, `M/analytics/PoolCellOpportunity.kt`, `M/compose/NativeAdOnboarding.kt:28-30` |

### 5.10 Rewarded

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| R-01 | Feature mở hộp thoại hỏi xem quảng cáo trước một hành động có điều kiện (MC-05), khi khóa `is_show_<mã vị trí>` của vị trí rewarded bật (ví dụ `is_show_rw_get_item`). Hộp thoại có tiêu đề, nút "Xem quảng cáo" (cao 56 dp, bo 28 dp), nút "Không, cảm ơn", và chỗ để app vẽ nội dung riêng | feature, ads | `M/compose/RewardedAdDialog.kt` |
| R-02 | Thưởng trao ngay trong callback thưởng của SDK, không chờ quảng cáo đóng | ads | `M/manager/RewardedAdManagerImpl.kt:215-276` |
| R-03 | Quảng cáo chưa sẵn sàng thì không có thưởng; hành động theo chính sách của vị trí (MC-05, mặc định chặn). Chỉ báo "kiểm tra kết nối" khi lỗi mạng: máy đang không có mạng, hoặc lượt nạp gần nhất lỗi mạng (mã 2, hay mã bất kỳ lúc máy không có mạng); cờ lỗi mạng xóa ở đầu mỗi lượt nạp (A 03:175-180). App luôn nhận "thất bại" để đóng hộp thoại, không báo lỗi lần nữa | ads | `M/manager/RewardedAdManagerImpl.kt:277-288`, `R.md:226-228` |
| R-04 | Hiện lỗi thì báo thất bại. Đóng mà chưa được thưởng thì không gọi gì | ads | `M/manager/RewardedAdManagerImpl.kt:257-266` |
| R-05 | Rewarded không qua cổng `ads_interval` và không đẩy mốc | ads | `M/AdsKit.kt:426-444` |
| R-06 | Rewarded không kiểm "đang có quảng cáo toàn màn hình khác" và không chặn lần bấm "Xem" thứ hai: lần bấm thứ hai trước khi quảng cáo lên màn vẫn gọi hiện như AdsKit, và ghi cơ hội `busy` (A 03:40 và A 06:216, đã chốt: giữ đúng hành vi này) | ads | A 03:40, A 06:216, A 06:294, `M/manager/RewardedAdManagerImpl.kt:233-242` |

### 5.11 Native ở một bước của luồng (giới thiệu và luồng khác)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| OB-01 | Mỗi luồng có native là một vị trí (MC-06), bật bằng `is_show_<mã vị trí>`; luồng giới thiệu theo AdsKit dùng vị trí `native_onboarding`, khóa `is_show_native_onboarding`. Spec của app chọn bước nào có quảng cáo; `:core:ads` cung cấp thẻ native 250 dp (đặt ngoài pager) và native toàn màn hình không có nút điều khiển | app, ads | `DG:181`, `M/compose/NativeAdOnboarding.kt`, `M/compose/NativeAdFullscreen.kt` |
| OB-02 | Hai trang kề nhau không dùng chung một quảng cáo: mỗi ô lấy một quảng cáo riêng từ pool | ads | commit `bb15e59` |

## 6. Chặn quảng cáo tự hiện (hợp đồng với vỏ app)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| G-01 | Chặn dùng **bộ đếm**, không dùng cờ đúng sai: mỗi nguồn nhả không xóa chặn của nguồn khác; bộ đếm không xuống âm | ads | `M/compose/ForegroundAdSuppressor.kt` |
| G-02 | Bật chặn thì báo xuống SDK **ngay** (Activity có thể dừng trước khung hình sau); nhả chặn thì để vỏ app hạ ở lần dựng lại sau | ads | như trên |
| G-03 | Ba cách dùng. Mở màn chọn ảnh, camera, hộp thoại quyền: bật ngay trước khi mở, nhả ở dòng đầu của callback, có lưới an toàn khi composable rời màn. Màn chiếm toàn màn hình (native sau inter, trang giới thiệu có native toàn màn): chặn theo trang đang hiện. Rời app có chủ đích (chia sẻ, mở link, mở Play): đánh dấu sau khi mở thành công, nhả khi Activity chính hiện lại | feature, ads | `M/compose/ForegroundAdSuppressor.kt`, `R.md:350-381` |
| G-04 | **Hợp đồng của vỏ app:** `MainActivity` nhả dấu "rời app" ở mỗi lần hiện lại; giao diện gốc đọc trạng thái chặn **trong composition** và báo xuống `:core:ads`. Thiếu bước này thì sau một form UMP hay một lần bấm quảng cáo, quảng cáo khi quay lại app tắt tới hết process | app | `R.md:350-374` |
| G-05 | Rời app mà lệnh mở hụt (app không xuống nền): dấu tự nhả sau đúng một lượt | ads | `T/compose/LeavesAppReleaseSignalTest.kt` |
| G-06 | Mỗi lần Activity chính hiện lại, việc gỡ cờ kẹt (F-03) chạy **trước** việc hỏi lại đồng ý (S-09). Thứ tự các việc còn lại (nhả dấu rời app, nạp bù native) do `:app` chọn | app, ads | A 04:157-163 |

### 6.1 Việc của vỏ app (`:app`)

Hợp đồng với app chủ của AdsKit (A 07) chia cho `:app` của dự án mẫu. Mỗi app tạo từ dự án mẫu giữ nguyên các việc này.

| # | Việc | Thiếu thì hỏng gì | AdsKit |
| --- | --- | --- | --- |
| HD-01 | Khởi tạo `:core:ads` trong `Application` (hoặc khởi tạo sớm của Hilt), trước mọi màn hình, đúng một lần | Lần lên foreground đầu không được ghi là khởi động mới, resume có thể bung ngay lúc mở app; hồi phục kẹt và theo dõi vòng đời không chạy | A 07:11-13 |
| HD-02 | Đặt kế hoạch nạp sẵn **trước** khi bước đồng ý có kết quả. Dự án mẫu: `:core:ads` có kế hoạch mặc định suy ra từ danh sách vị trí (app open, rồi khe của bước đầu luồng nếu có, rồi các khe còn lại); `:app` đặt lại kế hoạch trong `Application` nếu cần | Mọi khe nạp cùng lúc, quảng cáo người dùng thấy đầu tiên về muộn tới 6 giây; khe không có trong kế hoạch không bao giờ được nạp sẵn hay nạp bù | A 07:31-33 |
| HD-03 | Gọi hoàn tất cưỡng bức inter khi Activity đã giao cho inter bị hủy mà inter chưa báo đóng (N-04) | Hành động của người dùng (sang màn sau, lưu) không bao giờ chạy | A 07:86-88 |
| HD-04 | Đưa giá trị công tắc nhánh rút gọn (bản đã lưu hoặc mặc định) vào `:core:ads` **trước** bước hỏi đồng ý của mỗi lần mở app, và đưa lại khi Remote Config đổi | Lượt hỏi đầu dùng sai luật chốt (S-06, S-12) | A 07:41-44 |
| HD-05 | Đặt một lần ở gốc giao diện bộ chứa lớp phủ của `:core:ads` (cùng chỗ cấp `LocalAdsController`). Màn native sau inter là lớp phủ do `:core:ads` vẽ, không phải đích trong `AppNavHost`: luồng inter của feature và luồng resume (T-025) đều dùng được mà không cần route của `:app` (câu hỏi 31 của spec ads) | Combo và resume nối combo không hiện được | A 07:79-84 |
| HD-06 | Màn loading dò app open bằng "nạp nếu cần"; lượt dò không phải cơ hội và không đo; app tự bỏ app open thì tự ghi một cơ hội `not_ready` (A-04, A-06) | Mỗi nhịp dò thành một cơ hội bị chặn ảo, báo cáo bị nhấn chìm | A 07:75-77, A 07:94-97 |
| HD-07 | Báo nhóm thu hút người dùng (UA) **trước khi phát hành** bản đầu tiên gửi `ad_placement` sang AppsFlyer hay gửi sự kiện đếm inter; ghi ngày đổi lên dashboard | Số liệu phân bổ trên dashboard đổi đột ngột mà không ai biết lý do | A 07:123-131 |

## 7. Vị trí

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| PL-01 | Feature luôn truyền vị trí khai báo trong `AdPlacement`; không có vị trí rỗng | feature | `R.md:160` |
| PL-02 | Vị trí nội bộ của `:core:ads`: `preload` (lượt nạp chưa gắn màn), `pool` (lượt nạp của pool), `resume`, **thêm 2026-10-26:** `resume_inter`, `native_combo` | ads | `M/analytics/AdTelemetry.kt:181-190` |
| PL-03 | Vị trí gắn với lượt **hiện**, ghi đè ở mọi lượt hiện. Giữ giá trị cũ thì doanh thu của quảng cáo sau bị tính cho màn trước | ads | `M/manager/InterstitialAdManagerImpl.kt:70-76`, `T/manager/InterstitialTelemetryTest.kt:233` |
| PL-04 | Cổng và lượt hiện của cùng một thao tác ra cùng một vị trí. Lượt chỉ để dò trạng thái (màn loading hỏi lặp) ra "không đo". Hàm đổi nguồn ra vị trí ném lỗi thì coi như không có vị trí, luồng quảng cáo chạy tiếp | ads | `M/AdsKit.kt:1251-1256`, commit `435c390` |

## 8. Ghi số liệu

### 8.1 Doanh thu theo lượt hiện

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| T-01 | Mọi định dạng nghe sự kiện doanh thu (paid event) của SDK, gắn lúc nạp. Mỗi lượt ghi: định dạng, lớp adapter, mã đơn vị, tiền tệ, giá trị, mạng đã trả quảng cáo, vị trí (đọc lúc sự kiện về) | ads, analytics | `M/manager/InterstitialAdManagerImpl.kt:146-158`, `M/compose/BannerAdFixed.kt:105` |
| T-02 | Native có dự phòng: sau khi native hiện 2 giây mà chưa có sự kiện doanh thu thì vẫn ghi một lượt với `USD`, giá trị 0. Sự kiện doanh thu về trước thì hủy dự phòng. Mỗi quảng cáo chỉ ghi một lần. Banner không có dự phòng | ads | `M/manager/NativeAdManagerImpl.kt:221-245,316`, `M/manager/NativeAdPoolManager.kt:249-273` |
| T-03 | Giá trị bằng micro chia 1 000 000. Không biết mạng thì ghi `admob` | ads | `M/analytics/AdRevenueMapping.kt:4,17-18` |
| T-04 | Mã định dạng: `INTER`, `REWARDED`, `APP_OPEN`, `BANNER`, `NATIVE_FULL`, `NATIVE_COLLAP`, `NATIVE_SMALL`, `NATIVE_ONBOARDING`, `NATIVE_CONTENT`, `NATIVE_MEDIUM` | ads | `M/analytics/AdAnalyticsTracker.kt:4-15` |

### 8.2 Firebase

AdMob đã liên kết Firebase thì SDK tự ghi `ad_impression`. Ta ghi doanh thu theo vị trí bằng tên riêng **`ad_impression_placement`** để không đếm đôi (`M/analytics/FirebaseAdEventConfig.kt:23-33`, `R.md:392`).

| Sự kiện | Tham số | Khi nào gửi | AdsKit |
| --- | --- | --- | --- |
| `ad_impression_placement` | `ad_format`, `ad_platform` (`admob`), `ad_source` (tên mạng), `ad_unit_name` (mã đơn vị), `currency`, `value` (số thực), `ad_placement` | Mọi lượt hiện, luôn gửi, không phụ thuộc công tắc số liệu vòng đời hay trạng thái đồng ý (Analytics tự đọc chuỗi đồng ý TCF, ADR-0013) | `M/analytics/FirebaseAdTracker.kt:73-95` |
| `ad_load_success`, `ad_load_fail` | `ad_format`, `ad_placement`, `load_attempt`, `latency_ms`; khi lỗi thêm `load_error_code` (chuỗi), `load_state` | Nhóm của định dạng bật **và** cổng đồng ý mở | `M/analytics/FirebaseAdTracker.kt:97-129` |
| `ad_opportunity` | `ad_format`, `ad_placement`, `opportunity_result` (`shown`, `not_ready`, `throttled`, `busy`, `suppressed`), `load_state` (`ready`, `loading`, `idle`, `no_fill`, `network`, `sdk_throttled`, `invalid`, `internal`, `unknown`) | Như trên | `M/analytics/FirebaseAdTracker.kt:130-135`, `M/analytics/AdTelemetry.kt:74-100` |
| `ad_show_fail` | `ad_format`, `ad_placement`, `show_error_code`, `show_fail_reason` (`internal`, `ad_reused`, `not_ready`, `app_not_foreground`, `mediation_show_error`, `unknown`) | Như trên | `M/analytics/FirebaseAdTracker.kt:136-141` |
| `ad_clicked` | `ad_format`, `ad_placement` | Như trên | `M/analytics/FirebaseAdTracker.kt:142-145` |
| `ad_reward_earned` | `ad_placement` | Nhóm toàn màn hình | `M/analytics/FirebaseAdTracker.kt:146` |
| `reward_dialog` | `ad_placement`, `dialog_action` (`show`, `dismiss`) | Nhóm toàn màn hình | `M/analytics/FirebaseAdTracker.kt:147-150` |

Luật đi kèm:

- Nhóm **toàn màn hình**: `INTER`, `REWARDED`, `APP_OPEN`, `NATIVE_FULL`. Các định dạng khác thuộc nhóm **khe**. Mỗi nhóm có một khóa bật tắt số liệu vòng đời, mặc định tắt (mục 10) (`M/analytics/AdTelemetry.kt:165-168`, `M/analytics/FirebaseAdEventConfig.kt:13-21`).
- `load_attempt` chỉ có ở khe có bộ đếm lỗi liên tiếp (inter, app open, rewarded, native một quảng cáo, thu gọn, banner): thành công sau 2 lỗi thì là 3. Pool và lượt làm mới native nhỏ không có (`M/compose/NativeAdSmall.kt:194,230`).
- `latency_ms` tính từ lúc gửi chính yêu cầu đó tới callback đầu tiên của nó; lượt bị chặn ở cổng không có; kết quả không do `:core:ads` gửi (lượt tự làm mới của banner) không có (A 06:157). Tham số không có giá trị thì bỏ hẳn, không gửi rỗng.
- Firebase: vị trí rỗng thì **bỏ** tham số `ad_placement`, không gửi `""` (A 06:87).
- **Vị trí của lượt nạp:** inter, app open, rewarded luôn là `preload`; native một quảng cáo và thu gọn là vị trí của màn đang dùng khe, chưa có thì `preload`; pool luôn là `pool`; banner là vị trí của màn đang hiện lúc có kết quả, chưa có thì bỏ tham số; vòng làm mới của native nhỏ là vị trí hiện tại (A 06:174-179).
- `load_state` của cơ hội chụp lúc quyết định, trước mọi lượt nạp do chính nhánh đó kích: có quảng cáo thì `ready`, đang nạp thì `loading`, có lỗi gần nhất thì nhãn lỗi đó, còn lại `idle`. Định dạng không có khe một quảng cáo riêng thì `idle`. `shown` của native sau inter và ô pool luôn là `ready` (A 06:203).
- Không có vị trí thì không phải cơ hội: lượt hiện không có vị trí vẫn hiện quảng cáo nhưng không ghi `ad_opportunity` (A 06:197).
- "Nhóm bật" nghĩa là: khóa của nhóm bật **và** đang được phục vụ quảng cáo (đồng ý cho phép, SDK đã khởi tạo), đọc lại ở mỗi sự kiện (A 06:135-139).
- `load_state` sau lỗi: offline thì luôn là `network`; mã 3 và 9 là `no_fill`; mã 1 là `sdk_throttled` nếu SDK báo tự chặn, không thì `invalid`; mã 8, 10, 11 là `invalid`; mã 0 là `internal`. Nhãn tính sau khi đã hẹn thử lại; trạng thái nạp chụp **trước** mọi nhánh làm đổi trạng thái (`M/analytics/AdTelemetry.kt:114-121`, commit `340f9bd`, `570769f`).
- **Khi nào ghi `ad_opportunity`:** cổng inter chặn một vị trí (xét `busy`, rồi `not_ready`, rồi `throttled`; không ghi khi cổng đồng ý đóng); mỗi lượt gọi hiện đúng một kết quả; resume bị chặn (vị trí `resume`; lần đầu của process và resume tắt **không** phải cơ hội; resume không bao giờ ra `throttled` vì `ads_interval`, xem AS-01); rewarded bấm lần hai khi quảng cáo chưa đóng là `busy` (R-06); ô pool và native sau inter theo P-07, K-07 (`M/AdsKit.kt:1263-1271`, `M/controller/AppOpenAdPolicy.kt:39-55`, commit `3e934be`).
- Không gửi thông báo lỗi của SDK lên số liệu (có thể chứa mã đơn vị). Bộ ghi số liệu ném lỗi thì nuốt, không làm hỏng luồng quảng cáo (`M/analytics/FirebaseAdTracker.kt:112-115`, `M/analytics/AdTelemetry.kt:198-205`).

### 8.3 AppsFlyer (ADR-0027)

| Sự kiện | Tham số | Mặc định | AdsKit |
| --- | --- | --- | --- |
| `af_ad_revenue` | `monetization_network` (rỗng khi SDK không báo, khác Firebase ghi `admob`), `mediation_network` (`admob`), `af_currency`, `af_revenue` (số thực), `country` (theo SIM, rồi mạng, rồi ngôn ngữ máy, viết hoa), `ad_placement` (chuỗi rỗng khi chưa có, khác Firebase), `ad_unit`, `ad_type` | Bật. Gửi cho **mọi** lượt hiện, kể cả lượt dự phòng giá trị 0 | `M/analytics/AppsFlyerAdTracker.kt:128-160` |
| `af_revenue` | | **Không bao giờ gửi**: gửi cùng `af_ad_revenue` thì AppsFlyer cộng doanh thu hai lần | `M/analytics/AppsFlyerEventConfig.kt:20-24`, commit `aeaf575` |
| `af_inters_ad_eligible` | Không có | Bật. Gửi khi qua cổng, ngay trước khi hiện | `M/AdsKit.kt:1230` |
| `af_inters_api_called` | `ad_format` (`INTER`), `ad_unit_id`, `ad_source` | Bật. Gửi khi nạp thành công | `M/manager/InterstitialAdManagerImpl.kt:142` |
| `af_inters_displayed` | Như trên | Bật. Gửi khi quảng cáo hiện | `M/analytics/AppsFlyerEventConfig.kt:50-53` |
| `af_rewarded_ad_eligible`, `af_rewarded_api_called`, `af_rewarded_ad_displayed` | Như trên, `REWARDED` | Bật. Eligible gửi khi người dùng bấm xem | `M/analytics/AppsFlyerEventConfig.kt:71-90` |
| `af_native_ad_eligible`, `af_native_api_called`, `af_native_ad_displayed` | `ad_format` theo biến thể native | Bật. Eligible gửi khi khe native vào màn, không tính nạp sẵn hay vẽ lại; displayed khi native ghi lượt hiển thị đầu | `M/analytics/AppsFlyerEventConfig.kt:92-111` |
| `af_inters_displayed_{N}_times`, N từ 1 tới 20 | Không có | Bật. Gửi khi inter thứ N từ lúc cài hiện (N-05) | `M/analytics/AppsFlyerAdTracker.kt:117-121`, `M/analytics/AdRevenueMapping.kt:10-11,27` |

Luật đi kèm: **mỗi sự kiện có công tắc từ xa riêng** (khóa `is_log_af_*`, mục 10, mặc định bật như AdsKit), đổi giữa phiên có hiệu lực từ sự kiện kế tiếp, dùng để tắt khẩn một sự kiện làm hỏng số liệu (A 06:34-41); `af_revenue` không có công tắc vì không bao giờ gửi; gửi bằng hàm ghi sự kiện thường của AppsFlyer, không dùng hàm ghi doanh thu quảng cáo riêng, vì công cụ kiểm SDK của đối tác chỉ nhận sự kiện trong app (commit `0088661`); số liệu vòng đời của Firebase không chuyển sang AppsFlyer (`T/analytics/AppsFlyerAdTrackerTest.kt:301`); vị trí trong doanh thu AppsFlyer không phụ thuộc công tắc của Firebase (`R.md:395`).

## 9. Mediation và khe quảng cáo

| # | Luật | AdsKit |
| --- | --- | --- |
| M-01 | AdMob điều phối. Chín mạng con qua adapter chính thức của AdMob: AppLovin, Meta Audience Network, DT Exchange (Fyber), InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads (kèm SDK Unity Ads) | `build.gradle.kts:41-50`, `DG:75` |
| M-02 | Phiên bản adapter: AdsKit không ghi; ta chọn bản mới nhất ổn định lúc làm plan, ghi trong `gradle/libs.versions.toml` | `build.gradle.kts:41-50` |
| M-03 | Khóa SDK AppLovin là cấu hình của từng app, nhập trong ánh xạ nguồn quảng cáo trên AdMob, app không mang khóa; không nằm trong resource của module hay trong repo. Adapter `13.6.4.2` chỉ đọc khóa từ cấu hình mediation AdMob gửi xuống (`MediationConfiguration.getServerParameters().getString("sdkKey")`, đọc bytecode ở plan T-034), không đọc khóa trong app. AdsKit để khóa trong resource của thư viện: không làm theo | `src/main/res/values/strings.xml:9` (không chép giá trị) |
| M-04 | UMP khai báo riêng, không đi kèm `play-services-ads` | `build.gradle.kts:34-38` |
| M-05 | Khe quảng cáo (một mã đơn vị mỗi khe), theo tên của bản đặc tả (A 01:9-22) để mỗi tên một nghĩa: `app_open`, `interstitial`, `rewarded`, `banner` (tự làm mới; mã nguồn AdsKit gọi `bannerPartner`), `banner_collapsible` (nạp tay; mã nguồn gọi `banner`), `native_small` (A: `nativeBanner`), `native_collap`, `native_fullscreen`, `native_full`, `native_step` (A: `nativeOnboarding`), `native_content`, `native_medium`. Vị trí dùng khe theo định dạng (MC-02); mã theo khe hay theo vị trí là câu hỏi 14 của spec ads | A 01:9-24, `M/AdUnitIds.kt:3-25` |
| M-06 | Mã thử của Google cho staging theo định dạng (inter, banner, rewarded, app open, native) | `DG:127-133` |
| M-07 | Quy tắc ProGuard của từng adapter theo tài liệu của mạng đó; AdsKit để file rỗng dù tài liệu ghi có, nên ta kiểm bằng build release của staging | `consumer-rules.pro`, `DG:189-208` |

## 10. Khóa Remote Config

Tên khóa giống hệt AdsKit cho khái niệm chung, kể cả lỗi chính tả `show_resum_ads`. Khóa mới đặt theo cùng kiểu (ADR-0026 thay mục 1 của ADR-0023). Mọi khóa vẫn khai báo một chỗ, có kiểu, mặc định an toàn và mô tả, và vào bảng khóa sinh tự động (ADR-0023, các mục còn lại).

Cột "Nguồn tên": **AdsKit** là tên có trong bảng khóa chính thức của AdsKit (`DG:171-181`); **tên do ta đặt** là tên AdsKit không có (bản đặc tả độc lập chỉ ghi tên trường, không ghi tên khóa), ta đặt theo kiểu của AdsKit; **mẫu** là khóa suy ra theo mô hình chung (MC-03, MC-04).

### 10.1 Khóa chung của cả app

| Khóa | Kiểu | Mặc định | Nghĩa | Ai đọc | Nguồn tên | AdsKit |
| --- | --- | --- | --- | --- | --- | --- |
| `show_open_ads` | boolean | false | App open lúc mở app, từ lần mở thứ hai | app | AdsKit | `M/AdFeatureFlags.kt:4`, `DG:171` |
| `show_open_ads_first_open` | boolean | false | App open ở lần mở đầu sau khi cài | app | AdsKit | `M/AdFeatureFlags.kt:5`, `DG:172` |
| `open_ad_type` | string | `aoa` | `aoa` hay `inter` lúc mở app | app | AdsKit | `M/AdFeatureFlags.kt:6`, `DG:173` |
| `show_resum_ads` | boolean | false | Quảng cáo khi quay lại app | ads | AdsKit | `M/AdFeatureFlags.kt:7`, `DG:174` |
| `resume_ad_type` | string | `aoa` | Chỉ đúng `inter` mới là interstitial; giá trị khác là app open | ads | AdsKit | `M/AdFeatureFlags.kt:8`, `DG:175` |
| `ads_interval` | long (giây) | 45 | Khoảng tối thiểu từ lúc inter gần nhất của nhóm mặc định rời màn tới inter sau | ads | AdsKit | `M/AdFeatureFlags.kt:10`, `DG:176` |
| `is_show_native_after_inter` | boolean | false | Combo inter rồi native toàn màn hình | ads | AdsKit | `M/AdFeatureFlags.kt:11`, `DG:177` |
| `is_show_native_bottom` | boolean | false | Khe đáy của mọi thanh đáy: native nhỏ (true) hay banner (false) | ads | AdsKit | `M/AdFeatureFlags.kt:12`, `DG:178` |
| `is_show_native_collap` | boolean | true | Lớp native thu gọn trên thanh đáy khi khe đáy là native (D-01). Hiệu lực = `is_show_native_bottom` và khóa này | ads | tên do ta đặt | A 05:56 |
| `time_expand_collap_ad` | long (giây) | 30 | Bung lại native thu gọn sau khi người dùng thu gọn; 0 là không bung lại | ads | tên do ta đặt (suy từ trường `timeExpandCollapAd`) | `M/AdFeatureFlags.kt:16-23`, A 01:42 |
| `is_enable_consent_shortcut` | boolean | **false** | Công tắc từ xa của nhánh rút gọn `CACHED_EARLY`, `ERROR_ALLOWED` (S-12). Cần thêm cấu hình lúc build của `:app`, mặc định cũng tắt. Tắt thì thu hồi ngay `ERROR_ALLOWED` | ads | tên do ta đặt | A 02:24-26, `M/AdsKit.kt:876-879` |
| `is_log_ad_fullscreen_events` | boolean | false | Số liệu vòng đời Firebase của nhóm toàn màn hình (mục 8.2) | ads | tên do ta đặt | `M/analytics/FirebaseAdEventConfig.kt:13-21` |
| `is_log_ad_slot_events` | boolean | false | Số liệu vòng đời Firebase của nhóm khe | ads | tên do ta đặt | như trên |
| `is_log_af_ad_revenue` | boolean | true | Gửi `af_ad_revenue` (mục 8.3) | analytics | tên do ta đặt | A 06:37 |
| `is_log_af_inters_ad_eligible`, `is_log_af_inters_api_called`, `is_log_af_inters_displayed` | boolean | true | Công tắc từng sự kiện phễu inter | analytics | tên do ta đặt | A 06:34 |
| `is_log_af_rewarded_ad_eligible`, `is_log_af_rewarded_api_called`, `is_log_af_rewarded_ad_displayed` | boolean | true | Công tắc từng sự kiện phễu rewarded | analytics | tên do ta đặt | A 06:35 |
| `is_log_af_native_ad_eligible`, `is_log_af_native_api_called`, `is_log_af_native_ad_displayed` | boolean | true | Công tắc từng sự kiện phễu native | analytics | tên do ta đặt | A 06:36 |
| `is_log_af_inters_displayed_n_times` | boolean | true | Công tắc `af_inters_displayed_{N}_times` | analytics | tên do ta đặt | A 06:39 |

### 10.2 Khóa suy ra theo vị trí và nhóm

| Mẫu khóa | Kiểu | Mặc định | Nghĩa | Ai đọc | Nguồn tên |
| --- | --- | --- | --- | --- | --- |
| `is_show_<mã vị trí>` | boolean | false | Bật một vị trí (MC-03), thay `ads_<vị trí>_enabled` của đợt 2. Ví dụ `is_show_sample_bottom_banner`, `is_show_rw_get_item` (vị trí `rw_get_item`, trùng khóa AdsKit `DG:180`), `is_show_native_onboarding` (vị trí `native_onboarding`, trùng khóa AdsKit `DG:181`) | ads | mẫu; trùng AdsKit khi mã vị trí trùng |
| `ads_interval_<nhóm>` | long (giây) | theo spec | Khoảng cách của nhóm giãn cách riêng (MC-04). Ví dụ `ads_interval_sounds` (nhóm `sounds`, trùng tên AdsKit dùng ở app, `M/AdsKit.kt:124-127`) | ads | mẫu |
| `<tham số>_<mã vị trí>` | theo tham số | theo spec | Tham số riêng của một vị trí, chỉ khi spec cần. Ví dụ `native_every_<mã vị trí>` (chèn mỗi N mục, MC-07), `rw_no_ad_policy_<mã vị trí>` (string `block`, `allow`, `allow_after_<n>`, MC-05). Mẫu tên theo câu hỏi 28 của spec ads | ads | mẫu (tên do ta đặt) |

Khóa của AdsKit **không** khai báo:

- `time_reload_collap_ad` (`DG:179`): có trong bảng khóa của AdsKit nhưng không còn hiệu lực (A 01:40); native làm mới cố định 60 giây (CL-02).
- `resume_ads_interval` và `time_reload_bottom_ad` **không phải** tên khóa của AdsKit: `DG:171-181` không có hai tên này. AdsKit chỉ có trường `resumeAdsInterval` (bộ `CURRENT`, `M/AdFeatureFlags.kt:9`) và `timeReloadBottomAd` (không thành phần nào đọc, A 01:41). PARTNER không có cổng thời gian ở resume (A-11) nên ta không cần cả hai.
- Khóa của ta bị bỏ hay đổi: xem ADR-0026 và spec ads: giữ công tắc tổng, tên `is_show_ads` (câu hỏi 7); bỏ khóa cá nhân hóa ở vùng có luật, chuỗi đồng ý của UMP quyết (câu hỏi 8).

## 11. Hằng số cứng (không đổi từ xa)

| Hằng số | Giá trị | AdsKit |
| --- | --- | --- |
| Chờ đồng ý, SDK, app open ở màn loading; nhịp kiểm | 10 giây, 35 giây, 40 giây; 250 ms | `M/behavior/PartnerAdsConstants.kt:16-25` |
| Hạn dùng app open; khóa sau khi đóng | 1 giờ; 1 giây | `M/behavior/PartnerAdsConstants.kt:30-33` |
| Nạp lại inter, rewarded sau khi đóng | 2 giây | `M/behavior/PartnerAdsConstants.kt:38` |
| Thử lại toàn màn hình, banner | `min(n x 2 giây, 10 giây)`, không giới hạn | `M/behavior/PartnerAdsConstants.kt:42-43` |
| Thử lại native | 2 giây nhân đôi tới 60 giây (n tối đa 10); 5 giây khi chưa có quảng cáo | `M/behavior/PartnerAdsConstants.kt:50-56` |
| Hạn dùng native | 55 phút | `M/behavior/PartnerAdsConstants.kt:48` |
| Làm mới native tại chỗ (đáy, thu gọn) | 60 giây; native đáy trễ 500 ms sau lớp thu gọn | `M/behavior/PartnerAdsConstants.kt:59`, `M/compose/NativeAdSmall.kt:39` |
| Chờ lần nạp đầu và lần làm mới của native thu gọn | 10 giây, kiểm mỗi 200 ms | `M/compose/NativeAdCollap.kt:77,147` |
| Đếm ngược native sau inter; chờ inter rời màn | 3 giây; 2 giây | `M/compose/NativeAfterInterScreen.kt:34,42` |
| Nút đóng native sau inter; nút thu gọn | 36 dp, X 20 dp, cách mép 12 dp; 32 dp, biểu tượng 20 dp, cách mép 4 dp | `M/compose/NativeAfterInterScreen.kt:183-193`, `M/compose/NativeAdCollap.kt:242-244` |
| Chiều cao native thu gọn, thẻ ở bước của luồng, khe đáy (vẽ và chừa chỗ) | 250 dp, 250 dp, 80 dp | `M/compose/NativeAdCollap.kt:221`, `M/compose/NativeAdOnboarding.kt:70`, A 05:68, A 08:44 |
| Nhịp nạp lại tay của banner thu gọn | 30 giây | A 08:40 |
| Số sao tối đa của native | 5 | A 08:46 |
| Cỡ pool ô lưới, giới thiệu, medium | 5, 3, 3 | `M/manager/NativeAdPoolManager.kt:425`, `M/AdsKit.kt:457,482` |
| Nhịp thử lấy của ô pool | 500 ms; 2 giây khi cạn | `M/compose/PooledNativeAd.kt:26-29` |
| Dự phòng lượt hiển thị native | 2 giây, USD 0 | `M/manager/NativeAdManagerImpl.kt:316` |
| Nhóm nạp sẵn: trần, nhịp kiểm | 6 giây, 200 ms | `M/AdsKit.kt:91,98` |
| Gộp đổi mạng | 3 giây | `M/net/AdsNetworkMonitor.kt:91` |
| Trần cờ "đang hiện" | 10 phút | `M/manager/FullscreenShowState.kt:64` |
| Số lần đếm `af_inters_displayed_N_times` | 1 tới 20 | `M/behavior/PartnerAdsConstants.kt:64` |

## 12. Lỗi AdsKit đã gặp: bản của ta phải tránh

Lỗi AdsKit đã sửa (mỗi dòng nên có test tương ứng trong plan):

| # | Lỗi | Bản của ta làm | AdsKit |
| --- | --- | --- | --- |
| L-A01 | Mốc ghi ở hàm hoàn tất chung sau một cờ chặn; lần hết giờ hạ cờ trước nên mốc không bao giờ được ghi | Ghi mốc ở callback thật của SDK (I-05) | `f2dab3d` |
| L-A02 | Hai quảng cáo toàn màn hình chồng nhau, callback của cái đầu bị nuốt | Một cổng chung, bỏ lượt (F-01) | `e3c1551` |
| L-A03 | Callback đóng thất lạc làm cờ kẹt tới hết process; hồi phục hiểu nhầm lần hiện lại ngay sau khi hiện quảng cáo | Trần 10 phút và hồi phục cần "che trước rồi hiện lại" (F-02, F-03) | `0bfd6be`, `47ad489` |
| L-A04 | Quảng cáo resume bung sau vài giây vì cụm inter và native chưa ghi mốc | Mốc ở cuối cụm; ghi mốc khi hạ cờ kẹt (I-02) | `47ad489` |
| L-A05 | Đếm cơ hội resume khi resume đang tắt | Resume tắt không phải cơ hội (mục 8.2) | `3e934be` |
| L-A06 | Dùng chung một quảng cáo native cho nhiều chỗ, mất lượt hiển thị và doanh thu | Mỗi lượt hiện một quảng cáo riêng (O-01) | `51897f2` |
| L-A07 | Vòng làm mới native nhỏ mất bộ ghi doanh thu | Luôn gắn bộ ghi tổng hợp | `51897f2` |
| L-A08 | Pager giới thiệu hiện hai trang kề nhau cùng một quảng cáo | Dùng pool (OB-02) | `bb15e59` |
| L-A09 | Pool không có quảng cáo thì khung chờ hiện mãi | Có trạng thái cạn, ẩn ô, vẫn thử lấy (P-05, P-06) | `a4b33ca` |
| L-A10 | Đồng hồ native sau inter chờ cờ inter không có trần, nút đóng không bao giờ hiện | Chờ tối đa 2 giây (K-02) | `4362a78` |
| L-A11 | Màn native sau inter tự chặn bằng cờ đúng sai, tạo lại Activity thì cờ bị hạ | Chặn bằng bộ đếm, nơi gọi sở hữu (G-01, K-05) | `4362a78`, `c7f90b1` |
| L-A12 | Vòng làm mới native thu gọn xóa thao tác thu gọn của người dùng | Đọc trạng thái hiện tại; bung lại có hẹn giờ riêng (CL-03, CL-04) | `70d52db`, `28f6941` |
| L-A13 | Lớp nổi lấp cả màn, đẩy thanh điều hướng ra ngoài, chạm rơi vào quảng cáo | Chỉ lấp chiều rộng (D-03) | `b4781fa` |
| L-A14 | Đồng ý chỉ chặn ở nạp sẵn và hiện; các vòng tự nạp vẫn chạy sau khi rút; banner nạp trước khi người dùng trả lời form | Cổng ở đầu mọi lượt nạp (S-15, B-01) | `0a5c65b` |
| L-A15 | Chốt đồng ý khi form lỗi chưa trả lời; hai form chồng nhau | Chỉ chốt kết quả sạch; gộp lượt hỏi (S-06, S-07) | `34e2fc8` |
| L-A16 | Không thử lại, hoặc thử lại cố định 30 giây; bật chế độ máy bay lúc mở app thì không bao giờ nạp lại | Thử lại theo luật; nạp lại khi mạng về (F-05, S-24) | `8022d95` |
| L-A17 | Bộ đếm lỗi cộng dồn suốt process, lần thứ 6 bỏ cuộc vĩnh viễn | Bộ đếm về 0 khi thành công (F-05, O-05) | `46b0cdd` |
| L-A18 | 15 yêu cầu gửi cùng lúc, quảng cáo người dùng thấy đầu tiên về muộn nhất | Kế hoạch nạp sẵn theo nhóm, một nguồn thứ tự (S-22) | `dce73a0` |
| L-A19 | Làm mới native vẫn chạy ở nền | Làm mới tại chỗ chỉ ở foreground (CL-02) | `c881b26` |
| L-A20 | Gửi cả `af_ad_revenue` và `af_revenue`, doanh thu nhân đôi; dùng hàm doanh thu riêng thì công cụ kiểm không nhận | Chỉ `af_ad_revenue`, bằng hàm ghi sự kiện thường (mục 8.3) | `aeaf575`, `0088661` |
| L-A21 | Hàm đổi nguồn ra vị trí ném lỗi làm vỡ luồng inter | Bọc lại, lỗi thì coi như không có vị trí (PL-04) | `435c390` |
| L-A22 | Vị trí trong doanh thu banner và native đáy đứng yên ở màn đầu | Đọc vị trí lúc có sự kiện (D-05) | `b4781fa` |
| L-A23 | Ảnh native bị cắt vì đặt cách co giãn sau khi gắn quảng cáo | Đặt trước (O-07) | `f3947af` |
| L-A24 | Chữ native theo theme tối của máy, không đọc được | Màu chữ theo token của nền quảng cáo, không theo theme máy | `9994dbc`, `94dbec5` |

Rủi ro còn trong AdsKit (chưa có test hay commit sửa), bản của ta làm khác:

| # | Rủi ro trong AdsKit | Bản của ta làm | AdsKit |
| --- | --- | --- | --- |
| AR-01 | Mốc `ads_interval` đo bằng giờ hệ thống; người dùng chỉnh giờ lùi thì inter có thể bị chặn rất lâu | Đo bằng đồng hồ đơn điệu của máy (thời gian từ lúc khởi động). Mốc chỉ sống trong process (I-03) nên đồng hồ này đủ dùng; khoảng âm coi như đã qua cổng | `M/controller/InterstitialThrottleDecision.kt:23-25` |
| AR-02 | Khoảng chừa dưới nội dung tính native đáy cao 80 dp, trong khi khe vẽ cao 56 dp | Theo bản đặc tả (đã chốt): 80 dp cho cả khe và khoảng chừa, một hằng số; test so hai giá trị (D-02, D-04) | `M/compose/BottomAdDecision.kt:31`, `M/compose/BottomAdSlot.kt:48` |
| AR-03 | AdsKit đã dời lượt thử lại của banner tới lần hiện lại; chỉ vòng nạp lại tay, chạy khi thiếu đơn vị tự làm mới, chưa kiểm foreground | Mọi lượt nạp lại, thử lại của banner chỉ ở foreground (B-05) | `M/compose/BannerAdFixed.kt:61-73,130-131,143-147` |
| AR-04 | Khóa SDK AppLovin nằm trong resource của thư viện, có trong repo | Khóa là cấu hình của app, nhập trong ánh xạ nguồn quảng cáo trên AdMob, app không mang khóa, không vào repo; `scripts/secret-scan.py` chặn (M-03) | `src/main/res/values/strings.xml:9` |
| AR-06 | Tài liệu ghi có quy tắc ProGuard nhưng file rỗng | Kiểm bằng build release của staging (M-07) | `consumer-rules.pro` |

Rewarded bấm hai lần (gọi SDK hiện hai lần, `M/manager/RewardedAdManagerImpl.kt:233-242`) **không** còn là rủi ro ta sửa: bản đặc tả đã chốt giữ hành vi này, chỉ ghi cơ hội `busy` (R-06).

Chỗ bản đặc tả độc lập tự mâu thuẫn, ta **không** theo:

| # | Bản đặc tả nói | Ta làm | Dẫn chứng |
| --- | --- | --- | --- |
| AS-01 | Bảng kết quả của resume ghi `throttled` gồm cả "cửa `ads_interval`" | Resume không có cổng `ads_interval` nên không bao giờ ra `throttled` vì lý do đó; `throttled` của resume chỉ là khóa 1 giây sau app open (A-08). Đây là vế sót từ bộ `CURRENT`; chính bản đặc tả nói resume PARTNER không có cổng thời gian | A 06:224 ngược A 04:37-41; `T/controller/ResumeAdPartnerRulesTest.kt:13` |

Chỗ ta lệch có chủ đích khỏi bản đặc tả (đã ghi ở ADR-0026, ADR-0027): luôn ghi doanh thu bằng `ad_impression_placement` (A để mặc định `ad_impression`, A 06:32); không bao giờ gửi `af_revenue` (A cho bật tùy chọn, A 06:38); mốc thời gian đo bằng đồng hồ đơn điệu (AR-01).

## 13. Ghi chú cho người viết plan

- Mọi luật thuần (mục 1 tới 7) tách thành hàm hoặc lớp thuần, có unit test trên JVM với đồng hồ giả, như AdsKit làm.
- Gom mọi cổng (đồng ý, chồng nhau, mốc, chặn) vào một chỗ trong `:core:ads`; feature và vỏ app không gọi SDK.
- Feature chỉ dùng composable và hàm của `:core:ads` với một vị trí trong `AdPlacement` (CLAUDE.md, bước 7).
- Không mang API chỉ để giữ chữ ký cũ của AdsKit; ta không phải thư viện dùng chung giữa nhiều repo.
- Theo mô hình chung (MC-01 tới MC-10): thuộc tính của vị trí đọc từ `AdPlacement`, khóa suy ra theo mẫu; không đặt tên hàm, cờ hay composable theo tính năng của một app (onboarding, export, sounds, gallery).
- Kịch bản nghiệm thu `AT-*` của bản đặc tả độc lập là danh sách test gợi ý cho từng ticket (ghi trong phạm vi ticket).
- Ghi log bằng `AppLogger` (ADR-0019) ở mỗi quyết định cổng, đường đồng ý, kết quả nạp và hiện; không ghi mã đơn vị.
