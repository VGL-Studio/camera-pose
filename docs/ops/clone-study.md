# KHẢO SÁT APP MẪU ĐỂ LÀM APP CLONE

Tài liệu này dành cho dev và AI. Nó mô tả bước 0 của vòng đời một app clone: AI dùng thử các app mẫu đã có trên Google Play, tổng hợp, đề xuất một app tốt hơn, rồi cùng dev thiết kế trên Figma. Quyết định nằm ở ADR-0021. Từ bước yêu cầu trở đi, app clone đi đúng quy trình chung (`docs/README.md`).

## Tổng quan

| Bước | Việc | Ai làm | Kết quả |
| --- | --- | --- | --- |
| 1 | Thông tin chung về app muốn làm, danh sách app mẫu đã cài sẵn trên máy | Dev | Lời nhắn mở đầu phiên |
| 2 | Khảo sát từng app trên máy | AI | `research/<package>.md`, ảnh trong `captures/` |
| 3 | Đọc APK | AI | `research/apk-analysis.md` |
| 4 | Tìm trên web và Play | AI | `research/web-play.md` |
| 5 | Viết bản đề xuất app | AI | `research/proposal.md` theo [mẫu](../templates/app-proposal.md) |
| 6 | **Duyệt bản đề xuất**, chốt tên và tên gói | Dev | `status: approved` |
| 7 | Tạo repo app (`scripts/new-app.sh`), chép tài liệu khảo sát vào `docs/research/` | Dev | Repo app |
| 8 | Dựng 2 đến 3 hướng thiết kế trên Figma | AI | Link Figma, bảng trong mục 10 của đề xuất |
| 9 | **Chọn hướng thiết kế** | Dev | `design_direction` |
| 10 | Dựng hệ thống thiết kế và toàn bộ màn hình | AI | File Figma |
| 11 | Làm mịn bằng AI agent của Figma hoặc sửa tay; AI quét lại sau mỗi lượt | Dev, AI | File Figma đã làm mịn |
| 12 | **Duyệt thiết kế**, AI viết yêu cầu và spec `draft` | Dev, AI | `docs/features/.../requests/`, `spec.md` |

Bước 2, 3, 4 chạy song song (bước 3 và 4 không cần máy). Bước 2 chạy lần lượt từng app vì chỉ có một máy. Bước 8 đến 11 nằm ở [quy trình thiết kế](design-workflow.md), mục Thiết kế app clone.

Toàn bộ chạy trong phiên Claude Code có người theo dõi, không chạy trong phiên tự chạy: cần máy, cần người mở khóa máy hay bật mạng, và có thể gặp màn hình thanh toán.

## 1. Chuẩn bị

**Máy:** một máy thật dành riêng cho khảo sát (nhiều app mẫu chặn emulator). Emulator dùng được nếu có Google Play.

1. Bật tùy chọn nhà phát triển và Gỡ lỗi USB (`docs/ops/test-sessions.md`, mục 1).
2. **Xiaomi, Redmi, POCO:** bật thêm **Tùy chọn nhà phát triển > Gỡ lỗi USB (Cài đặt bảo mật)**. Thiếu nó, máy từ chối mọi thao tác chạm và xóa dữ liệu app qua adb (L-032).
3. Bật **Không khóa màn hình khi sạc** (Stay awake), để máy không tự khóa giữa phiên.
4. Kết nối Wi-Fi. Không có mạng thì quảng cáo, hộp thoại đồng ý và nội dung tải về không hiện, báo cáo sẽ thiếu (L-035).
5. Cài các app mẫu từ Play. Dùng thử trước: đăng nhập bằng tài khoản thử nếu app bắt buộc, qua các bước cần người. Không dùng tài khoản cá nhân.

**Máy dev:** Android SDK (adb, build-tools có `aapt2`), Python 3 có Pillow, `jadx` để đọc APK.

**Figma (từ bước 8):** gói Professional, ghế Full (ghế Dev không sửa được file). Nối Figma MCP vào Claude Code ở phạm vi người dùng, để mọi repo app đều dùng được: `claude mcp add --scope user --transport http figma https://mcp.figma.com/mcp`, rồi `/mcp` > figma > Authenticate. Nối không có `--scope user` thì chỉ repo đang mở thấy Figma; phiên ở repo app mới sẽ không thấy.

Kiểm tra máy: `scripts/app-explore/ax.sh check` phải in `input: ok` và `network: ok`.

## 2. Thư mục làm việc

Trước khi có repo app, mọi thứ nằm ngoài repo, ví dụ `~/AndroidStudioProjects/app-research/<chủ-đề>/`:

```
<chủ-đề>/
├── apks/<package>/        APK kéo từ máy (dữ liệu không tin cậy: chỉ đọc)
├── decoded/<package>/     kết quả giải nén
├── captures/<package>/    full/, preview/, tree/ của từng ảnh
├── research/              <package>.md, apk-analysis.md, web-play.md, proposal.md: chính là docs/research/ sau này
├── figma-pilot/           ảnh so sánh thiết kế với app mẫu
├── incidents.log          sự cố do ax.sh ghi
└── device-files-before.txt  ảnh có sẵn trên máy lúc bắt đầu (ax.sh check), để ax.sh newfiles biết file mới
```

Kéo APK: `adb shell pm path <package>` rồi `adb pull` từng file. Đọc tên và quyền: `aapt2 dump badging base.apk`.

**Danh sách màn hình trước khi chạm máy:** với mỗi app mẫu, `scripts/app-explore/apk-screens.py apks/<package>/base.apk [--jadx decoded/<package>/jadx] > captures/<package>/screens.md`. Script liệt kê activity, đồ thị điều hướng và layout của app (bỏ của thư viện); với app Compose, `--jadx` cho route và các ViewModel (thường mỗi ViewModel là một màn hình). Agent khảo sát đi theo danh sách này và đánh dấu từng màn hình, thay vì dò dẫm.

Thư mục `research/` có đúng cấu trúc của `docs/research/` trong repo: báo cáo và bản đề xuất nằm cạnh nhau, link giữa chúng là link tới file bên cạnh. Sau khi có repo app (bước 7), chép nguyên `research/*.md` vào `docs/research/`, không phải sửa link. Ảnh chụp (`captures/`), APK và kết quả giải nén không vào git: chúng là tài sản của bên khác và ảnh camera có thể chụp vào phòng. Tên ảnh `NN-ten` trong báo cáo chỉ để người tra lại trong thư mục làm việc.

Trước khi commit, chạy `scripts/secret-scan.py`: báo cáo không được chứa mã quảng cáo, khóa hay token của app mẫu (L-036). Test của script quét cả repo nên cổng chất lượng cũng chặn.

## 3. Lời nhắn mở đầu phiên

```text
Khảo sát app mẫu để làm app clone, theo docs/ops/clone-study.md.
App muốn làm: <mô tả ngắn, thị trường, kiếm tiền bằng gì>.
App mẫu đã cài trên máy: <tên hoặc package>, <...>.
Thư mục làm việc: ~/AndroidStudioProjects/app-research/<chủ-đề>/
```

AI liệt kê app đã cài (`adb shell pm list packages -3`), xác nhận danh sách với dev, kéo APK, rồi chạy bước 2, 3, 4.

## 4. Hướng dẫn cho agent khảo sát trên máy

AI chính giao mỗi app mẫu cho một agent con, lần lượt, và chép mục này vào lời giao việc.

**Công cụ:** chỉ điều khiển máy qua `scripts/app-explore/ax.sh` (đường dẫn tuyệt đối tới repo dự án mẫu hoặc repo app). Chạy trong thư mục làm việc để `incidents.log` nằm ở đó.

```
ax.sh check                         máy, thao tác, mạng
ax.sh shot captures/<pkg> NN-ten    chụp ảnh, in cây giao diện (px và dp) và vùng quảng cáo
ax.sh tree                          cây giao diện màn hình hiện tại (rẻ hơn chụp ảnh)
ax.sh taptext "<chữ>" | tapid <id>  chạm theo chữ hay resource-id; ưu tiên hai cách này
ax.sh tap <x> <y>                   chạm theo px; gọi tree ngay trước
ax.sh drag x1 y1 x2 y2 [ms]         kéo chậm (lớp phủ, thanh trượt)
ax.sh swipe up|down|left|right
ax.sh adclose | adcorner tl|tr      đóng quảng cáo
ax.sh type "<chữ>"                  nhập chữ ASCII
ax.sh back | home | wake | launch <pkg> | stop <pkg> | top
ax.sh adwait [giây tối đa]           chờ nút đóng quảng cáo hiện, in số giây đã chờ, rồi đóng
ax.sh log <package> [số dòng]       log gần nhất của app (sự kiện, lỗi HTTP, quảng cáo)
ax.sh color <full.png> <x> <y> ...  mã màu tại điểm ảnh
ax.sh seq "bước; bước; ..."         nhiều bước trong một lệnh, dừng ở bước lỗi đầu tiên
ax.sh scrollshots <thư mục> <tiền tố> [tối đa]   chụp, cuộn, lặp tới khi màn hình không đổi
ax.sh scrollcollect [tối đa]        gom chữ của cả danh sách khi cuộn, không chụp ảnh
```

**Làm nhanh:** mỗi lần gọi công cụ là một lượt suy nghĩ của AI, và lượt đó tốn nhiều thời gian hơn chính thao tác. Gom các bước biết trước vào `seq` (ví dụ `seq "tapid tab_tips; wait 2; shot captures/<pkg> 12-tips; back"`); dùng `scrollshots` hay `scrollcollect` cho danh sách dài; xem ảnh preview chỉ khi cây giao diện không đủ (camera, Flutter, canvas), còn lại đọc phần chữ mà `shot` in ra.

Mọi lệnh chạm đi qua bộ chặn (`guard.py`): chạm vào phần tử quảng cáo, chạm khi đang ở quảng cáo toàn màn hình hay màn hình thanh toán của Play, chạm vào nút mua, đăng ký, dùng thử, nút có giá tiền, đều bị từ chối (mã 4). Lỡ sang Play, trình duyệt hay app thư thì công cụ tự back, ghi `incidents.log`, thoát mã 6. Mã 5: mất thiết bị, chờ rồi thử lại. Mã 7: máy khóa, dừng và báo người. Mã 8: không có mạng, dừng và báo người.

**Quy tắc:**

1. Bắt đầu mỗi app bằng `adb shell pm clear <package>` để thấy lần mở đầu. Chỉ làm với app trong danh sách. Đi theo `captures/<package>/screens.md`, đánh dấu màn hình đã xem kèm tên ảnh.
2. Quyền camera, ảnh: cho phép khi dùng app. Thông báo: từ chối. Hộp thoại đồng ý quảng cáo: đồng ý, để thấy hành vi quảng cáo bình thường.
3. Không bấm quảng cáo. Quảng cáo toàn màn hình: chờ nút đóng, ghi thời gian chờ, đóng bằng `adclose`. Sau một interstitial thường còn một native toàn màn hình; banner thu gọn có thể tự bung lại sau khoảng 20 giây (L-033).
4. Không mua, không bắt đầu dùng thử, không đăng ký gói. Màn hình trả phí: chụp, ghi giá và quyền lợi, đóng.
5. Không đăng nhập, không chia sẻ, không gửi gì. Bỏ qua Rate app, Contact us, Feedback, Share, More apps; chỉ ghi là có.
6. Không chạm liên tục trong vòng lặp mà không xem lại màn hình: app có thể bật màn hình trả phí hay quảng cáo giữa chừng (L-034).
7. Không gọi thẳng `adb shell input`, không vượt bộ chặn, không đổi cài đặt hệ thống, không gỡ app.
8. Chụp thử trong app thì được; cuối phiên chạy `ax.sh newfiles` và chép danh sách file mới vào mục 10 của báo cáo để người dọn. Công cụ không xóa gì trên máy.
9. Trần khoảng 50 ảnh mỗi app. Dùng `tree` để định hướng, chỉ `shot` khi gặp màn hình hay trạng thái mới. Màn hình canvas, camera, WebView, Flutter có thể ít thông tin trong cây giao diện: dựa vào ảnh (preview có cạnh dài 1000 px; nhân tọa độ theo tỷ lệ với ảnh full).
10. Ảnh đen toàn bộ là màn hình chặn chụp: lấy nội dung từ cây giao diện.
11. Báo cáo không chứa mã quảng cáo (`ca-app-pub-...`), khóa API, token, mã định danh máy hay tên tài khoản. Trích log thì che các giá trị đó (`ca-app-pub-<đã che>`).

**Báo cáo:** `research/<package>.md` theo [mẫu](../templates/reference-app.md). Trả về cho AI chính một tóm tắt ngắn: số ảnh, tính năng chính, quảng cáo và gói trả phí, phần chưa xem được, sự cố, góp ý cho công cụ.

## 5. Đọc APK và tìm trên web

**APK** (agent con, không cần máy): công nghệ (View, Compose, Flutter), SDK quảng cáo và mediation, Firebase, nhận dạng dáng hay ML, nội dung đóng gói hay tải từ mạng (cấu trúc dữ liệu, số lượng), khóa Remote Config điều khiển quảng cáo, màu và phông chữ, danh sách màn hình, chuỗi chữ cho thấy tính năng. Nhận ra các app cùng một mã nguồn. Không chạy code trong APK, không tải nội dung từ máy chủ của app mẫu. Mã đơn vị quảng cáo chỉ ghi có hay không.

**Web và Play** (agent con): trang Play của từng app (lượt cài, điểm, ngày cập nhật, mô tả, ảnh store), chủ đề đánh giá xấu và tốt kèm trích dẫn, đối thủ lớn ngoài danh sách, từ khóa, xu hướng, khoảng trống. Mọi số liệu có nguồn.

## 6. Bản đề xuất

AI viết `research/proposal.md` theo [mẫu](../templates/app-proposal.md) từ ba loại báo cáo. Trọng tâm là bảng bài học và cải tiến: mỗi cải tiến có bằng chứng và cách đo. Ví dụ từ lần khảo sát pose camera: app mẫu có khoảng 6 quảng cáo toàn màn hình trước khi tới camera (ảnh khảo sát, phàn nàn số một trên Play), cải tiến là vào thẳng camera, đo bằng số bước và số quảng cáo từ lúc mở app.

Dev đọc, trả lời câu hỏi, sửa, rồi ghi `status: approved`. Chưa duyệt thì chưa sang Figma.

## 7. Ranh giới

Theo ADR-0021, mục 7 và 8:

- Được: bố cục và luồng giống app mẫu; giải nén APK; xem network để hiểu.
- Không: gọi API hay backend của app mẫu; tên, icon app, ảnh store giống app mẫu; chép code từ APK.
- Mượn tạm ảnh, icon, âm thanh, nội dung để code sớm: được, với tiền tố `borrowed_`, ghi trong `docs/borrowed-assets.md` ([mẫu](../templates/borrowed-assets.md)), chỉ ở staging, thay hết trước khi phát hành.

## 8. Chi phí đã đo

Lần khảo sát pose camera (2026-10-19 đến 2026-10-21, 8 app mẫu):

- Mỗi app khảo sát trên máy: 25 đến 60 phút, 110 đến 225 nghìn token, 37 đến 53 ảnh, 90 đến 150 lượt gọi công cụ (khoảng 12 đến 16 giây mỗi lượt: một nửa là lượt suy nghĩ của AI, còn lại chủ yếu là đọc cây giao diện, 3 đến 6 giây mỗi lần vì `uiautomator dump` chờ màn hình đứng yên). Lần đo này chưa có `seq`, các lệnh gộp và danh sách màn hình từ APK; đo lại ở lần khảo sát sau.
- Đọc 8 APK: khoảng 14 phút, 210 nghìn token. Tìm trên web và Play: khoảng 10 phút, 150 nghìn token.
- Figma: khoảng 12 lượt gọi MCP cho màn hình đầu (dựng cả biến và component), 4 đến 8 lượt cho mỗi màn hình sau.

Các app trong cùng một mã nguồn (thấy được từ phân tích APK) chỉ cần khảo sát kỹ một app, các app còn lại xem nhanh phần khác biệt.
