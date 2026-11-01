---
id: T-083
title: Thẻ native `native_onboarding` ở luồng giới thiệu
feature: onboarding
spec: features/onboarding/spec.md   # confirmed 2026-11-01, câu 6, 8, 9; vị trí cũng ghi ở features/ads/spec.md câu 12
scenarios: [KB-6]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-082]
sprint:
plan: features/onboarding/plans/T-083-the-native-luong-gioi-thieu.md
created: 2026-10-31
---

# T-083: THẺ NATIVE `NATIVE_ONBOARDING` Ở LUỒNG GIỚI THIỆU

## Mô tả

Thêm vị trí `native_onboarding` (định dạng `native_step`) vào luồng giới thiệu của T-082, theo yêu cầu của Nhật ngày 2026-10-31.

Thẻ native 250 dp hiện ở đáy vùng trang, ở mọi trang, kể cả trang cuối (câu 6 của spec onboarding). Khi chuyển trang, thẻ đổi sang quảng cáo khác. Trang 1 chờ quảng cáo tối đa 5 giây khi quảng cáo được phép, nút "Tiếp" mờ trong lúc chờ (câu 8). Khi không có quảng cáo, luồng đi tiếp bình thường và nút "Tiếp" không nhảy. Vị trí bật lúc phát hành.

## Phạm vi

- **`AdPlacement`:** thêm `NATIVE_ONBOARDING("native_onboarding", AdFormat.NATIVE_STEP)`, một dòng.
  - Không có trang toàn trang (câu 6).
- **Luồng giới thiệu:**
  - `NativeStepAd(placement, step)` đặt ngoài pager, trong khung cao `NativeStepAdDefaults.height()`, theo `docs/ops/ads-guide.md`.
  - Màn nhận thẻ qua tham số composable, để screenshot test truyền `NativeAdCard` giả.
- **Chờ ở trang 1 (câu 8):**
  - Chờ quảng cáo tối đa 5 giây, chỉ khi vị trí bật và cổng đồng ý cho phép.
  - Nút "Tiếp" mờ, không bấm được trong lúc chờ.
  - Plan nêu cách biết "thẻ đã có quảng cáo" từ khung, có thể là một trạng thái mà `NativeStepAd` báo ra. Phần này có thể là sửa khung; khi đó đề xuất về dự án mẫu. Test dùng đồng hồ giả.
- **Màn thấp và trang cuối:** thẻ không che nút "Bật camera" và dòng ghi chú. Ở khung thấp `w360dp-h640dp`, trang cuộn được, nút luôn thấy được (KB-6).
- **Nạp sẵn:** pool `native_step` vào nhóm nạp sẵn đầu chỉ ở lần mở đầu tiên. Plan kiểm log `Preload group` và thứ tự với hộp thoại đồng ý (câu 8).
- **Bản staging:** bật `is_show_native_onboarding`, cập nhật `StagingRemoteValuesModuleTest`. Production bật trên Firebase lúc phát hành (việc của người; ghi vào `docs/ops/manual-test.md` hay danh sách phát hành).
- **Bảng khóa:** sinh lại, thêm `is_show_native_onboarding`.
- **Tài liệu:** `docs/ops/manual-test.md` thêm bước luồng giới thiệu có quảng cáo. `scripts/ads-plan.py` có dòng mô tả vị trí nếu cần.
- **Mã production:** thêm `native_onboarding` vào danh sách mã cần tạo. Mã "Native 5 - Step" Nhật đã tạo trên AdMob; giá trị không vào repo.

**Không bao gồm:**

- Các trang của luồng: T-082.
- Mã quảng cáo thật và `scripts/release.sh`: T-075.

## Rủi ro sơ bộ

Ticket chạm điểm chặn của `risk-scoring.md`: thêm vị trí quảng cáo, và quảng cáo ở lần mở đầu tiên ngay cạnh trang xin quyền camera. Không thêm SDK, quyền hay thư viện. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật).

Plan phiên bản 4 (2026-11-01): spec câu 9 coi "Không đồng ý" trên hộp thoại UMP là vẫn được nạp quảng cáo không cá nhân hóa, nên trên emulator trang 1 cũng chờ; plan thêm bộ lọc `enabled=true` vào `scripts/device-scenario.py` (phần dùng chung, đề xuất về dự án mẫu) và sửa 8 tệp kịch bản có sẵn để không chạm nút "Tiếp" đang mờ. Dòng cuối của Định nghĩa hoàn thành đã sửa theo spec câu 9 (plan, mục 8, câu 13; Nhật, 2026-11-01).

## Định nghĩa hoàn thành

- [ ] Đạt KB-6 của spec onboarding.
- [ ] Khóa `is_show_native_onboarding` mặc định tắt; bản staging bật; bảng khóa sinh lại khớp code.
- [ ] Screenshot test trạng thái `ad` khớp ảnh thiết kế; ảnh `_small` cho thấy nút "Tiếp" không bị che.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật bản staging sau khi xóa dữ liệu app: thẻ hiện ở mọi trang, đổi khi chuyển trang; trang 1 chờ không quá 5 giây khi quảng cáo được phép theo hộp thoại đồng ý (UMP), kể cả khi chọn "Không đồng ý" (quảng cáo không cá nhân hóa, spec câu 9); vị trí tắt hay hộp thoại chưa cho nạp quảng cáo thì không chờ; luồng luôn đi được. Không bấm vào quảng cáo.
