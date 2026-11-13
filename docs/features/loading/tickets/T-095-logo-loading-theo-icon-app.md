---
id: T-095
title: Logo màn loading dùng biểu tượng app
feature: loading
spec: features/loading/spec.md   # confirmed 2026-11-13, KB-6 (tiêu chí logo màn loading)
scenarios: [KB-1, KB-6]
owner: Nhật
status: ready                # thiết kế bốn frame loading đã sửa và Nhật duyệt ngày 2026-11-13
risk: high                   # chạm module core (:core:designsystem), risk-scoring.md mục 1, điểm 7
size: S
depends_on: []
sprint:
plan:
auto_run:
created: 2026-11-13
---

# T-095: LOGO MÀN LOADING DÙNG BIỂU TƯỢNG APP

## Mô tả

Theo yêu cầu 2026-11-13, logo trên màn loading đổi từ logo tạm (hình người nét đen trên ô vàng, viền đen, bóng sticker) thành biểu tượng app: ảnh máy ảnh cam và ảnh tư thế, bo góc, không viền, không bóng, ô 112 dp, cùng chỗ như cũ. Áp cho cả bốn trạng thái (đang tải, lỗi; sáng, tối). Người dùng bấm biểu tượng app rồi thấy đúng hình đó trên màn đầu tiên.

## Phạm vi

- **`BrandLogo` (`core/designsystem`):** vẽ ảnh logo của app lấp kín ô 112 dp, bo `Radius.sheet`, bỏ viền, bóng sticker và màu nền `secondaryContainer`. Ảnh lấy từ `docs/brand/app-icon/play-store-512.png`, đưa vào tài nguyên dạng ảnh (WebP hay PNG, cỡ đủ cho ô 112 dp ở `xxxhdpi`); giữ `brand_mark.xml`, vì nó còn làm icon của `PrimaryButton` ở `DesignSystemCatalog.kt` và `ComponentsTest.kt`. Plan chọn chỗ đặt ảnh (`core/designsystem` theo quy ước "mỗi app thay logo của mình" hay truyền từ app) và đề xuất thay đổi về dự án mẫu khi đổi cách `BrandLogo` nhận logo (`docs/ops/from-app.md`).
- **Không có mô tả cho trình đọc màn hình:** giữ như cũ, vì tên app hiện ngay dưới logo.
- **Ảnh chuẩn:** screenshot test của màn loading (bốn ảnh cùng tên thiết kế, ảnh `_small`, ảnh `failed-server`) và hai ảnh có `BrandLogo` của `core/designsystem` (`DesignSystem_brand_light.png`, `DesignSystem_brand_dark.png`, do `DesignSystemCatalogScreenshotTest` ghi) ghi lại theo plan, lệnh có `--rerun`; chạy `scripts/design-compare.sh loading`, cập nhật mục 8 của `design/README.md`.
- **Test:** `ComponentsTest` kiểm logo không còn viền và bóng (đo màu điểm ảnh ngoài mép ô bằng nền màn), cỡ 112 dp, bo góc. Test cũ `brandLogo_fill_isSecondaryContainer` khẳng định nền `#FFC83D`, trái thiết kế mới: plan viết lại câu khẳng định theo ADR-0031 ở mục 3.5; `brandLogo_is112dpSquare` giữ nguyên.

**Không bao gồm:**

- Biểu tượng app trên máy (đã có), biểu tượng một màu, màn hình chào của hệ thống: giữ như spec.
- Bố cục khác của màn loading.

## Rủi ro sơ bộ

Chạm điểm chặn 7 của `risk-scoring.md`: module core dùng chung (`core/designsystem`, thành phần của hệ thống thiết kế, ADR-0008), cùng ảnh chuẩn và thiết kế. Không thêm thư viện, quyền hay SDK; cỡ APK tăng khoảng vài chục KB vì thêm một ảnh. Rủi ro cao, cần duyệt chéo.

## Câu hỏi cho người

Spec loading đã xác nhận ngày 2026-11-13. Thiết kế: component `BrandLogo` trên Figma đã đổi, bốn frame `LoadingScreen_*` và số liệu trong `design/` xuất lại; Nhật duyệt trong phiên.

## Định nghĩa hoàn thành

- [ ] Tiêu chí logo màn loading của KB-6 và tiêu chí "đúng thiết kế" của KB-1 đạt ở cả bốn trạng thái.
- [ ] Có test cho logo (cỡ, không viền, không bóng) và screenshot test cùng tên bốn ảnh thiết kế.
- [ ] `scripts/design-compare.sh loading` chạy, mục 8 của `design/README.md` cập nhật.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật: logo màn loading giống biểu tượng trên màn hình chính, sáng và tối; đã nghiệm thu chéo.
