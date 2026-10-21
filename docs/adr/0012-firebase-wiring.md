---
id: ADR-0012
title: Nối Firebase vào các module core, bản staging build bằng file cấu hình giả khi không có file thật
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0012: NỐI FIREBASE VÀO CÁC MODULE CORE, BẢN STAGING BUILD BẰNG FILE CẤU HÌNH GIẢ KHI KHÔNG CÓ FILE THẬT

## Bối cảnh

ADR-0003 chọn Firebase Remote Config cho cờ tính năng. App cũng cần Firebase Analytics để ghi sự kiện và Crashlytics để biết app bị dừng đột ngột. Trước quyết định này, `:core:analytics` và `:core:config` chỉ có bản cài đặt tạm.

Plugin `google-services` bắt buộc có file `google-services.json` khi build. CI build mọi PR mà không có bí mật nào (ADR-0009, ADR-0010), nên không có file thật. Phiên tự chạy cũng không được chạm vào cấu hình thật. File production thì không bao giờ được nằm trong repo (ADR-0007).

## Quyết định

**Vị trí của từng thư viện:**

| Thư viện | Module | Feature dùng qua |
| --- | --- | --- |
| Firebase Analytics | `:core:analytics` | `Analytics` |
| Firebase Remote Config | `:core:config` | `FeatureFlags` |
| Firebase Crashlytics | `:app` | Không dùng trực tiếp; Crashlytics tự ghi lỗi làm app dừng |

Phiên bản Firebase lấy theo Firebase BoM trong `gradle/libs.versions.toml`.

**File cấu hình:**

| File | Nằm ở | Ai đưa vào |
| --- | --- | --- |
| Staging giả | `app/src/staging/google-services.json`, có trong repo | Repo |
| Staging thật | `app/src/staging/debug/` hoặc `app/src/staging/release/`, bị `.gitignore` bỏ qua | `staging.yml` lấy từ bí mật của repo; dev tự chép trên máy khi muốn chạy thử thật |
| Production | `app/src/production/`, bị `.gitignore` bỏ qua | Người phát hành, trên máy của mình (ADR-0009) |

- Plugin `google-services` tìm file theo thứ tự `src/staging/debug/`, `src/debug/staging/`, `src/staging/`, rồi mới đến `src/stagingDebug/`. File thật vì vậy phải đặt trong `src/staging/debug/` hoặc `src/staging/release/` mới thay được file giả. Đặt ở `src/stagingDebug/` thì plugin vẫn dùng file giả mà không báo gì. Nhờ thứ tự này, file thật thay file giả mà không phải sửa file nào trong repo.
- Task của plugin chọn file lúc chạy, và build cache của Gradle có thể trả về kết quả dựng từ file khác (đã gặp khi thử: thêm file thật xong vẫn nhận cấu hình giả từ cache). `app/build.gradle.kts` vì vậy không cho cache task này.
- File giả có `project_id` là `fake-staging-project`. Nó chỉ để build qua được: Analytics, Remote Config và Crashlytics không gửi được gì, Remote Config dùng giá trị mặc định. App vẫn chạy bình thường.
- Không có file giả cho production. Thiếu file production thì build production hỏng, để không bao giờ phát hành nhầm một bản không có Firebase.

**Cờ tính năng (`:core:config`):**

- Một cờ bật khi bản build bật riêng nó (`StagingFeatureFlagsModule`), hoặc khi Remote Config trả về `true`. Remote Config chưa có giá trị cho cờ thì dùng giá trị mặc định của cờ (luôn tắt).
- Đọc ngay giá trị đã kích hoạt từ lần tải trước, không chờ mạng. Giá trị mới tải xong hoặc được đẩy về (real-time) thì được kích hoạt và phát tiếp qua `Flow`, màn hình tự cập nhật.
- Lỗi tải Remote Config không làm app dừng; app giữ giá trị đang có.

**Kiểm thử:** test không gọi Firebase. `:core:config` tách phần đọc Remote Config sau interface `RemoteFlagSource`, test dùng bản giả. Feature test dùng bản giả của `Analytics` và `FeatureFlags` như trước.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| File giả trong repo cho staging, file thật đè lên theo biến thể | CI và phiên tự chạy build được mà không cần bí mật; không phải sửa file trong repo khi có file thật | Bản build từ file giả không gửi được số liệu, dễ nhầm là Firebase hỏng |
| Chỉ áp dụng plugin `google-services` khi có file thật | Không có file giả | Build CI và build phát hành khác nhau; lỗi cấu hình Firebase chỉ lộ ra lúc phát hành |
| Đưa file staging thật vào bí mật cho cả `pr-check.yml` | Build CI có số liệu thật | Workflow kiểm tra PR phải đọc bí mật, trái với ADR-0009 |

## Hệ quả

- **Tích cực:** CI, phiên tự chạy và máy của dev build được ngay sau khi clone; feature không phụ thuộc Firebase; production không thể build thiếu cấu hình.
- **Phải chấp nhận:**
  - Firebase Analytics thêm quyền `com.google.android.gms.permission.AD_ID` (cùng `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`) qua manifest của thư viện. Khai báo Data safety trên Google Play phải ghi việc thu thập mã quảng cáo và dữ liệu sự cố.
  - Analytics chưa nối với sự đồng ý của người dùng châu Âu. Việc này làm cùng hộp thoại đồng ý của quảng cáo (UMP, ADR-0004) ở phần 2 của kế hoạch hoàn thiện dự án mẫu.
  - Build `stagingRelease` bằng file giả vẫn qua, và bước tải mapping file lên Crashlytics không báo lỗi (đã thử trên máy). Nên log build không cho biết Crashlytics có nhận được mapping file hay không. Bản staging phát cho nhóm chỉ build trong `staging.yml`, nơi có file thật.
  - Khi khởi tạo app mới, phải đổi `package_name` trong file giả theo `applicationId` mới, nếu không plugin báo lỗi.
- **Việc AI phải tuân thủ:** không gọi Firebase từ module feature; không sửa hay xóa file `google-services.json` giả, trừ khi đổi `applicationId` theo plan đã duyệt; không tạo, đọc hay chép file `google-services.json` thật vào bất kỳ đâu.
