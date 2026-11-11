---
id: ADR-1009
title: Combo native toàn màn hình hiện sau interstitial của màn, không sau `open_app_inter` và `resume_inter`; `MainActivity` đặt `FullScreenAdHost`
status: accepted             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-11-10
deciders: [Nhật]
supersedes: ADR-1007         # chỉ vế "`MainActivity` không đặt `FullScreenAdHost`"; các vế khác của ADR-1007 giữ nguyên
superseded_by:
---

# ADR-1009: COMBO NATIVE TOÀN MÀN HÌNH HIỆN SAU INTERSTITIAL CỦA MÀN, KHÔNG SAU `OPEN_APP_INTER` VÀ `RESUME_INTER`; `MAINACTIVITY` ĐẶT `FULLSCREENADHOST`

> **Ghi chú 2026-11-11 (đồng bộ dự án mẫu đợt 6, `151b4c2..e16632e`):** dự án mẫu đã nhận đề xuất của ADR này (issue `from-app` #445 của mẫu): luật A-12 của `docs/ops/ads-business-rules.md` nay là "`resume_inter` không nối combo", `ResumeAdTurns` của khung tự làm vậy, và ADR-0026 đã bị ADR-0045 thay. App không còn lệch luật của mẫu ở phần `resume_inter`; các vế về lệch A-12 và dòng `combo = false` riêng của app bỏ khỏi mục Quyết định và Việc AI phải tuân thủ. AI ghi theo lời anh Nhật ("Sửa ADR-1009 theo đề xuất", 2026-11-11).

## Bối cảnh

Khung AdsKit của dự án mẫu (ADR-0026) có combo: khi `is_show_native_after_inter` bật lúc một interstitial hoàn tất, một quảng cáo native toàn màn hình của `native_combo` hiện tiếp, đếm ngược `time_close_native` giây rồi đóng được (K-01 tới K-08 của `docs/ops/ads-business-rules.md`). Lớp phủ do `:core:ads` vẽ qua `FullScreenAdHost`, và `:app` phải đặt composable đó một lần ở gốc giao diện (HD-05). Luật A-12 của khung còn nối combo sau cả interstitial dùng thay quảng cáo khi quay lại app (`resume_inter`).

Lúc lấy khung (T-074, ADR-1007), spec ads giữ combo tắt, nên `MainActivity` không đặt `FullScreenAdHost` (ADR-1007, một vế của mục Quyết định). Phiên thử TS-20261102-01 cho thấy khi khóa bị bật trên Firebase staging, lượt combo vẫn mở mà không có gì vẽ: không đóng được, chặn quảng cáo quay lại app không được gỡ (FB-20261102-01). Sau đó Nhật quyết bật combo (spec ads, câu 18, 2026-11-02): combo sau mọi interstitial, trừ khi interstitial dùng thay quảng cáo mở app hay quảng cáo khi quay lại app (`open_app_inter`, `resume_inter`); bật sẵn ở staging; production bật khi phát hành. Điều này khác luật A-12 của khung ở phần `resume_inter`, và trái vế của ADR-1007 về `FullScreenAdHost`. ADR-1007 đã `accepted` và `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md` là file lấy nguyên từ dự án mẫu, không sửa (`docs/CLAUDE.md`, mục Khi viết ADR; plan T-090, mục 8, câu 5), nên lệch có chủ đích này phải ghi ở một ADR của app để đợt đồng bộ sau và các phiên đọc `foundation.md` biết.

## Quyết định

- `MainActivity` đặt `FullScreenAdHost()` một lần trong `setContent`, trong `AppTheme` và `AdsHost` (qua `AppAdsRoot`), ngay sau lời gọi `NetworkDialogHost(...) { AppNavHost(...) }`, ngoài khối nội dung của nó (HD-05; plan T-090, mục 8, câu 1). Vế "`MainActivity` không đặt `FullScreenAdHost`" của ADR-1007 không còn áp dụng; các vế khác của ADR-1007 giữ nguyên.
- Combo hiện sau mọi interstitial của màn (`review_interstitial`, `inter_pose_detail`, và vị trí interstitial của màn thêm sau này), theo khóa `is_show_native_after_inter` và luật K-01 tới K-08 của khung. Combo không hiện sau `open_app_inter` (khung đã vậy) và không hiện sau `resume_inter` (spec ads, câu 18).
- `resume_inter` không nối combo theo luật A-12 của khung (ADR-0045, `docs/ops/ads-business-rules.md`); app không có code riêng cho việc này.
- Bản staging bật sẵn `is_show_native_after_inter` trong `StagingRemoteValuesModule`; Firebase staging vẫn tắt được vì giá trị Firebase thắng giá trị cục bộ (ADR-1007). Production bật khi phát hành, bằng khóa trên Firebase production (việc của người, ADR-0007, ADR-0009), cần mã quảng cáo thật của `native_combo` (T-075).
- Đề xuất về dự án mẫu (`docs/ops/from-app.md`) đã được nhận: `resume_inter` không nối combo (A-12), và khung không mở lượt combo khi không có host nào vẽ (K-09) (issue #445 của mẫu).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Đặt `FullScreenAdHost` ngoài `NetworkDialogHost`, sửa thẳng một dòng `combo = false` trong `ResumeAdTurns` (đã chọn) | Ít lệch mẫu nhất; `NetworkDialogHost` chỉ bọc nội dung điều hướng; test đọc mã nguồn có một vị trí rõ; đợt đồng bộ chỉ phải giữ một dòng | Hai file luật của mẫu còn ghi `resume_inter` nối combo; phải đọc ADR này để biết lệch |
| Thêm công tắc của khung cho `resume_inter` nối combo (mặc định như mẫu) | Dự án mẫu nhận lại nguyên văn | Nhiều code lệch mẫu hơn; khi mẫu nhận công tắc thì đợt đồng bộ thay dòng này bằng cách của mẫu, nên đề xuất `from-app` thay vì làm ở app |
| Đặt `FullScreenAdHost` trong khối nội dung của `NetworkDialogHost`, sau `AppNavHost` | Đúng chữ "sau `AppNavHost`" của HD-05 | Không khác gì về hành vi (lớp phủ và popup mạng là hai cửa sổ hộp thoại riêng, popup chờ theo cờ `fullScreenAdShowing`); `NetworkDialogHost` bọc thêm một thứ không phải nội dung điều hướng |
| Giữ combo tắt, không đặt `FullScreenAdHost` (ADR-1007) | Không đổi gì | Trái spec ads câu 18; khóa bật nhầm trên Firebase thì lượt combo kẹt như FB-20261102-01 |

## Hệ quả

- **Tích cực:** combo thật sự hiện, đếm ngược và đóng được sau interstitial của màn; không còn lượt combo vô hình giữ chặn quảng cáo quay lại app; `resume_inter` đóng là về màn cũ ngay.
- **Phải chấp nhận:** mỗi interstitial của màn dài thêm một lượt combo (theo dõi giữ chân ngày 1 sau khi phát hành, ticket T-090); popup mất mạng chờ lâu hơn khi mất mạng trong lúc combo (`features/network/spec.md`, mục 5); mọi kịch bản emulator đi qua interstitial của màn phải có bước đóng lớp phủ combo khi nó hiện (`wait str=ads_close_full_screen | ...`, `tap_if str=ads_close_full_screen`).
- **Việc AI phải tuân thủ:** giữ đúng một lời gọi `FullScreenAdHost()` trong `MainActivity`, trong `AppTheme`, ngay sau `NetworkDialogHost`; không nối combo sau `resume_inter` hay `open_app_inter` (luật A-12 của khung); interstitial của màn (vị trí không toàn app) luôn đi qua `showInterstitial` của `:core:ads` để combo nối theo khóa `is_show_native_after_inter`; kịch bản emulator đi qua interstitial của màn thêm bước `wait str=ads_close_full_screen | <màn đích>` rồi `tap_if str=ads_close_full_screen` sau bước đóng interstitial; không đổi luật K-01 tới K-08 của khung ở app.
