---
date: 2026-10-31
author: Nhật (AI ghi từ lời trong phiên)
feature: onboarding
status: specified
---

# YÊU CẦU: LUỒNG GIỚI THIỆU NHIỀU TRANG CÓ QUẢNG CÁO NATIVE STEP

## Mình muốn gì

Lời của Nhật: "đưa vào các màn hình welcome (khoảng 3-5 màn), trong đó có chèn ads native step".

Màn chào một trang hiện nay đổi thành một luồng giới thiệu từ 3 đến 5 trang. Trong luồng có thẻ quảng cáo native ở bước (`native_step` của khung quảng cáo).

## Vì sao

Có thêm chỗ đặt quảng cáo ở lần mở đầu tiên, khi app chưa hiện quảng cáo nào khác: quảng cáo mở app không hiện ở lần mở đầu, theo `show_open_ads_first_open` tắt.

## Người dùng sẽ trải nghiệm thế nào

Người dùng mở app lần đầu, xem lần lượt các trang giới thiệu bằng nút "Tiếp", rồi tới trang cuối có nút "Bật camera". Thẻ quảng cáo nằm ở đáy vùng trang.

## Những gì chưa cần làm lần này

Chưa nêu. AI đặt các điểm chưa rõ thành câu hỏi ở mục 9 của spec onboarding.

## Quảng cáo, dữ liệu, file tĩnh

- Vị trí quảng cáo mới: native ở bước của luồng giới thiệu.
- Không nêu sự kiện hay file tĩnh mới.

## Tham khảo

`docs/ops/ads-guide.md`, dòng "Native ở một bước của luồng" (`NativeStepAd`, `NativeFullScreenStep`, T-032).
