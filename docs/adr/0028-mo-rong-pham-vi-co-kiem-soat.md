---
id: ADR-0028
title: Phiên tự chạy được nối chỗ gọi mà task khác cùng danh sách đổi, và phải tự sửa điểm nên sửa trong phạm vi plan, đều có test viết trước
status: superseded           # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-24
deciders: [Nhật, Tuấn]
supersedes:                  # không thay ADR nào; đổi điều kiện dừng 1 của docs/risk-scoring.md (mục 4) và dòng "Nên sửa" của docs/auto-run.md (bước 6)
superseded_by: ADR-0029
---

# ADR-0028: PHIÊN TỰ CHẠY ĐƯỢC NỐI CHỖ GỌI MÀ TASK KHÁC CÙNG DANH SÁCH ĐỔI, VÀ PHẢI TỰ SỬA ĐIỂM NÊN SỬA TRONG PHẠM VI PLAN, ĐỀU CÓ TEST VIẾT TRƯỚC

> **Ghi chú 2026-10-25:** ADR-0029 thay ADR này. Quyết định 1 (nối chỗ gọi, tối đa 3 file và 40 dòng) được nới thành: phần ngoài mục 3.2 cần cho task thì sửa tiếp, merge, báo sau. Quyết định 2 (tự sửa điểm nên sửa) giữ nguyên, chép ở ADR-0029, quyết định 3.

## Bối cảnh

Mục đích của phiên tự chạy là đi hết danh sách mà không dừng giữa chừng. Hai quy tắc làm phiên dừng hay để lỗi lại cho người, dù việc cần làm nhỏ và đã rõ ý. Cả hai được thấy ở app camera-pose (issue #132, bài học L-058, L-059 của app; ADR-1003 của app).

**Điều kiện dừng 1** (`docs/risk-scoring.md`, mục 4) dừng task khi cần sửa file không có trong mục 3.2 của plan. Ở app, một task merge trước và tạo file màn hình máy gập, file này gọi thẳng hai hàm giao diện. Task sau thêm tham số cho hai hàm đó, nên phải nối thêm file vừa tạo:

- File này không có trong mục 3.2 của task sau, vì lúc viết plan nó chưa tồn tại.
- Plan của task trước đã ghi ý định giữ hành vi đó ở màn máy gập.
- Phần sửa gồm 2 test viết trước và vài dòng truyền tham số. Người quyết định "sửa ngay, không cần plan".

Phiên càng đông task thì càng hay gặp trường hợp này: các plan được viết song song, trước khi biết task nào merge trước.

**Dòng "Nên sửa" của bước 6** (`docs/auto-run.md`) ghi "Sửa nếu nằm trong phạm vi plan, nếu không thì ghi vào báo cáo". Câu này không nói rõ thế nào là trong phạm vi, cũng không bắt buộc phải sửa, nên phiên thường ghi vào báo cáo.

- Một báo cáo phiên của app còn 13 điểm nên sửa chưa sửa.
- Một trong số đó là lỗi người dùng gặp được: hủy composition khi đang chờ quảng cáo thì callback kết thúc không được gọi, màn hình đứng yên.
- Hai điểm nên sửa khác được sửa trong khoảng 30 phút, có test viết trước.

Nếu không quyết, phiên tiếp tục dừng task vì chỗ gọi, và lỗi đã được rà soát tìm ra vẫn lọt lên staging.

## Quyết định

**1. Nối chỗ gọi mà task khác cùng danh sách đổi.** Phiên được sửa một file không có trong mục 3.2 khi đủ mọi điều kiện sau:

- File đó được tạo hay đổi, sau khi plan được duyệt, bởi một task khác của cùng danh sách chạy đã merge trong phiên này hay phiên trước. Task đang làm được chuyển sang từ một danh sách chạy trước thì task đã merge của danh sách đó cũng tính là cùng danh sách; nhật ký plan ghi mã danh sách chạy của task đã đổi chỗ gọi.
- Phần sửa chỉ nối chỗ gọi: truyền tham số mới, gọi hàm mới, đổi tên theo thay đổi của chính plan, ở các file gọi thứ mà plan đổi. Phần sửa không thêm hành vi nào chưa ghi trong plan đang chạy hay plan đã tạo file đó.
- Phần sửa nằm ngoài mọi điểm chặn ở mục 1 của `docs/risk-scoring.md` (cả chín điểm), xét cả file lẫn nội dung sửa:
  - không sửa file của bất kỳ module `:core:*` nào (điểm 7), file cấu hình build (`*.gradle.kts`, `build-logic/`, `gradle/`, `consumer-rules.pro`, `proguard-rules.pro`), CI và quy trình (`.github/`, `scripts/`), `AndroidManifest.xml` (điểm 3);
  - không đổi vị trí, định dạng hay nhịp quảng cáo, sự kiện phân tích hay dữ liệu gửi ra khỏi máy, hộp thoại đồng ý, thứ tự khởi tạo SDK, liên kết ra ngoài app (điểm 1, 4, 5, 6), kể cả khi dòng đó nằm trong file của feature;
  - không thêm hay đổi đường dẫn file trên R2 (`AssetPath`), khóa DataStore hay lược đồ dữ liệu cục bộ (điểm 8, 9): các điểm này không đếm được theo file, nên phiên xét từng dòng sửa;
  - không sửa ảnh chuẩn screenshot của module khác.

  Phần nối chạm một điểm chặn, dù chỉ một dòng, thì dừng theo điều kiện 1.
- Có test viết trước cho phần nối, thất bại đúng lý do, commit trước code như bước 3.
- Phần sửa nhỏ: tổng cộng không quá 3 file thêm và 40 dòng code cho mỗi task. Đếm bằng `git diff --numstat origin/main...HEAD -- <các file ngoài mục 3.2>`, cộng dòng thêm và dòng xóa, không tính file test (`src/test/`, `src/androidTest/`, `scripts/tests/`). Mô tả PR ghi lệnh đó và kết quả.

Vượt bất kỳ điều kiện nào thì dừng như điều kiện 1 cũ.

**2. Tự sửa điểm nên sửa trong phạm vi plan.** Ở bước 6, phiên phải sửa mỗi điểm nên sửa khi đủ mọi điều kiện sau:

- Phần sửa chỉ chạm file của mục 3.2, hoặc file được phép theo quyết định 1.
- Có test viết trước, thất bại đúng lý do trước khi sửa. Không viết được test thì không sửa, ghi vào báo cáo.
- Phần sửa không đổi điều mà test của mục 4 của plan đang kiểm.

Điểm nên sửa mâu thuẫn với plan, spec hay một test của plan (ví dụ plan tự mâu thuẫn giữa mục 4 và mục 8) thì không tự sửa: ghi vào báo cáo kèm phương án để người quyết.

Sửa xong thì chạy lại bước 5 và rà soát độc lập lần nữa cho phần vừa sửa. Giới hạn 3 lần sửa tính riêng cho từng điểm nên sửa, như "3 lần sửa cho cùng một lỗi", không cộng dồn giữa các điểm của một task. Một điểm hết 3 lần sửa thì hoàn tác phần sửa của điểm đó, ghi vào báo cáo, rồi làm tiếp các điểm còn lại.

Mỗi lần sửa lại cần một lần rà soát, và lần rà soát đó có thể tìm ra điểm nên sửa mới, nên cần thêm giới hạn tổng: mỗi task có tối đa 2 vòng rà soát lại vì điểm nên sửa. Sau vòng thứ 2, mọi điểm nên sửa còn lại, kể cả điểm mới mà vòng đó tìm ra, không sửa nữa mà ghi vào báo cáo để người quyết. Điểm mức chặn ở bất kỳ vòng nào vẫn xử lý như mức chặn (quay lại bước 4, giới hạn 3 lần sửa cho cùng một lỗi).

**3. Ghi lại.** Mỗi lần dùng quyết định 1 hay 2 đều phải ghi lại:

- vào nhật ký của plan: file, lý do, task đã đổi chỗ gọi, test;
- vào mô tả PR, mục "Ngoài mục 3.2" và "Đã sửa theo rà soát".

Rà soát độc lập (bước 6) xét riêng phần ngoài mục 3.2. Phần đó chỉ được giữ khi đúng là nối chỗ gọi; nếu không, rà soát báo mức chặn.

**4. Áp dụng.** `docs/risk-scoring.md` (mục 4, điều kiện 1 và đoạn "Không phải dừng") và `docs/auto-run.md` (bước 4, 5, 6, 10 và nội dung giao việc cho phiên con) dẫn về ADR này, cùng PR với ADR.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Nối chỗ gọi có giới hạn và tự sửa điểm nên sửa, có test viết trước (đã chọn) | Phiên không dừng vì việc nhỏ đã rõ ý; lỗi rà soát tìm ra được sửa trước khi merge; ranh giới đo được (số file, số dòng, loại file, test) | Ranh giới rộng hơn mục 3.2; cần rà soát độc lập xét kỹ phần thêm; có thể tốn thêm vòng sửa |
| Giữ nguyên, dừng task và để người quyết | Ranh giới chặt nhất | Task dừng vì vài dòng nối; lỗi đã biết lọt lên staging |
| Mỗi plan tự liệt kê trước mọi file có thể phải nối khi task khác merge trước | Không đổi điều kiện dừng | Người viết plan không biết trước file của plan chưa merge; mục 3.2 phình ra, `run-check.py` báo hai plan cùng sửa file tràn lan |
| Xếp mọi task cùng sửa một vùng thành chuỗi phụ thuộc | Không cần nối | Một task dừng thì cả chuỗi bị bỏ qua; không giải quyết điểm nên sửa |
| Cho phiên sửa mọi điểm nên sửa, kể cả ngoài phạm vi | Ít lỗi để lại nhất | Phiên tự đổi phạm vi đã duyệt; dễ vượt điều kiện dừng 3, 5, 8 |

## Hệ quả

- **Tích cực:**
  - Ít task dừng vì thứ tự merge.
  - Lỗi mà rà soát độc lập tìm ra được sửa trong phiên, với test.
  - Báo cáo phiên ngắn hơn, ít việc để lại cho người.
- **Phải chấp nhận:**
  - Một task có thể chạm tới 3 file ngoài mục 3.2.
  - Rà soát độc lập phải xét thêm phần đó.
  - Mỗi lần sửa theo rà soát tốn thêm thời gian bước 5 và một lần rà soát.
- **Việc AI phải tuân thủ:** chỉ sửa file ngoài mục 3.2 để nối chỗ gọi mà task khác của cùng danh sách đã tạo hay đổi sau khi plan được duyệt (tính cả task đã merge ở danh sách trước khi task được chuyển sang); phần nối có test viết trước, tối đa 3 file và 40 dòng code mỗi task đếm bằng `git diff --numstat` không tính test, nằm ngoài mọi điểm chặn ở mục 1 của `risk-scoring.md` (gồm mọi module `:core:*`, đường dẫn R2, khóa DataStore, xét từng dòng sửa); ở bước 6, phải sửa điểm nên sửa nằm trong file được phép, có test viết trước thất bại đúng lý do, không đổi điều test của plan kiểm; giới hạn 3 lần sửa tính riêng cho từng điểm nên sửa, và mỗi task tối đa 2 vòng rà soát lại vì điểm nên sửa, sau đó điểm còn lại ghi vào báo cáo; điểm nên sửa mâu thuẫn với plan, spec hay test của plan thì ghi vào báo cáo kèm phương án, không tự sửa; mỗi phần ngoài mục 3.2 và mỗi lần sửa theo rà soát được ghi vào nhật ký plan và mô tả PR, và được rà soát độc lập xét riêng; mọi điều kiện dừng khác của `risk-scoring.md` vẫn áp dụng.
