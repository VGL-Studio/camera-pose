---
id: ADR-1003
title: Phiên tự chạy được nối chỗ gọi mà task khác cùng danh sách đổi, và phải tự sửa điểm nên sửa trong phạm vi plan, đều có test viết trước
status: proposed             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-10-24
deciders: [Nhật, Tuấn]
supersedes:                  # không thay ADR nào; đổi điều kiện dừng 1 của docs/risk-scoring.md (mục 4) và dòng "Nên sửa" của docs/auto-run.md (bước 6)
superseded_by:
---

# ADR-1003: PHIÊN TỰ CHẠY ĐƯỢC NỐI CHỖ GỌI MÀ TASK KHÁC CÙNG DANH SÁCH ĐỔI, VÀ PHẢI TỰ SỬA ĐIỂM NÊN SỬA TRONG PHẠM VI PLAN, ĐỀU CÓ TEST VIẾT TRƯỚC

## Bối cảnh

Mục đích của phiên tự chạy là đi hết danh sách mà không dừng giữa chừng. Hai quy tắc hiện nay làm phiên dừng hay để lỗi lại cho người, dù việc cần làm nhỏ và đã rõ ý.

**Điều kiện dừng 1** (`docs/risk-scoring.md`, mục 4) dừng task khi cần sửa file không có trong mục 3.2 của plan (L-058). Ở RUN-20261023-04, T-020 merge trước T-017 và tạo `CameraUnfolded.kt`, file gọi thẳng `ShutterButton` và `PoseOverlay`. T-017 thêm tham số cho hai hàm đó, nên phải nối thêm `CameraUnfolded.kt`.

- File này không có trong mục 3.2 của T-017, vì lúc viết plan nó chưa tồn tại.
- Plan T-020 (mục 3) đã ghi ý định: màn máy gập giữ `MatchHint` và màu viền nút chụp.
- Phần sửa gồm 2 test viết trước và vài dòng truyền tham số (PR #81, commit `2e28f8c`, `2d04149`). Người quyết định "sửa ngay, không cần plan".

Phiên càng đông task thì càng hay gặp trường hợp này: các plan được viết song song, trước khi biết task nào merge trước.

**Dòng "Nên sửa" của bước 6** (`docs/auto-run.md`) ghi "Sửa nếu nằm trong phạm vi plan, nếu không thì ghi vào báo cáo". Câu này không nói rõ thế nào là trong phạm vi, cũng không bắt buộc phải sửa, nên phiên thường ghi vào báo cáo (L-059).

- Báo cáo RUN-20261023-04 còn 13 điểm nên sửa chưa sửa.
- Một trong số đó là lỗi người dùng gặp được: T-018 hủy composition khi đang chờ quảng cáo thì `onFinished` không được gọi, màn xem lại đứng yên.
- Trên PR #81, hai điểm nên sửa (TalkBack đọc điểm liên tục; có người mà không đoạn nào so được) được sửa trong khoảng 30 phút, có test viết trước.

Nếu không quyết, phiên tiếp tục dừng task vì chỗ gọi, và lỗi đã được rà soát tìm ra vẫn lọt lên staging.

## Quyết định

**1. Nối chỗ gọi mà task khác cùng danh sách đổi.** Phiên được sửa một file không có trong mục 3.2 khi đủ mọi điều kiện sau:

- File đó được tạo hay đổi, sau khi plan được duyệt, bởi một task khác của cùng danh sách chạy đã merge trong phiên này hay phiên trước.
- Phần sửa chỉ nối chỗ gọi: truyền tham số mới, gọi hàm mới, đổi tên theo thay đổi của chính plan, ở các file gọi thứ mà plan đổi. Phần sửa không thêm hành vi nào chưa ghi trong plan đang chạy hay plan đã tạo file đó.
- File không thuộc điểm chặn ở mục 1 của `docs/risk-scoring.md`:
  - không thuộc `.github/`, `scripts/`, `build-logic/`, `gradle/`, file `*.gradle.kts`;
  - không phải `AndroidManifest.xml`, `consumer-rules.pro`, `proguard-rules.pro`;
  - không phải file của `:core:ads`, file của `:core:analytics`, ảnh chuẩn của module khác.
- Có test viết trước cho phần nối, thất bại đúng lý do, commit trước code như bước 3.
- Phần sửa nhỏ: tổng cộng không quá 3 file thêm và 40 dòng code (không tính test) cho mỗi task.

Vượt bất kỳ điều kiện nào thì dừng như điều kiện 1 cũ.

**2. Tự sửa điểm nên sửa trong phạm vi plan.** Ở bước 6, phiên phải sửa mỗi điểm nên sửa khi đủ mọi điều kiện sau:

- Phần sửa chỉ chạm file của mục 3.2, hoặc file được phép theo quyết định 1.
- Có test viết trước, thất bại đúng lý do trước khi sửa. Không viết được test thì không sửa, ghi vào báo cáo.
- Phần sửa không đổi điều mà test của mục 4 của plan đang kiểm.

Điểm nên sửa mâu thuẫn với plan, spec hay một test của plan (ví dụ plan tự mâu thuẫn giữa mục 4 và mục 8) thì không tự sửa: ghi vào báo cáo kèm phương án để người quyết.

Sửa xong thì chạy lại bước 5 và rà soát độc lập lần nữa cho phần vừa sửa. Mỗi lần sửa tính vào giới hạn 3 lần sửa của task.

**3. Ghi lại.** Mỗi lần dùng quyết định 1 hay 2 đều phải ghi lại:

- vào nhật ký của plan: file, lý do, task đã đổi chỗ gọi, test;
- vào mô tả PR, mục "Ngoài mục 3.2" và "Đã sửa theo rà soát".

Rà soát độc lập (bước 6) xét riêng phần ngoài mục 3.2. Phần đó chỉ được giữ khi đúng là nối chỗ gọi; nếu không, rà soát báo mức chặn.

**4. Áp dụng.** ADR này chỉ có hiệu lực khi đã `accepted`. Khi đó:

- một plan rủi ro cao (điểm chặn 7) sửa `docs/risk-scoring.md` (mục 4, điều kiện 1) và `docs/auto-run.md` (bước 6, dòng "Nên sửa"; bước 10, nhật ký) để dẫn về ADR này;
- `scripts/foundation.py` thêm quy tắc vào `docs/foundation.md`.

PR soạn ADR này không sửa hai file đó.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Nối chỗ gọi có giới hạn và tự sửa điểm nên sửa, có test viết trước (đã chọn) | Phiên không dừng vì việc nhỏ đã rõ ý; lỗi rà soát tìm ra được sửa trước khi merge; ranh giới đo được (số file, số dòng, loại file, test) | Ranh giới rộng hơn mục 3.2; cần rà soát độc lập xét kỹ phần thêm; có thể tốn thêm vòng sửa |
| Giữ nguyên, dừng task và để người quyết | Ranh giới chặt nhất | Task dừng vì vài dòng nối; lỗi đã biết lọt lên staging (T-018) |
| Mỗi plan tự liệt kê trước mọi file có thể phải nối khi task khác merge trước | Không đổi điều kiện dừng | Người viết plan không biết trước file của plan chưa merge; mục 3.2 phình ra, `run-check.py` báo hai plan cùng sửa file tràn lan |
| Xếp mọi task cùng sửa một vùng thành chuỗi phụ thuộc | Không cần nối | Một task dừng thì cả chuỗi bị bỏ qua (L-015); không giải quyết điểm nên sửa |
| Cho phiên sửa mọi điểm nên sửa, kể cả ngoài phạm vi | Ít lỗi để lại nhất | Phiên tự đổi phạm vi đã duyệt; dễ vượt điều kiện dừng 3, 5, 8 |

## Hệ quả

- **Tích cực:**
  - Ít task dừng vì thứ tự merge.
  - Lỗi mà rà soát độc lập tìm ra được sửa trong phiên, với test.
  - Báo cáo phiên ngắn hơn, ít việc để lại cho người.
  - L-058, L-059 có cách áp dụng.
- **Phải chấp nhận:**
  - Một task có thể chạm tới 3 file ngoài mục 3.2.
  - Rà soát độc lập phải xét thêm phần đó.
  - Mỗi lần sửa theo rà soát tốn thêm thời gian bước 5 và một lần rà soát.
  - Quy tắc của dự án mẫu (`risk-scoring.md`) khác ở app này cho tới khi dự án mẫu nhận đề xuất (`docs/ops/from-app.md`).
- **Việc AI phải tuân thủ:** chưa áp dụng khi ADR này còn `proposed` và `docs/risk-scoring.md`, `docs/auto-run.md` chưa dẫn tới nó; ở app này, phiên được sửa file ngoài mục 3.2 chỉ để nối chỗ gọi mà task khác của cùng danh sách đã tạo hay đổi sau khi plan được duyệt; phần nối có test viết trước, tối đa 3 file và 40 dòng code mỗi task, không chạm file của điểm chặn ở mục 1 của `risk-scoring.md`; ở bước 6, phiên phải sửa điểm nên sửa nằm trong file được phép, có test viết trước thất bại đúng lý do, không đổi điều test của plan kiểm; điểm nên sửa mâu thuẫn với plan, spec hay test của plan thì ghi vào báo cáo kèm phương án, không tự sửa; mỗi phần ngoài mục 3.2 và mỗi lần sửa theo rà soát được ghi vào nhật ký plan và mô tả PR, và được rà soát độc lập xét riêng; mọi điều kiện dừng khác của `risk-scoring.md` vẫn áp dụng.
