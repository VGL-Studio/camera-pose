---
id: T-079
title: Thanh đáy native thay hai banner ở thư viện và chi tiết tư thế; interstitial khi chụp với tư thế (tắt lúc phát hành)
feature: ads
spec: features/ads/spec.md   # confirmed 2026-10-30, câu 7 tới 11; vị trí của màn ở mục 5 của features/poses/spec.md (confirmed 2026-10-30, câu hỏi 5)
scenarios: [KB-4, KB-6]      # của features/poses/spec.md
owner: Tuấn
status: on-staging
risk: high
size: M
depends_on: [T-074]          # khung AdsKit đã có trong app; T-074 đang on-staging, chờ nghiệm thu trên máy
sprint:
plan: features/ads/plans/T-079-thanh-day-native-va-vi-tri-dot-2.md   # phiên bản 2 approved, 2026-10-31
created: 2026-10-30
---

# T-079: THANH ĐÁY NATIVE THAY HAI BANNER Ở THƯ VIỆN VÀ CHI TIẾT TƯ THẾ; INTERSTITIAL KHI CHỤP VỚI TƯ THẾ

## Mô tả

Rà file kế hoạch quảng cáo, Remote Config và sự kiện của nhóm (2026-10-30) sau T-074 cho ra câu 7 tới 11 của `features/ads/spec.md`. Sau ticket này, đáy thư viện và chi tiết tư thế là thanh đáy native nhỏ của khung (`poses_bottom`, `pose_detail_bottom`) thay hai banner `poses_banner`, `poses_detail_banner`. App có thêm hai vị trí tắt lúc phát hành: ô quảng cáo xen trong lưới thư viện (`poses_in_collection`, mỗi 8 tư thế) và interstitial khi bấm "Chụp với tư thế này" (`inter_pose_detail`). `review_interstitial` giữ nguyên mã và luật đếm ảnh.

**Cập nhật 2026-10-31 (Nhật):** ô quảng cáo trong lưới (`poses_in_collection`) tách sang T-080. Rà soát độc lập trước RUN-20261031-01 thấy hàm lưới của khung để lại ô trống khi pool cạn, trái KB-6. Nhật chọn sửa khung để bỏ ô khi pool cạn, và phần sửa này phải chạy thử trước khi duyệt. Các dòng về `poses_in_collection` dưới đây thuộc T-080.

## Phạm vi

- **`AdPlacement` (`core/ads`):** thêm `POSES_BOTTOM` (`native_small`), `POSE_DETAIL_BOTTOM` (`native_small`), `POSES_IN_COLLECTION` (`native_in_collection`, `nativeEvery = 8`), `INTER_POSE_DETAIL` (`interstitial`, nhóm giãn cách mặc định); bỏ `POSES_BANNER`, `POSES_DETAIL_BANNER`. Mỗi vị trí một dòng (`docs/ops/ads-guide.md`, mục 5, bước 2). Không thêm cờ hay hàm mang tên tính năng vào `core/ads` (MC-10).
- **Thư viện (`PosesScreen.kt`):** bọc màn trong `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM)`, đặt `padding` vào lưới; lưới dùng `itemsWithInListAds(AdPlacement.POSES_IN_COLLECTION)` của `LazyGridScope`, ô quảng cáo chiếm một ô, áp cho "Tất cả", từng nhóm và bố cục 3 cột khi mở gập. Bỏ `PosesBottomBanner` ở `PosesComponents.kt` nếu không còn chỗ dùng.
- **Chi tiết (`PoseDetailScreen.kt`):** `BottomAdBarScaffold(AdPlacement.POSE_DETAIL_BOTTOM)` dưới thanh nút chính; nút "Chụp với tư thế này" gọi interstitial của khung cho `AdPlacement.INTER_POSE_DETAIL` (plan chọn giữa `rememberShowInterstitial(placement, onFinished)` và `rememberShowFullScreenAd`; vị trí này không có luật đếm ảnh) rồi mới mở camera trong `onFinished`. Không có quảng cáo hay chưa đủ nhịp thì đi tiếp ngay; `pose_selected` ghi một lần như cũ.
- **Bản staging (`StagingRemoteValuesModule`):** theo câu 11 của spec ads: bật `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_native_bottom`, `is_show_inter_pose_detail`, `is_show_poses_in_collection`; đặt `is_show_native_collap` là `false`; không bật `is_show_native_after_inter`. Cập nhật `StagingRemoteValuesModuleTest`.
- **Bảng khóa:** sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` (`-PremoteConfigKeys.update=true`): thêm `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_poses_in_collection`, `native_every_poses_in_collection`, `is_show_inter_pose_detail`; bỏ `is_show_poses_banner`, `is_show_poses_detail_banner`.
- **Thiết kế và ảnh chuẩn:** theo câu hỏi 5 của spec poses: bốn frame `ad` vẽ lại với thanh đáy native, thêm frame ô quảng cáo trong lưới; screenshot test cùng tên, khung chuẩn, khung thấp `_small` và `_unfolded`; chạy `scripts/design-compare.sh poses`. Ảnh chuẩn đổi ghi lại theo plan, lệnh có `--rerun` (L-114 của mẫu).
- **Tài liệu:** `docs/ops/manual-test.md` mục 11 (Quảng cáo): thay bước banner bằng thanh đáy, thêm bước ô trong lưới và interstitial ở chi tiết. Sau khi merge, chạy `python3 scripts/ads-plan.py --keep <file trên Drive>` và đổi trạng thái các dòng thành `Đã code`.

**Không bao gồm:**

- Mã quảng cáo thật, `scripts/release.sh`: T-075. Danh sách mã cần tạo của T-075 thêm bốn vị trí mới và bỏ hai banner.
- Lớp native thu gọn: tắt (câu 10); không đổi code của lớp.
- Combo native sau interstitial: giữ tắt (câu 6).
- Bỏ `sample_native_small` và `:feature:sample`: quyết ở T-075 hay một ticket riêng.
- Đặt giá trị trên Firebase production: người làm, theo `docs/ops/remote-config.md` mục 5.

## Rủi ro sơ bộ

Chạm điểm chặn của `risk-scoring.md`: quảng cáo, vị trí quảng cáo mới, module core (`core/ads`), ảnh chuẩn và thiết kế. Không thêm SDK, thư viện hay quyền: thanh đáy, ô trong lưới và interstitial đã có trong khung từ T-074. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật). Plan nên chia hai lượt trong cùng danh sách chạy, mỗi lượt một PR: (1) thanh đáy thay banner, bảng khóa, ảnh chuẩn; (2) `poses_in_collection`, `inter_pose_detail`.

## Câu hỏi cho người

Câu 7 tới 11 của `features/ads/spec.md` và câu hỏi 5 của `features/poses/spec.md` đã có câu trả lời; Nhật xác nhận hai spec ngày 2026-10-30.

Việc trước khi soạn plan: AI vẽ lại bốn frame trạng thái `ad` và thêm frame `PosesScreen_ad-collection_light` trên Figma, xuất ảnh vào `docs/features/poses/design/screens/` (câu hỏi 5 của spec poses). Plan dùng ảnh mới cho screenshot test cùng tên.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí của KB-4, KB-6 của spec poses đạt; bốn khóa `is_show_*` mới mặc định tắt.
- [ ] `AdPlacement` không còn `POSES_BANNER`, `POSES_DETAIL_BANNER`; `review_interstitial` và luật đếm ảnh không đổi (test có sẵn vẫn qua, không sửa câu khẳng định).
- [ ] Có test cho: thanh đáy chừa chỗ chỉ khi khe có quảng cáo; nút chính ở chi tiết không bị che ở khung thấp; ô trong lưới đúng vị trí (sau mỗi N, không đầu, không cuối, ẩn khi pool cạn); bấm "Chụp với tư thế này" mở camera đúng tư thế khi có quảng cáo, khi không có và khi khóa tắt; giá trị của bản staging.
- [ ] Bảng khóa sinh lại khớp code; `scripts/ads-plan.py` chạy được; manifest gộp không đổi.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật: thanh đáy ở thư viện và chi tiết, ô trong lưới, interstitial khi chụp với tư thế (khi khóa bật trên staging), không bấm quảng cáo; đã nghiệm thu chéo.
