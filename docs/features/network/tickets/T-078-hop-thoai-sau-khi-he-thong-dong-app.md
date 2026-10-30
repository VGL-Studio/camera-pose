---
id: T-078
title: Hộp thoại mất mạng hiện ngay khi mở lại app sau khi hệ thống đóng app ở nền
feature: network
spec: features/network/spec.md
scenarios: [KB-3]
owner: Nhật
status: on-staging
risk: medium
size: S
depends_on: [T-077]
sprint:
plan: features/network/plans/T-078-hop-thoai-sau-khi-he-thong-dong-app.md
auto_run:
created: 2026-10-30
---

# T-078: HỘP THOẠI MẤT MẠNG HIỆN NGAY KHI MỞ LẠI APP SAU KHI HỆ THỐNG ĐÓNG APP Ở NỀN

## Mô tả

T-077 cho hộp thoại mất mạng hiện ngay khi người dùng quay lại app mà không có mạng (KB-3, tiêu chí thứ hai, Nhật xác nhận 2026-10-29). Plan T-077 nhận ra "quay lại" khi `MainActivity` bắt đầu sau một lần dừng không do đổi cấu hình (`leftApp` trong `NetworkDialogController`, mục 3.1 của plan).

Rà soát danh sách chạy RUN-20261030-01 thấy một trường hợp lọt: app ở nền bị hệ thống đóng tiến trình (thiếu bộ nhớ), rồi người dùng mở lại từ màn đa nhiệm. Điều hướng được khôi phục về màn sau loading, không qua loading. Controller là đối tượng mới, `leftApp = false`, nên coi đây là lần mở đầu: hộp thoại chỉ hiện theo đường cũ của KB-1, sau thời gian chờ 3 giây, hay khi một lần tải thất bại. Spec KB-3 nói quay lại một màn sau màn loading thì hiện ngay.

Nhật chấp nhận khoảng hở này cho T-077 và chọn làm ticket riêng (RUN-20261030-01, mục Kiểm tra trước khi duyệt, điểm nên sửa thứ hai).

Phần "điều hướng được khôi phục, không qua loading" là nhận định của rà soát, chưa kiểm trên code: plan kiểm lại trước khi chọn cách làm (mục 4.1).

## Phạm vi

- Xác định cách app khôi phục sau khi tiến trình bị đóng: màn nào hiện, loading có chạy lại không, controller có được bật (`arm`) không. Ghi kết quả chạy thử vào plan.
- `core/network` và `:app`: coi lần mở lại có trạng thái được khôi phục (ví dụ `savedInstanceState != null` trong `MainActivity.onCreate`) là một lần quay lại, để lần kiểm 2 giây của T-077 chạy. Plan chọn cách cụ thể và ghi vì sao.
- Test: test Robolectric tái hiện việc khôi phục sau khi tiến trình bị đóng (ví dụ dựng lại activity với `savedInstanceState`), kiểm hộp thoại hiện sau tối đa 2 giây khi không có mạng và không hiện khi có mạng; lần mở đầu thật (không có trạng thái khôi phục) vẫn bị bỏ qua.
- Gộp thêm (Nhật quyết 2026-10-30, khi duyệt bản nháp đầu của plan, câu 2): hộp thoại đang hiện không bị ẩn khi activity dựng lại trong cùng tiến trình (xoay trên máy màn lớn, gập máy, đổi sáng tối, đổi ngôn ngữ). Lỗi có từ trước T-078: khung hình đầu của activity mới báo "chưa qua loading". Sửa ở phần báo của `AppNavHost`, không đổi điều hướng.
- Kiểm thủ công trên máy thật hay emulator: đưa app ra nền, `adb shell am kill <gói>`, tắt mạng, mở lại từ màn đa nhiệm.

**Không bao gồm:**

- Đổi định nghĩa "có mạng" (T-076) hay khoảng 2 giây (T-077).
- Đổi spec: KB-3 đã nói đúng hành vi cần có, ticket chỉ làm code khớp spec.
- Thêm sự kiện phân tích.

## Rủi ro sơ bộ

Chạm module core (`:core:network`, điểm chặn 7 của `risk-scoring.md`) và vỏ app (`MainActivity`). Rủi ro: coi nhầm lần mở đầu sau khi cài (hay sau khi người dùng vuốt bỏ app) là lần quay lại, làm hộp thoại hiện trước khi loading xong; plan phải tách được ba trường hợp: mở mới, khôi phục sau khi tiến trình bị đóng, xoay màn hình.

## Định nghĩa hoàn thành

- [ ] Tiêu chí KB-3 thứ hai đạt cả khi mở lại sau khi hệ thống đóng app ở nền.
- [ ] Có test tự động cho phần logic và phần khôi phục.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging; đã nghiệm thu chéo.
