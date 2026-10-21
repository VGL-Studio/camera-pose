---
id: ADR-0014
title: Giá trị mặc định của tham số quảng cáo, thay bảng mặc định trong ADR-0013
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
---

# ADR-0014: GIÁ TRỊ MẶC ĐỊNH CỦA THAM SỐ QUẢNG CÁO, THAY BẢNG MẶC ĐỊNH TRONG ADR-0013

## Bối cảnh

ADR-0013 khai báo ba tham số quảng cáo qua Remote Config, kèm giá trị mặc định do AI đề xuất. Giá trị mặc định là giá trị app dùng khi chưa nhận được Remote Config: lần mở đầu không có mạng, lấy cấu hình bị lỗi, hoặc project Firebase của app chưa khai báo khóa. Đây là quyết định kinh doanh, nên người phụ trách sản phẩm chốt lại.

## Quyết định

| Khóa | Mặc định theo ADR-0013 | Mặc định mới |
| --- | --- | --- |
| `ads_<vị trí>_enabled` | bật | tắt |
| `ads_personalized_regulated_regions_enabled` | tắt | bật |
| `ads_interstitial_min_interval_seconds` | 120 | 45 |

Phần còn lại của ADR-0013 giữ nguyên.

- **Vị trí mặc định tắt:** vị trí chỉ hiện quảng cáo khi nhóm bật khóa `ads_<vị trí>_enabled` trên Remote Config của từng app. Quảng cáo toàn màn hình chỉ được tải sẵn khi vị trí đã bật, và được tải ngay khi vị trí chuyển sang bật.
- **Cá nhân hóa ở vùng có luật mặc định bật:** người đã bấm đồng ý nhận quảng cáo cá nhân hóa. Người từ chối vẫn chỉ nhận quảng cáo không cá nhân hóa, vì Google đọc lựa chọn của họ từ chuỗi đồng ý. Muốn chặn cá nhân hóa ở một app thì tắt khóa trên Remote Config.
- **45 giây giữa hai quảng cáo toàn màn hình:** rào chắn "chờ người dùng ngừng chạm 1 giây" vẫn áp dụng.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Vị trí mặc định tắt | Không vị trí nào hiện quảng cáo trước khi nhóm chủ động bật | Remote Config lỗi hay quên bật thì không có doanh thu từ vị trí đó, và không có lỗi nào báo |
| Vị trí mặc định bật (ADR-0013) | Doanh thu không phụ thuộc Remote Config | Vị trí có vấn đề vẫn hiện trên máy chưa nhận lệnh tắt |
| Cá nhân hóa mặc định bật | Doanh thu cao hơn từ người đã đồng ý | Cần hộp thoại đồng ý trên AdMob của từng app được cấu hình đúng |
| 45 giây | Nhiều lượt hiển thị hơn | Dễ làm phiền người dùng hơn; app thao tác nhanh nên tăng khoảng này trên Remote Config |

## Hệ quả

- **Tích cực:** người phụ trách sản phẩm quyết định khi nào từng vị trí bắt đầu có quảng cáo.
- **Phải chấp nhận:**
  - Mỗi app mới phải bật từng vị trí trên Remote Config của project Firebase production trước khi phát hành, nếu không app không có quảng cáo. Danh sách việc trước khi phát hành phải có bước này.
  - Bản staging đang build bằng file cấu hình Firebase giả (ADR-0012) nên không nhận được Remote Config, và không hiện quảng cáo. Khi có project Firebase staging thật, nhóm bật vị trí trên Remote Config của project đó để thử quảng cáo.
  - Hộp thoại đồng ý trên AdMob của mỗi app phải được kiểm tra trước khi phát hành, vì cá nhân hóa bật sẵn với người đã đồng ý.
- **Việc AI phải tuân thủ:** không đổi các giá trị mặc định này khi không có quyết định mới của người phụ trách sản phẩm; vị trí mới vẫn mặc định tắt.
