---
id: ADR-0009
title: Phiên tự chạy dùng Claude Code routine, staging chạy trên GitHub Actions, phát hành production ngoài GitHub
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0009: PHIÊN TỰ CHẠY DÙNG CLAUDE CODE ROUTINE, STAGING CHẠY TRÊN GITHUB ACTIONS, PHÁT HÀNH PRODUCTION NGOÀI GITHUB

## Bối cảnh

Phiên tự chạy (xem `docs/auto-run.md`) phải chạy được khi không có ai ngồi máy, bắt đầu vào thời điểm do dev chọn, và để lại dấu vết rõ ràng. Cần chọn nơi chạy phiên, tài khoản mà phiên dùng, nơi chạy CI và cách phát hành.

Tổ chức dùng gói GitHub Free. Với repo private trên gói này, GitHub không có environment, bí mật gắn với environment hay người duyệt bắt buộc. Mọi bí mật đặt trên GitHub đều là bí mật chung của repo, và workflow chạy trên bất kỳ nhánh nào trong repo đều đọc được. Nghĩa là AI chỉ cần đẩy một workflow lên nhánh của mình là có thể chạm tới mọi bí mật đặt trên GitHub.

## Quyết định

**1. Nơi chạy phiên tự chạy: Claude Code routine** trên hạ tầng cloud của Anthropic.

- Routine có hai cách kích hoạt: **Run now** trên giao diện, và **kích hoạt qua API**. Không đặt lịch lặp cố định.
- Workflow chạy tay `start-run.yml` trên GitHub gọi API của routine. Nhờ vậy cả hai dev khởi động được phiên từ GitHub, không cần đăng nhập tài khoản Claude sở hữu routine. Mã kích hoạt của routine được lưu thành bí mật chung của repo.
- Prompt của routine yêu cầu AI thực hiện danh sách chạy đã duyệt theo `docs/auto-run.md`.

**2. Tài khoản: dùng tài khoản Claude và tài khoản GitHub hiện có** của người phụ trách, trong giai đoạn chạy thử với các app đầu tiên.

- Routine thuộc tài khoản Claude hiện có, dùng chung hạn mức với việc dùng Claude hằng ngày của tài khoản đó.
- Commit và PR do AI tạo mang tên tài khoản GitHub hiện có. Việc của AI được nhận biết qua ba dấu hiệu: nhánh có tiền tố `claude/`, commit có dòng `Co-Authored-By: Claude`, mô tả PR có đường dẫn tới phiên chạy.
- Chuyển sang tài khoản riêng khi quy trình đã ổn định, hoặc khi phiên thường xuyên dừng vì hết hạn mức. Việc chuyển chỉ cần tạo routine trên tài khoản mới và đổi mã kích hoạt trong `start-run.yml`, không phải sửa gì khác trong repo.

**3. Môi trường cloud của routine:**

- Mức mạng **Custom**: giữ danh sách tên miền mặc định, thêm `dl.google.com` để tải Android SDK.
- Setup script cài Android SDK (command-line tools, platform, build-tools). JDK 21 và Gradle có sẵn.
- Biến môi trường tăng thời gian chờ lệnh (`BASH_DEFAULT_TIMEOUT_MS`, `BASH_MAX_TIMEOUT_MS`) vì build Android có thể vượt 2 phút.
- Không đưa vào môi trường bất kỳ cấu hình production nào.

**4. Kiểm thử chia hai nơi:**

- Trong phiên tự chạy: unit test, test giao diện bằng Robolectric, screenshot test bằng Roborazzi. Các test này chạy trên JVM, không cần emulator.
- Trong GitHub Actions: toàn bộ kiểm tra trên, thêm test trên emulator.

**5. Tên nhánh:** `claude/T-NNN-<tu-khoa>`, khớp với tiền tố mặc định của routine.

**6. GitHub Actions chỉ dùng cho kiểm tra và staging:**

| Workflow | Khi nào chạy | Việc làm | Bí mật dùng |
| --- | --- | --- | --- |
| `pr-check.yml` | Mọi PR vào nhánh chính | Cổng chất lượng (xem ADR-0010) | Không |
| `main-guard.yml` | Mỗi lần nhánh chính thay đổi | Kiểm tra lại nhánh chính, xử lý khi đỏ (xem ADR-0010) | Không |
| `staging.yml` | Cuối phiên tự chạy, hoặc dev chạy tay | Build biến thể staging, phát qua Firebase App Distribution | Cấu hình staging |
| `start-run.yml` | Dev chạy tay | Kích hoạt phiên tự chạy qua API của routine | Mã kích hoạt routine |

Bí mật trên GitHub chỉ gồm cấu hình staging và mã kích hoạt routine. Nếu bị lộ, thiệt hại giới hạn trong môi trường staging và hạn mức sử dụng Claude.

**7. Phát hành production ngoài GitHub.**

- Người phụ trách phát hành build bản ký và đẩy lên Google Play từ máy của mình, bằng script `scripts/release.sh` trong repo.
- Khóa upload, file cấu hình Firebase production, mã quảng cáo thật và khóa R2 production được cất trong trình quản lý mật khẩu của nhóm. Chúng không bao giờ nằm trong repo hay trên GitHub.
- Script phát hành chỉ build từ commit đã có trên nhánh chính và đã qua `main-guard.yml`.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Routine trên cloud của Anthropic | Không cần máy riêng, dùng hạn mức gói Claude, kích hoạt linh hoạt | Môi trường giới hạn tài nguyên, không có emulator |
| Claude Code chạy trong GitHub Actions | Gần CI | Tính phí theo token qua API, tốn thêm phút Actions |
| Tài khoản riêng cho phiên tự chạy | Lịch sử tách bạch, hạn mức riêng | Tốn thêm chỗ ngồi và công quản lý tài khoản |
| Phát hành production bằng GitHub Actions | Tự động hoàn toàn | Trên gói Free, khóa phát hành thành bí mật chung, AI chạm tới được |

## Hệ quả

- **Tích cực:** không tốn thêm chi phí tài khoản; cấu hình production hoàn toàn nằm ngoài tầm với của AI; cả nhóm khởi động được phiên tự chạy.
- **Phải chấp nhận:** phiên tự chạy và việc dùng Claude hằng ngày của chủ tài khoản cùng trừ vào một hạn mức; PR của AI mang tên người; phát hành production cần một người thao tác trên máy có cấu hình production.
- **Việc AI phải tuân thủ:** đặt tên nhánh theo `claude/T-NNN-<tu-khoa>`; ghi đường dẫn phiên chạy vào mô tả PR; không cố cài hay chạy emulator trong phiên; không đọc, in ra hay chuyển đi bất kỳ bí mật nào của repo; không viết workflow mới có dùng bí mật, trừ khi đó chính là plan rủi ro cao đã được duyệt chéo; không chạm vào `scripts/release.sh`.
