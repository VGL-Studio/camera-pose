# HƯỚNG DẪN REMOTE CONFIG

Tài liệu này dành cho dev, AI và người vận hành. Nó nói cách đặt tên, khai báo, thử và đổi các giá trị cấu hình từ xa qua Firebase Remote Config. Quyết định nằm ở ADR-0003 (cờ tính năng và tham số kinh doanh), ADR-0012 (nối Firebase), ADR-0014 (giá trị mặc định của quảng cáo), ADR-0018 (chờ cấu hình ở màn hình loading), ADR-0023 (quy ước khóa) và ADR-0026 (khóa quảng cáo theo tên AdsKit).

## 1. Khóa dùng để làm gì

| Loại | Ví dụ | Ai đọc |
| --- | --- | --- |
| Cờ tính năng | `feature_camera_enabled` | `FeatureFlags` (`:core:config`), feature theo dõi bằng `Flow` |
| Quảng cáo | `show_open_ads`, `show_resum_ads`, `ads_interval`, `is_show_native_bottom`, `is_show_inter_save`, `ads_interval_sounds` | `:core:ads`, vỏ app (app open lúc mở app, luồng giới thiệu) |
| Tham số của module | `assets_catalog_path`, `review_min_saved_photos_count` | Module core sở hữu tham số, qua `RemoteSettings` |

Feature không đọc Remote Config trực tiếp (ADR-0003); nó đọc cờ qua `FeatureFlags`, còn tham số đi qua module core sở hữu.

## 2. Đặt tên và kiểu

- Chữ thường, nối bằng gạch dưới.
- Cờ tính năng: `feature_<ten>_enabled`. Tham số của module: `<module>_<tham số>`, đơn vị ở cuối (`_seconds`, `_count`, `_percent`).
- **Quảng cáo** (ADR-0026, thay mục 1 của ADR-0023 cho khóa quảng cáo): khái niệm có trong AdsKit dùng **đúng tên của AdsKit**, kể cả lỗi chính tả `show_resum_ads`. Bảng khóa ở [ads-business-rules.md](ads-business-rules.md), mục 10. Khóa mới đặt cùng kiểu với AdsKit:
  - Khóa của vị trí **suy ra từ khai báo vị trí**, không đặt tay (mục Mô hình chung của `ads-business-rules.md`): bật tắt `is_show_<mã vị trí>` (ví dụ `is_show_inter_save`); nhóm giãn cách riêng `ads_interval_<nhóm>`; tham số riêng `<tham số>_<mã vị trí>` (câu hỏi 28 của spec ads). Mã vị trí trùng tên AdsKit thì khóa trùng đúng chuỗi AdsKit (`rw_get_item`, `native_onboarding`, nhóm `sounds`).
  - Khóa chung khác: `show_<thứ>_ads` cho loại quảng cáo theo thời điểm; loại `<thứ>_type` (giá trị chuỗi, ví dụ `aoa`, `inter`); thời gian `time_<việc>_<thứ>`; công tắc số liệu `is_log_<thứ>`.
  - Khóa không có trong bảng khóa chính thức của AdsKit ghi rõ "tên do ta đặt" trong bảng khóa (mục 10 của `ads-business-rules.md`).
  - Đơn vị ghi trong mô tả khóa (giây, lần), không ghi ở cuối tên.
- Kiểu: boolean, long, double, string. Không dùng khối JSON làm cấu hình (ADR-0023). Danh sách ngắn thì dùng string, ghi định dạng trong mô tả (ví dụ `"vi,en,hi"`).
- Khóa đã phát hành không đổi tên, không đổi kiểu. Cần khác thì tạo khóa mới; khóa cũ giữ tới khi không còn bản app nào đọc.

## 3. Thêm một khóa

1. **Có trong spec hay plan:** cờ tính năng mới đi cùng feature; tham số kinh doanh (quảng cáo, giới hạn, ngưỡng) phải có trong spec đã duyệt.
2. **Khai báo một chỗ** ở module sở hữu, bằng `RemoteKey` của `:core:config` (`RemoteKey.boolean`, `long`, `double`, `string`): tên, kiểu, giá trị mặc định, mô tả một câu viết tiếng Anh (hiện trên Firebase, tối đa 256 ký tự). Mặc định là giá trị an toàn khi chưa tải được cấu hình: cờ và vị trí quảng cáo tắt; giới hạn tần suất nghiêng về ít quảng cáo.
   - Cờ tính năng: một mục trong `FeatureFlag` (`:core:config`), có mô tả; khóa có kiểu là `FeatureFlag.remoteKey`.
   - Khóa quảng cáo chung: một `RemoteKey` trong `AdsSettings` (`:core:ads`), thêm vào `AdsSettings.all()` (test báo lỗi nếu quên), tên theo bảng khóa của `ads-business-rules.md`. Tên không có trong bảng khóa chính thức của AdsKit thì mô tả tự kết thúc bằng câu `Name set by the template, not an AdsKit key.` `:core:ads` chỉ đọc khóa qua `AdsFlags`.
   - Khóa của vị trí quảng cáo không khai báo tay: suy ra từ dòng của vị trí trong `AdPlacement` và tự có trong `AdsSettings.all()`: `AdsSettings.placementEnabled` (`is_show_<mã vị trí>`), `AdsSettings.interval` (`ads_interval_<nhóm>` của nhóm giãn cách riêng), `AdsSettings.nativeEvery` (`native_every_<mã vị trí>` khi vị trí khai báo `nativeEvery`). Mô tả của khóa suy ra kết thúc bằng `Derived key of ...`.
   - Công tắc sự kiện AppsFlyer `is_log_af_*`: trong `AppsFlyerSettings` (`:core:analytics`).
   - Tham số khác: một `RemoteKey` trong module core sở hữu, đọc qua `RemoteSettings.value(key)`. Module core mới có khóa thì thêm vào `AppRemoteKeys` (`:app`).
3. **Sinh lại bảng khóa** `docs/remote-config-keys.md` và file mẫu `docs/remote-config-template.json` bằng lệnh `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`, commit cùng PR. Quên thì `RemoteConfigKeysTest` trong `scripts/check.sh` báo lỗi.
4. **Test:** giá trị mặc định có test (như `AdsSettingsTest`, test mặc định tắt của `FeatureFlag`); ViewModel dùng nguồn cờ giả.
5. **Staging:** cần thấy tính năng trên staging thì khai báo trong `app/src/staging`: cờ thêm vào `StagingFeatureFlagsModule`; tham số khác thêm một phần tử `LocalRemoteValue.of(<khóa>, <giá trị>)` vào tập trong `StagingRemoteValuesModule` (qualifier `@LocalRemoteValues` của `:core:config`). Mỗi khóa chỉ một giá trị cục bộ; trùng tên thì app dừng ngay khi khởi tạo. Production không có module này nên không có giá trị cục bộ nào.

## 4. Đọc giá trị trong app

- Đọc giá trị đã kích hoạt, không chờ mạng (ADR-0012). Giá trị mới tới qua lần tải sau hay qua cập nhật thời gian thực; feature theo dõi `Flow`, không đọc một lần.
- Chỉ màn hình loading chờ tải cấu hình lần đầu, có giới hạn thời gian (ADR-0018).
- Thứ tự ưu tiên khác nhau giữa cờ và tham số:
  - Cờ tính năng: cờ bật riêng cho bản build (`StagingFeatureFlagsModule`) luôn bật, Firebase không tắt được; cờ khác theo giá trị từ Firebase, rồi mặc định trong code (tắt).
  - Tham số (`RemoteSettings`, kể cả khóa quảng cáo): giá trị từ Firebase > giá trị cục bộ của bản build (`StagingRemoteValuesModule`, chỉ staging) > mặc định trong code (ADR-0023, mục 5). Firebase staging vì vậy vẫn đổi được tham số và tắt được từng vị trí quảng cáo trên staging.

## 5. Đổi giá trị trên Firebase

Đổi giá trị là đổi hành vi của app đang chạy ở máy người dùng: làm như một lần phát hành nhỏ (ADR-0023).

1. **Ghi trước:** khóa, giá trị cũ, giá trị mới, điều kiện, lý do, người đổi, vào sổ đổi cấu hình của app (`docs/ops/` của app, mỗi lần một dòng).
2. **Dùng điều kiện để bật dần:** theo phiên bản app (chỉ bản đã có code đọc khóa), quốc gia, hay phần trăm người dùng. Vị trí quảng cáo mới: bật cho phần nhỏ người dùng trước, xem giữ chân và điểm Play rồi mới mở rộng.
3. **Kiểu giá trị trên Firebase khớp kiểu trong code** (boolean là `true`/`false`, số không có đơn vị).
4. **Bấm Publish** và kiểm trên một máy (DebugView hay log của app).
5. **Quay về:** dùng lịch sử phiên bản của Remote Config trên Firebase; ghi lần quay về vào sổ.
6. **Sự cố quảng cáo** (vi phạm chính sách, lỗi hiển thị): tắt khóa của định dạng hay vị trí bị cảnh báo (ví dụ `is_show_native_after_inter`, `show_open_ads`), rồi mới tìm nguyên nhân. Công tắc tổng: `is_show_ads` (spec ads, câu hỏi 7).
7. **Project staging chỉ đặt khóa muốn đổi**, không nhập cả file mẫu `docs/remote-config-template.json`. File mẫu đặt mọi khóa bật quảng cáo `false`, mà giá trị từ Firebase thắng giá trị cục bộ, nên nhập file là mất quảng cáo trên staging.

## 6. Thử nghiệm A/B

Dùng Firebase A/B Testing trên một khóa đã có, với một chỉ số chính (ví dụ giữ chân D7 hay ARPDAU) và chỉ số chặn (điểm Play, tỷ lệ crash). Không chạy hai thử nghiệm cùng đổi một khóa. Ghi thử nghiệm và kết quả vào sổ đổi cấu hình.

## 7. Bảng khóa

[`docs/remote-config-keys.md`](../remote-config-keys.md) liệt kê mọi khóa của bản app hiện tại: khóa, kiểu, mặc định, module, mô tả. Sinh từ code (`AppRemoteKeys` của `:app`), không sửa tay; `RemoteConfigKeysTest` báo lỗi khi lệch. Sinh lại: `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`.

[`docs/remote-config-template.json`](../remote-config-template.json) là cùng danh sách theo định dạng template của Firebase Remote Config. Nhập file này thay cả bộ cấu hình của project, nên chỉ dùng cho project Firebase mới; project đang chạy thì thêm từng khóa bằng tay theo bảng.
