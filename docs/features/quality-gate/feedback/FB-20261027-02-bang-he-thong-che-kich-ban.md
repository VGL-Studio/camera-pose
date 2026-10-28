---
id: FB-20261027-02
title: Bảng cài đặt nhanh của hệ thống mở đè lên app, cả sáu tệp kịch bản lỗi ở bước đầu
reporter: AI (RUN-20261027-02, device-session cuối phiên)
source: staging
build: nhánh chính 6f3c01d (bản staging của device-session, lần chạy 40576589779)
feature: quality-gate
ticket: T-070
severity: nang
status: fixed
created: 2026-10-27
---

# FB-20261027-02: BẢNG CÀI ĐẶT NHANH CỦA HỆ THỐNG MỞ ĐÈ LÊN APP, CẢ SÁU TỆP KỊCH BẢN LỖI Ở BƯỚC ĐẦU

## Điều đã xảy ra

Ở `device-session` cuối phiên RUN-20261027-02 (lần chạy 40576589779, phiên thử `TS-20261027-02`), cả sáu tệp kịch bản theo plan (T-056, T-020, T-058, T-064, T-022, T-024) ra `lỗi` ở cùng bước 2, `consent decline 30`: "không thấy hộp thoại đồng ý hay màn hình đầu của app". Crash 0, ANR 0.

- **Kết luận (có bằng chứng):** cả sáu `fail.xml` chỉ có node của `com.android.systemui` (68 node mỗi tệp); `fail.png` là bảng cài đặt nhanh của hệ thống kéo xuống hết màn hình. App vẫn chạy phía dưới: ở tệp đầu, `20:35:00.623 I VGL.Navigation: Main screen: ONBOARDING` và `20:35:00.654 I VGL.Onboarding: Onboarding shown`, trong 30 giây chờ của bước 2 (bắt đầu khoảng 20:34:48). `step_fresh` của `scripts/device-scenario.py` dừng app, xóa dữ liệu, cấp quyền rồi mở app, nhưng không đóng bảng hệ thống, nên bảng che app ở mọi tệp.
- **Giả thuyết (chưa chắc):** monkey (1500 sự kiện, chạy ngay trước, 20:32:34 tới khoảng 20:34:40) vuốt từ mép trên và để bảng mở. `--pct-syskeys 0` chỉ tắt phím hệ thống, không tắt cử chỉ vuốt. Lần này app mở được camera (`Camera ready: back` 5 lần, T-062), nên đường đi của monkey khác các lần trước; các lần chạy thử của T-062 (40366043473, 40366063060) không gặp.

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính `6f3c01d` với `reason` bất kỳ.
2. Xem bảng "Kịch bản theo plan" của `summary.md` và `scenarios/*/fail.xml`.

## Mong đợi

Trình chạy kịch bản bắt đầu mỗi tệp từ màn hình của app, không bị bảng hay hộp thoại của hệ thống che. Đề xuất: `step_fresh` (và trước phần kịch bản theo plan, sau monkey) chạy `adb shell cmd statusbar collapse`; khi dump màn hình chỉ có `com.android.systemui` thì đóng bảng một lần rồi đọc lại trước khi báo lỗi.

## Ảnh chụp, video

Trong artifact `device-session` của lần chạy 40576589779: `TS-20261027-02/scenarios/*/fail.png`, `fail.xml` (không đưa vào repo).

## Thiết bị

- Máy: emulator của CI (API 34, `google_apis`, x86_64, `-camera-back emulated`)
- Phiên bản Android: 14
- Ngôn ngữ, chế độ tối: mặc định của trình chạy

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** đề xuất của AI: lỗi của trình chạy kịch bản (`scripts/device-scenario.py`, file của dự án mẫu), không phải của app
- **Xử lý:** nhóm chọn ngày 2026-10-28: ticket T-070 sửa trình chạy kịch bản và soạn plan ngay. `fresh` đóng bảng (`cmd statusbar collapse`) trước khi mở app; lần dump chỉ có node của `com.android.systemui` thì đóng bảng một lần rồi đọc lại. Đề xuất về dự án mẫu. Trong lúc chờ, các tiêu chí kịch bản của T-020, T-058, T-062, T-064 chưa được kiểm trên emulator
- **Lý do:** lỗi của trình chạy, không phải của app: app mở màn đầu trong 30 giây chờ (`VGL.Onboarding: Onboarding shown`), chỉ bị bảng của hệ thống che. Đóng bảng ở trình chạy đúng cho mọi nguyên nhân để bảng mở (monkey hay thông báo của hệ thống), nên không đổi tham số của monkey. Tái hiện trên `scripts/tests/fake-adb` (bảng mở tới khi có lệnh đóng): `ok.txt` ra đúng lỗi của phiên, `step=2 consent decline 5 FAIL không thấy hộp thoại đồng ý hay màn hình đầu của app` (plan T-070, mục 4).
- **Liên kết:** ticket và plan T-070 (`docs/features/quality-gate/plans/T-070-dong-bang-he-thong.md`); plan T-054 và T-052 (cùng trình chạy).
