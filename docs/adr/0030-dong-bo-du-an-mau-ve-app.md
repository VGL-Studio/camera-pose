---
id: ADR-0030
title: Đưa thay đổi của dự án mẫu về app theo từng đợt, bằng PR đồng bộ dựng từ diff của dự án mẫu
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-26
deciders: [Nhật]
supersedes:
superseded_by:
---

# ADR-0030: ĐƯA THAY ĐỔI CỦA DỰ ÁN MẪU VỀ APP THEO TỪNG ĐỢT, BẰNG PR ĐỒNG BỘ DỰNG TỪ DIFF CỦA DỰ ÁN MẪU

## Bối cảnh

App được tạo bằng `scripts/new-app.sh`: chép dự án mẫu ở một commit, đổi tên gói, rồi mở một repo git mới. Hai repo không có lịch sử chung, và đường dẫn mã nguồn khác nhau vì tên gói, nên app không merge thẳng được từ dự án mẫu.

Chiều app về dự án mẫu đã có quy trình (`docs/ops/from-app.md`). Chiều ngược lại chỉ có một dòng "app tự chép qua một PR thường", và đến nay chưa ai làm. App camera-pose được tạo từ commit `9475da7` ngày 2026-10-21. Năm ngày sau, dự án mẫu đã có thêm 101 commit, khoảng 70 nghìn dòng trên 648 file. Trong đó có cả bộ quảng cáo theo AdsKit (ADR-0026, ADR-0027): `:core:ads` của dự án mẫu có khoảng 85 file, của app còn 9 file.

Chép tay từng phần không theo kịp, và dễ chép sót hay chép đè phần app đã sửa.

## Quyết định

**1. Mỗi app ghi mốc đồng bộ** trong `docs/template-sync.txt`: commit của dự án mẫu mà app đang theo (`base=`), tên gói của app (`package=`), và các phần app tạm giữ ở mốc cũ (`hold=<đường dẫn> <commit>`). `new-app.sh` ghi file này cho app mới. App tạo trước ADR này thì tạo tay theo `docs/ops/to-app.md`, mục 3.

**2. PR đồng bộ dựng bằng `scripts/template-sync.sh`** của dự án mẫu. Script làm bốn việc:

- dựng dự án mẫu ở mốc của app và ở commit đích, đổi tên gói giống `new-app.sh`;
- bỏ phần không bao giờ đồng bộ: lịch sử và dữ liệu riêng của dự án mẫu, trang sinh ra từ tài liệu;
- gộp ba chiều vào một nhánh mới của app (`template-sync/<commit>`). Chỉ thay đổi của dự án mẫu từ mốc cũ đi vào; chỗ app cũng sửa thành xung đột để người gộp;
- chuyển mốc `base=` trong cùng commit.

Script không đẩy nhánh, không mở PR.

**3. Theo đợt, không liên tục.** Đồng bộ khi app cần một phần mới của dự án mẫu, hay sau mỗi đợt lớn của dự án mẫu. Chỉ làm giữa hai phiên tự chạy của app. Phần lớn (một module core thay gần hết, cần thêm SDK, đổi ảnh chuẩn) thì giữ lại (`hold=`), rồi đưa vào bằng một task có plan của app (`--take`).

**4. Phần chung của app về dự án mẫu trước.** Trước mỗi đợt, cải tiến chung của app chưa về dự án mẫu (bài học phạm vi `chung` chưa có issue `from-app`, phần app sửa ở file của dự án mẫu) được đưa về qua `from-app.md`. Như vậy, khi gặp xung đột ở phần của dự án mẫu thì lấy bản mẫu mà không mất gì.

**5. Phân loại file** theo `docs/ops/to-app.md`, mục 2:

- không bao giờ đồng bộ;
- của dự án mẫu, app không sửa: xung đột thì lấy bản mẫu;
- cả hai cùng sửa: gộp tay;
- chỉ của app.

Giá trị riêng của app nằm trong file của dự án mẫu (hạn chót của kịch bản emulator) được liệt kê ở mục 6 của tài liệu đó và đặt lại sau mỗi đợt, tới khi dự án mẫu chuyển chúng ra chỗ cấu hình riêng.

**6. Người làm:** AI trong phiên có người theo dõi, trên máy có cả hai repo, theo yêu cầu của người phụ trách app hay dự án mẫu. Phiên tự chạy của app không đọc được repo mẫu (L-101) nên không chạy script này. PR đồng bộ merge bằng `scripts/merge-if-green.sh` của app như mọi PR. Người phụ trách app duyệt phần gộp tay.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| PR đồng bộ dựng từ diff của dự án mẫu, theo đợt (đã chọn) | Chỉ thay đổi của dự án mẫu đi vào; phần app đã sửa thành xung đột, không bị đè; dùng git thường, không đổi cấu trúc app | Người phải gộp xung đột; app nào đi xa dự án mẫu thì đợt đầu nặng |
| Chép tay từng phần qua PR thường (cách cũ) | Không cần công cụ | Không theo kịp; dễ sót, dễ đè; chưa ai làm |
| Tách module core thành thư viện dùng chung (AAR, git submodule, composite build) | App nhận bản mới bằng một dòng phiên bản | `:core:ads` còn đổi hằng ngày; `AdPlacement` là của riêng từng app; phiên tự chạy của app không sửa được core khi cần; thêm việc phát hành phiên bản. Xem lại khi quảng cáo ổn định và có từ 3 app |
| Tạo app bằng fork của GitHub, merge thẳng từ dự án mẫu | Merge thường | Tên gói khác làm mọi file mã nguồn đổi đường dẫn; fork không đổi được quyền và chế độ riêng tư như repo thường |

## Hệ quả

- **Tích cực:**
  - App nhận sửa lỗi quy trình và module core của dự án mẫu theo nhịp của mình.
  - Biết chính xác app đang ở mốc nào của dự án mẫu.
  - Phần app tự đi xa dự án mẫu lộ ra thành xung đột, không âm thầm.
- **Phải chấp nhận:**
  - Đợt đầu của app tạo trước ADR này nặng. Thử với camera-pose ngày 2026-10-26: đồng bộ toàn bộ cho 105 file xung đột. Giữ lại phần code (`app`, `core`, `feature`, cấu hình build) thì còn 50, trong đó 46 ở phần của dự án mẫu.
  - Tài liệu của dự án mẫu dẫn số bài học của dự án mẫu, khác số trong sổ bài học của app.
- **Việc AI phải tuân thủ:** không chép tay code hay tài liệu của dự án mẫu về phần dùng chung của app, dùng `scripts/template-sync.sh` theo `docs/ops/to-app.md`; không sửa `base=` hay `hold=` của `docs/template-sync.txt` ngoài PR đồng bộ; trước khi đồng bộ, đưa cải tiến chung của app chưa về dự án mẫu qua `from-app.md`; xung đột ở phần của dự án mẫu thì lấy bản mẫu, trừ phần app chưa đưa về và giá trị riêng của app ở mục 6 của `to-app.md`; phần app cũng sửa thì gộp tay, giữ phần riêng của app; mô tả PR đồng bộ liệt kê cách gộp từng file xung đột; phần giữ lại vì lớn thì đưa vào bằng một task có plan.
