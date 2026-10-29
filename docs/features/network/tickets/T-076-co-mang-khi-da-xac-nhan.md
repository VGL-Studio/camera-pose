---
id: T-076
title: Chốt định nghĩa "có mạng" theo ADR-0024 điều 1 (INTERNET và VALIDATED) hay ghi ngoại lệ của app
feature: network
spec: features/network/spec.md
scenarios: [KB-1]
owner: Nhật
status: backlog
risk: medium
size: S
depends_on: []
sprint:
plan:
auto_run:
created: 2026-10-28
---

# T-076: CHỐT ĐỊNH NGHĨA "CÓ MẠNG" THEO ADR-0024 ĐIỀU 1 HAY GHI NGOẠI LỆ CỦA APP

## Mô tả

ADR-0024 điều 1 (dự án mẫu, đã đồng bộ về app) định nghĩa "có mạng" là mạng mặc định có `NET_CAPABILITY_INTERNET` **và** `NET_CAPABILITY_VALIDATED`. `ConnectivitySource.kt` của app chỉ đòi `NET_CAPABILITY_INTERNET` (`core/network/.../ConnectivitySource.kt:21,48`). Đây là lựa chọn có chủ đích ở câu 1 của plan T-011: có máy và emulator xác nhận chậm hay không bao giờ, nên dễ hiện hộp thoại mất mạng oan. Wi-Fi không ra được Internet thì đã bắt qua lỗi tải vì mạng (KB-1, tiêu chí 2).

Bản mẫu (`hasUsableInternet`, T-016 của mẫu) đòi cả hai. T-074 mang về `InternetConnectivity` của khung quảng cáo, dùng chung `ConnectivitySource` của app, nên app và khung phải dùng cùng một định nghĩa. Nên xong trước T-074.

## Phạm vi

- Người chốt một trong hai hướng (câu hỏi dưới):
  - **(a) Theo ADR-0024:** `ConnectivitySource` đòi thêm `NET_CAPABILITY_VALIDATED`, như `hasUsableInternet` của mẫu; test của `AndroidConnectivitySource` thêm trường hợp có INTERNET mà chưa VALIDATED; kiểm kịch bản device-session của popup mất mạng trên emulator CI không hiện oan.
  - **(b) Ngoại lệ của app:** viết ADR app (mã từ 1001) `supersedes` điều 1 của ADR-0024 cho app này, ghi lý do của plan T-011; không đổi code. Nếu lý do có ích chung thì mở issue `from-app` để dự án mẫu xem lại điều 1.
- Spec network: KB-1 ghi rõ "máy báo không còn kết nối" theo định nghĩa đã chốt.

**Không bao gồm:**

- Các phần khác của `core/network` mẫu (`LostAfterDelay`, tự đóng popup, sự kiện `network_lost_shown`, `ScreenName`): app giữ bản riêng theo spec network.
- `InternetConnectivity` cho khung quảng cáo: T-074.

## Câu hỏi cho người

1. Chọn (a) hay (b)? Đề xuất của AI: (a) nếu device-session trên emulator CI và máy thật không hiện popup oan; không thì (b).

## Rủi ro sơ bộ

Chạm module core (`:core:network`, điểm chặn 7 của `risk-scoring.md`) ở hướng (a). Rủi ro chính: popup mất mạng hiện oan trên máy xác nhận mạng chậm, chặn người dùng ở mọi màn.

## Định nghĩa hoàn thành

- [ ] Hướng đã chốt ghi trong spec network (và ADR app nếu chọn b).
- [ ] Hướng (a): có test cho INTERNET mà chưa VALIDATED; device-session của popup mất mạng qua, không hiện oan.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging; đã nghiệm thu chéo.
