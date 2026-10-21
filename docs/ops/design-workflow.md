# QUY TRÌNH THIẾT KẾ: TỪ CLAUDE DESIGN HOẶC FIGMA TỚI GIAO DIỆN THẬT

Tài liệu này dành cho chủ feature (người dựng thiết kế), người phụ trách sản phẩm (người duyệt) và dev. Quyết định nằm ở ADR-0008 và ADR-0020; app clone dựng thiết kế theo mục 8 (ADR-0021). Nhóm dùng Claude Design hoặc Figma tùy dự án; từ bước bàn giao trở đi, hai công cụ đi chung một đường.

## Tổng quan

| Bước | Việc | Ai làm |
| --- | --- | --- |
| 1 | Dựng hệ thống thiết kế trên công cụ từ code (một lần cho mỗi app, làm lại khi hệ thống đổi) | Chủ feature |
| 2 | Dựng màn hình ở khung chuẩn, đủ trạng thái | Chủ feature |
| 3 | Duyệt thiết kế | Người phụ trách sản phẩm |
| 4 | Xuất ảnh, lập gói bàn giao trong repo | Chủ feature, AI giúp trong phiên có người theo dõi |
| 5 | AI viết giao diện bằng Compose theo plan | Phiên tự chạy |
| 6 | Đối chiếu: screenshot test, ảnh so sánh, rà soát độc lập | Phiên tự chạy |
| 7 | Duyệt giao diện thật trên ảnh so sánh và bản staging | Người phụ trách sản phẩm |

## 1. Hệ thống thiết kế: từ code sang công cụ

Code là gốc (ADR-0008). Nguồn để dựng trên công cụ:

- Ảnh catalog: `core/designsystem/src/test/screenshots/DesignSystem_*.png` (màu sáng, màu tối, chữ, khoảng cách và thành phần).
- Giá trị: `docs/design-system/tokens.json` (mã màu theo vai trò Material 3, cỡ chữ, chiều cao dòng, độ đậm, khoảng cách).

**Claude Design:** tạo design system mới, ở bước đưa nguồn vào, tải lên các ảnh catalog và `tokens.json` (Claude Design chỉ ghi rõ cách đọc mã nguồn React, nên không dùng cách trỏ thẳng vào repo Compose). Xem lại kết quả, chạy thử một dự án nhỏ, rồi bật **Published**. Hướng dẫn chính thức: https://support.claude.com/en/articles/14604397-set-up-your-design-system-in-claude-design

**Figma:** trong file thư viện của app, tạo biến màu (hai chế độ Light, Dark), kiểu chữ và biến khoảng cách theo `tokens.json`, đặt tên giống tên trong file (ví dụ `primary`, `bodyLarge`, `md`). Dựng thành phần `PrimaryButton` giống ảnh catalog. REST API biến của Figma cần gói Enterprise, nhưng Figma MCP (gói Professional, ghế Full) tạo được biến qua Plugin API: AI làm việc này trong phiên có người theo dõi (mục 8), hoặc người làm bằng tay.

Khi `:core:designsystem` đổi (plan rủi ro cao, ADR-0008), test `DesignTokensTest` và ảnh catalog đổi theo. Chủ feature cập nhật lại công cụ theo các file mới.

## 2. Dựng màn hình

- Khung **411 x 891** (Figma: frame 411 x 891; Claude Design: yêu cầu bản mẫu cho điện thoại kích thước 411 x 891).
- **Điện thoại màn hình gập** (khi spec hay bản đề xuất ghi app hỗ trợ): thêm frame khung mở ra **673 x 841** cho các màn hình có bố cục đổi khi màn hình rộng (lưới thêm cột, chia hai cột, công cụ dời sang cạnh), tên `<Ten>Screen_<trạng thái>_<light|dark>_unfolded`. Khi gập lại, app dùng bố cục điện thoại nên không cần frame riêng. Máy tính bảng không thuộc khung này; app không hỗ trợ tablet thì loại tablet bằng danh mục thiết bị trên Play Console.
- Tên trạng thái viết thường, có thể nối bằng gạch nối (`permission-denied`, `no-person`).
- Chỉ dùng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế. Cần thành phần mới thì ghi vào mục 3 của mẫu thiết kế; đó là thay đổi module core, rủi ro cao.
- Đủ trạng thái có trong spec: thường là bình thường, đang tải, rỗng, lỗi, và chế độ tối.
- Chữ trên thiết kế là chữ thật, đúng như sẽ hiện trong app.
- Bố cục phải dùng được ở màn hình thấp (360 x 640): ghi trong mục 6 của mẫu thiết kế phần nào cuộn, phần nào ghim ở đáy, khoảng trống nào co lại. Không cần dựng thêm frame ở khung thấp; app kiểm tra bằng screenshot test `<Ten>Screen_<trạng thái>_small.png` (L-028). Emulator của `device-session.yml` có màn hình 320 x 640, nên lỗi bố cục ở màn hình thấp còn lộ ra ở đó.

## 3. Duyệt thiết kế

Người phụ trách sản phẩm xem trên công cụ (link chia sẻ quyền xem hoặc bình luận), góp ý bằng bình luận, đồng ý thì ghi `approved_by`, `approved_at` trong `design/README.md` (bước 4).

## 4. Xuất ảnh và lập gói bàn giao

Gói nằm ở `docs/features/<feature>/design/`, theo [mẫu thiết kế](../templates/design.md):

```text
docs/features/<feature>/design/
├── README.md            mẫu thiết kế: màn hình, trạng thái, bảng chữ, ánh xạ thành phần, link công cụ
├── screens/             ảnh PNG 1233 x 2673, mỗi màn hình và trạng thái một ảnh
│   ├── LoadingScreen_progress_light.png
│   └── LoadingScreen_failed_light.png
├── context/             số liệu của từng màn hình lấy từ công cụ, cùng tên với ảnh, đuôi .md
│   ├── LoadingScreen_progress_light.md
│   └── LoadingScreen_failed_light.md
└── review/              do scripts/design-compare.sh sinh ra (bước 6), không sửa tay
```

`context/` cho AI số liệu chính xác của bản đã duyệt (bố cục, khoảng cách, màu, chữ, tên biến) mà không phải đọc công cụ thiết kế lúc làm. Phiên tự chạy không nối với Figma hay Claude Design: thiết kế có thể đổi sau khi duyệt, công cụ có thể lỗi hay hết lượt giữa phiên, và connector có cả quyền sửa thiết kế (ADR-0020).

Mỗi file `context/<tên ảnh>.md` gồm:

1. **Nguồn:** link frame (Figma: link có `node-id`), ngày lấy, người lấy.
2. **Giá trị dùng:** màu, kiểu chữ, khoảng cách, đối chiếu theo giá trị với `docs/design-system/tokens.json` (Figma: `scripts/figma-export.py` làm việc này). Giá trị không có trong `tokens.json` được đánh dấu "ngoài hệ thống" để plan quyết định.
3. **Cấu trúc và số liệu:** cây phần tử theo thứ tự từ trên xuống, với kích thước, khoảng cách, căn lề, màu, kiểu chữ và chữ của từng phần tử (Figma: do `scripts/figma-export.py` sinh).
4. **Code tham khảo của công cụ** (nếu có), để nguyên trong khối code, ghi rõ "chỉ để đọc số liệu, không chép": công cụ sinh code web, app viết bằng Compose (ADR-0008).

AI lập `context/` trong phiên có người theo dõi, cùng lúc với `screens/`; người duyệt xem file trước khi commit. Thiết kế đổi sau khi duyệt thì lấy lại cả ảnh và `context/`.

Tên ảnh là `<Ten>Screen_<trạng thái>_<light|dark>.png`, trùng tên ảnh chuẩn screenshot mà plan sẽ tạo (ví dụ `LoadingScreen_failed_light.png` cho composable `LoadingScreen` ở trạng thái lỗi, giao diện sáng).

**Xuất từ Figma (cách chính, mọi gói Figma):** đặt các frame trong một section hay trang, tên frame trùng tên ảnh (`LoadingScreen_failed_light`, khung 411 x 891). Chọn section, bấm **Copy link**, rồi chạy trên máy (hoặc nhờ AI chạy trong phiên có người theo dõi):

```sh
scripts/figma-export.py "<link section>" <feature>
```

Script gọi REST API của Figma, ghi `screens/<frame>.png` ở mức 3x (kiểm tra đúng 1233 x 2673, hay 2019 x 2523 với frame `_unfolded`) và `context/<frame>.md` (bố cục, khoảng cách, màu, chữ, đối chiếu với `tokens.json`). Frame có tên khác quy tắc bị bỏ qua, nên các bản nháp để cùng section không sao. REST API không bị giới hạn như Figma MCP, nên dùng được cả với gói Starter.

Token: Figma > **Settings > Security > Personal access tokens > Generate new token**, chỉ chọn quyền **File content: Read-only**, hạn ngắn nhất đủ dùng. Lưu vào biến môi trường của máy, không lưu vào repo hay routine:

```sh
echo 'export FIGMA_TOKEN=<token>' >> ~/.zshrc   # rồi mở lại terminal và Claude Code
```

Script đọc token từ biến môi trường và không bao giờ in ra. Lộ token thì thu hồi ở cùng trang trong Figma.

**Figma MCP (gói Professional trở lên):** dùng thêm khi cần đọc sâu hơn, ví dụ tên biến của Figma (`get_variable_defs`) hay Code Connect. Gói Starter chỉ có vài lượt gọi MCP mỗi tháng; gói Professional có 200 lượt mỗi ngày. Cách tay khi không có token: chọn frame, Export setting **PNG, 3x**, **Export**.

**Xuất từ Claude Design:** tài liệu chưa có xuất PNG. Mở bản mẫu trong Chrome, bật DevTools > chế độ thiết bị, đặt kích thước 411 x 891 và DPR 3, mở menu (dấu ba chấm) > **Capture screenshot**. Ảnh ra đúng 1233 x 2673. Lưu kèm gói **Export > .zip** vào `design/handoff/`, rồi nhờ AI đọc HTML/CSS trong gói để viết `context/` theo cùng cấu trúc (không có tên biến thì ghi giá trị và token tương ứng trong `tokens.json`).

**Nối Figma MCP vào Claude Code (chỉ cho phiên có người theo dõi):** `claude mcp add --scope user --transport http figma https://mcp.figma.com/mcp`, rồi `/mcp` > figma > Authenticate (phạm vi người dùng, để mọi repo app đều dùng được). Không thêm connector này vào routine.

Kiểm tra gói: mỗi ảnh trong `screens/` đúng 1233 x 2673 (`sips -g pixelWidth -g pixelHeight <ảnh>` trên macOS) và có file `context/` cùng tên, tên đúng quy tắc, `README.md` có bảng chữ và bảng ánh xạ.

## 5. AI viết giao diện

Plan của ticket ghi trong mục 2 đường dẫn gói thiết kế, và trong mục 4 một screenshot test cho mỗi ảnh trong `screens/`, cùng tên. AI lấy số liệu từ `context/`, nhìn ảnh trong `screens/` để hiểu tổng thể, viết lại bằng Compose theo token, không chép code web của bản mẫu (ADR-0008). Giá trị đánh dấu "ngoài hệ thống" trong `context/` được plan quyết định trước: làm tròn về token gần nhất, hoặc thêm token (thay đổi module core, rủi ro cao).

## 6. Đối chiếu

Trong phiên tự chạy (`docs/auto-run.md`, bước 5 và 6):

1. Screenshot test và test giao diện qua (cổng chặn).
2. `scripts/design-compare.sh <feature>` sinh ảnh ghép thiết kế, thật, khác biệt và `review/summary.md` với tỷ lệ điểm ảnh khác. Không chặn merge.
3. Phiên rà soát độc lập xem từng ảnh ghép theo mục 7 của mẫu thiết kế và xếp sai lệch theo mức chặn, nên sửa, ghi chú.
4. `review/` được commit cùng PR của task, `summary.md` được dán vào mô tả PR.

Tỷ lệ khác biệt chỉ để tham khảo: phông chữ và cách vẽ chữ luôn làm lệch vài phần trăm. Mắt người và phiên rà soát quyết định.

## 7. Duyệt giao diện thật

Người phụ trách sản phẩm mở `design/review/*_compare.png` trên PR hoặc trong repo, và xem bản staging. Đồng ý thì ghi vào báo cáo phiên hoặc vào PR; ảnh chuẩn screenshot từ đó giữ giao diện. Không đồng ý thì ghi phản hồi (`docs/README.md`, bước phản hồi), plan sửa đi qua họp đánh giá như thường.

## 8. Thiết kế app clone: chọn hướng, AI dựng, người làm mịn

Với app clone (ADR-0021, `docs/ops/clone-study.md`), bước 1 đến 3 ở trên làm theo cách sau. Điều kiện: bản đề xuất app đã được dev duyệt; Figma Professional ghế Full đã nối MCP; repo app đã có.

**Mục tiêu:** app mới tốt hơn app mẫu và có nhận diện riêng. Giữ các khuôn tương tác người dùng đã quen (ví dụ nút chụp tròn ở giữa đáy, hàng lựa chọn cuộn ngang). Đổi màu, bộ icon, phông chữ, hình dạng (bo góc, thẻ, nút), kiểu hình minh họa, giọng văn.

### 8.1. Chọn hướng thiết kế

1. AI dựng 2 đến 3 hướng phong cách trong file Figma của app, mỗi hướng một trang `Direction/<tên>` gồm:
   - Bảng phong cách: màu (sáng, tối), kiểu chữ, khoảng cách, bo góc, icon mẫu, nút, thẻ, chip.
   - Hai màn hình quan trọng nhất ở khung 411 x 891, dựng bằng chính bảng phong cách đó (ví dụ màn hình chính và màn hình chức năng chính).
2. Các hướng khác nhau thật sự (ví dụ tối và tối giản, sáng và ấm, nhiều màu và vui), không chỉ đổi màu nhấn. Icon lấy từ một bộ có giấy phép dùng thương mại rõ ràng (ví dụ Material Symbols, Lucide), cả app dùng một bộ.
3. AI ghi bảng các hướng vào mục 10 của bản đề xuất, kèm link từng trang và ảnh so sánh với màn hình tương ứng của app mẫu tốt nhất.
4. **Dev chọn một hướng hoặc ghép**, ghi `design_direction` trong bản đề xuất. Dev có thể làm mịn hướng được chọn (8.3) trước khi AI dựng toàn bộ.

Đổi ý ở bước này chỉ tốn vài màn hình; đổi sau khi đã dựng toàn bộ thì tốn cả bộ.

### 8.2. AI dựng cấu trúc

AI dùng Figma MCP trong phiên có người theo dõi:

- Hệ thống thiết kế từ hướng đã chọn: biến màu (chế độ Light, Dark), kiểu chữ, biến khoảng cách và bo góc, component (nút, thẻ, chip, thanh điều hướng, icon dạng component có thể đổi). Tên biến khớp `tokens.json` khi có vai trò tương ứng.
- Đủ màn hình và trạng thái trong mục 7 của bản đề xuất, tên frame và khung theo mục 2 của tài liệu này. Mọi màu, chữ, khoảng cách dùng biến; phần lặp lại là instance của component.
- Số đo bám theo bản đề xuất và báo cáo khảo sát (dp từ cây giao diện), đã cải tiến theo bảng bài học.
- Ảnh và icon mượn tạm từ app mẫu (nếu có) đặt tên `borrowed_...` trong Figma để dễ thay.

### 8.3. Người làm mịn

Dev làm mịn phần thẩm mỹ bằng AI agent của Figma (sửa được trên file hiện có) hoặc sửa tay: màu, bo góc, bóng, nhịp khoảng cách, cân đối thị giác, icon.

- Sửa ở component và biến trước, màn hình sau: sửa một component thì mọi màn hình đổi theo.
- Trước mỗi lượt, lưu mốc phiên bản: File > Save to version history.
- Xong một lượt thì báo AI, kèm vài dòng đã đổi gì.

### 8.4. AI quét lại sau mỗi lượt làm mịn

AI đọc file qua MCP (chỉ đọc) và báo:

- Instance bị tách khỏi component.
- Màu, cỡ chữ, khoảng cách viết cứng thay vì dùng biến.
- Frame bị đổi tên hay đổi khung khác 411 x 891 (673 x 841 với frame `_unfolded`), làm `figma-export.py` và screenshot test không khớp.
- Phông chữ, kiểu chữ nằm ngoài hệ thống.
- Màn hình hay trạng thái trong bản đề xuất còn thiếu.

Giá trị mới dev muốn giữ thì AI đưa vào biến hay component, rồi áp lại cho các màn hình. Khi quét sạch và dev hài lòng, người phụ trách sản phẩm duyệt (mục 3) và đi tiếp mục 4 như mọi app.

### 8.5. Thước đo khi duyệt

- So từng màn hình với màn hình tương ứng của app mẫu tốt nhất (ảnh ghép đặt cạnh nhau): không kém hơn.
- Các cải tiến trong bảng bài học của bản đề xuất có mặt trên thiết kế.
- Chữ đủ tương phản, vùng chạm tối thiểu 48 dp, có chế độ tối, chữ tiếng Việt dài không bị cắt, quảng cáo không che phần điều khiển.

