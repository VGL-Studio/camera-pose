---
id: ADR-0029
title: Phiên tự chạy sửa tiếp phần ngoài plan cần cho task, merge rồi báo sau, thay vì dừng task
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-25
deciders: [Nhật]
supersedes: ADR-0028         # quyết định 2 của ADR-0028 (tự sửa điểm nên sửa) giữ nguyên, chép ở quyết định 3; đổi điều kiện dừng 1, 5 của docs/risk-scoring.md (mục 4)
superseded_by:
---

# ADR-0029: PHIÊN TỰ CHẠY SỬA TIẾP PHẦN NGOÀI PLAN CẦN CHO TASK, MERGE RỒI BÁO SAU, THAY VÌ DỪNG TASK

## Bối cảnh

Trong 7 phiên tự chạy gần nhất có 4 lần dừng task. Ba lần cùng một gốc: danh sách file của plan (mục 3.2) được đoán bằng cách đọc code, nên thiếu một file hay một test mà thay đổi của plan kéo theo.

- T-014 (RUN-20261023-01): một ví dụ trong KDoc ở file ngoài plan làm test của task đỏ.
- T-024, lần 1 (rà soát trước RUN-20261026-01): `AdsFlagsTest` đỏ khi thêm vị trí toàn app.
- T-024, lần 2 (RUN-20261026-01): `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` đỏ, vì chuỗi cấu hình trong test chưa có hai vị trí mới. Phần sửa đúng chỉ là thêm `open_app`, `open_app_inter` vào chuỗi đó.

Mỗi lần dừng tốn một vòng: người sửa plan, duyệt lại, chạy phiên mới. Task phụ thuộc cũng bị bỏ qua: T-024 dừng nên T-025 và T-036 không chạy.

Sau mỗi lần như vậy, nhóm thêm một quy tắc "trường hợp X thì nhớ chạy thử": L-020, L-029, L-042, L-067, L-089. Mỗi quy tắc chỉ chặn đúng trường hợp đã gặp. ADR-0028 nới điều kiện dừng 1 cho một trường hợp hẹp (nối chỗ gọi, tối đa 3 file và 40 dòng, ngoài mọi module `:core:*`), nên cả hai lần dừng của T-024 vẫn nằm ngoài.

Có cân nhắc viết test trước và chạy toàn bộ test ngay lúc soạn plan. Người quyết không làm: việc đó dồn thời gian chờ vào ban ngày của dev. Test vẫn viết trong phiên tự chạy.

## Quyết định

**1. Phần ngoài mục 3.2 cần cho task: sửa tiếp, merge, báo sau.** Khi phiên thấy phải sửa một file hay test không có trong mục 3.2 thì sửa luôn, không dừng task, nếu phần sửa cần để thay đổi của plan chạy đúng và các test sẵn có vẫn qua. Ví dụ: nối chỗ gọi, cập nhật dữ liệu của một test cũ theo dòng enum mới, sửa một ví dụ trong chú thích. CI xanh và rà soát độc lập không còn điểm chặn thì merge như mọi task.

Không giới hạn số file, số dòng hay module. Các giới hạn sau vẫn giữ, vượt thì dừng như cũ:

- Không sửa file trong mục "Không được làm" của `CLAUDE.md`: `.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `scripts/release.sh`, `build-logic/`, `gradle/libs.versions.toml`, cấu hình production, `app/src/staging/google-services.json`.
- Không thêm thư viện hay quyền, không chạm điểm chặn 1 tới 6, 8, 9 của `docs/risk-scoring.md` (mục 1: quảng cáo, SDK, quyền, dữ liệu gửi đi, hộp thoại đồng ý và thứ tự khởi tạo SDK, liên kết ra ngoài, đường dẫn R2, lược đồ dữ liệu cục bộ) mà plan chưa nêu (điều kiện dừng 2). Điểm chặn 7 (module core, cấu hình build, quy trình) không làm dừng, trừ file của gạch trên.
- Điều kiện dừng 3, 4, 6, 7, 8, 9 của `docs/risk-scoring.md` (mục 4) giữ nguyên.

**2. Test cũ ngoài mục 3.2.** Một test cũ đỏ vì thay đổi của plan thì xét như sau:

- Test đỏ chỉ vì dữ liệu của nó chưa có phần plan thêm (dòng enum, khóa, tham số mới; số đếm hay danh sách mong đợi): được cập nhật dữ liệu đó. Test vẫn phải bắt được lỗi: dữ liệu mới được kiểm như dữ liệu cũ.
- Test đỏ vì thay đổi làm hỏng hành vi cũ mà test giữ: đó là lỗi của code, sửa code, không sửa test. Code không thể vừa đúng plan vừa giữ hành vi đó thì dừng theo điều kiện 3 (plan mâu thuẫn với hành vi đã có).
- Không bao giờ xóa test hay câu khẳng định, nới phép so sánh (bằng thành chứa, đúng số thành không nhỏ hơn), thêm `@Ignore` hay đổi điều test kiểm về hành vi cũ (điều kiện 5).
- Ảnh chuẩn screenshot ngoài mục 3.2 lệch thì vẫn dừng: `CLAUDE.md` chỉ cho cập nhật ảnh chuẩn khi plan cho phép.

Điều kiện dừng riêng ở mục 7 của plan vẫn áp dụng: người duyệt đã chọn chúng cho task đó. Plan mới không ghi điều kiện dừng chỉ vì một file hay test nằm ngoài mục 3.2; ghi điều kiện về hành vi hay phạm vi cần giữ.

**3. Tự sửa điểm nên sửa, giữ từ ADR-0028.** Ở bước 6, phiên phải sửa mỗi điểm nên sửa khi có test viết trước thất bại đúng lý do và phần sửa không đổi điều mà test của mục 4 của plan kiểm. File được sửa gồm mục 3.2 và phần ngoài mục 3.2 theo quyết định 1. Giới hạn 3 lần sửa cho từng điểm và 2 vòng rà soát lại cho mỗi task giữ như ADR-0028. Điểm mâu thuẫn với plan, spec hay test của plan thì ghi vào báo cáo kèm phương án.

**4. Thấy sớm.** Ở bước 3, khi test viết trước đã biên dịch được (khung rỗng), phiên chạy test JVM của mọi module, không chỉ module của plan. Lệnh dùng `--continue` để thấy hết test đỏ. Test đỏ ngoài mục 3.2 được xếp theo quyết định 2 trước khi viết code.

**5. Ghi lại và báo sau.** Mỗi phần ngoài mục 3.2 được ghi ở:

- nhật ký plan, dòng "Lệch so với plan": file, vì sao cần, loại (nối chỗ gọi, dữ liệu test, code, tài liệu);
- mô tả PR, mục "Ngoài mục 3.2", kèm kết quả `git diff --stat origin/main...HEAD -- <các file ngoài mục 3.2>`;
- báo cáo phiên, mục Cần người kiểm tra, một dòng `- [ ]` cho mỗi task có phần ngoài plan, để người xem sau khi đã merge.

Rà soát độc lập xét riêng phần ngoài mục 3.2: có thật cần cho task không, test cũ có bị nới không. Vi phạm quyết định 1 hay 2 là mức chặn.

**6. Áp dụng.** `docs/risk-scoring.md` (mục 4) và `docs/auto-run.md` (bước 3 đến 6, 10, nội dung giao việc cho phiên con) dẫn về ADR này, cùng PR với ADR. ADR-0028 chuyển sang `superseded`.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Sửa tiếp phần ngoài plan cần cho task, merge, báo sau (đã chọn) | Task không dừng vì thiếu sót của mục 3.2; task phụ thuộc vẫn chạy; người chỉ đọc lại sau | Phạm vi một task có thể rộng hơn plan đã duyệt; người thấy phần lệch sau khi đã merge; rà soát độc lập gánh thêm |
| Viết test trước và chạy toàn bộ test lúc soạn plan, trên nhánh dựng thử | Lệch lộ ra trước khi duyệt | Dồn thời gian chờ vào ban ngày của dev; người quyết không làm |
| Làm xong, mở PR nhưng không merge, chờ người | Người xem phần lệch trước khi lên nhánh chính | Task phụ thuộc vẫn bị bỏ qua trong phiên; người phải merge tay |
| Giữ ADR-0028, thêm quy tắc chạy thử cho từng trường hợp (L-089) | Ranh giới chặt | Mỗi trường hợp mới lại làm một task dừng |

## Hệ quả

- **Tích cực:**
  - Task không dừng vì mục 3.2 thiếu một file hay một test.
  - Task phụ thuộc chạy được trong cùng phiên.
  - Ít bài học kiểu "nhớ chạy thử trường hợp X".
- **Phải chấp nhận:**
  - Phần ngoài plan lên nhánh chính trước khi người xem.
  - Rà soát độc lập là chốt duy nhất trước khi merge cho phần đó.
  - Bước 3 tốn thêm một lần chạy test JVM của mọi module.
- **Việc AI phải tuân thủ:** phần ngoài mục 3.2 cần để thay đổi của plan chạy đúng thì sửa tiếp và merge, không dừng task, không giới hạn số file hay module; vẫn dừng khi phải sửa file trong mục "Không được làm" của `CLAUDE.md`, khi phần ngoài plan thêm thư viện, quyền hay chạm điểm chặn 1 tới 6, 8, 9 của `risk-scoring.md` mà plan chưa nêu, hay khi gặp điều kiện dừng 3, 4, 6, 7, 8, 9; test cũ đỏ chỉ vì dữ liệu chưa có phần plan thêm thì cập nhật dữ liệu đó và vẫn kiểm dữ liệu mới; test cũ đỏ vì hành vi cũ bị hỏng thì sửa code, không sửa test; không xóa test hay câu khẳng định, không nới phép so sánh, không thêm `@Ignore`; ảnh chuẩn screenshot ngoài mục 3.2 lệch thì dừng; điều kiện dừng riêng ở mục 7 của plan vẫn áp dụng; ở bước 3 chạy test JVM của mọi module với `--continue` khi test viết trước đã biên dịch được; mỗi phần ngoài mục 3.2 ghi vào nhật ký plan, mô tả PR (mục "Ngoài mục 3.2" kèm `git diff --stat`) và một dòng `- [ ]` ở mục Cần người kiểm tra của báo cáo phiên; rà soát độc lập xét riêng phần đó, vi phạm là mức chặn; ở bước 6 phải sửa điểm nên sửa có test viết trước, giới hạn 3 lần sửa mỗi điểm và 2 vòng rà soát lại mỗi task.
