---
date: 2026-11-09
author: Nhật (AI ghi từ lời trong phiên)
feature: onboarding
status: specified
---

# YÊU CẦU: LUỒNG GIỚI THIỆU 4 TRANG CÓ ẢNH MINH HỌA

## Mình muốn gì

Lời của Nhật: "Tôi có một mục đích ẩn sau việc thiết kế các trang onboarding, đó là tạo thêm cơ hội để hiển thị quảng cáo ở mỗi trang, từ đó tăng doanh thu. Do vậy, số lượng các trang này mới nhiều hơn bình thường. Hãy đánh giá lại xem chúng ta chèn bao nhiêu trang onboarding là hợp lý, và nội dung những trang đó nên là như thế nào. Nội dung bao gồm tiêu đề và body, kèm theo đó là ảnh trang trí. Xem xét sử dụng các ảnh trang trí mà tôi đã tạo ở đây. Sau đó, thiết kế lại các trang này trên Figma."

AI đề xuất luôn 4 trang, bỏ trang tư thế premium riêng, mỗi trang một ảnh minh họa của Nhật, chữ mới ngắn hơn. Nhật duyệt ngày 2026-11-09 ("Duyệt.").

Sau đó Nhật chọn thêm: "Chọn phương án khoá nút, tối đa 3 giây": trang 3 giữ nút "Tiếp" tối đa 3 giây để chờ quảng cáo cho trang 4.

## Vì sao

Mỗi trang là một chỗ đặt thẻ quảng cáo native ở lần mở đầu tiên. Luôn 4 trang thì luôn có 4 chỗ, không phụ thuộc cờ tư thế premium. Pool `native_step` có 3 quảng cáo và nạp bù lần lượt (P-01a, P-02, P-03 của `docs/ops/ads-business-rules.md`), nên trang thứ 5 dễ không kịp có quảng cáo, trong khi mỗi trang thêm làm một phần người dùng rời luồng trước khi tới camera.

## Người dùng sẽ trải nghiệm thế nào

Người dùng mở app lần đầu, xem lần lượt 4 trang bằng nút "Tiếp": lời hứa của app, kho tư thế, cách dùng nét viền, rồi trang bật camera. Mỗi trang có một ảnh minh họa lớn, tiêu đề một dòng và lời giới thiệu hai dòng. Thẻ quảng cáo ở đáy vùng trang như hiện nay.

## Những gì chưa cần làm lần này

- Trang thứ 5 về chia sẻ ảnh. Chỉ thử khi đã có số liệu `onboarding_step_viewed`. Ảnh `image_onboarding_3.png` có logo Instagram và Facebook, phải bỏ logo trước khi dùng.
- Đổi vị trí, định dạng hay số lượng quảng cáo.

## Quảng cáo, dữ liệu, file tĩnh

- Không thêm vị trí quảng cáo hay sự kiện phân tích.
- Không tải file từ R2: 4 ảnh đóng gói trong app, để trang 1 không phải chờ tải ảnh. Ảnh nguồn ở `design/handoff/`.

## Tham khảo

- Figma: section "Onboarding flow v2", https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=95-2761
