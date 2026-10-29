# LÀM NHANH CÓ NGƯỜI THEO DÕI

Hướng dẫn cho dev muốn làm ngay một việc nhỏ của code app trong ngày, cùng AI, không chờ phiên tự chạy. Quy tắc gốc ở [ADR-0034](../adr/0034-lam-nhanh-co-nguoi-theo-doi.md); phần AI phải làm ở [docs/CLAUDE.md](../CLAUDE.md), mục Khi làm nhanh có người theo dõi.

## 1. Khi nào dùng

Dùng đường làm nhanh khi đủ mọi điều sau:

- Dev ngồi cùng AI tới khi xong và trả lời được ngay. Thời gian chờ gồm CI của PR (khoảng 10 đến 15 phút) và `main-guard` sau khi merge (khoảng 5 đến 12 phút, gấp đôi khi nó chạy lại test thất bại).
- Việc nhỏ: cỡ S, hoặc M nếu dev thấy ổn; rủi ro Thấp (tổng 0 đến 4) theo [risk-scoring.md](../risk-scoring.md), và không dùng kỹ thuật mới cần chạy thử (tiêu chí E dưới 2).
- Không chạm điểm chặn nào ở [mục 1 của risk-scoring.md](../risk-scoring.md), kể cả điểm 7. Ví dụ: quảng cáo; SDK mới hay nâng major SDK có sẵn; quyền mới; sự kiện phân tích, thuộc tính người dùng, dữ liệu gửi ra ngoài; hộp thoại đồng ý, thứ tự khởi tạo SDK; liên kết ra ngoài app, quảng bá chéo; module `:core:*`, cấu hình build, CI, quy trình phiên tự chạy; đường dẫn R2; bảng Room hay khóa DataStore cần chuyển dữ liệu cũ. Cũng không thêm thư viện.
- Việc đã có trong spec đã chốt, hoặc dev ngồi cùng là người xác nhận spec của feature đó.
- Lúc bắt đầu không có phiên tự chạy đang chạy, không có issue `main-broken` hay `env-broken` đang mở.

Dev nào ngồi cùng cũng duyệt được plan làm nhanh, không cần là chủ feature.

Ví dụ hợp: sửa chữ hiển thị sai, chỉnh khoảng cách một màn hình, thêm một trạng thái lỗi kèm nút thử lại, sửa một lỗi giao diện đã rõ nguyên nhân.

Không hợp, đi đường thường: thêm vị trí quảng cáo, thêm màn hình mới có thiết kế chưa duyệt, đổi thành phần dùng chung của `:core:designsystem`, việc cần chạy thử kỹ thuật mới.

Việc về script, tài liệu hay quy trình không cần đường này: vẫn làm bằng PR thường trong phiên có người theo dõi.

## 2. Bắt đầu

Mở Claude Code trong repo của app và nói việc cần làm, ví dụ:

> Làm nhanh: màn hình kết quả của feature `photo-filter` hiện sai chữ "Lưu ảnh" thành "Luu anh".

AI kiểm điều kiện ở mục 1. Thiếu điều nào thì AI nói rõ và đề xuất đi đường thường.

## 3. Diễn biến

| Bước | AI làm | Dev làm |
| --- | --- | --- |
| 1. Tài liệu | Ghi lời dev thành file yêu cầu, sửa spec nếu cần, viết ticket và plan đủ mục (`lane: quick`, mục 3.5 từ `scripts/existing-tests.py`), chạy `plan-check.py` | Trả lời câu hỏi nếu có; xác nhận phần spec đổi, khi dev là người xác nhận spec của feature |
| 2. Duyệt plan | Đưa tóm tắt: phạm vi, file sẽ sửa, test, điểm rủi ro, câu hỏi | Đồng ý, hoặc yêu cầu sửa. Lời đồng ý được ghi vào plan |
| 3. Làm | Viết test trước, viết code, chạy `scripts/check.sh`, nhờ một agent khác rà soát độc lập, sửa theo rà soát | Trả lời khi AI hỏi về chỗ lệch so với plan |
| 4. PR và CI | Ghi nhật ký vào plan, mở một PR chứa mọi thứ, kiểm các danh sách chạy chưa chạy có bị ảnh hưởng không, chờ CI | Xem PR nếu muốn; quyết khi AI báo ảnh hưởng tới danh sách chạy |
| 5. Merge | Merge bằng `scripts/merge-if-green.sh`, chờ `main-guard` | Không kích hoạt phiên tự chạy khi PR còn mở |
| 6. Kiểm tra | Chạy tay `device-session.yml` hay `staging.yml` khi cần | Thử trên máy nếu plan có dòng kiểm tra thủ công; báo kết quả để AI ghi vào nhật ký |

Thường cả task là một PR và một vòng CI. CI đỏ thì AI sửa và đẩy lại; dừng hay revert thì có thêm một PR tài liệu. Không có họp, danh sách chạy hay báo cáo phiên.

## 4. Khi phải dừng

AI dừng đường làm nhanh khi gặp điều kiện dừng ở [mục 4 của risk-scoring.md](../risk-scoring.md), khi thấy việc chạm một điểm chặn ở mục 1 của tài liệu đó, hoặc khi dev muốn bỏ:

- PR làm nhanh được đóng, nhánh giữ lại.
- Yêu cầu, spec nếu đã xác nhận, ticket (`stopped`) và plan có nhật ký lần dừng vẫn lên nhánh chính bằng một PR tài liệu, để không mất lịch sử.
- Muốn làm tiếp thì plan có phiên bản mới, đi đường thường: họp đánh giá rủi ro, danh sách chạy, phiên tự chạy.

Nhánh chính đỏ sau khi merge (issue `main-broken`) vì commit của PR làm nhanh: AI cùng dev revert theo ADR-0025, rồi một PR tài liệu chuyển ticket sang `stopped`, nhật ký plan ghi "đã merge rồi revert". Commit gây lỗi không phải của PR làm nhanh thì AI không revert, chỉ báo dev.

## 5. So với phiên tự chạy

| | Phiên tự chạy | Làm nhanh |
| --- | --- | --- |
| Người | Không cần ngồi cùng | Dev ngồi cùng suốt task |
| Việc | Mọi mức rủi ro, nhiều task | Một task nhỏ, rủi ro Thấp, không điểm chặn |
| Duyệt plan | Họp, PR plan riêng, duyệt chéo khi rủi ro cao | Dev ngồi cùng đồng ý ngay trong phiên |
| Số PR | Ticket, plan, danh sách chạy, mỗi task một PR, báo cáo | Thường một PR |
| Giữ nguyên | Yêu cầu, ticket, plan, test viết trước, rà soát độc lập, CI, `merge-if-green.sh`, nhật ký | Như cột bên |
| Xem lại sau | Báo cáo phiên | Nhật ký plan và PR; plan ghi `lane: quick` |
