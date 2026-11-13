---
id: T-094
title: Thanh đáy native nhỏ ở màn xem lại ảnh (review_bottom, tắt lúc phát hành)
feature: ads
spec: features/ads/spec.md   # confirmed 2026-11-13, câu 19; vị trí của màn ở mục 5 của features/camera/spec.md (confirmed 2026-11-13)
scenarios: [KB-6, KB-9, KB-11]   # của features/camera/spec.md; tiêu chí của thanh đáy ở KB-9 và KB-11
owner: Nhật
status: approved                # frame trạng thái ad của câu 19, ý b đã duyệt (Nhật, 2026-11-13)
risk: high
size: M
depends_on: [T-093]          # cùng sửa AdPlacement, StagingRemoteValuesModule và bảng khóa; làm sau để không trùng file
sprint:
plan: features/ads/plans/T-094-thanh-day-native-o-man-xem-lai.md
auto_run:
created: 2026-11-13
---

# T-094: THANH ĐÁY NATIVE NHỎ Ở MÀN XEM LẠI ẢNH

## Mô tả

Theo câu 19 của `features/ads/spec.md`, đáy màn xem lại có thanh quảng cáo native nhỏ `review_bottom`, dưới hàng nút Lưu, Chụp lại, Chia sẻ, Tư thế tiếp; ảnh co lại cho vừa phần còn lại, giữ tỷ lệ, không bị cắt, không bị che. Thanh hiện ở mọi trạng thái của màn (`default`, `saving`, `save-error`), cả khi mở từ ô ảnh vừa chụp. Production tắt lúc phát hành, rồi bật dần (câu 19, ý d); bản staging bật sẵn.

## Phạm vi

- **`AdPlacement` (`core/ads`):** thêm `REVIEW_BOTTOM("review_bottom", AdFormat.NATIVE_SMALL)`, một dòng, chú thích dẫn spec.
- **Màn xem lại (`feature/camera`, `review/ReviewScreen.kt`):** `BottomAdBarScaffold(AdPlacement.REVIEW_BOTTOM)` dưới hàng nút; vùng ảnh nhận phần còn lại. Hộp thoại "Bỏ ảnh này?" và thông báo lỗi lưu phủ lên trên như hôm nay. Bố cục mở gập theo thiết kế `_unfolded`. Không đổi `review_interstitial`, luật đếm ảnh, combo native và lời hỏi đánh giá.
- **Không có lớp thu gọn ở `review_bottom` (câu 19, ý e):** màn xem lại không bao giờ hiện lớp native thu gọn nổi, dù `is_show_native_collap` bật. Cần sửa khung theo mô hình chung: một thuộc tính khai báo của vị trí (ví dụ trên dòng `AdPlacement`, không mang tên tính năng) mà `BottomAdBarScaffold` đọc; plan chọn cách và đề xuất về dự án mẫu (`docs/ops/from-app.md`). Có test: khóa bật, thanh đáy của `review_bottom` không có lớp nổi, `poses_bottom` vẫn có.
- **Bản staging (`StagingRemoteValuesModule`):** bật `is_show_review_bottom` (câu 19, ý a); cập nhật `StagingRemoteValuesModuleTest`.
- **Bảng khóa:** sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json`, thêm `is_show_review_bottom`.
- **Thiết kế và ảnh chuẩn:** screenshot test cùng tên `ReviewScreen_ad_dark`, `ReviewScreen_ad_dark_unfolded` (thẻ native giả qua tham số composable); ảnh khung thấp `_small` có thanh quảng cáo; chạy `scripts/design-compare.sh camera`. Ảnh chuẩn có sẵn không đổi.
- **Tài liệu:** `docs/ops/manual-test.md`, mục Quảng cáo: thêm bước thanh đáy ở màn xem lại. Ghi cách bật dần 10% của câu 19, ý d vào `docs/ops/remote-config.md` hay ghi chú phát hành theo plan. Sau khi merge, chạy `scripts/ads-plan.py --keep <file trên Drive>`.

**Không bao gồm:**

- `settings_bottom`: T-093.
- Bật dần trên Firebase production, mã quảng cáo thật: việc của người (ADR-0007, ADR-0009).
- Quảng cáo ở màn camera: spec giữ không có (bài học 3).

## Rủi ro sơ bộ

Chạm điểm chặn của `risk-scoring.md`: vị trí quảng cáo mới, module core (`core/ads`), thiết kế và ảnh chuẩn; màn xem lại là luồng chính (chụp, lưu ảnh) và đi ngược một phần bài học 3. Thêm sửa khung `core/ads` để tắt lớp thu gọn theo vị trí (câu 19, ý e), cần đề xuất về dự án mẫu. Không thêm SDK hay quyền. Rủi ro cao, cần duyệt chéo. Cỡ M (gần L; plan có thể tách phần sửa khung thành ticket riêng) vì màn có nhiều trạng thái, bố cục mở gập và ảnh phải co lại đúng tỷ lệ.

## Câu hỏi cho người

Câu 19 của `features/ads/spec.md` đã có câu trả lời; Nhật xác nhận spec ngày 2026-11-13.

Việc trước khi soạn plan: AI dựng `ReviewScreen_ad_dark`, `ReviewScreen_ad_dark_unfolded` trên Figma, Nhật duyệt, xuất ảnh vào `docs/features/camera/design/screens/` và cập nhật gói bàn giao. Frame `_unfolded` (đã duyệt 2026-11-13): thanh trải hết bề rộng đáy, ảnh giữ 450 x 600, nút chính "Lưu ảnh" dời lên trên thanh; đặt dưới cột nút phải thì cột chỉ rộng 175, thẻ native nhỏ không đủ chỗ cho chữ; khe native của khung hiện cao 80 dp; luật D-02 của `docs/ops/ads-business-rules.md` (cao theo bề rộng, tới 200 dp) chưa có trong code, khi khung đồng bộ theo D-02, D-04 thì xét lại bố cục mở gập. Luật MC-13 (màn khai báo tắt lớp nổi) có trong tài liệu luật nhưng chưa có trong code; plan xét làm phần ý e theo mô hình đó và đề xuất thêm luật vào `ads-business-rules.md`, `ads-guide.md` của dự án mẫu. Plan xét chuyện hàng nút nhảy khi thanh co về 0 lúc nạp lỗi (bấm nhầm, `docs/ops/ads-guide.md`). Thiết kế đã duyệt ngày 2026-11-13, ticket `ready`.

## Định nghĩa hoàn thành

- [ ] Tiêu chí của KB-6, KB-9, KB-11 của spec camera đạt khi có và không có thanh quảng cáo; `is_show_review_bottom` mặc định tắt.
- [ ] Có test cho: thanh chừa chỗ và co về 0 theo hành vi hiện tại của khung (khe 80 dp, chỉ chừa chỗ khi khe đã có quảng cáo, nạp lỗi thì co về 0; như thanh đáy của thư viện); ảnh không bị cắt hay che và bốn nút không bị che ở khung thấp và khi mở gập; thanh có ở `saving`, `save-error` và khi mở từ ô ảnh vừa chụp; hộp thoại "Bỏ ảnh này?" vẫn phủ lên; giá trị của bản staging.
- [ ] Test có sẵn của `review_interstitial`, luật đếm ảnh và hỏi đánh giá vẫn qua, không sửa câu khẳng định.
- [ ] Bảng khóa sinh lại khớp code; manifest gộp không đổi.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật: chụp, lưu, chia sẻ, chụp lại với thanh đáy bật trên staging, không bấm quảng cáo; đã nghiệm thu chéo.
