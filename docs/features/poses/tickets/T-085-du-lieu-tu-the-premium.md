---
id: T-085
title: Dữ liệu tư thế premium và danh sách đã mở khóa (`:core:poses`, catalog)
feature: poses
spec: features/poses/spec.md   # confirmed 2026-10-31, câu 6, 9, 10, 12
scenarios: [KB-8]              # phần dữ liệu: đánh dấu premium, mở khóa vĩnh viễn, tư thế mặc định không premium
owner: Nhật
status: on-staging
risk: high
size: M
depends_on: []
sprint:
plan: features/poses/plans/T-085-du-lieu-tu-the-premium.md
created: 2026-10-31
---

# T-085: DỮ LIỆU TƯ THẾ PREMIUM VÀ DANH SÁCH ĐÃ MỞ KHÓA

## Mô tả

Phần dữ liệu của tư thế premium, theo yêu cầu của Nhật ngày 2026-10-31. Ticket làm ba việc:

- `:core:poses` biết tư thế nào là premium.
- `:core:poses` biết tư thế nào đã mở khóa trên máy.
- `:core:poses` mở khóa một tư thế vĩnh viễn.

Ticket này chưa có giao diện hay quảng cáo; phần đó ở T-086. Sau ticket này người dùng chưa thấy gì khác: cờ `feature_premium_poses_enabled` tắt thì mọi tư thế dùng được.

## Phạm vi

- **Catalog:**
  - Trường `premium` cho mỗi tư thế (câu 6), trong một phiên bản catalog mới. Catalog cũ không có trường thì mọi tư thế miễn phí.
  - Cập nhật khuôn catalog trong tài liệu nội dung.
  - Script kiểm catalog (`scripts/setup-check-app.sh` hay script nội dung) báo lỗi khi tư thế mặc định hay bộ khởi đầu là premium.
- **`:core:poses`:**
  - `Pose` có `premium`.
  - Repository mới, hay mở rộng repository đã có, giữ danh sách đã mở khóa trong DataStore, cùng cách với danh sách đã lưu.
  - Có `Flow` "tư thế này có dùng được không", gộp cờ, `premium` và danh sách đã mở.
  - Hàm `unlock(poseId)` ghi danh sách. Lỗi ghi ném `IOException` để ViewModel bắt.
- **Cờ:** `FeatureFlag.PREMIUM_POSES` (`feature_premium_poses_enabled`, mặc định tắt), bật ở `StagingFeatureFlagsModule` (câu 10). Sinh lại bảng khóa.
- **Sao lưu:** kiểm cấu hình sao lưu của app và ghi kết quả vào plan (câu 9). Có đổi cấu hình thì phải nêu ở câu hỏi của plan.
- **Test:**
  - Đọc catalog có và không có `premium`.
  - Mở khóa rồi đọc lại sau khi tạo lại repository.
  - Cờ tắt thì mọi tư thế dùng được.
  - Lỗi ghi.
- **Nội dung:** người làm nội dung đặt `premium` cho các tư thế đã chọn và đẩy catalog mới lên R2 sau khi ticket merge. Đây là việc của người.

**Không bao gồm:**

- Dấu premium, hộp thoại mở khóa, quảng cáo có thưởng, sự kiện `premium_pose_unlocked`: T-086.

## Rủi ro sơ bộ

Ticket chạm điểm chặn của `risk-scoring.md`:

- Sửa module core `:core:poses`.
- Thêm dữ liệu lưu trên máy.
- Đổi khuôn catalog trên R2.

Không thêm SDK, quyền hay thư viện. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật).

## Định nghĩa hoàn thành

- [ ] Catalog mới có `premium` đọc đúng; catalog cũ vẫn chạy, mọi tư thế miễn phí.
- [ ] Mở khóa giữ qua lần mở app sau; cờ tắt thì không tư thế nào bị khóa.
- [ ] Script kiểm catalog báo lỗi khi tư thế mặc định hay bộ khởi đầu là premium.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
