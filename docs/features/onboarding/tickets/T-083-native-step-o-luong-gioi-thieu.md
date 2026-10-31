---
id: T-083
title: Thẻ native `native_onboarding` ở luồng giới thiệu
feature: onboarding
spec: features/onboarding/spec.md   # changed 2026-10-31, chờ câu 6, 8; vị trí cũng ghi ở features/ads/spec.md câu 12
scenarios: [KB-6]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-082]
sprint:
plan:
created: 2026-10-31
---

# T-083: THẺ NATIVE `NATIVE_ONBOARDING` Ở LUỒNG GIỚI THIỆU

## Mô tả

Thêm vị trí `native_onboarding` (định dạng `native_step`) vào luồng giới thiệu của T-082, theo yêu cầu của Nhật ngày 2026-10-31.

Thẻ native 250 dp hiện ở đáy vùng trang, ở các trang mà câu 6 của spec onboarding chọn. Khi chuyển trang, thẻ đổi sang quảng cáo khác. Khi không có quảng cáo, luồng đi tiếp bình thường và nút "Tiếp" không nhảy.

## Phạm vi

- **`AdPlacement`:** thêm `NATIVE_ONBOARDING("native_onboarding", AdFormat.NATIVE_STEP)`, một dòng.
  - Nếu câu 6 chọn có trang toàn trang thì thêm `native_onboarding_full` (`AdFormat.NATIVE_FULL_SCREEN`).
- **Luồng giới thiệu:**
  - `NativeStepAd(placement, step)` đặt ngoài pager, trong khung cao `NativeStepAdDefaults.height()`, theo `docs/ops/ads-guide.md`.
  - Màn nhận thẻ qua tham số composable, để screenshot test truyền `NativeAdCard` giả.
- **Màn thấp:** xử lý khung thấp `w360dp-h640dp` theo câu 6: trang cuộn được, hay ẩn thẻ khi không đủ chỗ.
- **Nạp sẵn:** pool `native_step` vào nhóm nạp sẵn đầu chỉ ở lần mở đầu tiên. Plan kiểm log `Preload group` và thứ tự với hộp thoại đồng ý (câu 8).
- **Bản staging:** bật `is_show_native_onboarding`, cập nhật `StagingRemoteValuesModuleTest`.
- **Bảng khóa:** sinh lại, thêm `is_show_native_onboarding`.
- **Tài liệu:** `docs/ops/manual-test.md` thêm bước luồng giới thiệu có quảng cáo. `scripts/ads-plan.py` có dòng mô tả vị trí nếu cần.
- **Mã production:** thêm `native_onboarding` vào danh sách mã cần tạo. Mã "Native 5 - Step" Nhật đã tạo trên AdMob; giá trị không vào repo.

**Không bao gồm:**

- Các trang của luồng: T-082.
- Mã quảng cáo thật và `scripts/release.sh`: T-075.

## Rủi ro sơ bộ

Ticket chạm điểm chặn của `risk-scoring.md`: thêm vị trí quảng cáo, và quảng cáo ở lần mở đầu tiên ngay cạnh trang xin quyền camera. Không thêm SDK, quyền hay thư viện. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật).

## Định nghĩa hoàn thành

- [ ] Đạt KB-6 của spec onboarding.
- [ ] Khóa `is_show_native_onboarding` mặc định tắt; bản staging bật; bảng khóa sinh lại khớp code.
- [ ] Screenshot test trạng thái `ad` khớp ảnh thiết kế; ảnh `_small` cho thấy nút "Tiếp" không bị che.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật bản staging sau khi xóa dữ liệu app: thẻ hiện, đổi khi chuyển trang; từ chối đồng ý thì luồng vẫn đi được. Không bấm vào quảng cáo.
