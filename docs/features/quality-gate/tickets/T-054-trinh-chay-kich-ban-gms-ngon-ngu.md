---
id: T-054
title: Trình chạy kịch bản chạy lại tệp khi app bị giết theo Google Play services, đặt app về tiếng Anh, ghi đúng thời gian chờ của `fresh`
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là FB-20261025-03 và mục Đề xuất của báo cáo RUN-20261026-01
scenarios: []
owner: Tuấn
status: approved
risk: high
size: S
depends_on: [T-052]
sprint:
plan: features/quality-gate/plans/T-054-trinh-chay-kich-ban-gms-ngon-ngu.md
created: 2026-10-26
---

# T-054: TRÌNH CHẠY KỊCH BẢN CHẠY LẠI TỆP KHI APP BỊ GIẾT THEO GOOGLE PLAY SERVICES, ĐẶT APP VỀ TIẾNG ANH, GHI ĐÚNG THỜI GIAN CHỜ CỦA `fresh`

## Mô tả

Ở `device-session` cuối RUN-20261026-01, tệp T-022 lỗi ở bước 2 vì Google Play services trên emulator chết và hệ thống giết app theo (FB-20261025-03), không phải vì app sai. Từ T-050, T-051, chữ mặc định (`res/values/`) của mọi module là tiếng Anh, nên bộ chọn `str=` chỉ khớp khi app hiện tiếng Anh; máy để tiếng Việt thì kịch bản lỗi. Sau ticket này, một tệp mà app bị giết theo tiến trình đang chết được chạy lại một lần (lại bị giết thì `bỏ qua (môi trường)`), `fresh` đặt ngôn ngữ của app là tiếng Anh, và hai ghi chú rà soát của T-052 được sửa.

## Phạm vi

- `scripts/device-scenario.py`:
  - bước lỗi mà `logcat.txt` của phiên có dòng `Killing <pid>:<gói của app>/... depends on provider ... in dying proc <tiến trình>` sau dấu của tệp: chạy lại tệp một lần từ đầu; lần chạy lại cũng bị giết, hay không còn thời gian, thì `bỏ qua (môi trường)` (Tuấn chọn ngày 2026-10-26);
  - `fresh` đặt ngôn ngữ riêng của app là `en` từ Android 13; dưới 13, máy không để tiếng Anh thì tệp `bỏ qua (môi trường)`;
  - dòng `note` của `fresh` sau lần trả máy ghi thời gian chờ thật;
  - lệnh `device ...` rồi `fresh` trong cùng tệp cũng chờ sau khi dừng app.
- `scripts/tests/run.sh`, `scripts/tests/fake-adb`: test mới.
- `docs/ops/test-sessions.md` mục 5.
- FB-20261025-03 sang `planned`.

**Không bao gồm:**

- Sửa app để không giữ provider phông chữ của Google Play services (tắt `EmojiCompatInitializer`): Tuấn không chọn.
- Đổi bước 7 (`device locale en-US`) của kịch bản T-022 (plan mục 8, câu 3).
- `.github/`, `scripts/device-session.sh`, app.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (CI và quy trình phiên tự chạy): trình chạy kịch bản chạy ở `device-session` cuối mỗi phiên. Việc nhận ra app bị giết chỉ kiểm thật được khi Google Play services chết lại trên emulator.

## Định nghĩa hoàn thành

- [ ] Tệp mà app bị giết theo tiến trình đang chết được chạy lại một lần, kết quả là của lần chạy lại; lần chạy lại cũng bị giết thì `bỏ qua (môi trường)`.
- [ ] `fresh` đặt ngôn ngữ của app là tiếng Anh trước khi mở app (Android 13 trở lên).
- [ ] Có test tự động cho phần logic chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
