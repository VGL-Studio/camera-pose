---
date: 2026-10-21
author: AI, từ bản đề xuất đã duyệt (docs/research/proposal.md)
feature: onboarding
status: specified
---

# YÊU CẦU: MÀN CHÀO VÀ XIN QUYỀN CAMERA

## Mình muốn gì

Người dùng mới mở app lần đầu thấy một màn chào duy nhất: app làm được gì, và một nút "Bật camera". Bấm nút thì máy hỏi quyền camera, rồi vào thẳng camera đã có sẵn một tư thế. Từ lần mở thứ hai, app vào thẳng camera.

## Vì sao

Phàn nàn số một về app mẫu là quảng cáo và màn hình chen giữa lúc mở app (bản đề xuất, bài học 1 và 2): Pose Camera cần khoảng 13 chạm và 6 quảng cáo toàn màn hình mới tới camera. App mình đặt đích 2 chạm, 0 quảng cáo. Màn giải thích trước hộp quyền hệ thống giúp người dùng ít từ chối quyền hơn (bài học 16).

## Người dùng sẽ trải nghiệm thế nào

Mở app, màn loading chạy xong thì hiện màn chào. Người dùng đọc nhanh, bấm "Bật camera", máy hỏi quyền, bấm "Cho phép", camera mở với nét viền tư thế đầu tiên. Nếu từ chối quyền, camera hiện lời giải thích và nút mở cài đặt máy.

## Những gì chưa cần làm lần này

Nhiều trang giới thiệu, màn chọn ngôn ngữ (app theo ngôn ngữ của máy), đăng nhập, quảng cáo.

## Quảng cáo, dữ liệu, file tĩnh

- Không quảng cáo.
- Sự kiện: `onboarding_complete`, `camera_permission_result` (bản đề xuất, câu 9.2).
- Không dùng file trên R2.

## Tham khảo

- Bản đề xuất: mục 4 (bài học 1, 2, 8, 16), mục 6 (luồng mở lần đầu), mục 7, mục 10.
- Thiết kế: trang Screens trên Figma, frame `OnboardingScreen_default_light`, `OnboardingScreen_default_dark`.
