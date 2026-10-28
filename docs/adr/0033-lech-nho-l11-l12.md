---
id: ADR-0033
title: Thêm hai lệch nhỏ phiên tự làm rồi báo, L11 sửa phần dựng của test chỉ đỏ vì máy thiếu cấu hình chung và L12 sửa điểm nên sửa chỉ về chú thích hay định dạng
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-28
deciders: [Nhật]
supersedes:                  # bổ sung danh sách lệch nhỏ của ADR-0032, quyết định 1; không thay quy tắc nào của nó
superseded_by:
---

# ADR-0033: THÊM HAI LỆCH NHỎ PHIÊN TỰ LÀM RỒI BÁO, L11 SỬA PHẦN DỰNG CỦA TEST CHỈ ĐỎ VÌ MÁY THIẾU CẤU HÌNH CHUNG VÀ L12 SỬA ĐIỂM NÊN SỬA CHỈ VỀ CHÚ THÍCH HAY ĐỊNH DẠNG

> **Ghi chú 2026-10-28:** rà soát của app camera-pose (PR VGL-Studio/camera-pose#208) chỉ ra hai chỗ L11 hẹp hơn hay rộng hơn ADR-1006 của app mà nó thay. Đã sửa ở quyết định 1 và mục Việc AI phải tuân thủ: (1) danh sách cấu hình chung là ví dụ, không phải danh sách đóng, và có thêm múi giờ, ngôn ngữ; (2) chỉ sửa phần dựng của chính test đang đỏ, không sửa phần dùng chung của `scripts/tests/run.sh` hay bộ chạy test; với test có sẵn (không có điều kiện đột biến) thì phải tái hiện lỗi trên nhánh chính sạch trước khi sửa. Phần còn lại giữ nguyên.

Nguồn: L11 từ app camera-pose, ADR-1006 của app (issue #243 của dự án mẫu, PR VGL-Studio/camera-pose#203). L12 từ bài học L-115 (RUN-20261028-02, T-049).

## Bối cảnh

ADR-0032 liệt kê mười lệch nhỏ L1 tới L10 mà phiên tự làm rồi báo. Hai tình huống mới đã gặp mà không mục nào phủ:

- **Test chỉ đỏ vì máy chạy thiếu cấu hình chung.** Ở RUN-20261027-02 của app camera-pose, test của T-068 qua trên máy dev nhưng đỏ trên runner, vì runner không có cấu hình git toàn cục: `git init --bare` không có `-b main` nên nhánh mặc định khác, và `git commit-tree` thiếu tên người commit. Phiên sửa phần dựng của test (`origin/main..`, `-c user.name=... -c user.email=...`), điều test kiểm không đổi, rà soát độc lập xác nhận. Nhưng không có mục nào cho phép, nên phiên ghi loại "sửa chi tiết test". Lần sau phiên có thể dừng theo điều kiện 3 của `risk-scoring.md` mục 4, hay sửa rộng hơn mức cần. `GIT_CONFIG_GLOBAL=/dev/null` ở đầu `scripts/tests/run.sh` (L-113) đã phủ git, chưa phủ `JAVA_HOME`, `ANDROID_HOME`, `TMPDIR` hay test Gradle.
- **Điểm nên sửa chỉ về chú thích hay định dạng.** Rà soát độc lập của T-049 xếp nên sửa một dòng KDoc dài 163 ký tự. Luật Nên sửa của `docs/auto-run.md` bước 6 đòi test viết trước thất bại đúng lý do; chú thích không có test, nên phiên không sửa, để người quyết, và một việc xuống dòng phải chờ một PR riêng (L-115). Tương tự, hai ghi chú của rà soát T-048 về chú thích lỗi thời của `decide` không sửa được trong phiên.

## Quyết định

**1. Thêm hai mục vào danh sách lệch nhỏ của ADR-0032, quyết định 1.** Mọi quy tắc khác của ADR-0032 áp dụng như cho L1 tới L10: không vượt quyết định 2 của nó, ghi vào mục "Quyết định AI đã tự làm" của báo cáo (task, loại, quyết định, lý do, cách hoàn tác).

- **L11. Test chỉ đỏ vì máy chạy thiếu cấu hình chung.** Một test (của task hay test có sẵn) qua trên máy này mà đỏ trên máy khác (runner của CI, môi trường cloud, máy dev), chỉ vì máy đó thiếu hay đặt khác một cấu hình chung, ví dụ cấu hình git toàn cục, nhánh mặc định, `JAVA_HOME`, `ANDROID_HOME`, `TMPDIR`, múi giờ (`TZ`), ngôn ngữ (locale mặc định của JVM hay của máy). Khi đó sửa phần dựng của chính test đang đỏ cho nó tự đặt cấu hình cần (`git init -b main`, `git -c user.name=... -c user.email=...`, `${TMPDIR:-/tmp}`, đặt biến môi trường, múi giờ hay locale trong test và trả lại sau test). Không sửa phần dùng chung của `scripts/tests/run.sh` (đầu tệp, hàm và bản giả dùng chung), `scripts/tests/app.sh` hay bộ chạy test. Chỉ là L11 khi đủ ba điều:
  - tái hiện được lỗi trên máy của phiên bằng cách bỏ hay đổi cấu hình đó (`GIT_CONFIG_GLOBAL=/dev/null <lệnh>`, `env -u JAVA_HOME <lệnh>`, `TZ=Asia/Tokyo <lệnh>`), và test qua sau khi sửa với cùng lệnh đó; với test có sẵn, lỗi phải tái hiện được trên nhánh chính sạch (chưa có thay đổi của task), để chắc lỗi do máy chứ không do task;
  - câu khẳng định và giá trị mong đợi không đổi, không bỏ bước nào của test;
  - với test của task: đột biến bỏ code mới vẫn làm test thất bại (L-108). Test có sẵn không có điều kiện này; điều kiện tái hiện trên nhánh chính sạch ở trên thay cho nó.

  Không phải L11: sửa câu khẳng định hay giá trị mong đợi, bỏ bước của test, sửa code đang được kiểm cho test qua, thêm cấu hình vào workflow CI (`.github/` vẫn là file "Không được làm" của `CLAUDE.md`) hay vào script dựng môi trường (`scripts/cloud-setup.sh`, `scripts/local-setup.sh`) thay cho sửa test.
- **L12. Điểm nên sửa chỉ về chú thích hay định dạng.** Rà soát độc lập xếp nên sửa một điểm mà phần sửa chỉ đổi chú thích (KDoc, chú thích `//` trong code, chú thích trong script) hay định dạng (xuống dòng, thụt lề), trong file của mục 3.2 hay phần ngoài mục 3.2 được phép theo ADR-0029. Khi đó sửa mà không cần test viết trước. Chỉ là L12 khi:
  - `git diff -w` của phần sửa chỉ có dòng chú thích, hay chỉ khác khoảng trắng và chỗ xuống dòng;
  - `scripts/check.sh` vẫn qua sau khi sửa.

  Không phải L12: đổi chữ hiển thị cho người dùng, nội dung log, tên lớp, hàm hay biến, chú thích `@Suppress` hay luật lint, bất kỳ dòng code chạy nào; sửa chú thích ở file ngoài phạm vi được phép. Các điểm đó theo luật Nên sửa như cũ.

**2. Rà soát độc lập.** Phiên rà soát của task nhận danh sách lệch L11 kèm lệnh tái hiện và kết quả trước, sau khi sửa, như L1 tới L7 và L10 (ADR-0032, quyết định 4). L12 là phần sửa của một điểm nên sửa, nên đi qua vòng rà soát lại của `docs/auto-run.md` bước 6 như mọi phần sửa điểm nên sửa; vòng đó nhận thêm `git diff -w` của phần sửa. Lệch không đủ điều kiện của L11 hay L12 là mức chặn.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| ADR mới bổ sung hai mục vào danh sách của ADR-0032 (đã chọn) | Không sửa ADR đã `accepted` (`docs/CLAUDE.md`); ranh giới của từng mục rõ, rà soát kiểm được | Danh sách nằm ở hai ADR |
| Sửa thẳng ADR-0032 | Danh sách ở một chỗ | Trái quy tắc không sửa ADR đã `accepted` |
| L11: thêm cấu hình chung vào `scripts/tests/run.sh` hay workflow CI cho mọi test | Một chỗ sửa cho mọi test | `.github/` là file "Không được làm"; cấu hình chung che lỗi mà máy khác của người dùng sẽ gặp; không phủ test Gradle |
| L12: giữ như cũ, mỗi điểm chú thích một PR riêng | Người thấy trước mọi sửa | Một PR và một vòng CI cho một dòng chú thích; điểm dễ bị quên |

## Hệ quả

- **Tích cực:**
  - Phiên không dừng hay sửa rộng khi test chỉ đỏ vì máy khác cấu hình.
  - Điểm nên sửa về chú thích được sửa ngay trong PR của task.
- **Phải chấp nhận:**
  - Phần dựng của test và chú thích đổi trên nhánh chính trước khi người xem.
  - Rà soát độc lập có thêm việc kiểm lệnh tái hiện của L11 và `git diff -w` của L12.
- **Việc AI phải tuân thủ:** danh sách lệch nhỏ của ADR-0032 có thêm L11 và L12, theo mọi quy tắc khác của ADR-0032; L11 chỉ sửa phần dựng của chính test đang đỏ cho nó tự đặt cấu hình chung mà máy chạy thiếu hay đặt khác (ví dụ git toàn cục, nhánh mặc định, `JAVA_HOME`, `ANDROID_HOME`, `TMPDIR`, múi giờ, ngôn ngữ; danh sách không đóng), khi tái hiện được lỗi bằng cách bỏ hay đổi cấu hình đó, câu khẳng định và giá trị mong đợi không đổi, không bỏ bước của test, với test của task đột biến bỏ code mới vẫn làm test thất bại, với test có sẵn lỗi tái hiện được trên nhánh chính sạch; L11 không sửa phần dùng chung của `scripts/tests/run.sh`, `scripts/tests/app.sh` hay bộ chạy test; L11 không sửa câu khẳng định, không sửa code đang được kiểm, không thêm cấu hình vào workflow CI hay script dựng môi trường thay cho sửa test; L12 sửa điểm nên sửa không cần test viết trước chỉ khi `git diff -w` của phần sửa chỉ có chú thích hay khoảng trắng và chỗ xuống dòng, trong file của mục 3.2 hay phần ngoài mục 3.2 được phép theo ADR-0029, và `scripts/check.sh` vẫn qua; L12 không đổi chữ hiển thị, nội dung log, tên trong code hay dòng code chạy; mỗi lệch L11, L12 ghi vào mục "Quyết định AI đã tự làm" của báo cáo; rà soát độc lập xét L11 kèm lệnh tái hiện như L1 tới L7 và L10, xét L12 ở vòng rà soát lại của điểm nên sửa kèm `git diff -w`, lệch không đủ điều kiện là mức chặn.
