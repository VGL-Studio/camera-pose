---
id: T-093
title: Thanh đáy native nhỏ ở màn Cài đặt (settings_bottom, tắt lúc phát hành)
feature: ads
spec: features/ads/spec.md   # confirmed 2026-11-13, câu 19; vị trí của màn ở mục 5 của features/settings/spec.md (confirmed 2026-11-13)
scenarios: [KB-1]            # của features/settings/spec.md
owner: Nhật
status: on-staging                # frame trạng thái ad của câu 19, ý b đã duyệt (Nhật, 2026-11-13)
risk: high
size: S
depends_on: []
sprint:
plan: features/ads/plans/T-093-thanh-day-native-o-man-cai-dat.md
auto_run:
created: 2026-11-13
---

# T-093: THANH ĐÁY NATIVE NHỎ Ở MÀN CÀI ĐẶT

## Mô tả

Theo câu 19 của `features/ads/spec.md`, đáy màn Cài đặt có thanh quảng cáo native nhỏ `settings_bottom`, cùng luật với `poses_bottom`. Danh sách cài đặt cuộn phía trên thanh, không bị che. Production tắt lúc phát hành, bản staging bật sẵn.

## Phạm vi

- **`AdPlacement` (`core/ads`):** thêm `SETTINGS_BOTTOM("settings_bottom", AdFormat.NATIVE_SMALL)`, một dòng, chú thích dẫn spec (`docs/ops/ads-guide.md`, các bước thêm vị trí). Không thêm code mang tên tính năng vào `core/ads`.
- **Cài đặt (`feature/settings`, `SettingsScreen.kt`):** bọc màn trong `BottomAdBarScaffold(AdPlacement.SETTINGS_BOTTOM)`, đặt `padding` vào danh sách cuộn; hộp chọn giao diện và ngôn ngữ phủ lên như hôm nay. Mở trang chính sách vẫn qua `rememberStartOutsideApp`.
- **Bản staging (`StagingRemoteValuesModule`):** bật `is_show_settings_bottom` (câu 19, ý a); cập nhật `StagingRemoteValuesModuleTest`.
- **Bảng khóa:** sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` (`-PremoteConfigKeys.update=true`), thêm `is_show_settings_bottom`.
- **Thiết kế và ảnh chuẩn:** screenshot test cùng tên hai frame `SettingsScreen_ad_light`, `SettingsScreen_ad_dark`, truyền thẻ native giả qua tham số composable (như `gridAds` của `PosesScreen`); ảnh khung thấp `_small` ở trạng thái nhiều nội dung nhất; chạy `scripts/design-compare.sh settings`. Ảnh chuẩn có sẵn không đổi.
- **Tài liệu:** `docs/ops/manual-test.md`, mục Quảng cáo: thêm bước thanh đáy ở Cài đặt. Sau khi merge, chạy `scripts/ads-plan.py --keep <file trên Drive>`.

**Không bao gồm:**

- `review_bottom` của màn xem lại: T-094.
- Mã quảng cáo thật trong cấu hình phát hành, khóa trên Firebase production: việc của người (ADR-0007, ADR-0009).

## Rủi ro sơ bộ

Chạm điểm chặn của `risk-scoring.md`: vị trí quảng cáo mới, module core (`core/ads`), thiết kế và ảnh chuẩn. Không thêm SDK, thư viện hay quyền: thanh đáy đã có trong khung. Rủi ro cao, cần duyệt chéo; Nhật (người phụ trách sản phẩm) đã chốt vị trí ở câu 19.

## Câu hỏi cho người

Câu 19 của `features/ads/spec.md` đã có câu trả lời; Nhật xác nhận spec ngày 2026-11-13.

Việc trước khi soạn plan: AI dựng `SettingsScreen_ad_light`, `SettingsScreen_ad_dark` trên Figma bằng khối thanh đáy native nhỏ có sẵn, Nhật duyệt, xuất ảnh vào `docs/features/settings/design/screens/` và cập nhật gói bàn giao. Thiết kế đã duyệt ngày 2026-11-13, ticket `ready`.

## Định nghĩa hoàn thành

- [ ] Tiêu chí của KB-1 của spec settings đạt; `is_show_settings_bottom` mặc định tắt.
- [ ] Có test cho: thanh chừa chỗ và co về 0 theo hành vi hiện tại của khung (khe 80 dp, chỉ chừa chỗ khi khe đã có quảng cáo, nạp lỗi thì co về 0; như thanh đáy của thư viện); dòng cuối của danh sách cuộn tới được, không bị thanh che ở khung thấp; giá trị của bản staging.
- [ ] Bảng khóa sinh lại khớp code; manifest gộp không đổi.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật: thanh đáy ở Cài đặt (khóa bật trên staging), không bấm quảng cáo; đã nghiệm thu chéo.
