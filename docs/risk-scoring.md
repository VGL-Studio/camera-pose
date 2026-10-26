# THANG ĐIỂM RỦI RO VÀ ĐIỀU KIỆN DỪNG

Mỗi plan được AI chấm điểm rủi ro trước khi đưa vào họp đánh giá. Điểm rủi ro quyết định ai phải duyệt plan và mức độ thận trọng trong phiên tự chạy. Thang điểm này được điều chỉnh qua các buổi retro; mỗi lần điều chỉnh ghi lý do vào mục Lịch sử thay đổi ở cuối tài liệu.

## 1. Điểm chặn: tự động xếp rủi ro cao

Plan chạm tới **bất kỳ** điểm nào dưới đây được xếp rủi ro **Cao**, không cần tính điểm:

1. Quảng cáo: thêm, bớt hoặc di chuyển vị trí quảng cáo; đổi định dạng, tần suất; nâng cấp SDK quảng cáo hoặc adapter mediation.
2. SDK bên thứ ba mới, hoặc nâng cấp phiên bản chính (major) của SDK hiện có.
3. Quyền truy cập mới trong `AndroidManifest.xml`.
4. Thu thập dữ liệu: thêm hoặc sửa sự kiện phân tích, thuộc tính người dùng, dữ liệu gửi ra khỏi máy.
5. Hộp thoại xin đồng ý của người dùng, hoặc thứ tự khởi tạo các SDK.
6. Liên kết ra ngoài app, quảng bá chéo sang app khác.
7. Module core dùng chung (`:core:*`), cấu hình build, CI hoặc quy trình phiên tự chạy.
8. Cách đặt tên hoặc đường dẫn file trên R2.
9. Lược đồ dữ liệu cục bộ (bảng Room, khóa DataStore) cần chuyển đổi dữ liệu cũ.

## 2. Năm tiêu chí tính điểm

Với plan không chạm điểm chặn, AI chấm từng tiêu chí từ 0 đến 3 và ghi lý do ngắn.

| Tiêu chí | 0 | 1 | 2 | 3 |
| --- | --- | --- | --- | --- |
| **A. Phạm vi thay đổi** | Một file, không đổi giao diện công khai của lớp nào | Vài file trong một module feature | Nhiều phần trong một module, có đổi giao diện công khai | Nhiều module |
| **B. Độ rõ của yêu cầu** | Spec đã xác nhận, đủ tiêu chí chấp nhận, có thiết kế đã duyệt | Spec rõ, thiếu vài chi tiết nhỏ AI phải tự quyết | Còn câu hỏi mở ảnh hưởng tới cách làm | Yêu cầu mơ hồ hoặc mâu thuẫn |
| **C. Khả năng kiểm chứng tự động** | Mọi tiêu chí chấp nhận đều có test tự động | Phần lớn có test, phần còn lại kiểm tra bằng mắt dễ dàng | Phụ thuộc nhiều vào kiểm tra thủ công trên máy thật | Khó kiểm chứng: thời gian, thông báo, vòng đời ứng dụng, phụ thuộc thiết bị |
| **D. Ảnh hưởng chéo** | Không chạm file nào ngoài module của feature | Dùng thành phần dùng chung nhưng không sửa | Sửa file mà task khác trong sprint cũng sửa | Thay đổi hành vi mà feature khác đang dựa vào |
| **E. Độ mới của kỹ thuật** | Lặp lại đúng mẫu đã có trong dự án | Biến thể nhỏ của mẫu đã có | Kỹ thuật chưa có trong dự án nhưng phổ biến | Kỹ thuật mới, ít tài liệu, hoặc API Android mới |

## 3. Quy đổi và quyền duyệt

| Tổng điểm | Mức | Ai duyệt | Ghi chú |
| --- | --- | --- | --- |
| 0 đến 4 | **Thấp** | Chủ feature tự duyệt | Nêu ngắn trong họp đánh giá rủi ro |
| 5 đến 8 | **Trung bình** | Chủ feature duyệt | Trình bày trong họp, dev còn lại được quyền yêu cầu duyệt chéo |
| 9 trở lên, hoặc chạm điểm chặn | **Cao** | Bắt buộc dev còn lại duyệt chéo | Cân nhắc tách thành task nhỏ hơn trước khi duyệt |

Ngoại lệ khi nhóm chỉ có một dev: người đó vừa duyệt vừa duyệt chéo plan rủi ro cao, ghi rõ lý do ở mục 9 của plan. Phiên tự chạy không coi đây là lý do bỏ qua task. Khi nhóm có dev thứ hai, quy tắc duyệt chéo áp dụng lại đầy đủ.

Thêm hai quy tắc:

- Tiêu chí **B** đạt 3 điểm thì plan không được duyệt, dù tổng điểm thấp. Phải làm rõ yêu cầu trước.
- Plan rủi ro cao chạm tới quảng cáo, dữ liệu hoặc hộp thoại đồng ý cần người phụ trách sản phẩm biết trước khi duyệt, vì đây là quyết định kinh doanh và tuân thủ.

## 4. Điều kiện dừng trong phiên tự chạy

Trong lúc thực hiện plan đã duyệt, AI **phải dừng** task, không tự xoay xở, khi gặp một trong các tình huống sau:

1. Cần sửa file trong mục "Không được làm" của `CLAUDE.md` (`.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `scripts/release.sh`, `build-logic/`, `gradle/libs.versions.toml`, cấu hình production) mà plan chưa nêu. File khác ngoài danh sách của plan thì không dừng: sửa tiếp theo ADR-0029 (dưới).
2. Cần thêm thư viện, quyền truy cập hoặc chạm điểm chặn 1 tới 6, 8, 9 ở mục 1 mà plan chưa nêu. Điểm chặn 7 (module core, cấu hình build, quy trình) không làm dừng với phần ngoài mục 3.2 theo ADR-0029, trừ file của điều kiện 1. Để ít gặp điều kiện này, quyền và thư viện được quyết lúc lập plan (`docs/CLAUDE.md`, mục Khi viết plan; mục 3.3 của mẫu plan; `scripts/plan-check.py`, quy tắc 8).
3. Phát hiện spec, thiết kế hoặc ADR mâu thuẫn với plan hoặc với nhau.
4. Test vẫn thất bại sau 3 lần sửa liên tiếp cho cùng một lỗi.
5. Muốn làm cho test qua bằng cách xóa test hay câu khẳng định, nới phép so sánh, thêm `@Ignore`, đổi điều test cũ kiểm về hành vi đã có, hạ tiêu chí kiểm tra, hoặc bỏ qua quy tắc lint. Cập nhật dữ liệu của test cũ chỉ vì dữ liệu đó chưa có phần plan thêm thì không phải dừng (ADR-0029, quyết định 2).
6. Phần việc thực tế lớn hơn rõ rệt so với ước lượng trong ticket.
7. Bất kỳ thao tác nào chạm tới cấu hình production: mã quảng cáo thật, project Firebase production, bucket R2 production, khóa ký ứng dụng.
8. Cần thay đổi điều mà test viết trước đang kiểm tra để code qua được.
9. Phiên rà soát độc lập và phiên viết code hiểu spec khác nhau.

**Không phải dừng: phần ngoài mục 3.2 cần cho task (ADR-0029).** AI sửa tiếp một file hay test không có trong danh sách file của plan, rồi merge như mọi task, khi phần sửa cần để thay đổi của plan chạy đúng và các test sẵn có vẫn qua: nối chỗ gọi, cập nhật dữ liệu của test cũ theo dòng enum, khóa hay tham số mà plan thêm, sửa ví dụ trong chú thích. Không giới hạn số file, số dòng hay module. Vẫn dừng khi gặp điều kiện 1 (file "Không được làm"), 2, 3, 4, 6, 7, 8 hay 9.

- Test cũ đỏ chỉ vì dữ liệu của nó chưa có phần plan thêm: cập nhật dữ liệu, test vẫn kiểm phần mới như phần cũ.
- Test cũ đỏ vì hành vi cũ bị hỏng: sửa code, không sửa test. Không thể vừa đúng plan vừa giữ hành vi đó thì dừng theo điều kiện 3.
- Ảnh chuẩn screenshot ngoài mục 3.2 lệch: dừng, vì `CLAUDE.md` chỉ cho cập nhật ảnh chuẩn khi plan cho phép.
- Điều kiện dừng riêng ở mục 7 của plan vẫn áp dụng.
- Ghi vào nhật ký plan (file, vì sao cần, loại), mô tả PR (mục "Ngoài mục 3.2", kèm `git diff --stat origin/main...HEAD -- <các file đó>`) và báo cáo phiên (một dòng `- [ ]` ở mục Cần người kiểm tra). Rà soát độc lập xét riêng phần này; vi phạm ADR-0029 là mức chặn (`auto-run.md`, bước 6).

**Không phải dừng: chuyển sang phương án dự phòng đã duyệt.** Plan có mục 3.4 (Phương án dự phòng) đã được duyệt cùng plan. Khi phương án chính gặp đúng điều kiện chuyển ghi trong mục đó, hoặc gặp điều kiện 4 ở trên, AI chuyển sang phương án dự phòng thay vì dừng: chỉ chạm file của mục 3.2 (cộng phần ngoài mục 3.2 theo ADR-0029, như với phương án chính), giữ nguyên test viết trước và tiêu chí chấp nhận, đếm lại giới hạn 3 lần sửa từ đầu, ghi vào nhật ký lúc chuyển và lý do. Chỉ chuyển một lần; phương án dự phòng cũng không qua thì dừng như bình thường. Phương án dự phòng không bao giờ là lý do để vượt điều kiện 1, 2, 3, 5, 7, 8 hay 9.

Khi dừng, AI làm ba việc:

- Không merge, giữ nguyên nhánh của task để dev xem.
- Chuyển sang task tiếp theo trong danh sách chạy, trừ khi task đó phụ thuộc task vừa dừng.
- Ghi vào mục Nhật ký thực hiện của plan: dừng ở đâu, vì sao, đã thử những gì, đề xuất hướng xử lý.
- Chuyển trạng thái plan và ticket sang `stopped`.

Khi bắt đầu làm việc lại, nhóm trả lời câu hỏi "vì sao plan chưa đủ tốt" và ghi bài học vào plan sửa lại. Nếu bài học mang tính chung, ghi vào sổ bài học [lessons.md](lessons.md); retro quyết định cập nhật thang điểm này, `CLAUDE.md` hay tài liệu khác (auto-run.md, mục 6), để lần sau AI tự tránh.

## 5. Lịch sử thay đổi

| Ngày | Thay đổi | Lý do |
| --- | --- | --- |
| 2026-10-14 | Phiên bản đầu tiên | Thiết lập quy trình |
| 2026-10-15 | Mục 4: bài học chung ghi vào sổ bài học, retro quyết định sửa gì | Có vòng cải tiến khép kín sau mỗi phiên |
| 2026-10-17 | Mục 3: ngoại lệ duyệt chéo khi nhóm chỉ có một dev | Retro RUN-20261016-02: nhóm hiện có một dev, plan rủi ro cao không có người duyệt chéo khác |
| 2026-10-22 | Mục 4, điều kiện 2: quyền và thư viện quyết lúc lập plan | Tránh dừng task vì quyền hay thư viện chỉ lộ ra trong phiên, từ app camera-pose (L-046) |
| 2026-10-22 | Mục 4: chuyển sang phương án dự phòng đã duyệt không phải dừng | Ở app camera-pose, ba lần dừng task đều vì kỹ thuật của plan không chạy, trong khi cách thay đã rõ từ lúc viết plan (L-041) |
| 2026-10-24 | Mục 4, điều kiện 1: nối chỗ gọi mà task khác cùng danh sách đổi không phải dừng, trong giới hạn của ADR-0028 (đếm 3 file và 40 dòng bằng `git diff --numstat`, không tính test; "cùng danh sách" tính cả task đã merge ở danh sách trước khi task được chuyển sang) | Ở app camera-pose, một task phải nối chỗ gọi trong file mà task khác cùng danh sách vừa tạo, ngoài mục 3.2 (L-077) |
| 2026-10-25 | Mục 4, điều kiện 1 và 5, đoạn "Không phải dừng": phần ngoài mục 3.2 cần cho task thì sửa tiếp, merge, báo sau, không giới hạn số file hay module; chỉ file "Không được làm" của `CLAUDE.md` còn làm dừng theo điều kiện 1; test cũ được cập nhật dữ liệu theo phần plan thêm (ADR-0029, thay ADR-0028) | 3 trên 4 lần dừng gần nhất vì mục 3.2 thiếu file hay test (T-014, T-024 hai lần); người quyết không dựng thử lúc soạn plan |
