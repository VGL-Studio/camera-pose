# ĐƯA CẢI TIẾN TỪ APP VỀ DỰ ÁN MẪU

Mọi app được tạo từ dự án mẫu `VGL-Studio/app-project-template`. Khi làm app, nhóm sẽ cải tiến quy trình, script, CI hay module core. Cải tiến có ích cho mọi app thì đưa về dự án mẫu, để app tạo sau được hưởng. Tài liệu này mô tả đường đi đó. Chiều ngược lại (đưa thay đổi của dự án mẫu về các app đã tạo) đi qua PR đồng bộ theo [to-app.md](to-app.md) (ADR-0030).

## 1. Ở app: nhận ra cải tiến chung

Một cải tiến là **chung** khi nó không phụ thuộc vào nghiệp vụ của app, thường nằm ở:

- Quy trình và tài liệu: `docs/auto-run.md`, `docs/CLAUDE.md`, `docs/risk-scoring.md`, `docs/templates/`, `docs/ops/`, `CLAUDE.md`.
- Môi trường và CI: `.github/`, `scripts/`.
- Cấu hình build: `build-logic/`, `gradle/`.
- Module core dùng chung: `:core:*`, trừ phần chỉ app này cần.

Ở retro sau phiên (`docs/auto-run.md`, mục 6), bài học trong `docs/lessons.md` của app được ghi phạm vi `chung` hoặc `app`.

Quy tắc khi sửa các phần trên ở app (`CLAUDE.md`, mục Không được làm):

- Cải tiến chung: làm ở app như bình thường nếu app cần ngay, rồi mở issue về dự án mẫu (mục 2) trong vòng một tuần. Không để app tự đi xa dự án mẫu ở các phần dùng chung mà không ai biết.
- Thay đổi chỉ app cần: ghi lý do trong PR, không cần mở issue.
- Quyết định kỹ thuật: ADR có `origin: template` không sửa ở app. App muốn khác thì viết ADR riêng (mã từ 1001, `supersedes` ghi ADR bị thay cho app này); nếu có ích chung thì mở issue (mục 2) để dự án mẫu sửa hay thay ADR gốc.

Phiên tự chạy của app (`claude --cloud`, routine) chỉ có quyền GitHub với repo của app, không đọc được repo mẫu. Plan của app chép code từ dự án mẫu dán nguyên đoạn code hay `diff` vào plan, không để phiên tự đọc repo mẫu (L-101).

## 2. Ở app: mở issue về dự án mẫu

Người dự retro (hoặc AI trong phiên có người theo dõi, theo yêu cầu của người) mở issue trong repo dự án mẫu:

1. Mở https://github.com/VGL-Studio/app-project-template/issues/new/choose, chọn **Cải tiến từ app**. Mẫu issue gắn sẵn nhãn `from-app`.
2. Điền: app nguồn, link PR đã merge ở app, phần bị ảnh hưởng, vấn đề, cải tiến đã làm, vì sao có ích cho mọi app và phần nào là riêng của app.
3. Ghi số issue vào dòng **Áp dụng** của bài học trong `docs/lessons.md` của app.

Phiên tự chạy không mở issue ở repo khác. Nó chỉ ghi bài học phạm vi `chung` vào báo cáo.

## 3. Ở dự án mẫu: xem xét và đưa vào

Người phụ trách dự án mẫu (hiện là anh Nhật):

1. Hai tuần một lần, xem các issue mở có nhãn `from-app`: https://github.com/VGL-Studio/app-project-template/issues?q=is%3Aopen+label%3Afrom-app
2. Với mỗi issue: nhận, hoặc đóng kèm lý do (chỉ đúng với app đó, đã có cách khác, chưa đủ bằng chứng).
3. Issue được nhận: thêm một bài học vào `docs/lessons.md` của dự án mẫu, trạng thái `accepted`, dòng **Ngày, nguồn** ghi app và số issue. AI (phiên có người theo dõi) làm thành PR cho dự án mẫu:
   - Tổng quát hóa: bỏ tên app, tên gói, giá trị cấu hình riêng; giữ đúng quy tắc của dự án mẫu.
   - Cải tiến chạm vào file bị cấm sửa (`CLAUDE.md`, mục Không được làm) thì ghi rõ trong mô tả PR như mọi thay đổi khác.
   - Kiểm chứng trong dự án mẫu (`scripts/check.sh`, test của script, chạy thử nếu cần).
   - Plan cần đọc code của app thì ghi lệnh `gh api` (PR dạng diff, hay file theo commit), không ghi `gh pr diff`: phiên tự chạy ở cloud không chạy được `gh pr` (`auto-run.md`, bước 7, L-063).
4. PR merge thì bài học chuyển `applied`, đóng issue kèm số PR.

## 4. Các app đã tạo trước đó

App nhận cải tiến mới của dự án mẫu qua PR đồng bộ (`docs/ops/to-app.md`, ADR-0030), không chép tay. Trước mỗi đợt đồng bộ, đưa xong cải tiến chung của app về dự án mẫu theo mục 1 đến 3. Khi đó xung đột ở phần của dự án mẫu lấy bản mẫu mà không mất gì.
