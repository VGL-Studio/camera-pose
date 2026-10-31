---
id: T-084
title: Mã quảng cáo theo lớp cho thanh đáy (banner, native thu gọn), chọn lớp bằng Remote Config
feature: ads
spec: features/ads/spec.md   # changed 2026-10-31, chờ câu 13 tới 15
scenarios: []                # spec ads không có kịch bản riêng; thanh đáy theo KB-6 của features/poses/spec.md
owner: Tuấn
status: backlog
risk: high
size: M
depends_on: [T-079]          # T-079 đưa thanh đáy vào thư viện và chi tiết
sprint:
plan:
created: 2026-10-31
---

# T-084: MÃ QUẢNG CÁO THEO LỚP CHO THANH ĐÁY, CHỌN LỚP BẰNG REMOTE CONFIG

## Mô tả

Theo yêu cầu của Nhật ngày 2026-10-31, thanh đáy của thư viện và chi tiết phải hiện được ba cách: banner, native nhỏ, hay native thu gọn. Nhóm chọn cách hiện bằng Remote Config.

Khung đã vẽ cả ba lớp, và hai khóa `is_show_native_bottom`, `is_show_native_collap` đã chọn lớp (câu 13 của spec ads). Phần còn thiếu là mã quảng cáo ở production. Hôm nay `ConfiguredAdUnitIds` chỉ có mã của định dạng chính của vị trí, nên lớp banner và lớp thu gọn không gửi yêu cầu nào.

Sau ticket này, bản production có mã cho cả ba lớp. Đổi hai khóa trên Firebase là đổi cách hiện, không cần phát bản mới.

## Phạm vi

- **Khung (`core/ads`, `AdUnitIds.kt`):**
  - `ConfiguredAdUnitIds` đọc thêm mục `<khóa vị trí>/<định dạng lớp>=<mã>`, theo câu 14.
  - `of(placement, format)` trả mã của lớp khi có.
  - `missing` (hay một danh sách riêng) báo thanh đáy thiếu mã lớp.
  - Bản staging giữ mã thử theo định dạng như hiện nay.
- **Test của khung:**
  - Đọc mục lớp.
  - Lớp không có mã thì không gửi yêu cầu (B-04 giữ nguyên).
  - Thiếu mã lớp thì báo.
  - Vị trí `gallery_*` không bao giờ thiếu.
- **Tài liệu:**
  - `docs/ops/ads-guide.md`, mục 5, bước 6, và `docs/ops/release.md`: cách khai báo mã lớp. `release.md` thuộc T-075 nếu T-075 thay cả file; plan nêu file nào sửa ở ticket nào.
  - `docs/ops/manual-test.md`: bước thử bốn tổ hợp của câu 13 trên Firebase staging.
- **Đề xuất về dự án mẫu** (`docs/ops/from-app.md`): mã theo lớp của thanh đáy. Câu 14 của spec AdsKit của mẫu (mã theo khe) cùng ý.
- **Nếu câu 13 chọn khóa mới `bottom_ad_type`:** sửa `BottomAdBarScaffold` và bảng khóa. Cỡ ticket lên L; plan tính lại.

**Không bao gồm:**

- Kiểm mã lớp trong `scripts/release.sh`: T-075, do người làm. T-075 cần biết khuôn của câu 14.
- Đổi giá trị trên Firebase production: người làm lúc phát hành (câu 15).
- Chọn lớp riêng cho từng màn: chỉ khi câu 13 chọn khóa theo vị trí.

## Rủi ro sơ bộ

Ticket chạm điểm chặn của `risk-scoring.md`: sửa module core `:core:ads` (cách lấy mã quảng cáo của bản production). Nó cũng ảnh hưởng tới cấu hình phát hành, nhưng không đọc hay tạo cấu hình đó. Không thêm SDK, quyền hay thư viện. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật).

## Định nghĩa hoàn thành

- [ ] Bản production nhận mã lớp theo khuôn của câu 14; test của khung kiểm đủ các trường hợp ở mục Phạm vi.
- [ ] Bản staging thấy đủ bốn tổ hợp của câu 13 khi đổi khóa trên Firebase staging.
- [ ] Tài liệu cách khai báo mã lớp đã cập nhật; T-075 có ghi chú khuôn mã lớp.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
