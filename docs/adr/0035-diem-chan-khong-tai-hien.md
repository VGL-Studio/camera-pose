---
id: ADR-0035
title: Điểm chặn của rà soát độc lập nói code chạy sai mà test chạy thật không tái hiện được thì phiên ghi "không tái hiện" và đi tiếp, có người kiểm sau
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes:                  # bổ sung mức Chặn của docs/auto-run.md bước 6; không thay ADR nào
superseded_by:
---

# ADR-0035: ĐIỂM CHẶN CỦA RÀ SOÁT ĐỘC LẬP NÓI CODE CHẠY SAI MÀ TEST CHẠY THẬT KHÔNG TÁI HIỆN ĐƯỢC THÌ PHIÊN GHI "KHÔNG TÁI HIỆN" VÀ ĐI TIẾP, CÓ NGƯỜI KIỂM SAU

Nguồn: app camera-pose, bài học L-052 của app (RUN-20261023-04, T-015), issue #250 của dự án mẫu; bài học L-118 của dự án mẫu.

## Bối cảnh

Rà soát độc lập (`docs/auto-run.md` bước 6) chỉ đọc plan, spec và phần thay đổi; nó không chạy code. Ở RUN-20261023-04 của camera-pose, rà soát của T-015 nêu một điểm chặn bằng cách đọc code: vùng chạm của thanh trượt dưới 48 dp. Phiên viết code viết một test chạm thật ở cả hai phía của thanh (`AdjustPanelTest.adjustSlider_touchHeight48`), và test qua: điểm chặn không tái hiện được.

Bước 6 có ba mức (Chặn, Nên sửa, Ghi chú). Mức Chặn luôn quay lại bước 4 và tính vào giới hạn 3 lần sửa. Không có cách ghi "đã kiểm, không tái hiện", nên phiên chỉ còn hai đường: sửa một thứ không sai cho tới hết 3 lần rồi dừng task, hoặc tự bỏ qua điểm chặn mà không để lại dấu. Phiên chạy qua đêm không có người để hỏi (ADR-0031), và hướng của nhóm là phiên chạy hết mà không dừng vô cớ.

## Quyết định

**1. Điểm chặn được ghi "không tái hiện" khi đủ bốn điều.** Điểm chặn nói code của task chạy sai (một hành vi, một số đo, một trạng thái giao diện) có thể được phiên viết code ghi là "không tái hiện" thay cho quay lại bước 4, khi:

- **Test chạy thật kiểm đúng điều rà soát nêu.** Test mới chạy code (unit test, test giao diện Robolectric, test của script), khẳng định đúng điều mà điểm chặn nói là sai, với cùng ngưỡng hay giá trị rà soát nêu. Không tính: test có sẵn chạy lại, test đọc mã nguồn hay so chuỗi trong code, test kiểm một điều gần giống.
- **Test qua trên code của task**, không sửa dòng code nào của task cho test qua. Đã sửa code thì đó là sửa điểm chặn theo bước 4 như thường, không phải "không tái hiện".
- **Đột biến làm test thất bại.** Tạm đổi code của task theo đúng hướng điểm chặn mô tả (ví dụ giảm vùng chạm xuống dưới 48 dp), chạy lại test thì test thất bại, rồi trả code lại. Ghi lệnh, phần đổi tạm và kết quả vào nhật ký plan (như bước đối chứng của L-055 và L-108).
- **Test nằm trong file test được phép** của mục 3.2, hay ngoài mục 3.2 nhưng được phép theo ADR-0029, và vào commit của task.

Thiếu một điều thì điểm vẫn là Chặn và xử lý như cũ.

**2. Không áp dụng cho các điểm chặn về quy tắc.** Chỉ điểm chặn nói code chạy sai mới được ghi "không tái hiện". Không áp dụng cho điểm chặn về:

- lệch so với plan ngoài danh sách của ADR-0032, ADR-0033;
- file ngoài mục 3.2 không được phép (ADR-0029);
- test viết trước bị đổi điều nó kiểm, test cũ bị xóa hay nới (L-093, L-095), câu khẳng định viết lại sai ADR-0031;
- cấu hình production, khóa bí mật, mã quảng cáo thật;
- giao diện sai thiết kế (chữ, trạng thái, thứ tự, vai trò màu);
- phiên rà soát và phiên viết code hiểu spec khác nhau. Trường hợp này vẫn dừng task theo bước 6.

**3. Rà soát lại.** Test mới và kết quả đột biến đi qua vòng rà soát lại của bước 6, như phần sửa của một điểm nên sửa, và tính vào giới hạn 2 vòng rà soát lại của task. Vòng đó nhận: nguyên văn điểm chặn, test, lần chạy qua và lần chạy đột biến. Rà soát lại thấy test không kiểm đúng điều đã nêu thì điểm là Chặn như cũ, quay lại bước 4. Mỗi điểm chặn chỉ được ghi "không tái hiện" một lần. Lần ghi đó không tính vào giới hạn 3 lần sửa.

**4. Báo cáo và người kiểm.** Điểm ghi "không tái hiện" được ghi ở hai chỗ của báo cáo phiên:

- mục "Quyết định AI đã tự làm": task, nguyên văn điểm chặn, tên test, lần đột biến, kết quả rà soát lại;
- một dòng `- [ ]` ở mục "Cần người kiểm tra", để người kiểm tay hành vi đó trên máy.

Mô tả PR ghi cùng nội dung ở mục "Đã sửa theo rà soát". Ở đường làm nhanh (ADR-0034), dev ngồi cùng quyết điểm chặn ngay trong phiên; điều 1 vẫn là bằng chứng phải đưa cho dev.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Ghi "không tái hiện" khi có test chạy thật, đột biến và rà soát lại (đã chọn) | Phiên không dừng vì điểm chặn sai; bằng chứng là test chạy thật nằm lại trong repo; người vẫn kiểm sau | Phiên tự gỡ một điểm chặn trước khi người xem; thêm việc cho rà soát lại |
| Giữ như cũ, điểm chặn luôn quay lại bước 4 | Đơn giản, rà soát luôn thắng | Điểm chặn sai làm task dừng sau 3 lần sửa không cần thiết, hoặc phiên bỏ qua mà không để lại dấu |
| Hạ mọi điểm chặn đọc code xuống Nên sửa | Không cần quy tắc mới | Mất mức chặn cho lỗi thật mà rà soát đọc code thấy đúng |
| Để người quyết sáng hôm sau | Người xem trước khi đi tiếp | Phiên qua đêm dừng task; trái hướng phiên chạy hết |

## Hệ quả

- **Tích cực:**
  - Điểm chặn sai không làm dừng task hay tốn 3 lần sửa.
  - Test chạy thật kiểm đúng điều rà soát lo ngại nằm lại trong repo, nên lần đổi code sau mà phạm đúng điều đó thì test đỏ.
- **Phải chấp nhận:**
  - Phiên tự gỡ một điểm chặn, và PR có thể merge trước khi người kiểm tay.
  - Một điểm chặn đúng có thể lọt nếu test và lần đột biến đều sai cùng hướng. Rà soát lại và mục Cần người kiểm tra là hai lớp chặn cho trường hợp đó.
- **Việc AI phải tuân thủ:**
  - Chỉ ghi "không tái hiện" cho điểm chặn nói code của task chạy sai, khi đủ bốn điều:
    - test mới chạy code, khẳng định đúng điều và ngưỡng mà điểm chặn nêu;
    - test qua mà không sửa code của task;
    - đột biến theo hướng điểm chặn làm test thất bại, ghi vào nhật ký plan;
    - test nằm trong file được phép và vào commit của task.
  - Không áp dụng cho điểm chặn về lệch plan, file ngoài phạm vi, test bị đổi hay xóa, ADR-0031, cấu hình production, bí mật, mã quảng cáo thật, thiết kế hay hiểu spec khác nhau.
  - Test và lần đột biến đi qua vòng rà soát lại, tính vào giới hạn 2 vòng. Rà soát lại không đồng ý thì điểm là Chặn như cũ.
  - Mỗi điểm chỉ ghi "không tái hiện" một lần.
  - Ghi vào mục "Quyết định AI đã tự làm", thêm một dòng `- [ ]` ở "Cần người kiểm tra", và ghi trong mô tả PR.
