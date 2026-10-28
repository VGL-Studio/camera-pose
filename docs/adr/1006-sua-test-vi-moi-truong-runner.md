---
id: ADR-1006
title: Phiên tự sửa chi tiết dựng môi trường của test khi test chỉ đỏ trên runner vì máy không có cấu hình chung, rồi báo người sau
status: accepted             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-10-28
deciders: [Nhật]
supersedes:                  # bổ sung ADR-0032 (thêm mục L11), không thay quy tắc nào của nó
superseded_by:
---

# ADR-1006: PHIÊN TỰ SỬA CHI TIẾT DỰNG MÔI TRƯỜNG CỦA TEST KHI TEST CHỈ ĐỎ TRÊN RUNNER VÌ MÁY KHÔNG CÓ CẤU HÌNH CHUNG, RỒI BÁO NGƯỜI SAU

## Bối cảnh

ADR-0032 liệt kê L1 tới L10, các lệch nhỏ mà phiên tự chạy được tự làm rồi báo. Ở RUN-20261027-02, T-068 có hai lần sửa không thuộc mục nào trong đó:

- Khối test `save-branch patch` dựng một kho trần giả bằng `git init --bare` không có `-b main`. Khi git không đặt `init.defaultBranch` (máy cloud, runner), bản sao mới không có nhánh `main` cục bộ, nên `git log main..` lỗi. Phiên đổi thành `origin/main..` (commit `9e63b5f`).
- Lệnh `git commit-tree` trong kho giả cần tên người commit. Runner không có cấu hình git toàn cục, nên CI vòng 1 đỏ `fatal: empty ident name`. Phiên thêm `-c user.name=t -c user.email=t@example.com` (commit `37d8e5a`).

Cả hai lần, điều test kiểm không đổi (rà soát độc lập xác nhận). Phiên ghi loại là "Sửa chi tiết test (auto-run.md bước 4)", vì không có mục L nào. Báo cáo đề xuất thêm một mục (RUN-20261027-02, Đề xuất cho buổi họp tiếp theo, ý 1). Bài học L-087 cùng lớp lỗi; dự án mẫu đã đặt `GIT_CONFIG_GLOBAL=/dev/null` cùng tên người commit ở đầu `scripts/tests/run.sh`, nên lỗi này ít gặp hơn nhưng vẫn có thể xảy ra với biến môi trường khác (`JAVA_HOME`, `ANDROID_HOME`, `TMPDIR`) hay với test Gradle.

Không có mục rõ ràng thì lần sau phiên có thể dừng theo điều kiện 3 của `risk-scoring.md` mục 4 (sửa test ngoài plan), hay ngược lại tự sửa rộng hơn mức cần.

## Quyết định

**Thêm mục L11 vào danh sách lệch nhỏ của ADR-0032, quyết định 1**, cho app này:

- **L11. Test chỉ đỏ vì máy chạy thiếu cấu hình chung.** Test của plan qua trên máy của phiên mà đỏ trên runner (hay ngược lại), và nguyên nhân là phần dựng môi trường của chính test dựa vào cấu hình chung của máy: cấu hình git toàn cục (tên người commit, `init.defaultBranch`), biến môi trường (`JAVA_HOME`, `ANDROID_HOME`, `TMPDIR`), múi giờ hay ngôn ngữ. Khi đó sửa phần dựng của test cho nó tự đặt cấu hình cần (`-b main`, `-c user.name=... -c user.email=...`, biến môi trường riêng của khối test, `origin/main` thay cho `main`), trong PR của task.
  - Chỉ là L11 khi: tái hiện được lỗi trên máy của phiên bằng cách bỏ cấu hình đó (ví dụ `GIT_CONFIG_GLOBAL=/dev/null bash scripts/tests/run.sh`, `env -u JAVA_HOME ...`); câu khẳng định, dữ liệu mong đợi và lệnh mà test kiểm không đổi; và test vẫn thất bại khi tạm bỏ dòng code mới của task (đột biến, L-108), ghi kết quả vào nhật ký plan.
  - Không phải L11: sửa câu khẳng định hay giá trị mong đợi, bỏ một bước của test, sửa code của script hay app cho test qua, sửa `scripts/tests/run.sh` ngoài khối test của task, thêm cấu hình vào workflow CI. Những việc này theo điều kiện dừng như cũ.

L11 tính như L1 tới L7 và L10: ghi vào mục "Quyết định AI đã tự làm" của báo cáo (loại `ADR-1006/L11`), và rà soát độc lập ở bước 6 xét lệch đó có đúng mục không.

Nhóm cũng xác nhận bảy quyết định tự làm của RUN-20261027-02 (L1, L2, L3, L4, hai lần sửa chi tiết test của T-068, điểm cắt sau T-068) là đúng ý. Hai lần sửa của T-068 từ nay thuộc L11.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Thêm L11 với điều kiện tái hiện và đột biến (đã chọn) | Phiên không dừng vì lỗi môi trường của test; ranh giới kiểm được | Thêm một mục cho rà soát |
| Để trong "sửa chi tiết test" của `docs/auto-run.md` bước 4 như cũ | Không thêm quy tắc | Không rõ được làm tới đâu; rà soát không có mục để xét |
| Chỉ dựa vào `GIT_CONFIG_GLOBAL=/dev/null` của `scripts/tests/run.sh` | Đã có từ dự án mẫu | Không phủ biến môi trường khác và test Gradle |

## Hệ quả

- **Tích cực:**
  - Phiên sửa được lỗi kiểu "qua trên máy, đỏ trên runner" mà không dừng task hay chờ người.
  - Hai lần sửa đã làm ở RUN-20261027-02 có mục để ghi.
- **Phải chấp nhận:**
  - Thêm một mục cho rà soát độc lập.
  - Đây là ADR của app; dự án mẫu cần nhận lại thành một mục của ADR-0032 (`docs/ops/from-app.md`).
- **Việc AI phải tuân thủ:** test chỉ đỏ trên runner hay trên máy của phiên vì phần dựng của test dựa vào cấu hình chung của máy (git toàn cục, `JAVA_HOME`, `ANDROID_HOME`, `TMPDIR`, múi giờ, ngôn ngữ) thì sửa phần dựng của test cho nó tự đặt cấu hình cần, ghi `ADR-1006/L11`; chỉ là L11 khi tái hiện được lỗi bằng cách bỏ cấu hình đó, câu khẳng định và giá trị mong đợi không đổi, và đột biến bỏ code mới vẫn làm test thất bại; không sửa câu khẳng định, không bỏ bước của test, không sửa code cho test qua, không thêm cấu hình vào workflow CI thay cho L11; rà soát độc lập xét lệch L11 như L1 tới L7 và L10.
