---
id: T-080
title: Ô quảng cáo trong lưới thư viện (`poses_in_collection`, tắt lúc phát hành); khung bỏ ô khi pool cạn
feature: ads
spec: features/ads/spec.md   # confirmed 2026-10-30, câu 9 và 11; vị trí của màn ở mục 5 của features/poses/spec.md
scenarios: [KB-6]            # của features/poses/spec.md, tiêu chí 5 (ô quảng cáo trong lưới)
owner: Tuấn
status: backlog
risk: high
size: L
depends_on: [T-079]          # T-079 sửa trước cùng các file AdPlacement.kt, PosesScreen.kt, StagingRemoteValuesModule.kt và bảng khóa
sprint:
plan:
created: 2026-10-31
---

# T-080: Ô QUẢNG CÁO TRONG LƯỚI THƯ VIỆN; KHUNG BỎ Ô KHI POOL CẠN

## Mô tả

Ticket tách từ T-079 ngày 2026-10-31, theo quyết định của Nhật. Rà soát độc lập trước RUN-20261031-01 phát hiện vấn đề sau ở hàm lưới của khung (`LazyGridScope.itemsWithInListAds` trong `:core:ads`):

- Hàm luôn đặt ô quảng cáo vào lưới. Khi pool cạn, ô có nội dung cao 0 nhưng vẫn chiếm một cột, nên hàng đó còn một ô trống.
- Điều này trái spec poses KB-6, tiêu chí 5: "hết quảng cáo thì ô ẩn, lưới không có ô trống".

Nhật chọn sửa khung để bỏ ô khi pool cạn.

Sau ticket này, lưới thư viện có một ô quảng cáo native sau mỗi `native_every_poses_in_collection` tư thế (mặc định 8): không ở đầu, không ở cuối, ở mọi bộ lọc và cả bố cục 3 cột khi mở gập. Hết quảng cáo thì ô biến mất, không để lại chỗ trống. Vị trí tắt lúc phát hành.

## Phạm vi

- **Khung (`core/ads`, `InListAds.kt`, có thể cả `NativeAdPool.kt`, `AdsController.kt`): lưới định dạng một ô (`native_in_collection`) không đặt ô khi ô đó không có quảng cáo để hiện.**
  - Ô đang giữ một quảng cáo thì giữ nguyên, không gỡ quảng cáo đang hiển thị.
  - Pool có quảng cáo lại thì ô quay lại.
  - Danh sách và hàng toàn chiều rộng không đổi hành vi.
  - Plan quyết cách biết "ô không có quảng cáo" (cờ `exhausted` của pool, kiểm lại 2 giây một lần như `NativePoolCell`, P-05; hay trạng thái của từng ô) và chạy thử bằng `scripts/check.sh` trước khi duyệt (tiêu chí E từ 2, mục 4.1 của plan).
  - Đề xuất phần sửa này về dự án mẫu (`docs/ops/from-app.md`).
- **`AdPlacement`:** thêm `POSES_IN_COLLECTION("poses_in_collection", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 8)`, một dòng. `keepSpaceWhenPoolEmpty` để mặc định.
- **Thư viện (`PosesScreen.kt`):** `rememberInListAds(AdPlacement.POSES_IN_COLLECTION)` ngoài lưới; lưới dùng `itemsWithInListAds(cards, ads, columns)`. Áp cho mọi bộ lọc, kể cả "Đã lưu" (câu 1 của plan T-079 phiên bản 1, Nhật trả lời 2026-10-31).
- **Ảnh chuẩn:** `PosesScreen_ad-collection_light` (ảnh thiết kế có từ PR #253). Ô quảng cáo cố định cho ảnh chuẩn dùng một hàm không trạng thái của khung, ví dụ `itemsWithFixedAds` (câu 5 của plan T-079 phiên bản 1). Chiều cao ô trong ảnh theo thẻ tư thế: `NativeAdCard` cỡ `CARD` cao 300, ô thật thêm lề dọc 16 x 2; plan nêu cách khớp hay đặt câu hỏi.
- **Bản staging:** bật `is_show_poses_in_collection` (câu 11 của spec ads), cập nhật `StagingRemoteValuesModuleTest`.
- **Bảng khóa:** sinh lại, thêm `is_show_poses_in_collection`, `native_every_poses_in_collection`.
- **Tài liệu:** `docs/ops/manual-test.md` mục 11, thêm bước ô trong lưới.

**Không bao gồm:**

- Thanh đáy, `inter_pose_detail`: T-079.
- Mã quảng cáo thật: T-075.
- Giữ chỗ cuộn theo tư thế khi ô quảng cáo vừa xuất hiện: chấp nhận lệch một ô (câu 3 của plan T-079 phiên bản 1).

## Rủi ro sơ bộ

Ticket chạm điểm chặn của `risk-scoring.md`: thêm vị trí quảng cáo, sửa module core `:core:ads` và hành vi chung của khung. Kỹ thuật mới (E = 2), nên phải chạy thử trước khi duyệt. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật). Không thêm SDK, thư viện hay quyền.

## Định nghĩa hoàn thành

- [ ] Đạt KB-6 tiêu chí 5 của spec poses: ô sau mỗi N tư thế, không đầu, không cuối, một ô, mọi bộ lọc, 3 cột khi mở gập; hết quảng cáo thì không còn ô trống.
- [ ] Có test của khung:
  - pool cạn thì lưới không có ô trống;
  - ô đang có quảng cáo vẫn giữ;
  - pool có lại thì ô quay lại;
  - danh sách và hàng toàn chiều rộng không đổi.
- [ ] Khóa `is_show_poses_in_collection` mặc định tắt; bản staging bật; bảng khóa sinh lại khớp code.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật: ô quảng cáo sau tư thế 8 ở "Tất cả" và một nhóm, không bấm quảng cáo; đã nghiệm thu chéo.
