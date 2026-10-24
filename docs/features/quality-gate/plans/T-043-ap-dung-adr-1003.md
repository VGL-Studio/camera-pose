---
ticket: T-043
title: Áp dụng ADR-1003 vào thang rủi ro và quy trình phiên tự chạy
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: []
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-043: ÁP DỤNG ADR-1003 VÀO THANG RỦI RO VÀ QUY TRÌNH PHIÊN TỰ CHẠY

## 1. Tóm tắt

Sửa `docs/risk-scoring.md` (mục 4, điều kiện 1; mục 5) và `docs/auto-run.md` (bước 6, bước 10) để dẫn về ADR-1003: phiên được nối chỗ gọi ngoài mục 3.2 khi task khác cùng danh sách đã tạo hay đổi file đó sau khi plan được duyệt, trong giới hạn của ADR; ở bước 6 phiên phải sửa điểm nên sửa nằm trong file được phép, có test viết trước; mọi phần ngoài mục 3.2 và mọi lần sửa theo rà soát được ghi vào nhật ký, mô tả PR và được rà soát độc lập xét riêng. Chỉ đổi tài liệu, chép đúng các vế của ADR, không thêm hay nới thêm.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là ADR-1003 (`docs/adr/1003-mo-rong-pham-vi-co-kiem-soat.md`, mục Quyết định 1 đến 4), từ L-058 và L-059.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-058 | Khi file cần sửa nằm ngoài mục 3.2 nhưng do task khác cùng danh sách tạo hay đổi sau khi plan được duyệt, và phần sửa chỉ nối chỗ gọi, thì phiên không dừng | Điều kiện 1 có ngoại lệ dẫn tới ADR-1003 với đủ giới hạn: test viết trước; tối đa 3 file và 40 dòng code mỗi task; không chạm file điểm chặn mục 1; ghi nhật ký và mô tả PR; rà soát độc lập xét riêng, không đúng là nối chỗ gọi thì báo chặn |
| L-058 | Ngoài các trường hợp trên, điều kiện 1 vẫn dừng task | Câu điều kiện 1 hiện có giữ nguyên, ngoại lệ là đoạn riêng giống đoạn "Không phải dừng: phương án dự phòng" |
| L-059 | Khi rà soát độc lập báo điểm nên sửa nằm trong file được phép thì phiên sửa, có test viết trước thất bại đúng lý do | Dòng "Nên sửa" của bảng bước 6 đổi từ "sửa nếu nằm trong phạm vi plan" thành bắt buộc, kèm điều kiện; mỗi lần sửa tính vào giới hạn 3 lần sửa |
| L-059 | Điểm nên sửa mâu thuẫn với plan, spec hay test của plan thì không tự sửa | Ghi vào báo cáo kèm phương án (như TalkBack của PR #81) |

Thiết kế tham chiếu: Không có thiết kế riêng (tài liệu quy trình, docs/ops/design-workflow.md).

ADR liên quan: ADR-1003 (quy tắc chép vào); ADR-1002 (không đổi).

## 3. Giải pháp

### 3.1. Cách làm

1. `docs/risk-scoring.md`, mục 4: thêm sau điều kiện 9 (trước đoạn "Không phải dừng: chuyển sang phương án dự phòng") đoạn **"Không phải dừng: nối chỗ gọi mà task khác cùng danh sách đổi (ADR-1003)"** liệt kê đúng các giới hạn ở mục Quyết định 1 và 3 của ADR. Điều kiện 1 thêm cuối câu: "(trừ phần nối chỗ gọi theo ADR-1003, dưới)". Đoạn phương án dự phòng giữ nguyên câu "không bao giờ là lý do để vượt điều kiện 1": thêm "(ngoại lệ duy nhất là nối chỗ gọi theo ADR-1003)".
2. `docs/risk-scoring.md`, mục 5: một dòng lịch sử ngày duyệt, lý do L-058 (PR #81).
3. `docs/auto-run.md`, bước 6:
   - nội dung rà soát thêm gạch đầu dòng: "Phần ngoài mục 3.2 (ADR-1003): đúng là nối chỗ gọi mà task khác cùng danh sách đổi, trong giới hạn; nếu không, mức chặn";
   - dòng "Nên sửa" của bảng: "Sửa (ADR-1003) khi nằm trong file của mục 3.2 hay phần nối chỗ gọi được phép: test viết trước thất bại đúng lý do, không đổi điều test của plan kiểm; tính vào giới hạn 3 lần sửa. Mâu thuẫn với plan, spec hay test của plan: ghi vào báo cáo kèm phương án, không sửa.";
   - thêm một câu sau bảng: phần sửa theo rà soát và phần ngoài mục 3.2 ghi trong mô tả PR, mục "Ngoài mục 3.2" và "Đã sửa theo rà soát".
4. `docs/auto-run.md`, bước 10: gạch đầu dòng mới: nhật ký ghi phần ngoài mục 3.2 (file, lý do, task đã đổi chỗ gọi, test) và từng điểm đã sửa theo rà soát.

Câu chữ chép từ ADR-1003 mục Quyết định và mục Việc AI phải tuân thủ; không thêm giới hạn hay nới thêm.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `docs/risk-scoring.md` | Mục 4: điều kiện 1, đoạn mới nối chỗ gọi, câu của đoạn phương án dự phòng; mục 5: dòng lịch sử |
| Sửa | `docs/auto-run.md` | Bước 6: gạch đầu dòng rà soát, dòng "Nên sửa", câu về mô tả PR; bước 10: gạch đầu dòng nhật ký |
| Sửa | `docs/lessons.md` | L-058, L-059: dòng Áp dụng khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-043-ap-dung-adr-1003.md` | Trạng thái theo phiên |

Nơi khác đọc hai tài liệu (`grep -rn "risk-scoring.md\|auto-run.md" scripts/ .github/`): `scripts/plan-check.py` chỉ dẫn mục 1 và mục 4 điều kiện 2 của `risk-scoring.md`, bước 5 của `auto-run.md` trong thông báo; `scripts/resume-check.py` dẫn mục 3, 4 của `auto-run.md` trong chú thích. Không script nào đọc nội dung mục 4 điều kiện 1, bước 6 hay bước 10, nên không ảnh hưởng. Các file khác chỉ nhắc `docs/auto-run.md` trong chú thích hay thông báo, dẫn tới mục và bước mà plan không sửa: `.github/workflows/device-session.yml` (mục 4), `.github/workflows/pr-check.yml` (bước 8), `.github/workflows/resume-run.yml` (mục 2, mục 4), `.github/workflows/start-run.yml` (mục 1), `scripts/cloud-env-check.sh` (mục 2), `scripts/cloud-setup.sh` (bước 5), `scripts/merge-if-green.sh` (mục 1, mục 3 "Khi dừng"), `scripts/status.py` (mục 3), `scripts/wait-run.sh` (bước 4, bước 8). `scripts/status.py` đọc dòng Trạng thái của `docs/lessons.md`; plan chỉ thêm chữ vào dòng Áp dụng của L-058, L-059, không đổi tiêu đề hay dòng Trạng thái. `scripts/foundation.py` đọc `docs/adr/`, không đọc hai file này; quy tắc của ADR-1003 đã có trong `docs/foundation.md` (PR duyệt ADR).

Phối hợp với các plan cùng sửa `docs/auto-run.md` (L-014): T-036 (mục 2, Kiểm tra đầu phiên), T-037 (bước 3, 4, 7), T-038 và T-039 (bước 8), T-040 (bước 9). Plan này chỉ sửa bước 6 và bước 10, không trùng đoạn nào. Câu về mô tả PR đặt ở bước 6, không ở bước 7 (của T-037). Không plan nào khác sửa `docs/risk-scoring.md`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| L-058, ngoại lệ có đủ giới hạn | Kiểm tra tài liệu | Đọc đối chiếu: đoạn mới của `risk-scoring.md` có đủ năm vế của ADR-1003 mục Quyết định 1 (task khác cùng danh sách, sau khi plan duyệt; chỉ nối chỗ gọi; test viết trước; tối đa 3 file và 40 dòng; không chạm file điểm chặn mục 1) và vế ghi nhật ký, rà soát xét riêng. Rà soát độc lập kiểm từng vế |
| L-058, điều kiện 1 vẫn dừng ở trường hợp khác | Kiểm tra tài liệu | Câu điều kiện 1 hiện có giữ nguyên chữ, chỉ thêm phần dẫn |
| L-059, nên sửa bắt buộc có điều kiện | Kiểm tra tài liệu | Dòng "Nên sửa" có: file được phép, test viết trước thất bại đúng lý do, không đổi điều test của plan kiểm, tính vào 3 lần sửa, mâu thuẫn thì ghi báo cáo |
| Không làm hỏng script đọc tài liệu | Test script có sẵn | `scripts/tests/run.sh` và `scripts/foundation.py --check` qua (CI `pr-check`) |
| Có hiệu lực thật | Kiểm tra ở phiên sau | Phiên kế tiếp gặp điểm nên sửa thì sửa và ghi "Đã sửa theo rà soát" trong PR |

**Ngoại lệ viết test trước:** tài liệu quy trình không có test tự động; kiểm bằng đối chiếu với ADR-1003 trong rà soát độc lập (bước 6).

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** không (tài liệu quy trình, không thuộc mục 1).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai tài liệu, bốn đoạn |
| B. Độ rõ của yêu cầu | 0 | Quy tắc đã chốt trong ADR-1003 |
| C. Khả năng kiểm chứng tự động | 3 | Không có test tự động cho tài liệu quy trình |
| D. Ảnh hưởng chéo | 3 | Đổi cách mọi phiên dừng hay sửa, cho mọi task |
| E. Độ mới của kỹ thuật | 0 | Sửa tài liệu |
| **Tổng** | 7 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `docs/risk-scoring.md`, `docs/auto-run.md` (mọi phiên đọc).
- Task khác trong sprint có thể xung đột: T-036, T-037, T-038, T-039, T-040 cùng sửa `docs/auto-run.md` ở đoạn khác (mục 3.2).
- Đề xuất thứ tự chạy: đầu danh sách, để các task sau trong cùng phiên đã dùng quy tắc mới. Phiên đọc lại `risk-scoring.md` và `auto-run.md` sau khi task này merge.

## 7. Điều kiện dừng riêng

Phần cần viết không khớp được với một vế của ADR-1003 (phải thêm hay bỏ quy tắc mới khớp với văn bản hiện có) thì dừng: đó là thay đổi ADR, không phải áp dụng.

## 8. Câu hỏi cho người duyệt

1. **Làm ở đâu.** Plan chỉ đổi tài liệu, nhưng chính phiên sẽ đọc tài liệu đó. Đề xuất của AI: làm ngay trong phiên có người theo dõi này (giống T-035), để danh sách chạy kế tiếp đã có quy tắc mới từ task đầu; không đưa vào danh sách chạy.
2. **Có hiệu lực cho task đang chạy trong phiên không.** Nếu đưa vào danh sách chạy: đề xuất của AI là chỉ từ task kế tiếp sau khi merge.
3. **Kiểm tra tự động giới hạn 3 file và 40 dòng.** Đề xuất của AI: chưa làm; rà soát độc lập kiểm, ghi số đo vào mô tả PR. Làm ticket riêng nếu phiên đầu cho thấy cần.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Làm ngay trong phiên có người theo dõi (câu 1).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-24, phiên có người theo dõi (Tuấn): đã merge

- **Kết quả:** đã merge, PR #91, commit `3bd5324`. Tài liệu quy trình, không có test tự động (ngoại lệ ở mục 4); `scripts/tests/run.sh` và `foundation.py --check` qua.
- **Rà soát độc lập:** 1 chặn, 7 nên sửa (đã sửa hết), 8 ghi chú (ghi trong mô tả PR #91, để người quyết vì là chữ của ADR).
- **Lệch so với plan:** sửa thêm bước 4 (bảng và gạch đầu dòng), bước 5 và đoạn giao việc cho phiên con của `auto-run.md`, vì các câu đó vẫn coi mục 3.2 là ranh giới tuyệt đối và sẽ làm phiên dừng sau khi nối chỗ gọi (điểm chặn của rà soát); tiêu chí L-058 không đạt nếu thiếu.
- **Cần người quyết:** cách đếm 3 file và 40 dòng; "cùng danh sách" khi task chuyển sang danh sách mới; giới hạn 3 lần sửa "của task" hay theo từng điểm; danh sách file cấm nối so với điểm chặn 7.
