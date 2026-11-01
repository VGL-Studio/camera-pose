---
id: T-088
title: Bỏ cờ `feature_camera_enabled` (không còn tác dụng từ T-081)
feature: camera
spec: features/camera/spec.md   # confirmed 2026-10-31, mục 8 (cờ không còn tác dụng, bỏ ở ticket này)
scenarios: []
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-081]          # T-081 bỏ chỗ đọc cờ trong chooseMainScreen
sprint:
plan: features/camera/plans/T-088-bo-co-camera.md
created: 2026-10-31
---

# T-088: BỎ CỜ `FEATURE_CAMERA_ENABLED`

## Mô tả

Từ T-081, app luôn mở camera sau màn chào (plan T-081, câu 1; Nhật quyết 2026-10-31), nên không code nào đọc cờ `feature_camera_enabled` nữa. Ticket này bỏ hẳn cờ khỏi code và bảng khóa. Người dùng không thấy gì khác.

## Phạm vi

- Bỏ `FeatureFlag.CAMERA` của `:core:config` và dòng của nó trong `StagingFeatureFlagsModule`.
- Sửa test nhắc cờ theo ADR-0029, ADR-0031 (tìm bằng `git grep -n "FeatureFlag.CAMERA\|feature_camera_enabled"`).
- Sinh lại bảng khóa: `docs/remote-config-keys.md`, `docs/remote-config-template.json` mất `feature_camera_enabled`.
- Spec camera mục 8: bỏ dòng cờ, ghi lịch sử (spec chuyển `changed`, người xác nhận lại).

**Không bao gồm:**

- Xóa khóa trên Firebase staging, production: người xóa tay sau khi bản không đọc cờ đã phát hành; bản cũ vẫn đọc cờ.

## Rủi ro sơ bộ

Sửa module core `:core:config` (điểm chặn 7). Không thêm SDK, quyền, thư viện. Rủi ro cao theo điểm chặn, việc nhỏ.

## Định nghĩa hoàn thành

- [ ] Không còn `FeatureFlag.CAMERA`, `feature_camera_enabled` trong code; bảng khóa sinh lại khớp code.
- [ ] Spec camera mục 8 không còn dòng cờ, đã xác nhận lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
