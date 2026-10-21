---
feature: loading
status: approved                # draft | approved
spec: features/loading/spec.md
tool: figma
link: https://www.figma.com/design/6Xyp5CRP04IzFSIwFHU8Df/Untitled?node-id=0-1
designer: Nhật
approved_by: Nhật
approved_at: 2026-10-16
---

# THIẾT KẾ: MÀN HÌNH LOADING

<!--
Quy trình: docs/ops/design-workflow.md. Quyết định: ADR-0008, ADR-0020.
screens/ và context/ do scripts/figma-export.py sinh, không sửa tay. Thiết kế đổi thì chạy lại:
  scripts/figma-export.py "<link trang Figma>" loading
handoff/: logo và biểu tượng xuất SVG từ Figma (REST API, format=svg), giữ nguyên như khi xuất.
Phiên tự chạy chỉ đọc thư mục này, không mở được Figma.
-->

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `LoadingScreen` | Đang tải, 68% | `LoadingScreen_progress_light` | KB-1 |
| `LoadingScreen` | Đang tải, 68%, chế độ tối | `LoadingScreen_progress_dark` | KB-1 |
| `LoadingScreen` | Lỗi, dừng ở 68% | `LoadingScreen_failed_light` | KB-2, KB-3 |
| `LoadingScreen` | Lỗi, dừng ở 68%, chế độ tối | `LoadingScreen_failed_dark` | KB-2, KB-3 |

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Mọi trạng thái | Tên app | Tên của app đang chạy (`app_name`). Thiết kế và screenshot test dùng chữ mẫu "Todo App" | `headlineLarge` |
| Mọi trạng thái | Khẩu hiệu | Mọi việc vào đúng nhịp. | `bodyMedium` |
| Đang tải | Nhãn tiến trình | Đang tải dữ liệu | `labelLarge` |
| Mọi trạng thái | Phần trăm | 68% (theo tiến độ, `%1$d%%`) | `bodyMedium` |
| Đang tải | Dòng hướng dẫn | Đang chuẩn bị không gian của bạn… | `bodySmall` |
| Lỗi | Nhãn tiến trình | Tiến độ đã dừng | `labelLarge` |
| Lỗi | Tiêu đề lỗi | Không thể tải dữ liệu | `titleMedium` |
| Lỗi | Lời hướng dẫn | Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục. | `bodyMedium` |
| Lỗi | Nút | Thử lại | chữ của `PrimaryButton` |
| Mọi trạng thái | Chân trang | Không gian riêng cho việc của bạn | `labelSmall` |

Dòng hướng dẫn dùng dấu ba chấm một ký tự (`…`, U+2026), như trên thiết kế. Lời hướng dẫn khi lỗi tự xuống dòng theo chiều rộng, không có dấu xuống dòng trong chữ.

## 3. Ánh xạ sang hệ thống thiết kế

Thiết kế dùng màu, phông và cỡ chữ chưa có trong hệ thống thiết kế (`context/`, mục 2: "ngoài hệ thống"). Quyết định: hệ thống thiết kế đổi theo thiết kế ở T-002 (màu thương hiệu xanh lục, phông Inter, thành phần mới); màn hình làm theo bảng dưới ở T-003. Cỡ chữ và khoảng cách lệch nhỏ được làm tròn về token gần nhất, không thêm token.

**Màu.** Mã màu theo `context/`; tên là vai trò Material 3 mà T-002 gán mã màu đó.

| Mã màu sáng | Mã màu tối | Vai trò | Dùng cho |
| --- | --- | --- | --- |
| #F7FAF8 | #101F1D | `background`, `surface` | Nền màn hình |
| #17352F | #EFF8F4 | `onSurface` | Tên app, tiêu đề lỗi |
| #6D8079 | #96ADA5 | `onSurfaceVariant` | Khẩu hiệu, nhãn đang tải, dòng hướng dẫn, lời hướng dẫn khi lỗi, chân trang, biểu tượng khóa |
| #147D68 | #84DCC1 | `primary` | Nền logo, phần đã chạy của thanh tiến trình, phần trăm khi đang tải, nền nút |
| #FFFFFF | #101F1D | `onPrimary` | Dấu trên logo, chữ và biểu tượng của nút |
| #EBF3EE | #192E27 | `primaryContainer` | Vòng tròn quanh logo |
| #DFEAE4 | #293D36 | `surfaceVariant` | Rãnh của thanh tiến trình khi đang tải |
| #BA5148 | #F49A90 | `error` | Nhãn, phần trăm, phần đã chạy khi lỗi; biểu tượng cảnh báo |
| #F0DEDA | #48332F | `errorContainer` | Rãnh của thanh tiến trình khi lỗi |

**Phần tử.**

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Logo trong vòng tròn | `BrandLogo` (mới, T-002) | `primaryContainer`, `primary`, `onPrimary` | | Vòng tròn 132, ô logo 88 bo góc 26, dấu 52 x 48; cách tên app `Spacing.lg` |
| Tên app, khẩu hiệu | `Text` | `onSurface`, `onSurfaceVariant` | `headlineLarge`, `bodyMedium` | Cách nhau `Spacing.sm` (thiết kế 10) |
| Nhãn và phần trăm | `Row`, hai đầu | Đang tải: `onSurfaceVariant` và `primary`. Lỗi: `error` cả hai | `labelLarge`, `bodyMedium` | Cách tên app và khẩu hiệu `Spacing.xl + Spacing.sm` (thiết kế 39) |
| Thanh tiến trình | `ProgressBar` (mới, T-002) | Đang tải: `primary` trên `surfaceVariant`. Lỗi: `error` trên `errorContainer` | | Cao 8, bo tròn hai đầu, không khe hở, không chấm cuối; cách nhãn `Spacing.md` (thiết kế 14) |
| Dòng hướng dẫn khi đang tải | `Text`, căn giữa | `onSurfaceVariant` | `bodySmall` | Cách thanh `Spacing.lg` (thiết kế 23) |
| Khối lỗi | `Column`, căn giữa | | | Cách thanh `Spacing.lg + Spacing.xs` (thiết kế 29); các phần cách nhau `Spacing.sm + Spacing.xs` (thiết kế 12) |
| Biểu tượng cảnh báo và tiêu đề lỗi | `Row`, `Icon` 18 | `error`, `onSurface` | `titleMedium` | Cách nhau `Spacing.sm` |
| Nút "Thử lại" | `PrimaryButton` có biểu tượng (T-002) | `primary`, `onPrimary` | chữ của `PrimaryButton` | Rộng hết vùng nội dung, cao 52, bo góc 16, biểu tượng 18 cách chữ `Spacing.sm` |
| Chân trang | `Row`, `Icon` 12 và `Text` | `onSurfaceVariant` | `labelSmall` | Biểu tượng cách chữ `Spacing.sm` (thiết kế 7) |
| Lề trái phải của vùng nội dung | | | | `Spacing.xl + Spacing.md` mỗi bên (thiết kế 46 và 47, vùng nội dung rộng 318) |

**Vị trí theo chiều dọc.** Logo bắt đầu ở khoảng một phần tư chiều cao màn hình (thiết kế: 220 trên 891) và không đổi giữa trạng thái đang tải và lỗi: khối lỗi hiện thêm bên dưới thanh tiến trình, không đẩy logo lên. Chân trang bám đáy màn hình, cách đáy `Spacing.xl` (thiết kế 32).

**Tài nguyên trong `handoff/`:**

| File | Dùng làm | Đặt ở |
| --- | --- | --- |
| `brand_mark.svg` | Dấu trên logo | `core/designsystem/src/main/res/drawable/brand_mark.xml` (T-002) |
| `ic_alert.svg` | Biểu tượng cảnh báo | `feature/loading/src/main/res/drawable/ic_loading_alert.xml` (T-003) |
| `ic_retry.svg` | Biểu tượng trên nút | `feature/loading/src/main/res/drawable/ic_loading_retry.xml` (T-003) |
| `ic_lock.svg` | Biểu tượng khóa ở chân trang | `feature/loading/src/main/res/drawable/ic_loading_lock.xml` (T-003) |

Chuyển SVG sang vector drawable bằng tay (đường `path` giữ nguyên, `strokeColor` để màu trắng, màu thật lấy bằng `tint` từ vai trò màu). Ba biểu tượng là bộ Lucide (giấy phép ISC), logo là của thiết kế.

- **Thành phần mới cần thêm vào hệ thống thiết kế:** `BrandLogo`, `ProgressBar`, biểu tượng cho `PrimaryButton`, phông Inter, bảng màu xanh lục, vai trò `errorContainer` trong `tokens.json`. Tất cả thuộc T-002, rủi ro cao (module core).

## 4. Luồng thao tác

`LoadingScreen` đang tải, chuẩn bị xong và đủ 2 giây thì tự sang màn hình mẫu. Lỗi thì ở lại `LoadingScreen`, bấm "Thử lại" thì về trạng thái đang tải, thanh tiến trình đi tiếp từ mức đã đạt (spec, KB-2).

## 5. Vị trí quảng cáo

Không có.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3), vị trí logo không đổi giữa hai trạng thái.
- **Được phép khác:**
  - Cách vẽ chữ của Android so với Figma; chiều cao dòng theo kiểu chữ của hệ thống thiết kế (lệch 1 đến 7 dp so với thiết kế).
  - Cỡ chữ lệch 1 sp do làm tròn về kiểu chữ có sẵn (nhãn tiến trình 13 thành 14, lời hướng dẫn 13 thành 14), nên hai dòng này rộng hơn thiết kế khoảng 8%. Retro 2026-10-17 chấp nhận, giữ thang chữ Material 3; thiết kế sau dùng kiểu chữ dựng từ `tokens.json`.
  - Chỗ xuống dòng của chữ nhiều dòng: app tự xuống dòng theo chiều rộng (mục 2), không theo chỗ xuống dòng cứng trên Figma. Retro 2026-10-17 chọn cách này vì chữ đổi theo ngôn ngữ và cỡ màn hình.
  - Khoảng cách lệch tới 3 dp do làm tròn về token (mục 3).
  - Độ dày nét của biểu tượng (1,5 đến 1,8 trên thiết kế) theo vector drawable.
  - Màn hình thấp hơn 800 dp: khoảng trống phía trên co lại còn 8% chiều cao; nội dung và chân trang cuộn chung khi dài hơn màn hình (T-005, L-028).

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế (sau T-002).
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp, trừ chỗ đã ghi ở mục 6.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640).
- [ ] Vùng quảng cáo tách bạch, không gây bấm nhầm.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

<!-- AI dán review/summary.md sau mỗi lần chạy scripts/design-compare.sh, kèm nhận xét của rà soát độc lập. -->

**Mốc trước khi làm (2026-10-16, giao diện của T-001):**

| Màn hình | Điểm ảnh khác |
| --- | --- |
| LoadingScreen_failed_dark | chưa có (T-001 không có ảnh này) |
| LoadingScreen_failed_light | 9.7% |
| LoadingScreen_progress_dark | 4.0% |
| LoadingScreen_progress_light | 3.5% |

Hai màn hình khác hẳn nhau (màu, phông, logo, bố cục) mà tỷ lệ chỉ vài phần trăm, vì phần lớn ảnh là nền. Tỷ lệ chỉ để so giữa các lần chạy; độ giống thiết kế do người và rà soát độc lập đánh giá trên ảnh ghép.

**Sau T-003 (2026-10-17, RUN-20261017-01):**

| Màn hình | Điểm ảnh khác |
| --- | --- |
| LoadingScreen_failed_dark | 5.6% |
| LoadingScreen_failed_light | 5.2% |
| LoadingScreen_progress_dark | 2.5% |
| LoadingScreen_progress_light | 2.5% |

Nhận xét của rà soát độc lập (0 chặn): bốn trạng thái (đang tải, lỗi, sáng, tối) đúng thứ tự thành phần, bảng chữ và vai trò màu của thiết kế; logo giữ nguyên vị trí giữa hai trạng thái, không có chữ bị cắt hay tràn ở khung 411 x 891. Các phần phía dưới lệch dần xuống so với thiết kế, nhiều nhất khoảng 10 dp ở nút "Thử lại", do chiều cao dòng của kiểu chữ và khoảng cách làm tròn về token; mỗi phần lệch đều nằm trong mục 6, chỉ tổng cộng dồn vượt một bước lưới 4 dp. Chữ ở chân trang và lời hướng dẫn khi lỗi rộng hơn một chút do khoảng cách chữ của kiểu chữ hệ thống, số dòng không đổi.

**Sau T-004 (2026-10-17, RUN-20261017-02):**

| Màn hình | Điểm ảnh khác |
| --- | --- |
| LoadingScreen_failed_dark | 5.6% |
| LoadingScreen_failed_light | 5.2% |
| LoadingScreen_progress_dark | 2.4% |
| LoadingScreen_progress_light | 2.4% |

Nhận xét của rà soát độc lập (0 chặn): sau khi bỏ giãn cách chữ, chân trang và dòng "Đang chuẩn bị không gian của bạn…" nay rộng gần bằng thiết kế. Lời hướng dẫn khi lỗi và nhãn tiến trình vẫn rộng hơn khoảng 8%, chỉ còn do cỡ chữ 14 sp so với 13 sp trên thiết kế (đã cho phép ở mục 6). Vì chữ hẹp lại, lời hướng dẫn nay xuống dòng sau "kiểm tra mạng" thay vì sau "kiểm tra", vẫn 2 dòng. Bố cục, thứ tự thành phần và vai trò màu không đổi.
