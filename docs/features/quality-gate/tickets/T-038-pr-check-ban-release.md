---
id: T-038
title: pr-check build bản release khi đổi thư viện hay module
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-053 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-038-pr-check-ban-release.md
created: 2026-10-24
---

# T-038: PR-CHECK BUILD BẢN RELEASE KHI ĐỔI THƯ VIỆN HAY MODULE

## Mô tả

T-019 thêm `play-review-ktx`, qua `check.sh`, `pr-check` và `main-guard`, nhưng bản release hỏng ở R8; lỗi chỉ lộ ở `device-session` cuối phiên và làm phiên không phát được staging (L-053, PR #78). Sau ticket này, `pr-check` build `:app:assembleStagingRelease` khi PR đổi `gradle/libs.versions.toml`, một `build.gradle.kts`, `proguard-rules.pro`, `consumer-rules.pro` hay `settings.gradle.kts`, và đỏ khi R8 báo thiếu lớp.

## Phạm vi

- `.github/workflows/pr-check.yml`: bước build release có điều kiện theo file đổi.
- `docs/auto-run.md` bước 8: lỗi R8 là lỗi thật của task.

**Không bao gồm:**

- Build release cho mọi PR (tốn thời gian CI).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: sửa `.github/` (điểm chặn 7), duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
