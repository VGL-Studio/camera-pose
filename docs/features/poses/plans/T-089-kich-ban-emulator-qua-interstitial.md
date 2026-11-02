---
ticket: T-089
title: Kịch bản emulator đóng interstitial `inter_pose_detail` trước khi chờ camera; ghi đúng nghĩa `consent decline`
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 4                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: low              # low | medium | high
token_estimate: 7.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (L-081)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-089: KỊCH BẢN EMULATOR ĐÓNG INTERSTITIAL `INTER_POSE_DETAIL` TRƯỚC KHI CHỜ CAMERA; GHI ĐÚNG NGHĨA `CONSENT DECLINE`

## 1. Tóm tắt

Ba tệp kịch bản emulator (T-058, T-022, T-024) chờ camera ngay sau khi bấm "Chụp với tư thế này", nhưng từ T-079 interstitial `inter_pose_detail` hiện trước camera (spec ads, poses KB-4), nên cả ba lỗi ở mọi `device-session` (FB-20261031-01). Plan thêm vào ba tệp, ngay sau lần bấm, một bước chờ nút "Close" của quảng cáo hoặc màn camera, chạm "Close" khi có, rồi mới chờ camera. Plan cũng ghi vào `docs/ops/test-sessions.md` rằng sau `consent decline` app vẫn có thể hiện quảng cáo. Kiểm bằng `scripts/device-scenario.py check` và `device-session` sau merge. Dev cũng chạy được ba tệp trên máy thật (mục 4).

## 2. Hiểu yêu cầu

Ticket không đổi hành vi của app (`scenarios: []`). Hành vi mà kịch bản phải đi qua:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| poses KB-4, tiêu chí 3 | Khi bấm "Chụp với tư thế này" thì camera mở với đúng tư thế đó | Từ T-079 có thể có interstitial `inter_pose_detail` xen giữa; camera mở sau khi đóng quảng cáo (`ShootWithAd.kt`: `gate.finish()` chạy trong `onFinished` của `rememberShowInterstitial`). Quảng cáo không hiện (khóa tắt, chưa nạp xong, chưa qua nhịp 45 giây) thì camera mở ngay. Kịch bản phải đi được cả hai đường |

Bằng chứng, `device-session` mới nhất (lần chạy 41334189241, TS-20261101-02, sau RUN-20261101-01): cả ba tệp vẫn lỗi ở bước `wait id=camera_screen 15` ngay sau `tap str=poses_detail_shoot`; logcat có `18:17:39.460 VGL.Poses: Shoot with pose` rồi `18:17:39.465 VGL.Ads: Interstitial shown: inter_pose_detail`. Cây giao diện lúc lỗi (`scenarios/T-058-chup-tu-chi-tiet/fail.xml`), quảng cáo thử của AdMob vẽ trong `WebView` của app:

```text
android.widget.Button  text='Close'  clickable=true  bounds=[284,12][308,36]
android.widget.TextView  text='Test Ad'  bounds=[129,0][191,21]
```

Nút đóng không có `resource-id` hay `content-desc`, chỉ có chữ `Close`. Không chuỗi nào trong `res/values/strings.xml` của app có giá trị `Close` (`grep -rn '>Close<' --include=strings.xml .`: rỗng), nên `text="Close"` không khớp nhầm phần tử của app.

Phần `consent decline`: logcat cùng phiên cho thấy mọi tệp chạy `consent decline` đều ghi `VGL.Ads: Consent path: GRANTED` (ví dụ `18:16:49.419`), rồi vẫn hiện interstitial. Bộ đồng ý UMP coi "Do not consent" là cho phép quảng cáo không cá nhân hóa (spec onboarding câu 9, RUN-20261031-02). Tài liệu hiện chưa nói điều này, nên người viết kịch bản dễ tưởng sau `consent decline` thì không có quảng cáo.

Thiết kế tham chiếu: Không có thiết kế riêng (chỉ sửa kịch bản và tài liệu).

ADR liên quan: ADR-0013 (hộp thoại đồng ý), ADR-0014 (vị trí bật bằng Remote Config), ADR-0029 (dữ liệu test cũ thiếu phần mới).

## 3. Giải pháp

### 3.1. Cách làm

Ở cả ba tệp, thay cặp dòng

```text
tap str=poses_detail_shoot
wait id=camera_screen 15
```

bằng

```text
tap str=poses_detail_shoot
# T-089: inter_pose_detail may show before the camera (spec poses KB-4, T-079); close it when it does
wait text="Close" | id=camera_screen 20
tap_if text="Close"
wait id=camera_screen 15
```

- `wait` thứ nhất chờ quảng cáo hay camera, cái nào tới trước. 20 giây: quảng cáo nạp sẵn hiện sau 5 ms, nhưng `WebView` có thể vẽ nút đóng muộn hơn khung hình đầu.
- `tap_if` chỉ chạm khi thấy nút; camera mở thẳng thì bỏ qua.
- `wait` cuối giữ nguyên như cũ.
- Uiautomator chỉ đọc cửa sổ trên cùng, nên lúc `AdActivity` còn mở thì không thấy `camera_screen` của `MainActivity` phía sau. Cũng không có chuyện camera mở trước rồi quảng cáo đè lên: camera chỉ mở trong `onFinished`.
- Vị trí trong từng tệp: T-058 dòng 25, 26; T-022 dòng 38, 39; T-024 dòng 48, 49 (số dòng ở nhánh chính `12b296e`). Khối đi qua luồng giới thiệu ở đầu tệp (T-083) giữ nguyên.
- Đã chạy `scripts/device-scenario.py check` trên bản sửa của T-058 (mục 4.1): qua.

`docs/ops/test-sessions.md`, mục 5, đoạn **Hộp thoại đồng ý**: thêm một gạch đầu dòng cuối, ngoài khối ví dụ (khối ví dụ được `scripts/tests/run.sh` dòng 4309 cắt từ `ticket: T-NNN` tới `shot landscape`, không đổi):

> - `consent decline` chạm "Do not consent", nhưng bộ đồng ý UMP vẫn cho nạp quảng cáo không cá nhân hóa (camera-pose: log `VGL.Ads: Consent path: GRANTED` sau khi từ chối, spec onboarding câu 9). Nên kịch bản sau `consent decline` vẫn có thể gặp quảng cáo toàn màn hình: bước ngay sau một điểm hiện quảng cáo chờ nút đóng của nó hoặc màn đích (`wait text="Close" | id=<màn đích> 20`, rồi `tap_if text="Close"`), như ba kịch bản của camera-pose T-089. Quảng cáo thử của AdMob vẽ nút đóng trong `WebView`, chỉ có chữ `Close`, không có `resource-id`.

Phản hồi FB-20261031-01: PR plan này ghi mục Phân loại (Nhật chọn ngày 2026-11-02) và `status: planned`. Phiên làm task ghi `fixed` khi ba tệp đạt ở `device-session` sau merge. Không đạt thì để `planned` và ghi vào báo cáo phiên.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` | Sau `tap str=poses_detail_shoot`: chờ `Close` hay camera, `tap_if text="Close"`, rồi `wait id=camera_screen 15` (mục 3.1) |
| Sửa | `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` | Như trên, ở bước (4) |
| Sửa | `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` | Như trên, ở bước (3) |
| Sửa | `docs/ops/test-sessions.md` | Mục 5, đoạn Hộp thoại đồng ý: một gạch đầu dòng về quảng cáo sau `consent decline` (mục 3.1). Khối ví dụ không đổi |
| Sửa | `docs/features/poses/feedback/FB-20261031-01-kich-ban-cho-camera-gap-interstitial.md` | Phiên: `status: fixed` và dòng kết quả ở mục Phân loại khi ba tệp đạt ở `device-session` sau merge |
| Sửa | `docs/features/poses/tickets/T-089-kich-ban-emulator-qua-interstitial.md` | `plan:` trỏ tới plan này; trạng thái theo bước 10 của `auto-run.md` |

Nơi đọc các file này (L-020 của mẫu):
- Ba tệp kịch bản: `scripts/device-session.sh` (bước 4, chạy mọi tệp trong `*/src/deviceScenarios/`) và `scripts/tests/run.sh` (`device-scenario.py check` trên mọi tệp ở PR đổi `feature/**`). Không đổi tên tệp hay phần đầu, nên không ảnh hưởng thêm.
- `docs/ops/test-sessions.md`: `scripts/tests/run.sh` dòng 4309, 4310 cắt khối ví dụ và mong đợi `"0 21"`; gạch đầu dòng mới nằm ở đoạn khác, ngoài khối đó. `scripts/device-session.sh` và `.github/workflows/device-session.yml` chỉ dẫn tên tài liệu trong chú thích.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm. Kịch bản chỉ đóng interstitial `inter_pose_detail` có sẵn; vị trí, nhịp, khóa Remote Config không đổi
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** chạm vào nút `Close` của `WebView` quảng cáo có đóng được quảng cáo trên emulator CI không. Mục 4.1 chỉ kiểm được cú pháp: máy dev lúc viết plan không có máy nào nối `adb`.
- **Điều kiện chuyển:** `device-session` sau merge (hay lần chạy tay trước merge, mục 4) báo bước `wait id=camera_screen 15` cuối `FAIL không thấy phần tử`, trong khi dòng `note` của bước `tap_if` có tọa độ chạm, và `fail.png` vẫn là quảng cáo thử.
- **Cách làm thay:** lần chạm đầu có thể rơi vào lúc `WebView` chưa nhận chạm. Thêm vòng thứ hai trước `wait` cuối của cả ba tệp: `wait id=camera_screen | text="Close" 10`, rồi `tap_if text="Close"`. Không dùng `back`: trình chạy không có lệnh `back` có điều kiện, và `back` khi quảng cáo đã đóng thì thoát khỏi camera. Hai vòng vẫn không đóng được thì dừng task, báo người (mục 7). Tiêu chí ở mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin; chỉ đổi tệp kịch bản emulator và tài liệu.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Cú pháp, `testTag` và chuỗi của ba tệp có thật | Script | `python3 scripts/device-scenario.py check feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` trả mã 0; `scripts/check.sh` (chạy `scripts/tests/run.sh`) qua, kể cả "the example of test-sessions.md passes check" |
| Ba kịch bản đi qua interstitial và kiểm được phần sau bước lỗi cũ (chụp ở T-058; Back thoát app, mất mạng, tối, tiếng Anh ở T-022; camera khi mở gập ở T-024) | Kiểm tra thủ công (emulator: `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`, `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`) | `device-session` cuối phiên: cả ba dòng `đạt` trong bảng "Kịch bản theo plan" của `summary.md`; `scenarios.txt` có `tap_if text="Close" ok` kèm dòng `note ... tap <x>,<y>` (quảng cáo hiện và được đóng) hay không có tọa độ (camera mở thẳng). `camera_screen` không có trên emulator thì `need id=camera_poses` cho `bỏ qua (môi trường)` như trước |
| Kịch bản cũng chạy được trên máy thật, nơi quảng cáo thử có thể có nút đóng khác cỡ, khác chỗ | Kiểm tra thủ công (máy thật: quảng cáo thật, khác emulator CI) | Dev chạy ba tệp trên máy staging: `scripts/device-session.sh start T089`, `scripts/device-session.sh scenarios --yes`, `scripts/device-session.sh stop`; ba tệp `đạt`, hay `bỏ qua (môi trường)` có lý do. Các bước tay ở mục "Kịch bản kiểm tay" dưới |
| Ghi chú `consent decline` đúng và dễ hiểu | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc gạch đầu dòng mới ở `docs/ops/test-sessions.md`, mục 5 |

**Kịch bản kiểm tay (máy thật, bản staging từ nhánh chính sau merge).** Kiểm đúng luồng mà ba tệp đi qua, để biết kịch bản tự động không che lỗi của app:

1. Xóa dữ liệu app (`scripts/device-session.sh reset`), mở app, chọn "Do not consent" ở hộp thoại đồng ý, đi hết luồng giới thiệu tới camera.
   - [ ] Log có `VGL.Ads: Consent path: GRANTED` (từ chối vẫn cho quảng cáo không cá nhân hóa, mục 2).
2. Mở thư viện, chạm thẻ đầu, bấm "Chụp với tư thế này".
   - [ ] Quảng cáo thử toàn màn hình hiện, có nút "Close" (X) ở góc trên phải; log `Interstitial shown: inter_pose_detail`.
   - [ ] Chạm "Close": camera mở với đúng tư thế vừa chọn (tên tư thế trên camera trùng thẻ).
3. Quay lại chi tiết, trong vòng 45 giây bấm "Chụp với tư thế này" lần nữa.
   - [ ] Không có quảng cáo; camera mở ngay (nhịp 45 giây).
4. Đợi hơn 45 giây, lặp lại bước 2 nhưng đóng quảng cáo bằng phím Back thay vì "Close".
   - [ ] Quảng cáo đóng, camera mở với đúng tư thế; không thoát app, không quay về chi tiết.
5. Đợi hơn 45 giây. Mở gập (máy gập) hay xoay ngang, rồi lặp lại bước 2.
   - [ ] Nút "Close" vẫn thấy và chạm được; camera mở.
6. Bấm "Chụp với tư thế này" khi mất mạng (tắt Wi-Fi và dữ liệu ở chi tiết, sau khi ảnh đã tải).
   - [ ] Hộp thoại mất mạng hiện. Không có quảng cáo treo, không có màn trắng.
7. Ở camera vừa mở qua quảng cáo, bấm chụp.
   - [ ] Màn xem lại mở (đúng phần mà T-058 không còn kiểm từ RUN-20261031-01).

**Ngoại lệ viết test trước:** ba tệp kịch bản là test chạy ở `device-session`, không "thất bại trước" được trong phiên (docs/CLAUDE.md, mục Khi viết plan). Lỗi gốc đã thấy ở hai lần `device-session` (41210750330 và 41334189241). Gạch đầu dòng ở `docs/ops/test-sessions.md` là tài liệu, không có test.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-02, trên `12b296e`, bản sửa của `T-058-chup-tu-chi-tiet.txt` theo đúng mục 3.1, đặt trong thư mục tạm (không commit).
- **Lệnh:** `python3 scripts/device-scenario.py check <tệp tạm>`
- **Kết quả:** mã 0, không lỗi: `text="Close"` trong ngoặc kép hợp lệ, cả ở `wait` có nhiều bộ chọn lẫn ở `tap_if`. Chưa chạy trên máy: không có thiết bị nối `adb` lúc viết plan. Phần chạm thật kiểm ở `device-session` sau merge; dev có thể chạy trước (mục 4, dòng máy thật, hay chạy tay `device-session.yml` trên nhánh task). Tiêu chí E dưới 2, mục này chỉ để ghi lại.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Plan không đổi vị trí, nhịp hay khóa quảng cáo (điểm 1), không đổi trình chạy, CI hay quy trình (điểm 7): chỉ đổi dữ liệu của ba kịch bản và thêm một câu giải thích vào tài liệu dùng chung (mục 8, câu 2).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Ba tệp kịch bản ở hai module feature, ba dòng mỗi tệp, cộng một câu tài liệu |
| B. Độ rõ của yêu cầu | 0 | Lỗi có bằng chứng ở hai phiên (log, `fail.xml`, ảnh); app đúng spec; hướng sửa đã nêu trong phản hồi và ticket |
| C. Khả năng kiểm chứng tự động | 1 | `check` tự động; lần chạm thật chỉ kiểm được ở `device-session` sau merge hay trên máy dev |
| D. Ảnh hưởng chéo | 1 | Ba tệp chạy ở `device-session` cuối mọi phiên; `docs/ops/test-sessions.md` là file của dự án mẫu |
| E. Độ mới của kỹ thuật | 1 | Lần đầu kịch bản chạm phần tử trong `WebView` quảng cáo bằng `text=`; các lệnh `wait`, `tap_if` đã dùng nhiều |
| **Tổng** | 4 | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `docs/ops/test-sessions.md` (lấy từ dự án mẫu). Ghi chú về `consent decline` có ích chung, đề xuất về dự án mẫu qua `docs/ops/from-app.md` (ghi trong mô tả PR của task).
- Task khác có thể xung đột: plan nào khác sửa ba tệp kịch bản này hay `docs/ops/test-sessions.md` trong cùng danh sách chạy (hiện không có plan `approved` nào chưa chạy). `T-086-mo-khoa-chi-tiet.txt` không đi qua nút "Chụp với tư thế này" nên không sửa.
- Đề xuất thứ tự chạy: độc lập, chạy đầu danh sách, để `device-session` cuối phiên kiểm được luôn.

## 7. Điều kiện dừng riêng

- Hai vòng chạm "Close" của mục 3.4 vẫn không đóng được quảng cáo trên emulator CI: dừng, không tắt `is_show_inter_pose_detail` ở staging hay sửa `scripts/device-scenario.py` để kịch bản qua (một việc là hành vi quảng cáo, việc kia là script của dự án mẫu).
- Muốn sửa code app (ví dụ thêm `testTag` cho quảng cáo): dừng; ticket không đổi hành vi app.

## 8. Câu hỏi cho người duyệt

1. Kịch bản chờ nút đóng bằng chữ `Close` của quảng cáo thử AdMob. Chữ này do AdMob vẽ, có thể đổi khi nâng SDK hay theo ngôn ngữ máy (`fresh` đặt app tiếng Anh, nhưng `WebView` quảng cáo có thể theo ngôn ngữ của máy). Đề xuất: chấp nhận; nếu chữ đổi, ba tệp lỗi rõ ràng ở bước `wait id=camera_screen` với `fail.png` là quảng cáo, và sửa lại dễ. Phương án khác: chạm theo tọa độ góc trên phải, nhưng trình chạy không có lệnh chạm tọa độ.
2. Sửa `docs/ops/test-sessions.md` (file của dự án mẫu) ngay ở app như ticket ghi, rồi đề xuất về dự án mẫu. Đề xuất: đồng ý. Tài liệu này đã có các ghi chú riêng "camera-pose", và đây chỉ là câu giải thích, không đổi quy trình, nên plan không coi là điểm chặn 7.
3. Đóng FB-20261031-01 (`fixed`) khi ba tệp đạt ở `device-session` sau merge, không chờ dev kiểm tay mục 4. Đề xuất: đồng ý; kiểm tay là để bắt lỗi của app mà kịch bản có thể che, không phải điều kiện của phản hồi.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
