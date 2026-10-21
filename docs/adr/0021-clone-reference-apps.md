---
id: ADR-0021
title: App mới có thể bắt đầu bằng việc AI khảo sát các app mẫu trên Play, đề xuất app tốt hơn, rồi thiết kế trên Figma cùng dev
status: proposed
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-21
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
supersedes:
superseded_by:
---

# ADR-0021: APP MỚI CÓ THỂ BẮT ĐẦU BẰNG VIỆC AI KHẢO SÁT CÁC APP MẪU TRÊN PLAY, ĐỀ XUẤT APP TỐT HƠN, RỒI THIẾT KẾ TRÊN FIGMA CÙNG DEV

## Bối cảnh

Nhiều app của nhóm là app clone: làm một app cùng loại với các app đã thành công trên Google Play, kiếm tiền bằng quảng cáo. Trước đây dev tự dùng thử các app đó, tự viết yêu cầu, rồi chủ feature tự dựng thiết kế. Việc này tốn nhiều ngày và phụ thuộc vào trí nhớ của người thử.

Lần thử đầu tiên với nhóm app "pose camera" (8 app, ngày 2026-10-19 đến 2026-10-21) cho thấy:

- AI điều khiển được app trên máy thật qua `adb`: chụp màn hình, đọc cây giao diện (cho số đo chính xác theo dp), chạm, vuốt. Mỗi app khoảng 40 ảnh và một báo cáo đủ để dựng lại giao diện.
- Phân tích APK cho biết thứ không thấy được trên màn hình: SDK quảng cáo, khóa Remote Config điều khiển quảng cáo, nội dung đóng gói, app nào cùng một mã nguồn.
- Đánh giá trên Play cho biết người dùng khó chịu điều gì; đó là chỗ app mới làm tốt hơn.
- AI dựng được màn hình trên Figma qua MCP (gói Professional, ghế Full): biến màu, component, chữ sửa được. Màn hình dựng lại sát app gốc, khoảng 4 đến 8 lượt gọi mỗi màn hình sau khi đã có component.
- Rủi ro thật: AI lỡ chạm vào quảng cáo của app mẫu và mở màn hình mua của Play khi chạm theo tọa độ cũ đúng lúc một quảng cáo hay màn hình trả phí vừa hiện lên.

## Quyết định

1. **Thêm bước 0 "Khảo sát app mẫu" trước bước yêu cầu**, cho app clone. Bước này chạy trong phiên có người theo dõi, không chạy trong phiên tự chạy. Dev cung cấp thông tin chung về app muốn làm và danh sách app mẫu đã cài sẵn (đã đăng nhập, đã qua những bước cần người) trên máy thật hay emulator nối với máy dev.
2. **AI khảo sát bằng bốn nguồn**: dùng thử từng app trên máy (`scripts/app-explore/ax.sh`), đọc APK, tìm trên web và Play (mô tả, lượt cài, đánh giá), và tổng hợp. Kết quả nằm trong một thư mục làm việc ngoài repo cho tới khi app có repo.
3. **AI viết bản đề xuất app** theo [mẫu](../templates/app-proposal.md): tên và tên gói đề nghị, tính năng, luồng, danh sách màn hình, bài học từ app mẫu và cải tiến tương ứng, quảng cáo (ghi thành câu hỏi), hướng thiết kế. **Dev duyệt bản đề xuất trước khi sang Figma.** Tên và tên gói do dev chốt, trước khi chạy `scripts/new-app.sh`.
4. **App mới phải tốt hơn app mẫu và có nhận diện riêng.** Mỗi cải tiến dẫn về một bài học có bằng chứng (ảnh, đánh giá, số đo) và có cách đo. Giữ các khuôn tương tác đã quen với người dùng; đổi màu, bộ icon, phông chữ, hình dạng, giọng văn.
5. **Chọn hướng thiết kế trước khi dựng toàn bộ.** AI dựng 2 đến 3 hướng phong cách trên Figma, mỗi hướng một bảng phong cách và hai màn hình chính. Dev chọn hoặc ghép. Hướng được chọn thành hệ thống thiết kế trên Figma, sau đó thành `:core:designsystem` của app.
6. **Thiết kế kết hợp:** AI dựng cấu trúc qua Figma MCP (đủ màn hình và trạng thái, tên frame và khung 411 x 891 theo ADR-0020, biến, component). Dev làm mịn bằng AI agent của Figma hoặc sửa tay, ưu tiên ở component và biến. Sau mỗi lượt làm mịn, AI quét file chỉ đọc và báo chỗ lệch cấu trúc. Từ bước bàn giao trở đi đi theo ADR-0020.
7. **Ranh giới khi khảo sát và clone:**
   - Được: bố cục và luồng giống app mẫu; giải nén APK và xem network để hiểu cách app làm.
   - Không: gọi API hay backend của app mẫu từ app mình; tên, icon app, ảnh store giống app mẫu; chép code từ APK (viết lại theo kiến trúc của mình).
   - Khi khảo sát: không bấm quảng cáo, không mua, không bắt đầu dùng thử, không đăng nhập bằng tài khoản cá nhân, không gửi hay chia sẻ gì ra ngoài. Công cụ chặn các cú chạm vào quảng cáo và nút mua; AI không vượt bộ chặn.
8. **Mượn tạm tài sản để code sớm:** được dùng tạm ảnh, icon, âm thanh, nội dung của app mẫu khi designer và người làm nội dung chưa xong. Mọi tài sản mượn có tiền tố `borrowed_` (trên R2: thư mục `borrowed/`), ghi trong `docs/borrowed-assets.md` theo [mẫu](../templates/borrowed-assets.md) kèm mã băm, chỉ nằm trong bản staging và R2 staging, và phải thay hết trước khi phát hành. Không tải bản build còn tài sản mượn lên Play Console, kể cả kênh thử nội bộ.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| AI khảo sát, đề xuất, thiết kế cùng dev (đã chọn) | Nhanh; đủ số đo để dựng lại giao diện; bài học có bằng chứng | Cần máy và người theo dõi; tốn token; AI không vào được mọi màn hình |
| Dev tự khảo sát, tự viết yêu cầu (cách cũ) | Không cần công cụ | Chậm; phụ thuộc trí nhớ; thiếu số đo |
| AI tự lái AI agent của Figma qua trình duyệt | Dùng được tính năng AI của Figma | Chậm, dễ vỡ khi giao diện đổi, khó kiểm soát đã sửa gì, tốn credit; MCP làm được việc tương tự chính xác hơn |
| Chép nguyên giao diện app mẫu | Ít việc thiết kế | Dễ bị Play coi là app sao chép; không tốt hơn app mẫu |

## Hệ quả

- **Tích cực:** bản đề xuất có bằng chứng; thiết kế bắt đầu từ số đo thật; app mới có lý do để tốt hơn app mẫu; mọi app tạo từ dự án mẫu dùng cùng cách làm.
- **Phải chấp nhận:** cần một máy thử riêng có app mẫu cài sẵn; máy Xiaomi phải bật "Gỡ lỗi USB (Cài đặt bảo mật)"; mỗi app mẫu tốn khoảng 150 đến 200 nghìn token để khảo sát; Figma Professional ghế Full; lịch sử git giữ lại tài sản mượn kể cả sau khi thay, nên repo phải luôn riêng tư.
- **Việc còn lại:** cơ chế kiểm tra tự động tài sản mượn (`scripts/borrowed-check.py`, nối vào `scripts/check.sh` và `scripts/release.sh`) cần plan rủi ro cao duyệt chéo vì đụng file được bảo vệ. Cho tới khi có, dev kiểm `docs/borrowed-assets.md` bằng tay trước khi phát hành.
- **Việc AI phải tuân thủ:** làm theo `docs/CLAUDE.md`, mục Khi khảo sát app mẫu và mục Khi thiết kế trên Figma; chỉ điều khiển máy qua `scripts/app-explore/ax.sh`; không vượt bộ chặn; không tự chuyển trạng thái bản đề xuất hay thiết kế; ghi mọi tài sản mượn vào `docs/borrowed-assets.md` trong cùng PR với file mượn.
