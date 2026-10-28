---
id: ADR-0031
title: Test có sẵn kiểm hành vi mà plan đổi được tìm từ lúc soạn plan, và phiên tự viết lại câu khẳng định khi plan đã duyệt rõ điều mới
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-27
deciders: [Nhật]
supersedes:
superseded_by:
---

# ADR-0031: TEST CÓ SẴN KIỂM HÀNH VI MÀ PLAN ĐỔI ĐƯỢC TÌM TỪ LÚC SOẠN PLAN, VÀ PHIÊN TỰ VIẾT LẠI CÂU KHẲNG ĐỊNH KHI PLAN ĐÃ DUYỆT RÕ ĐIỀU MỚI

## Bối cảnh

RUN-20261026-05 merge ba task, nhưng T-041 dừng ở bước 5, và T-045 phụ thuộc nó nên bị bỏ qua.

- Plan T-041 đã duyệt đặt khung thẻ 250 dp đè ở đáy vùng bước, để pager cao như nhau ở mọi bước.
- Test có sẵn `AdGalleryScreenTest.flowSteps_cardOutsidePager_followsStep` (T-032) khẳng định thẻ nằm *dưới* pager.
- Code đã viết xong, nhưng câu khẳng định cũ đỏ. Sửa nó là "đổi điều test cũ kiểm về hành vi đã có" (`risk-scoring.md` mục 4, điều kiện 5; ADR-0029, quyết định 2), nên phiên phải dừng.

Câu hỏi có sẵn đề xuất rõ, và người trả lời trong một câu theo đề xuất. Dù vậy, hai task phải chờ sang phiên sau. Phiên chạy trong đêm nên không ai trả lời giữa phiên được.

Hai chỗ hở:

1. **Lúc soạn plan, không ai tìm test có sẵn kiểm hành vi mà plan đổi.**
   - Chạy thử của plan dùng khung tạm cao 0 dp nên không thấy mâu thuẫn (L-106).
   - Hai phiên rà soát độc lập cũng không thấy.
   - ADR-0029 đã quyết không chạy toàn bộ test lúc soạn plan. Phần tìm này phải làm được mà không chạy test.
2. **Trong phiên, mọi câu khẳng định cũ mâu thuẫn với plan đều làm dừng task.**
   - Áp dụng cả khi plan đã duyệt rõ điều mới.
   - Áp dụng cả khi điều test cũ thực sự giữ (ở T-041: thẻ ngoài pager, không che nội dung của bước) vẫn kiểm được trên bố cục mới.

## Quyết định

**1. Tìm từ lúc soạn plan, bằng đọc code, không chạy test.**

- Plan sửa hay xóa code Kotlin (ngoài thư mục test) có mục 3.5 "Hành vi đã có bị đổi". Mỗi hành vi plan đổi có một dòng gồm:
  - test có sẵn đang khẳng định hành vi đó;
  - câu khẳng định giữ nguyên hay đổi; đổi thì viết sẵn câu mới, hoặc đặt câu hỏi ở mục 8.
- `scripts/existing-tests.py <plan>` lập danh sách để bắt đầu: test có sẵn dùng tên hay tag của các file plan sửa. Test đo vị trí, kích thước và dùng tên mà plan nêu đứng đầu.
- `scripts/plan-check.py` (quy tắc 13) báo lỗi khi plan `draft` sửa code Kotlin mà thiếu mục 3.5.
- Rà soát độc lập trước khi duyệt xét mục này như mục 3.2.
- Không bắt chạy thử. Câu khẳng định mới viết sẵn trong plan, rồi phiên dùng như test viết trước.

**2. Trong phiên, viết lại câu khẳng định cũ mà không dừng, khi đủ cả sáu điều:**

1. **Plan đã duyệt nói rõ điều mới.** Câu khẳng định cũ trái trực tiếp một câu của mục 3.1, mục 3.5, câu trả lời ở mục 8 hay spec. Ghi lại câu đó. Câu cũ đỏ vì một hành vi mà plan không nói tới thì đó là lỗi của code: sửa code (ADR-0029, quyết định 2).
2. **Ý định của test vẫn được kiểm.**
   - Viết ý định thành một câu: điều test giữ cho người dùng hay cho luật, dựa vào tên test, chú thích, kịch bản hay luật mà test dẫn.
   - Câu mới kiểm đúng ý định đó trên hành vi mới.
3. **Không nới.**
   - Không bớt câu khẳng định.
   - Không đổi phép bằng thành khoảng hay phép chứa.
   - Không tăng sai số.
   - Không bỏ đối chứng.
   - Không thêm `@Ignore`.
4. **Có đối chứng.** Câu mới đỏ khi ý định bị phá, ví dụ tạm đặt thẻ đè lên chữ của bước. Ghi lại lần chạy đó.
5. **Chỉ là câu khẳng định của test JVM.** Không áp dụng cho:
   - ảnh chuẩn screenshot (vẫn dừng);
   - test viết trước của chính plan (điều kiện 8);
   - câu kiểm một tiêu chí của spec hay một luật trong `ads-business-rules.md` mà plan không đổi.
6. **Tối đa 3 câu khẳng định mỗi task.** Hơn thế thì plan đã thiếu nhiều, nên dừng như cũ.

Thiếu một điều thì dừng theo điều kiện 3 hay 5 như trước.

**3. Rà soát và báo sau, như phần ngoài mục 3.2 của ADR-0029.**

- Rà soát độc lập (bước 6) nhận câu cũ, câu mới, câu của plan nói điều mới, câu ý định và lần chạy đối chứng. Vi phạm một trong sáu điều là mức chặn.
- Mô tả PR có mục "Test cũ viết lại (ADR-0031)".
- Nhật ký plan ghi ở dòng "Lệch so với plan".
- Báo cáo phiên có một dòng `- [ ]` ở mục Cần người kiểm tra cho mỗi task có câu viết lại.

**4. Áp dụng.** Cùng PR với ADR này:

- `docs/risk-scoring.md` (mục 4), `docs/auto-run.md` (bước 4, 6, 10), `CLAUDE.md` (Không được làm) dẫn về ADR này.
- `docs/CLAUDE.md` (Khi viết plan), mẫu plan (mục 3.5), mẫu phiên (Cần người kiểm tra), `docs/ops/pre-run-review.md` thêm phần của quyết định 1.
- `scripts/existing-tests.py` và quy tắc 13 của `scripts/plan-check.py`, có test.

ADR-0029 giữ nguyên; ADR này thêm một ngoại lệ hẹp cho quyết định 2 của nó.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tìm từ lúc soạn plan, và phiên tự viết lại câu khẳng định khi đủ sáu điều (đã chọn) | Phần lớn mâu thuẫn được quyết trước khi duyệt; phần còn lọt không làm mất task cả đêm; test vẫn giữ ý định, người xem lại sau | Phiên phải phán đoán "ý định" của test; dựa vào rà soát độc lập và đối chứng để không nới |
| Hỏi người ngay trong phiên, chờ trả lời rồi làm tiếp | Người quyết từng câu | Phiên chạy trong đêm, không ai trả lời |
| Chỉ tìm từ lúc soạn plan, giữ luật dừng | Không đổi luật về test | Phần lọt vẫn làm mất task và cả chuỗi phụ thuộc |
| Chỉ cho phiên tự viết lại | Không thêm việc lúc soạn plan | Mâu thuẫn chỉ lộ ra sau khi đã viết code; nhiều câu viết lại hơn, người phải xem lại nhiều hơn |
| Chạy toàn bộ test trên bản dựng thử lúc soạn plan | Thấy mọi test đỏ trước khi duyệt | ADR-0029 đã bỏ: dồn thời gian chờ vào ban ngày của dev |

## Hệ quả

- **Tích cực:**
  - Mâu thuẫn kiểu T-041 lộ ra lúc soạn plan, thành một dòng của mục 3.5 hay một câu hỏi.
  - Một câu khẳng định cũ không còn làm mất cả task và các task phụ thuộc trong đêm.
  - Mỗi câu viết lại có ý định, đối chứng và một dòng chờ người xem.
- **Phải chấp nhận:**
  - Danh sách của `existing-tests.py` là gợi ý theo tên, không đầy đủ (test dùng chuỗi tag viết tay, hay đi qua hàm phụ, có thể lọt) và có thể dài.
  - Phiên phán đoán ý định của test; người xem lại sau khi đã merge.
- **Việc AI phải tuân thủ:** plan sửa hay xóa code Kotlin ngoài thư mục test có mục 3.5 "Hành vi đã có bị đổi", liệt kê test có sẵn kiểm hành vi plan đổi (bắt đầu từ `scripts/existing-tests.py <plan>`) và câu khẳng định giữ nguyên hay câu mới viết sẵn, chỗ chưa quyết thì đặt câu hỏi ở mục 8; trong phiên, câu khẳng định của test JVM có sẵn đỏ vì trái trực tiếp điều plan đã duyệt nói rõ thì viết lại khi đủ sáu điều: có câu của plan nói điều mới, có câu ý định và câu mới kiểm đúng ý định đó, không nới (không bớt câu, không đổi bằng thành khoảng hay chứa, không tăng sai số, không bỏ đối chứng, không `@Ignore`), có đối chứng đỏ khi ý định bị phá, không phải ảnh chuẩn hay test viết trước của plan hay câu kiểm tiêu chí của spec hay luật nghiệp vụ mà plan không đổi, tối đa 3 câu mỗi task; thiếu một điều thì dừng như cũ; mỗi câu viết lại ghi vào nhật ký plan, mục "Test cũ viết lại (ADR-0031)" của mô tả PR và một dòng `- [ ]` ở mục Cần người kiểm tra; rà soát độc lập xét riêng từng câu, vi phạm là mức chặn.
