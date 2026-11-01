---
id: ADR-1008
title: App không có màn mẫu `:feature:sample` và vị trí quảng cáo `sample_native_small`; test của khung quảng cáo dùng vị trí `poses_bottom`
status: accepted             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-10-31
deciders: [Tuấn, Nhật]
supersedes: ADR-1007         # chỉ vế "Vị trí `gallery_*` và `sample_native_small`"; các vế khác của ADR-1007 giữ nguyên
superseded_by:
---

# ADR-1008: APP KHÔNG CÓ MÀN MẪU VÀ VỊ TRÍ `SAMPLE_NATIVE_SMALL`

## Bối cảnh

App được tạo từ dự án mẫu cùng `:feature:sample`: màn đếm số làm mẫu cho feature mới, có cờ `feature_sample_enabled`, sự kiện `sample_increment` và thanh đáy quảng cáo `sample_native_small`. ADR-0017 của dự án mẫu cho phép xóa feature mẫu bằng một ticket khi feature thật đầu tiên thay màn bắt đầu. App đã có màn chào và camera làm màn chính (spec loading, mục 2).

ADR-1007 ghi giữ `sample_native_small` trong `AdPlacement` chỉ vì khoảng 25 file test của `core/ads` dùng nó làm vị trí mẫu. Nhưng `ConfiguredAdUnitIds.missing` đòi mã quảng cáo thật cho mọi vị trí không phải `gallery_*`, kể cả `sample_native_small`. Bản production không có màn mẫu, nên không tạo mã AdMob cho vị trí đó (Nhật, 2026-10-31). Giữ vị trí thì T-075 phải tạo một mã không bao giờ dùng, hoặc bỏ qua một vị trí trong kiểm tra phát hành.

Ngoài ra, khi cờ camera tắt, `chooseMainScreen` mở màn mẫu. Bản production có cờ camera mặc định tắt, nên nếu Remote Config production chưa bật cờ, người dùng thấy màn mẫu.

## Quyết định

- App không có `:feature:sample`, cờ `feature_sample_enabled`, sự kiện `sample_increment` và vị trí `sample_native_small` (T-081).
- Vị trí `gallery_*` vẫn giữ trong `AdPlacement` như ADR-1007: chỉ có ở staging, không cần mã thật, test của khung dùng chúng.
- Test của khung quảng cáo trước đây dùng `SAMPLE_NATIVE_SMALL` nay dùng `POSES_BOTTOM`: cùng định dạng `NATIVE_SMALL`, không chỉ có ở staging. Chuỗi mong đợi đổi theo khóa `poses_bottom`.
- Sau loading, app mở màn chào lần đầu (khi cờ màn chào bật), còn lại mở camera. Không còn màn dự phòng.
- Đợt đồng bộ với dự án mẫu (`docs/ops/to-app.md`) không mang lại `feature/sample`, dòng `SAMPLE_NATIVE_SMALL` của `AdPlacement`, cờ `SAMPLE` của `FeatureFlag` hay phần nối của chúng trong `:app`. Test của khung lấy từ mẫu mà dùng `SAMPLE_NATIVE_SMALL` thì đổi sang `POSES_BOTTOM` khi gộp. Test của script lấy từ mẫu mà đọc file của `feature/sample` thì đổi sang file của module còn lại, như T-081.
- Đề xuất về dự án mẫu (`docs/ops/from-app.md`): test của khung dùng một vị trí riêng của test (`AdPlacementDeclaration` đã cho test tự khai báo) hay vị trí `gallery_*`; test của script dùng tệp mẫu trong `scripts/tests/fixtures/`, không đọc file của `feature/sample`. Khi đó app bỏ màn mẫu mà không phải sửa test của mẫu.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Bỏ màn mẫu và vị trí, test của khung dùng `POSES_BOTTOM` (đã chọn) | Danh sách mã cần tạo cho production đúng các vị trí thật; không còn màn mẫu trên production | Sửa khoảng 25 file test của `core/ads` lấy từ mẫu; mỗi đợt đồng bộ phải đổi lại test mới của mẫu dùng vị trí mẫu |
| Giữ vị trí, đánh dấu chỉ có ở staging (như `gallery_*`) | Không sửa test của khung | `stagingOnly` đọc theo tiền tố `gallery_` (`scripts/release.sh`, `AdPlacementTest`); đổi luật đó chạm `release.sh` và khung |
| Giữ cả màn mẫu và vị trí (ADR-1007) | Không đổi gì | T-075 phải tạo mã thật cho một vị trí không dùng; cờ camera tắt thì production mở màn mẫu |

## Hệ quả

- **Tích cực:** danh sách vị trí cần mã thật cho production chỉ có vị trí của app; màn chính sau loading luôn là màn chào hay camera; `foundation.md` và bảng khóa không còn nhắc màn mẫu.
- **Phải chấp nhận:** test của `core/ads` lệch bản mẫu ở tên vị trí; đợt đồng bộ phải đổi tên vị trí trong test mới của mẫu; `CLAUDE.md` dẫn feature thật của app làm mẫu thay cho `:feature:sample`.
- **Việc AI phải tuân thủ:** không thêm lại `:feature:sample`, cờ `feature_sample_enabled`, sự kiện `sample_increment` hay vị trí `sample_native_small`; test của khung cần một vị trí native small không chỉ có ở staging thì dùng `POSES_BOTTOM`; đồng bộ từ dự án mẫu thì bỏ `feature/sample` và dòng `SAMPLE_NATIVE_SMALL`, đổi test mới của mẫu dùng vị trí đó sang `POSES_BOTTOM`; vị trí `gallery_*` giữ theo ADR-1007.
