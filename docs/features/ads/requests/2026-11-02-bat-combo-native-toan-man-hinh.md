---
date: 2026-11-02
author: Nhật (AI ghi từ lời trong phiên)
feature: ads
status: specified
---

# YÊU CẦU: BẬT COMBO NATIVE TOÀN MÀN HÌNH SAU INTERSTITIAL

## Mình muốn gì

Lời của Nhật: "Tạo ticket để bật combo fullscreen native cho tôi". Khi được hỏi phạm vi: "Khung native fullscreen sẽ hiển thị ở mọi interstitial, ngoại trừ khi inter được sử dụng thay thế cho aoa/resume ads." Bản staging bật sẵn; production bật ngay khi phát hành.

## Vì sao

Phiên thử TS-20261102-01 (máy thật, bản `0.1.0-staging.22`): Firebase staging đang bật `is_show_native_after_inter`, nhưng app chưa đặt chỗ vẽ native toàn màn hình (`FullScreenAdHost`), nên lượt combo mở mà không hiện gì và không đóng được. Nhật muốn dùng combo thật thay vì chỉ tắt khóa.

## Người dùng sẽ trải nghiệm thế nào

- Sau mỗi interstitial của một màn (rời màn xem lại sau khi lưu ảnh, bấm "Chụp với tư thế này" ở chi tiết tư thế), một quảng cáo native toàn màn hình hiện ra, có đếm ngược trước khi đóng được.
- Đóng quảng cáo native thì quay về đúng chỗ mà interstitial dẫn tới.
- Interstitial hiện thay quảng cáo mở app hay quảng cáo khi quay lại app thì không có quảng cáo native theo sau.
