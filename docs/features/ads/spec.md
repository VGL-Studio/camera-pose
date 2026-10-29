---
feature: ads
status: draft            # draft | confirmed | changed
owner: Tuấn
product_owner: Nhật
requests:
design:                  # không có thiết kế riêng; giao diện quảng cáo theo khung của dự án mẫu
created: 2026-10-28
confirmed:
---

# SPEC: CHUYỂN QUẢNG CÁO CỦA APP SANG KHUNG ADSKIT CỦA DỰ ÁN MẪU

Spec này chốt các quyết định kinh doanh cần trước khi soạn plan T-074 (lấy khung quảng cáo AdsKit và AppsFlyer của dự án mẫu) và T-075 (`release.sh` theo khung mới). Vị trí quảng cáo của từng màn vẫn nằm ở mục 5 của spec màn đó (`features/camera/spec.md`, `features/poses/spec.md`). Khi spec này được xác nhận, các câu trả lời được chép vào hai spec đó trong PR của T-074, cùng lúc với code.

Spec để `draft` cho tới khi mục 9 có đủ câu trả lời, nên `scripts/plan-queue.py` bỏ qua T-074 và T-075.

## 1. Mục tiêu

- **Người dùng được gì:** quảng cáo hiện theo cùng một bộ luật đã kiểm trên máy thật ở dự án mẫu. Không có quảng cáo trong lúc ở camera, màn chào hay loading; quảng cáo mở app hiện ở điểm chuyển từ loading sang camera (câu 6).
- **Vì sao làm bây giờ:** dự án mẫu đã xong khung quảng cáo theo AdsKit PARTNER (`0.1.0-staging.18`). App đang giữ bộ cũ theo ADR-1004, và mọi sửa của dự án mẫu cho quảng cáo không tới được app.
- **Đo kết quả bằng gì:** doanh thu quảng cáo trên một người dùng mỗi ngày và tỷ lệ giữ chân ngày 1. So hai tuần trước và sau bản có khung mới.

## 2. Phạm vi

**Trong phạm vi:**

- Ba vị trí hiện có, giữ chỗ hiện:
  - `review_interstitial`: toàn màn hình, khi rời màn xem lại sau khi lưu ảnh;
  - `poses_banner`: banner đáy màn thư viện;
  - `poses_detail_banner`: banner đáy màn chi tiết tư thế.
- Luật tần suất, khóa Remote Config, theo câu trả lời ở mục 9. AppsFlyer chưa bật (câu 3).
- Hai vị trí toàn app của khung (câu 6): quảng cáo mở app (`open_app`), sau loading và trước camera; quảng cáo khi quay lại app (`resume`), trừ khi đang ở camera.

**Ngoài phạm vi:**

- Vị trí quảng cáo mới khác hai vị trí toàn app ở trên, kể cả combo native sau interstitial và native đáy có sẵn trong khung. Mỗi vị trí cần spec được duyệt và ticket riêng.
- AppsFlyer (câu 3), màn xem thử quảng cáo `AdGalleryActivity` và `SAMPLE_BOTTOM_BANNER` (câu 4).

## 3. Kịch bản sử dụng

Không có kịch bản mới. Ba vị trí giữ kịch bản của spec camera và poses. Tiêu chí kiểm ở mục 9 và ở Định nghĩa hoàn thành của T-074.

## 4. Giao diện

Không có. Banner và interstitial dùng giao diện của khung.

## 5. Quảng cáo

Ba vị trí của màn: theo `features/camera/spec.md` và `features/poses/spec.md`, mục 5, sau khi chép câu trả lời của mục 9 vào. Hai vị trí toàn app (`open_app`, `resume`) ghi ở đây theo câu 6, và chép vào mục 5 của `features/loading/spec.md` (quảng cáo mở app hiện sau loading) và `features/camera/spec.md` (không có quảng cáo khi quay lại app ở camera) trong PR của T-074. Không thêm vị trí khác.

## 6. Dữ liệu và phân tích

AppsFlyer chưa bật (câu 3): không gửi dữ liệu nào tới AppsFlyer, không có quyền mà SDK kéo vào. Sự kiện phân tích mà khung ghi qua Firebase Analytics (nếu có) được plan liệt kê và chép vào mục 6 của spec màn có vị trí trong PR của T-074.

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

Không có cờ mới. Mỗi vị trí có khóa bật tắt Remote Config (câu 2).

## 9. Câu hỏi còn mở

1. **Luật tần suất của `review_interstitial`.**
   - Hiện nay:
     - `AdsSettings.breakCapDefaults` ở `core/ads` cho `review_interstitial` dùng `BreakCap(firstBreak = 2, breaksBetween = 3, minIntervalSeconds = 120)`: không hiện trước ảnh lưu thứ 2, tối đa 1 lần mỗi 3 ảnh, cách lần trước 120 giây;
     - mỗi luật có khóa riêng: `ads_review_interstitial_first_break`, `ads_review_interstitial_breaks_between`, `ads_review_interstitial_min_interval_seconds`;
     - thêm nhịp chung 45 giây của mọi interstitial (`ads_interstitial_min_interval_seconds`).
   - Khung mới có nhịp chung `ads_interval` (mặc định 45 giây). Đây là bản đổi tên của nhịp chung cũ, không phải luật mới. Khung không có ba luật riêng của vị trí.
   - Phương án:
     - (a) giữ cả ba luật riêng, làm điều kiện trước khi gọi quảng cáo toàn màn hình của khung;
     - (b) bỏ ba luật riêng, chỉ dùng `ads_interval`;
     - (c) giữ "ảnh thứ 2" và "1 lần mỗi 3 ảnh", bỏ 120 giây riêng (chỉ còn `ads_interval`).
   - Đề xuất của AI: (c).
   - Trả lời: (c). Nhật, 2026-10-28. Giữ "không hiện trước ảnh lưu thứ 2" và "tối đa 1 lần mỗi 3 ảnh"; bỏ 120 giây riêng, chỉ còn nhịp chung `ads_interval`.
2. **Tên khóa Remote Config.**
   - Khóa bật tắt: `ads_review_interstitial_enabled`, `ads_poses_banner_enabled`, `ads_poses_detail_banner_enabled` đổi sang `is_show_review_interstitial`, `is_show_poses_banner`, `is_show_poses_detail_banner` (quy ước của khung).
   - Ba khóa giới hạn của câu 1: giữ tên cũ cho luật còn giữ, bỏ khóa của luật bị bỏ; `ads_interstitial_min_interval_seconds` thay bằng `ads_interval`.
   - Trên Firebase, người phụ trách tạo khóa mới với giá trị của khóa cũ trước khi bản mới phát; khóa cũ giữ tới khi bản cũ không còn người dùng.
   - Đề xuất của AI: đồng ý như trên.
   - Trả lời: đồng ý như trên. Nhật, 2026-10-28. Theo câu 1: giữ `ads_review_interstitial_first_break`, `ads_review_interstitial_breaks_between`; bỏ `ads_review_interstitial_min_interval_seconds`.
3. **AppsFlyer.**
   - Bật AppsFlyer như dự án mẫu (ADR-0027)? Staging không có khóa nên không gửi gì; production cần khóa riêng (T-075).
   - Bật thì mục 6 của spec màn có vị trí ghi thêm dữ liệu gửi đi, và mục 6 của spec ghi hai quyền đọc nguồn cài đặt mà SDK kéo vào manifest.
   - Đề xuất của AI: bật.
   - Trả lời: chưa bật. Nhật, 2026-10-28. T-074 lấy khung của `core/analytics` nhưng không đưa SDK AppsFlyer vào app: không thêm thư viện AppsFlyer, không có quyền đọc nguồn cài đặt, không gửi dữ liệu nào tới AppsFlyer. Plan kiểm làm được không; không được thì giữ `core/analytics` cũ (dòng `hold=`) và báo lại trong plan trước khi duyệt. Bật AppsFlyer sau cần spec và ticket riêng.
4. **Màn xem thử quảng cáo (staging) và `feature/sample`.**
   - Giữ màn xem thử của khung (`AdGalleryActivity`, chỉ có ở staging) để thử quảng cáo trên máy thật? Bỏ `SAMPLE_BOTTOM_BANNER` của `feature/sample`?
   - Đề xuất của AI: giữ màn xem thử; bỏ `SAMPLE_BOTTOM_BANNER` nếu app không còn dùng `feature/sample`.
   - Trả lời: bỏ cả hai. Nhật, 2026-10-28. Không mang `AdGalleryActivity` vào app; bỏ `SAMPLE_BOTTOM_BANNER`.
5. **Chia lượt.**
   - Một ticket (T-074) chia ba lượt trong plan (lấy code và build qua; nối vỏ app và chuyển vị trí; spec, ảnh chuẩn, tài liệu), mỗi lượt một PR, cùng một danh sách chạy? Hay tách thành ba ticket nối nhau?
   - Đề xuất của AI: một ticket, ba lượt.
   - Trả lời: một ticket, ba lượt. Nhật, 2026-10-28.
6. **Quảng cáo toàn app có sẵn trong khung.**
   - `AdPlacement` của khung có `open_app`, `open_app_inter`, `resume`, `resume_inter`, `native_combo`.
   - Ở production các khóa của chúng (`show_open_ads`, `show_resum_ads`, `is_show_native_after_inter`) mặc định tắt.
   - `StagingRemoteValuesModule.globalAdSwitchesOn()` của dự án mẫu lại bật sẵn ở staging:
     - quảng cáo mở app;
     - quảng cáo khi quay lại app (kể cả lúc đang ở camera);
     - trang native sau interstitial (sẽ nối sau `review_interstitial`);
     - native đáy.
   - Lấy nguyên phần `app/` thì bản staging trái mục 5 của spec camera ("Không vị trí quảng cáo nào ở camera").
   - Đề xuất của AI: giữ tắt mọi vị trí toàn app. `StagingRemoteValuesModule` của app không bật các khóa đó, chỉ giữ hai khóa ghi log (`is_log_ad_fullscreen_events`, `is_log_ad_slot_events`), có test kiểm. Muốn dùng vị trí nào thì thêm vào spec màn đó qua một ticket riêng.
   - Trả lời: bật quảng cáo mở app và quảng cáo khi quay lại app; native sau interstitial và native đáy giữ tắt. Nhật, 2026-10-28.
     - Quảng cáo mở app (`open_app`): hiện một lần khi loading xong, trước khi vào camera. Không hiện trong lúc ở màn chào hay loading.
     - Quảng cáo khi quay lại app (`resume`): không hiện khi người dùng quay lại app lúc đang ở màn camera (mục 5 của spec camera giữ nguyên).
     - Bản staging: `StagingRemoteValuesModule` của app bật `show_open_ads` và `show_resum_ads`; không bật `is_show_native_after_inter`, `is_show_native_bottom`. Production theo Remote Config, mặc định tắt.
     - `show_open_ads_first_open` (lần mở đầu tiên, còn hộp thoại đồng ý và onboarding): AI đề xuất giữ tắt; chốt khi duyệt plan T-074.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-28 | AI | Tạo bản đầu cho T-074, T-075 theo rà soát PR #209: chuyển câu hỏi từ ticket vào spec, thêm câu 6 (quảng cáo toàn app), sửa chỗ đặt luật tần suất và khóa ở câu 1, 2 |
| 2026-10-28 | Nhật | Trả lời sáu câu của mục 9 (AI ghi). Sửa mục 1, 2, 5, 6 theo câu 3, 4, 6: AppsFlyer chưa bật, bỏ màn xem thử, bật quảng cáo mở app (sau loading) và khi quay lại app (trừ camera) |
