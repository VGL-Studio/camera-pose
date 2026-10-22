---
ticket: T-026
title: Không ghi camera_permission_result khi quyền camera đã bị từ chối vĩnh viễn
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10.0M        # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [analytics]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-026: KHÔNG GHI CAMERA_PERMISSION_RESULT KHI QUYỀN CAMERA ĐÃ BỊ TỪ CHỐI VĨNH VIỄN

## 1. Tóm tắt

Camera vẫn gọi hộp hỏi quyền như hiện nay, nhưng trước và sau khi gọi, màn hình đọc thêm "máy có nên giải thích lý do xin quyền không" (`shouldShowRequestPermissionRationale`). Từ hai giá trị đó và kết quả, một hàm thuần suy ra hộp hỏi có thật sự hiện hay không; ViewModel chỉ ghi `camera_permission_result` khi hộp hỏi đã hiện. Kiểm chứng bằng unit test cho hàm suy luận và ViewModel, cùng một bước kiểm tra thủ công trên máy thật.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-7 | Khi chưa có quyền camera và máy còn hỏi được thì camera hỏi quyền khi mở; app ghi `camera_permission_result` | Giữ như T-013: hộp hỏi hiện và người dùng chọn thì ghi `granted` hoặc `denied` |
| KB-7 | Khi quyền camera đã bị từ chối vĩnh viễn (máy không còn hiện hộp hỏi) thì camera vào thẳng trạng thái chưa có quyền và app không ghi `camera_permission_result` | Máy trả "từ chối" ngay mà không hiện hộp hỏi: vào `PermissionDenied`, không ghi sự kiện, có ghi log |
| Mục 6 | `camera_permission_result` chỉ ghi khi hộp hỏi hiện ra và trả kết quả | Như trên |

Thiết kế tham chiếu: không đổi giao diện. Trạng thái chưa có quyền giữ như `design/screens/CameraScreen_permission-denied_dark.png`.

ADR liên quan: ADR-0019 (log), ADR-0012 (phân tích qua `:core:analytics`).

## 3. Giải pháp

### 3.1. Cách làm

Android không cho biết trực tiếp hộp hỏi có hiện hay không. Cách suy luận dựa trên `shouldShowRequestPermissionRationale` (gọi tắt là "nên giải thích"):

| Tình huống | Nên giải thích trước | Kết quả | Nên giải thích sau | Hộp hỏi hiện? |
| --- | --- | --- | --- | --- |
| Lần hỏi đầu, người dùng cho phép | sai | cho phép | | có |
| Lần hỏi đầu, người dùng từ chối | sai | từ chối | đúng | có |
| Lần hỏi thứ hai, người dùng từ chối (từ nay vĩnh viễn) | đúng | từ chối | sai | có |
| Đã bị từ chối vĩnh viễn, máy trả từ chối ngay | sai | từ chối | sai | không |
| Android 11 trở lên, lần hỏi đầu, chạm ra ngoài hộp hỏi | sai | từ chối | sai | có, nhưng bị coi là không (câu hỏi 1) |

Quy tắc: hộp hỏi được coi là đã hiện khi `granted || rationaleBefore || rationaleAfter`.

- Tạo `PermissionPrompt.kt` với hàm thuần `internal fun permissionDialogShown(granted: Boolean, rationaleBefore: Boolean, rationaleAfter: Boolean): Boolean` theo quy tắc trên.
- `CameraViewModel.onPermissionResult(granted: Boolean, dialogShown: Boolean = true)`: khi `dialogShown` sai thì không ghi sự kiện, ghi log `"Camera permission denied without dialog"`, vào `PermissionDenied`. Tham số mặc định `true` giữ nguyên hành vi và các test hiện có của T-013.
- `CameraRoute` (`CameraScreen.kt`): lấy activity bằng `LocalActivity.current` (cách `:core:ads` đang dùng). Khi nhận `CameraEvent.RequestPermission`, lưu `rationaleBefore` vào một biến `remember` rồi mới `launch`. Trong callback của launcher, đọc `rationaleAfter` và gọi `viewModel.onPermissionResult(granted, permissionDialogShown(granted, rationaleBefore, rationaleAfter))`. Activity null (chỉ xảy ra trong preview) thì coi như hộp hỏi đã hiện.
- Không lưu thêm gì vào DataStore: quy tắc chỉ dùng giá trị đọc tại chỗ, nên không cần cờ "đã từng hỏi" và không chạm lược đồ dữ liệu.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/PermissionPrompt.kt` | Hàm thuần `permissionDialogShown` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | Tham số `dialogShown` của `onPermissionResult`, bỏ ghi sự kiện khi không hiện hộp hỏi, log |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | `CameraRoute`: đọc "nên giải thích" trước và sau khi hỏi quyền |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/PermissionPromptTest.kt` | Unit test cho năm dòng của bảng ở mục 3.1 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Thêm hai test ở mục 4; test cũ giữ nguyên |
| Sửa | `docs/features/camera/plans/T-026-khong-ghi-quyen-tu-choi-vinh-vien.md` | Nhật ký thực hiện |
| Sửa | `docs/features/camera/tickets/T-026-khong-ghi-quyen-tu-choi-vinh-vien.md` | Trạng thái |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có (dùng cờ camera hiện có, không đổi).
- Quảng cáo: Không chạm.
- Sự kiện phân tích: `camera_permission_result` đã có trong spec; đổi điều kiện ghi theo spec mục 6 (điểm chặn 4). Tên và tham số giữ nguyên.
- File R2: Không chạm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Quy tắc suy luận, năm dòng của bảng mục 3.1 | Unit test | `PermissionPromptTest.granted_isShown` (`true, false, false` trả `true`); `firstDenial_isShown` (`false, false, true` trả `true`); `secondDenial_isShown` (`false, true, false` trả `true`); `permanentDenial_isNotShown` (`false, false, false` trả `false`); `deniedWithBothRationales_isShown` (`false, true, true` trả `true`) |
| KB-7, từ chối vĩnh viễn: không ghi sự kiện, vào trạng thái chưa có quyền | Unit test | `CameraViewModelTest.permissionDeniedWithoutDialog_logsNoEvent_andShowsDenied`: `onPermissionResult(granted = false, dialogShown = false)`; `analytics.events` rỗng; `uiState == PermissionDenied`; `controller.openCalls` rỗng; `RecordingAppLogger` có dòng `"Camera permission denied without dialog"` |
| KB-7, hộp hỏi hiện và từ chối: vẫn ghi | Unit test | `CameraViewModelTest.permissionDeniedWithDialog_logsEvent`: `onPermissionResult(granted = false, dialogShown = true)`; `analytics.events == [AnalyticsEvent("camera_permission_result", mapOf("result" to "denied", "source" to "camera"))]` |
| KB-7, giữ hành vi T-013 | Unit test | Các test hiện có `permissionGranted_logsEvent_andOpens`, `permissionDenied_logsEvent_andShowsDenied`, `deniedThenStartedWithPermission_opensWithoutNewEvent` qua mà không sửa |
| Giao diện không đổi | Screenshot test | Ảnh chuẩn hiện có của `CameraScreen` không đổi |
| Cách đọc "nên giải thích" trong `CameraRoute` | Kiểm tra thủ công | Trên máy thật bản staging, `adb logcat \| grep ' VGL\.'`: (1) cài mới, mở camera, từ chối: log `Camera permission denied` và DebugView có `denied`; (2) mở lại, từ chối lần hai: như (1); (3) mở lại lần ba (hộp hỏi không hiện): log `Camera permission denied without dialog`, không có sự kiện; (4) "Mở cài đặt", cho phép, quay lại: camera chạy |

**Ngoại lệ viết test trước:** Phần đọc "nên giải thích" trong `CameraRoute` không có test tự động: Robolectric không mô phỏng hộp hỏi quyền của hệ thống, và test trên emulator không chạm được hộp hỏi ổn định. Phần này nhỏ (vài dòng nối dữ liệu), logic nằm trong hàm thuần đã có test; kiểm tra bằng bước thủ công ở trên.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (sửa điều kiện ghi sự kiện phân tích `camera_permission_result`). Plan xếp **Cao**, cần duyệt chéo. Ticket chấm sơ bộ thấp; nếu không tính điểm chặn, năm tiêu chí dưới đây cho 3 điểm.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Ba file trong một module feature, một file mới nhỏ |
| B. Độ rõ của yêu cầu | 0 | Spec KB-7 và mục 6 nói rõ |
| C. Khả năng kiểm chứng tự động | 1 | Logic có unit test; phần đọc từ hệ thống chỉ kiểm thủ công |
| D. Ảnh hưởng chéo | 0 | Không file dùng chung; T-012 sẽ dùng cùng quy tắc nhưng chưa có code |
| E. Độ mới của kỹ thuật | 1 | `shouldShowRequestPermissionRationale` lần đầu dùng trong dự án; hành vi khác nhau giữa các bản Android (bảng mục 3.1) |
| **Tổng** | 3 | **Mức: Cao (điểm chặn 4)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: Không có.
- Task khác trong sprint có thể xung đột: T-014, T-016 (cùng sửa `CameraViewModel.kt`, `CameraScreen.kt`). Chạy T-026 trước hoặc sau, không cùng lúc.
- T-012 (màn chào) ghi `camera_permission_result` với `source` `onboarding`; plan T-012 nên dùng cùng quy tắc. Hàm `permissionDialogShown` nằm trong `:feature:camera`, nên T-012 cần một bản riêng hoặc đề xuất đưa vào module core (câu hỏi 2).
- Đề xuất thứ tự chạy: task độc lập, xếp trước chuỗi T-014, T-016.

## 7. Điều kiện dừng riêng

- Một test hiện có của T-013 thất bại sau khi đổi `onPermissionResult`: dừng, không sửa test cũ.
- `LocalActivity` không có trong phiên bản `activity-compose` của dự án: dừng, không thêm thư viện.

## 8. Câu hỏi cho người duyệt

1. **Chạm ra ngoài hộp hỏi ở lần hỏi đầu (Android 11 trở lên)** bị coi là "không hiện hộp hỏi", nên lần đó không ghi sự kiện. Đề xuất của AI: chấp nhận, vì người dùng không trả lời; muốn đếm được trường hợp này thì phải lưu cờ "đã từng hỏi" vào DataStore (thêm khóa, rủi ro cao hơn).
2. **T-012 cần cùng quy tắc.** Đề xuất của AI: T-012 chép hàm năm dòng này vào `:feature:onboarding` (không phụ thuộc feature khác, ADR-0001); nếu sau này thêm nơi thứ ba thì đưa vào một module core.
3. **Mức rủi ro.** Plan xếp Cao chỉ vì điểm chặn 4 (sửa điều kiện ghi sự kiện), dù thay đổi nhỏ. Cần một dev khác duyệt chéo.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
