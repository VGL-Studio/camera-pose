---
id: ADR-0018
title: Màn hình loading chờ tải Remote Config khi mở app, các màn hình khác vẫn không chờ
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển]
---

# ADR-0018: MÀN HÌNH LOADING CHỜ TẢI REMOTE CONFIG KHI MỞ APP, CÁC MÀN HÌNH KHÁC VẪN KHÔNG CHỜ

## Bối cảnh

ADR-0012 quyết định đọc ngay giá trị Remote Config đã kích hoạt từ lần tải trước, không chờ mạng, và không màn hình nào chặn để chờ Remote Config. Spec `features/loading/spec.md` (đã xác nhận ngày 2026-10-15) yêu cầu ngược lại cho một trường hợp: mỗi lần mở app, màn hình loading phải tải được cấu hình mới nhất rồi mới vào màn hình chính; không tải được thì báo lỗi và cho thử lại, kể cả khi đã có cấu hình từ lần trước.

## Quyết định

- `:core:config` có thêm `RemoteConfigRefresher`, với một hàm `refresh()` tải cấu hình mới nhất từ máy chủ, kích hoạt, và trả về thành công hay không. Chỉ màn hình loading được dùng hàm này để chặn màn hình. Các màn hình khác vẫn theo ADR-0012: đọc giá trị đã có, không chờ.
- `refresh()` luôn tải từ máy chủ, bỏ qua bộ nhớ đệm của Remote Config (khoảng tải tối thiểu bằng 0), vì spec yêu cầu có mạng mỗi lần mở app.
- Chờ tối đa 10 giây. Quá thời gian, mất mạng hay máy chủ báo lỗi đều là không thành công.
- Firebase giới hạn số lần tải nếu tải quá dày. Khi Firebase báo bị giới hạn, `refresh()` coi là thành công: máy chủ vừa trả lời trong lần tải gần đây, giá trị đã kích hoạt là mới.
- Bản build dùng file cấu hình Firebase giả (máy dev, CI, ADR-0012) không bao giờ tải được. Với cấu hình giả, `refresh()` trả về thành công ngay, không gọi mạng.
- `FirebaseRemoteFlagSource` không đổi. Giá trị do `refresh()` kích hoạt được cờ tính năng và tham số kinh doanh đọc ở lần đọc kế tiếp.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Loading chờ tải Remote Config từ máy chủ mỗi lần mở app | Đúng spec: vào màn hình chính là có cấu hình mới nhất | App không mở được khi không có mạng; có thể chạm giới hạn tải của Firebase |
| Loading dùng cấu hình đã có nếu tải không được | Mở được khi không có mạng | Trái câu trả lời của người phụ trách sản phẩm trong spec (mục 9, câu 1) |
| Giữ nguyên ADR-0012, loading chỉ chờ file R2 | Không đổi quyết định cũ | Không đáp ứng spec |

## Hệ quả

- **Tích cực:** khi cờ `feature_loading_enabled` bật, mọi màn hình sau loading có cấu hình mới nhất ngay từ đầu.
- **Phải chấp nhận:** khi cờ bật, người dùng không mở được app nếu không có mạng; mỗi lần mở app là một lần tải Remote Config. Cờ mặc định tắt, nên bản production chỉ chịu hệ quả này khi người phụ trách bật cờ.
- **Việc AI phải tuân thủ:** chỉ màn hình loading dùng `RemoteConfigRefresher`. Màn hình khác muốn chờ Remote Config cần spec và ADR riêng.
