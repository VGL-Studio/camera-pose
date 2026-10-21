---
id: ADR-0013
title: Quảng cáo AdMob chỉ khởi động sau hộp thoại đồng ý UMP, mã quảng cáo production đưa vào lúc build phát hành
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển]
---

# ADR-0013: QUẢNG CÁO ADMOB CHỈ KHỞI ĐỘNG SAU HỘP THOẠI ĐỒNG Ý UMP, MÃ QUẢNG CÁO PRODUCTION ĐƯA VÀO LÚC BUILD PHÁT HÀNH

## Bối cảnh

ADR-0004 chọn AdMob và đặt ra các yêu cầu: xin đồng ý trước khi tải quảng cáo, rào chắn chính sách nằm trong `:core:ads`, staging chỉ dùng mã thử, quảng cáo cá nhân hóa ở châu Âu bật tắt qua Remote Config. Trước quyết định này, `AdSlot` chưa hiển thị gì. ADR này ghi cách làm cụ thể.

## Quyết định

**Thư viện:** Google Mobile Ads SDK (`play-services-ads`) và User Messaging Platform (UMP), chỉ dùng trong `:core:ads`. Adapter của AppLovin và Unity thêm khi bật mediation thật.

**Sự đồng ý:**

- `MainActivity` gọi `AdsController.gatherConsent` mỗi lần mở app. UMP tự quyết định có cần hiện hộp thoại hay không.
- SDK quảng cáo chỉ khởi động khi UMP cho phép tải quảng cáo (`canRequestAds`). Người dùng đã đồng ý ở lần mở trước thì quảng cáo khởi động ngay, không chờ cập nhật.
- Nơi luật yêu cầu, `PrivacyOptionsButton` hiện nút mở lại hộp thoại. Mỗi app đặt nút này trong màn hình cài đặt; app mẫu đặt ở màn hình mẫu.
- UMP ghi chuỗi đồng ý theo chuẩn TCF. Firebase Analytics mặc định đọc chuỗi này (tắt bằng `google_analytics_tcf_data_enabled`), nên không cần code riêng để nối Analytics với sự đồng ý.

**Feature dùng quảng cáo qua ba hàm composable**, không bao giờ gọi SDK:

| Hàm | Dùng cho |
| --- | --- |
| `AdSlot(placement)` | Banner trong màn hình |
| `rememberShowInterstitial(placement)` | Quảng cáo toàn màn hình ở điểm dừng tự nhiên |
| `PrivacyOptionsButton()` | Nút mở lại hộp thoại đồng ý |

`MainActivity` cấp `AdsController` cho giao diện qua `LocalAdsController`. Test và preview không cấp gì, nên ba hàm trên không hiện gì và ảnh screenshot không bao giờ chứa quảng cáo.

**Tham số qua Remote Config** (khai báo trong `AdsSettings`):

| Khóa | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `ads_<vị trí>_enabled` | bật | Tắt nhanh một vị trí |
| `ads_personalized_regulated_regions_enabled` | tắt | Quảng cáo cá nhân hóa ở vùng có luật bắt buộc đồng ý |
| `ads_interstitial_min_interval_seconds` | 120 | Khoảng cách tối thiểu giữa hai quảng cáo toàn màn hình |

Vùng có luật bắt buộc đồng ý được nhận biết qua UMP: nơi UMP yêu cầu nút cài đặt quyền riêng tư. Ở đó, khi khóa thứ hai đang tắt, mọi yêu cầu quảng cáo đều xin quảng cáo không cá nhân hóa (`npa=1`).

**Rào chắn cho quảng cáo toàn màn hình** (`FullScreenAdGuard`):

- Không quá một quảng cáo trong khoảng `ads_interstitial_min_interval_seconds`.
- Không hiện khi người dùng đang chạm màn hình: chờ tới khi người dùng ngừng chạm 1 giây, chờ tối đa 3 giây rồi bỏ qua. Nhờ vậy cú chạm dành cho app không rơi vào quảng cáo.

**Mã quảng cáo:**

| | Staging | Production |
| --- | --- | --- |
| Mã ứng dụng AdMob | Mã thử của Google, trong `app/build.gradle.kts` | Tham số `-PadmobAppId` |
| Mã đơn vị quảng cáo | Mã thử của Google theo định dạng (`GoogleTestAdUnitIds`) | Tham số `-PadUnits="<vị trí>=<mã>;<vị trí>=<mã>"` |
| Hộp thoại đồng ý | Coi mọi máy là máy test ở châu Âu | Bình thường |

- Thiếu `-PadmobAppId` hoặc `-PadUnits` thì build production hỏng ngay ở bước đầu, với thông báo rõ ràng. Build staging và đồng bộ trong Android Studio không cần hai tham số này.
- `scripts/release.sh` (phần 5 của kế hoạch hoàn thiện) lấy hai giá trị từ trình quản lý mật khẩu. Script phải kiểm tra mọi vị trí đều có mã: một vị trí thiếu mã thì không hiện quảng cáo mà không báo lỗi khi build (`ConfiguredAdUnitIds.missing`).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| `AdsController` cấp qua `CompositionLocal` | Feature chỉ gọi một hàm composable; test và preview tự động không có quảng cáo | Màn hình nào nằm ngoài `MainActivity` phải tự cấp |
| Mỗi feature nhận `AdsController` qua ViewModel | Rõ phụ thuộc | ViewModel không có `Activity` để hiện quảng cáo; mọi test phải có bản giả |
| Mã production trong file `.properties` không commit | Build tay tiện hơn | Dễ lỡ commit; trái ADR-0009 |

## Hệ quả

- **Tích cực:** quảng cáo không bao giờ tải trước khi có sự đồng ý; người phụ trách sản phẩm tắt vị trí, đổi tần suất, bật cá nhân hóa mà không cần phát hành lại; bản staging thử được hộp thoại châu Âu ở bất kỳ đâu.
- **Phải chấp nhận:**
  - SDK quảng cáo thêm các quyền `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS` và, qua WorkManager, `FOREGROUND_SERVICE`. Khai báo Data safety phải khớp.
  - Bản staging luôn hiện hộp thoại châu Âu ở lần mở đầu, kể cả với người test ở Việt Nam.
  - App mẫu chưa có vị trí quảng cáo toàn màn hình. Phần này đã được thử trên emulator bằng một vị trí tạm (hiện sau khoảng 1 giây, lần thứ hai trong 120 giây bị chặn), rồi gỡ vị trí tạm.
- **Việc AI phải tuân thủ:** feature chỉ dùng `AdSlot`, `rememberShowInterstitial` và `PrivacyOptionsButton`; không gọi `AdsController` hay SDK quảng cáo từ feature; không viết mã quảng cáo thật vào code, tài liệu hay tham số build trong repo; gọi quảng cáo toàn màn hình chỉ ở điểm dừng tự nhiên, không bao giờ lúc mở hay đóng app.
