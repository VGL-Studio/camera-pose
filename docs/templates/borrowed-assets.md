# TÀI SẢN MƯỢN TẠM

<!--
Chép mẫu này thành docs/borrowed-assets.md của app khi mượn file đầu tiên (ADR-0021, mục 8).
Mỗi file mượn từ app mẫu có một dòng, thêm trong cùng PR với file đó. Không xóa dòng; khi thay xong thì đổi trạng thái.
Phát hành chỉ khi mọi dòng là replaced hoặc removed.
-->

Quy tắc:

- Tên file và tên tài nguyên có tiền tố `borrowed_` (ví dụ `borrowed_ic_pose_couple.xml`, chuỗi `borrowed_onboarding_title`). Trên R2, file mượn nằm dưới `borrowed/` và chỉ có trên R2 staging.
- Mã băm SHA-256 là của file gốc lấy từ app mẫu (`shasum -a 256 <file>`), để nhận ra file mượn cả khi bị đổi tên.
- Không tải bản build còn dòng `borrowed` lên Play Console, kể cả kênh thử nội bộ.
- Thay tài sản thì ghi lại ảnh chuẩn screenshot liên quan theo plan.

| Đường dẫn | Loại | App nguồn (package, phiên bản) | SHA-256 | Trạng thái | Thay bằng | Ngày thay |
| --- | --- | --- | --- | --- | --- | --- |

Loại: `icon`, `ảnh`, `âm thanh`, `nội dung`, `chuỗi chữ`. Trạng thái: `borrowed` → `replaced` hoặc `removed`.
