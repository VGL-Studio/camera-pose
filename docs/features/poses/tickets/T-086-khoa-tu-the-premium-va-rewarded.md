---
id: T-086
title: Khóa tư thế premium ở thư viện, chi tiết, camera; mở bằng quảng cáo có thưởng `rw_premium_pose`
feature: poses
spec: features/poses/spec.md   # confirmed 2026-10-31, câu 7, 8, 10, 11, 12; vị trí cũng ghi ở features/ads/spec.md câu 12; KB-1 của features/camera/spec.md
scenarios: [KB-8]
owner: Nhật
status: approved
risk: high
size: L
depends_on: [T-085, T-080]     # T-085 cho dữ liệu premium; T-080 sửa trước PosesScreen.kt, AdPlacement.kt và bảng khóa
sprint:
plan: features/poses/plans/T-086-khoa-tu-the-premium-va-rewarded.md
created: 2026-10-31
---

# T-086: KHÓA TƯ THẾ PREMIUM VÀ MỞ BẰNG QUẢNG CÁO CÓ THƯỞNG

## Mô tả

Phần người dùng thấy của tư thế premium, theo yêu cầu của Nhật ngày 2026-10-31. Tư thế premium chưa mở khóa có dấu premium ở thư viện, chi tiết và dải tư thế của camera. Người dùng chọn dùng tư thế đó thì hộp thoại mở khóa hiện. Xem hết quảng cáo có thưởng thì tư thế mở vĩnh viễn, và app làm tiếp việc người dùng vừa định làm.

## Phạm vi

- **Thiết kế (trước plan):** trên Figma, theo mục 4 của spec poses:
  - dấu premium trên thẻ, trên ô của dải camera và ở chi tiết;
  - nút chính của chi tiết khi tư thế còn khóa;
  - nội dung hộp thoại mở khóa;
  - bản sáng và tối.

  Lập gói bàn giao cho poses và camera.
- **`AdPlacement`:** thêm `RW_PREMIUM_POSE("rw_premium_pose", AdFormat.REWARDED)`, một dòng. Không khai báo `noAdPolicy`: chặn khi không có quảng cáo (câu 8).
- **Thư viện, chi tiết (`:feature:poses`):**
  - Dấu premium.
  - Nút chính khi còn khóa.
  - `rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, onGranted = ...)` và `RewardedAdDialog(...)`. Mở khóa xong thì về camera với tư thế đó (KB-4).
- **Camera (`:feature:camera`):**
  - Dấu premium trên ô của dải tư thế.
  - Chạm ô còn khóa thì hiện hộp thoại, không đổi tư thế đang dùng.
  - Mở khóa xong thì dùng tư thế đó.
  - Tư thế dùng lần trước mà bị khóa (catalog mới đánh dấu premium) thì camera dùng tư thế mặc định. Plan nêu trường hợp này.
- **Sự kiện:** `premium_pose_unlocked` (`pose_id`, `category`, `source`), khai báo hằng `EVENT_` (câu 11).
- **Bản staging:** bật `is_show_rw_premium_pose`, cập nhật `StagingRemoteValuesModuleTest`. Sinh lại bảng khóa.
- **Test:**
  - ViewModel: chọn tư thế khóa, được thưởng, đóng sớm, không có quảng cáo.
  - Giao diện, và screenshot cho mỗi ảnh thiết kế mới.
  - Ảnh `_small` của chi tiết có nút chính khi còn khóa.
- **Tài liệu:** `docs/ops/manual-test.md` thêm bước mở khóa trên máy thật. `scripts/ads-plan.py` có dòng mô tả vị trí nếu cần.
- **Mã production:** thêm `rw_premium_pose` vào danh sách mã cần tạo. Mã "Rewarded" Nhật đã tạo; giá trị không vào repo.

**Không bao gồm:**

- Dữ liệu premium, catalog, cờ `feature_premium_poses_enabled`: T-085.
- Mã quảng cáo thật và `scripts/release.sh`: T-075.
- Trang giới thiệu tư thế premium ở luồng giới thiệu: theo câu 4 của spec onboarding và plan T-082.

## Rủi ro sơ bộ

Ticket chạm điểm chặn của `risk-scoring.md`: thêm vị trí quảng cáo, thêm sự kiện phân tích, đổi hành vi chọn tư thế ở camera (màn chính). Có thiết kế mới ở hai feature. Không thêm SDK, quyền hay thư viện. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật).

Cỡ L, một lượt: một plan chạy đúng một lượt trong danh sách chạy (`docs/CLAUDE.md`, mục Khi tách spec thành ticket; L-142 của mẫu). Muốn chia nhỏ thì tách camera thành ticket riêng (plan T-086, mục 8, câu 10).

## Định nghĩa hoàn thành

- [ ] Đạt KB-8 của spec poses và hai tiêu chí tư thế premium ở KB-1 của spec camera.
- [ ] Khóa `is_show_rw_premium_pose` mặc định tắt; bản staging bật; bảng khóa sinh lại khớp code.
- [ ] Mỗi ảnh thiết kế mới có screenshot test cùng tên; chạy `scripts/design-compare.sh` cho poses và camera.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật bản staging: mở khóa từ chi tiết và từ camera, đóng quảng cáo sớm, chế độ máy bay (tư thế vẫn khóa); mở lại app thì tư thế vẫn mở. Xem quảng cáo thử có thưởng đến hết; không bấm vào quảng cáo.
