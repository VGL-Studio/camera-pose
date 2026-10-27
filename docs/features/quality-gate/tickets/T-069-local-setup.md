---
id: T-069
title: "`scripts/local-setup.sh`: cài JDK và Android SDK trên máy dev để chạy thử bằng đúng `scripts/check.sh`"
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là đề xuất 4e sau RUN-20261027-01, Tuấn chọn ngày 2026-10-27
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-069-local-setup.md
created: 2026-10-27
---

# T-069: `SCRIPTS/LOCAL-SETUP.SH` CÀI JDK VÀ ANDROID SDK TRÊN MÁY DEV

## Mô tả

Plan nào dùng kỹ thuật mới (tiêu chí E từ 2) hay có test của script phải chạy thử trước khi duyệt bằng đúng `scripts/check.sh` (L-075, `docs/CLAUDE.md`). Máy dev hiện không có JDK: mỗi lần chạy thử ngày 2026-10-26 và 27 (T-064, T-065, PR #179) đều phải tự tải Temurin và Android SDK vào thư mục tạm, hoặc chỉ chạy được một khối test thay cho `check.sh`. Ticket thêm một lệnh cài sẵn mọi thứ `check.sh` cần, cùng phiên bản với môi trường cloud, để lần chạy thử trước khi duyệt giống lần chạy của phiên.

## Phạm vi

- `scripts/local-setup.sh` (macOS, Linux; không cần sudo): JDK Temurin `JDK_RELEASE` và gói Android SDK đọc từ `scripts/cloud-setup.sh` (một nguồn phiên bản), kiểm mã băm của mọi file tải về, chạy lại không tải lại, ghi `env.sh` để `source`.
- `--check` (chỉ báo đã có gì, không tải), `--print-versions`, `--prefix`, `--sdk` (đường dẫn tuyệt đối). Chỉ máy dev: từ chối chạy trong môi trường cloud (có `~/.cloud-setup-version`).
- Test của script trong `scripts/tests/run.sh` (curl, Adoptium, danh sách gói của Google và `sdkmanager` giả).
- Ghi lệnh vào `CLAUDE.md` (mục Lệnh), quy tắc chạy thử của `docs/CLAUDE.md` và `docs/ops/setup-new-app.md` phần 1 (máy không cài Android Studio).

**Không bao gồm:**

- Emulator, system image, Android Studio.
- Đổi `scripts/cloud-setup.sh` (file dán vào môi trường cloud, của dự án mẫu).
- Init script mirror Maven Central (chỉ cần ở cloud, `cloud-setup.sh`).

## Rủi ro sơ bộ

Cao vì chạm điểm chặn 7 (quy trình: quy tắc chạy thử trong `docs/CLAUDE.md`). Script chỉ chạy trên máy dev, không chạy trong CI hay phiên; test của nó chạy trong `scripts/tests/run.sh` như mọi script.

## Định nghĩa hoàn thành

- [ ] `scripts/local-setup.sh` cài được JDK và SDK trên máy dev macOS vào thư mục người dùng; chạy lần hai không tải gì.
- [ ] Sau `source <prefix>/env.sh`, `scripts/check.sh` qua trên máy dev.
- [ ] Tải về sai mã băm thì dừng, không để lại thư mục cài dở.
- [ ] Test của script qua trong `scripts/tests/run.sh`; CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] `CLAUDE.md`, `docs/CLAUDE.md` và `docs/ops/setup-new-app.md` chỉ tới lệnh này.
