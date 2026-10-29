# KẾ HOẠCH QUẢNG CÁO, REMOTE CONFIG VÀ SỰ KIỆN TRÊN EXCEL

Tài liệu này dành cho dev và người phụ trách kinh doanh của mọi app tạo từ dự án mẫu. Nó nói cách sinh, điền, cập nhật và dùng file Excel kế hoạch quảng cáo của app. Luật quảng cáo nằm ở [ads-business-rules.md](ads-business-rules.md), cách thêm vị trí trong code ở [ads-guide.md](ads-guide.md), cách đặt tên và đổi khóa ở [remote-config.md](remote-config.md).

## 1. File này để làm gì

Nhóm kinh doanh và dev cùng nhìn một bảng khi bàn quảng cáo: vị trí nào, ở màn nào, khi nào hiện, bật hay tắt lúc phát hành, khóa Remote Config nào điều khiển, sự kiện nào đo. File thay cho bảng tính viết tay mà mỗi app tự làm một kiểu.

File không viết tay. Script `scripts/ads-plan.py` sinh file từ code của app, nên tên khóa, kiểu, giá trị mặc định và danh sách vị trí luôn đúng như code. Phần nhóm điền (màn hình, thời điểm, giá trị phát hành, ghi chú) được giữ lại khi sinh lại.

File không phải nguồn sự thật cuối cùng:

| Thông tin | Nguồn sự thật |
| --- | --- |
| Vị trí, khóa, sự kiện đã duyệt | Spec của feature, mục 5 và 6 |
| Vị trí, khóa, sự kiện đã có trong app | Code (`AdPlacement`, `docs/remote-config-keys.md`, hằng `EVENT_`) |
| Giá trị đang chạy ở máy người dùng | Firebase Remote Config và sổ đổi cấu hình của app |
| Bàn bạc, hoạch định, giá trị dự kiến lúc phát hành | File Excel này |

## 2. Chuẩn bị (một lần mỗi máy)

1. Có `python3` (đã có nếu làm theo [setup-new-app.md](setup-new-app.md), mục 1).
2. Cài thư viện đọc ghi Excel: `python3 -m pip install --user openpyxl`. Thiếu thư viện thì script báo lệnh cài.
3. Bảng khóa `docs/remote-config-keys.md` phải khớp code. Vừa thêm khóa hay vị trí mà chưa sinh lại bảng thì chạy `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` trước. `scripts/check.sh` báo lỗi khi bảng lệch code, nên trên nhánh chính bảng luôn khớp.

## 3. Sinh file lần đầu

Trong thư mục repo của app:

```
python3 scripts/ads-plan.py
```

Script in đường dẫn file và số vị trí, khóa, sự kiện đọc được, ví dụ:

```
Đã ghi build/ads-plan/notes_Ads_RC_Events.xlsx
  6 vị trí trong code, 0 vị trí đang hoạch định, 45 khóa Remote Config, 4 sự kiện của app.
```

File nằm trong `build/`, không vào git. Đổi tên nếu cần rồi đưa lên Google Drive của nhóm để mọi người cùng sửa. Chỉ giữ **một** bản đang dùng.

Lần đầu, file có sẵn vài dòng ví dụ (chữ nghiêng xám, trạng thái `Ví dụ`). Sửa thành vị trí thật của app, hoặc xóa. Không muốn có dòng ví dụ: `--no-examples`.

Tùy chọn:

| Tùy chọn | Tác dụng |
| --- | --- |
| `--out <file.xlsx>` | Ghi vào chỗ khác |
| `--keep <file cũ.xlsx>` | Giữ phần nhóm đã điền ở file cũ (mục 5) |
| `--no-examples` | Không thêm dòng ví dụ |
| `--with-staging` | Liệt kê cả vị trí `gallery_` của màn xem thử quảng cáo trên staging |
| `--dump` | In nội dung sẽ ghi dưới dạng JSON, không ghi file; không cần `openpyxl` (trừ khi kèm `--keep`). Dùng cho test |

## 4. Điền file

Màu ô:

| Màu | Nghĩa | Làm gì |
| --- | --- | --- |
| Vàng | Ô cần điền | Điền |
| Xám | Công thức | Không gõ đè |
| Xanh lá | Đọc từ code hoặc từ luật nghiệp vụ | Không sửa tay: đổi code rồi sinh lại |
| Đỏ (cột Kiểm tra) | Có lỗi | Sửa dòng đó tới khi cột ghi `OK` |

Từng sheet:

- **Vị trí QC.** Mỗi dòng một vị trí.
  - Dòng xanh lá đã có trong code. Năm vị trí toàn app (`open_app`, `open_app_inter`, `resume`, `resume_inter`, `native_combo`) luôn có, bật bằng khóa chung, không có khóa `is_show_` riêng.
  - Vị trí mới: điền mã (chữ thường, số, gạch dưới; cũng là giá trị `ad_placement` trong số liệu), chọn định dạng, ghi màn hình, khi nào hiện, chọn Bật hay Tắt lúc phát hành, để trạng thái `Đề xuất`.
  - Chỉ điền cột thuộc tính đúng với định dạng: nhóm giãn cách và số giây cho interstitial cần nhịp riêng; N cho native chèn trong danh sách; chính sách khi không có quảng cáo cho rewarded (`block` mặc định, `allow`, `allow_after_<n>`).
  - Khóa bật tắt, khóa tham số riêng, cách gọi trong code, kích thước tự hiện. Chọn mã trùng tên AdsKit thì khóa trùng luôn: mã `rw_get_item` cho khóa `is_show_rw_get_item`.
- **Remote Config.** Mọi khóa của app, chia theo nhóm (mở app, quay lại app, interstitial, thanh đáy, số liệu...). Chỉ điền cột **Giá trị phát hành**: giá trị dự định đặt trên Firebase production. Cột Mặc định trong code là giá trị khi app chưa tải được cấu hình. Khóa riêng của app chưa có trong code thì thêm ở nhóm Khóa của app. Cuối sheet là các khóa theo vị trí, tự sinh từ sheet Vị trí QC.
- **Sự kiện.** Phần đầu là sự kiện quảng cáo mà `:core:ads` và `:core:analytics` tự gửi cho mọi vị trí: dev không làm thêm gì, chỉ cần biết tên và tham số để đọc số liệu. Phần cuối là sự kiện của app: dòng xanh lá đọc từ hằng `EVENT_` trong code; sự kiện mới thêm vào dòng vàng, ghi khi nào gửi, tham số, và câu hỏi kinh doanh mà nó trả lời.
- **Định dạng.** Danh sách đóng các định dạng của `:core:ads`, kích thước, cách làm mới, mã `ad_format` trong số liệu. Cần định dạng không có ở đây thì đề xuất về dự án mẫu ([from-app.md](from-app.md)), không tự thêm.
- **Hằng số.** Giá trị cố định trong code (thời gian chờ, hạn dùng, nhịp làm mới). Không đổi được qua Remote Config; muốn đổi phải sửa dự án mẫu.

Không ghi vào file: mã đơn vị quảng cáo thật, khóa SDK (AppLovin...), khóa AppsFlyer, dữ liệu người dùng.

## 5. Một vòng làm việc

1. **Hoạch định.** Nhóm kinh doanh điền vị trí, khóa, sự kiện mới trên file, trạng thái `Đề xuất`. Sửa hết ô đỏ của cột Kiểm tra.
2. **Vào spec.** Dev hay AI chép các dòng đã chốt vào spec của feature: vị trí vào mục 5 (Quảng cáo), sự kiện vào mục 6. Người phụ trách sản phẩm duyệt spec. Đổi trạng thái trên file thành `Đã duyệt trong spec`. Chưa có trong spec đã duyệt thì không code (ADR-0004).
3. **Code.** Theo [ads-guide.md](ads-guide.md), mục 5: mỗi vị trí một dòng trong `AdPlacement`, sinh lại bảng khóa. Sau khi merge, sinh lại file (bước 4).
4. **Sinh lại file**, giữ phần đã điền:

   ```
   python3 scripts/ads-plan.py --keep <file đang dùng.xlsx>
   ```

   - Được cập nhật từ code: danh sách vị trí, định dạng, thuộc tính, khóa (tên, kiểu, mặc định), sự kiện của app.
   - Được giữ từ file cũ: màn hình, khi nào hiện, giá trị lúc phát hành, ảnh tham khảo, trạng thái, ghi chú; vị trí, khóa và sự kiện đang hoạch định chưa có trong code.
   - Không được giữ: dòng `Ví dụ`, và mọi thứ gõ đè vào ô xám hay ô xanh lá.
   - Vị trí vừa có trong code chuyển sang `Đã code`.

   Kiểm tra file mới rồi thay bản trên Drive. Giữ bản cũ tới khi chắc file mới đủ.
5. **Lên Firebase.** Đặt giá trị theo cột Giá trị phát hành, làm như một lần phát hành nhỏ: ghi sổ đổi cấu hình, bật dần theo phần trăm người dùng ([remote-config.md](remote-config.md), mục 5). Đổi trạng thái thành `Đã bật trên Firebase`.

Nên sinh lại file sau mỗi phiên tự chạy có đổi `AdPlacement`, khóa Remote Config hay sự kiện, và trước mỗi lần họp bàn quảng cáo.

## 6. Khi gặp lỗi

| Thông báo hay hiện tượng | Cách xử lý |
| --- | --- |
| `Thiếu thư viện openpyxl` | `python3 -m pip install --user openpyxl` |
| `Không thấy docs/remote-config-keys.md` | Sinh bảng khóa: `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` |
| `Cần đúng một AdPlacement.kt` | Chạy script ở gốc repo của app; `:core:ads` phải có đúng một file `AdPlacement.kt` |
| Một vị trí trong code không có trong file | Vị trí khai báo trên nhiều dòng. Đưa về một dòng như [ads-guide.md](ads-guide.md), mục 5, bước 2 yêu cầu (`scripts/release.sh` cũng chỉ đọc khai báo một dòng) |
| Khóa mới trong code không có trong file | Bảng khóa chưa sinh lại (mục 2, bước 3) |
| Ô xám hiện trống khi mở trên Google Sheets hay xem trước | Mở bằng Excel hay Google Sheets bản đầy đủ: công thức được tính khi mở file |
| Cột Kiểm tra đỏ ở dòng đã code | Code và file lệch nhau: báo dev, không sửa ô xanh lá |

## 7. Sửa script

Script là của dự án mẫu (`scripts/` đi về app bằng `scripts/template-sync.sh`, ADR-0030). App không sửa script tại chỗ. Thiếu cột, thiếu định dạng, sai chữ: đề xuất về dự án mẫu ([from-app.md](from-app.md)). Script đọc:

- Mọi file `AdPlacement.kt` dưới `core/ads/src/main` (theo mọi tên gói), khai báo một dòng.
- `docs/remote-config-keys.md`.
- Hằng `const val EVENT_... = "..."` trong `src/main` của mọi module.

Bảng định dạng, sự kiện quảng cáo, hằng số và chữ tiếng Việt của khóa chung nằm trong chính script, theo [ads-business-rules.md](ads-business-rules.md), mục 5, 8, 10, 11. Đổi luật nghiệp vụ ở tài liệu đó thì sửa script trong cùng PR.

Test của script nằm trong `scripts/tests/run.sh` (khối `ads-plan`): phần đọc code chạy bằng `--dump` ở mọi máy, kể cả CI; phần ghi và đọc lại file Excel chỉ chạy khi máy có `openpyxl`, không có thì in dòng `skip`. Một test đếm mọi dòng của enum `AdPlacement` trong repo: thêm vị trí mà script không đọc được thì test đỏ.
