---
id: T-089
title: Kịch bản emulator đóng interstitial `inter_pose_detail` trước khi chờ camera; ghi đúng nghĩa `consent decline`
feature: poses
spec: features/poses/spec.md
scenarios: []                # không đổi hành vi của app; sửa kịch bản emulator theo FB-20261031-01
owner: Nhật
status: approved              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: low                    # đánh giá sơ bộ: low | medium | high
size: S                      # S | M | L, xem giải thích ở cuối
depends_on: [T-083]          # các ticket phải xong trước, ví dụ [T-010]
sprint:                      # điền khi được chọn vào sprint
plan: features/poses/plans/T-089-kich-ban-emulator-qua-interstitial.md
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-01
---

# T-089: KỊCH BẢN EMULATOR ĐÓNG INTERSTITIAL `INTER_POSE_DETAIL` TRƯỚC KHI CHỜ CAMERA; GHI ĐÚNG NGHĨA `CONSENT DECLINE`

## Mô tả

Từ T-079 (PR #257), bấm "Chụp với tư thế này" ở chi tiết có thể hiện interstitial `inter_pose_detail` trước camera (spec ads KB-4, staging bật vị trí). Ba kịch bản emulator viết trước đó chờ camera ngay sau lần bấm, nên lỗi ở mọi `device-session` từ RUN-20261031-01 (FB-20261031-01), và phần sau bước lỗi không còn được kiểm. Ticket này sửa ba kịch bản cho đi qua interstitial khi nó hiện, và ghi đúng nghĩa của lệnh `consent decline` trong tài liệu kịch bản.

## Phạm vi

- `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`, `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`: sau khi bấm "Chụp với tư thế này", đóng interstitial nếu nó hiện (bằng lệnh có sẵn của `scripts/device-scenario.py`, ví dụ `back` hay `tap_if` nút đóng; plan chạy thử để chọn), rồi mới chờ `camera_screen`. Interstitial không hiện (khóa tắt, nhịp 45 giây) thì kịch bản vẫn đi được.
- Sau T-083, khối đi qua luồng giới thiệu ở đầu ba tệp đã là các vòng `wait id=onboarding_next enabled=true | id=onboarding_enable_camera | id=camera_screen 15` / `tap_if id=onboarding_next enabled=true` (bộ lọc `enabled=true` của `scripts/device-scenario.py`, plan T-083): giữ nguyên khối đó, chỉ sửa đoạn sau "Chụp với tư thế này".
- `docs/ops/test-sessions.md` mục 5: ghi rõ `consent decline` chạm "Do not consent" nhưng bộ đồng ý UMP vẫn cho nạp quảng cáo không cá nhân hóa (log `Consent path: GRANTED`), nên kịch bản sau `consent decline` vẫn có thể gặp quảng cáo (spec onboarding câu 9, RUN-20261031-02).
- FB-20261031-01 chuyển `planned` khi có plan (người phân loại phản hồi, `docs/CLAUDE.md` mục Khi xử lý phản hồi).

**Không bao gồm:**

- Hành vi quảng cáo của app (đúng spec, không đổi).
- Kịch bản và test của luồng giới thiệu có thẻ native (T-083).
- Thêm lệnh mới cho `scripts/device-scenario.py`: chỉ khi plan chạy thử thấy lệnh có sẵn không đủ, và khi đó plan ghi rõ (script dùng chung với dự án mẫu, đề xuất qua `docs/ops/from-app.md`).

## Rủi ro sơ bộ

Thấp: chỉ sửa tệp kịch bản và tài liệu, không đổi code app. Kịch bản chỉ chạy ở `device-session` cuối phiên, không "thất bại trước" được; plan kiểm bằng `scripts/device-scenario.py check` và một lần chạy `device-session.yml` sau merge.

## Định nghĩa hoàn thành

- [ ] Ba kịch bản đạt ở `device-session` sau merge, kể cả phần sau bước lỗi cũ.
- [ ] `scripts/device-scenario.py check` qua cho ba tệp.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] FB-20261031-01 được đóng theo ticket này.

<!--
Cỡ task. Plan dựa vào cỡ để ghi token_estimate; số token thật theo cỡ ở docs/metrics/token-usage.csv (docs/ops/token-usage.md):
S: thay đổi nhỏ trong một module, lặp lại mẫu có sẵn.
M: một màn hình hoặc một luồng xử lý mới trong một module.
L: nhiều màn hình hoặc nhiều phần. Ticket cỡ L nên cân nhắc tách nhỏ, vì phải chạy xong trong một lượt của phiên tự chạy.
-->
