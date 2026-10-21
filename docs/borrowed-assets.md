# TÀI SẢN MƯỢN TẠM

Danh sách tài sản mượn tạm của Camera Pose: Lines and Guides (ADR-0021, mục 8). Tạo từ `docs/templates/borrowed-assets.md`.

Quy tắc:

- Tên file và tên tài nguyên có tiền tố `borrowed_` (ví dụ `borrowed_ic_pose_couple.xml`, chuỗi `borrowed_onboarding_title`). Trên R2, file mượn nằm dưới `borrowed/` và chỉ có trên R2 staging.
- Mã băm SHA-256 là của file gốc lấy từ app mẫu (`shasum -a 256 <file>`), để nhận ra file mượn cả khi bị đổi tên.
- Không tải bản build còn dòng `borrowed` lên Play Console, kể cả kênh thử nội bộ.
- Thay tài sản thì ghi lại ảnh chuẩn screenshot liên quan theo plan.

| Đường dẫn | Loại | App nguồn (package, phiên bản) | SHA-256 | Trạng thái | Thay bằng | Ngày thay |
| --- | --- | --- | --- | --- | --- | --- |
| R2 staging: `borrowed/poses/img/v1/w01/preview_540.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/000c3f44381a/preview_540.webp` | `bb0f5631c66fcc59f656c08a2483e4134e10c8bed51ca8c6f1851b029ff1f975` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w01/preview_1200.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/000c3f44381a/preview_1200.webp` | `17799a72add56f50140e8b0d00fe90af8db2d1f1dd0b8ff151b8db8047d588eb` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w01/outline.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/000c3f44381a/ink_outline@2x.png (chuyển sang WebP không mất dữ liệu)` | `413da2b4581ce2312ce12c11e8e8b2017f74a024ab3acca0792ba5b8099252e6` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w02/preview_540.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/10329269af80/preview_540.webp` | `fcd353ff10bae829af9f451567f4bf2df92a114f3b2aa3645f037e6d21ef534e` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w02/preview_1200.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/10329269af80/preview_1200.webp` | `527c46b3bca781fcb914c3f4075fa3b057564cc2a527a8cfddf51e462e5d41e4` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w02/outline.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/10329269af80/ink_outline@2x.png (chuyển sang WebP không mất dữ liệu)` | `54a1cc496ff0e2f57d4c3e3b7dbaa4e2eb2a9877de3c9fc93e2f27e8214a0e4e` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w03/preview_540.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/17fc7c2ef8ed/preview_540.webp` | `180b516a83f3f90b34b261d738fb5df97bba9c678769495b7ae5791c12323676` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w03/preview_1200.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/17fc7c2ef8ed/preview_1200.webp` | `1daa1a7013b367833f2fde30504b10f6920991823024e9cb506e2552cec6462e` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/w03/outline.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Woman/17fc7c2ef8ed/ink_outline@2x.png (chuyển sang WebP không mất dữ liệu)` | `4d31ab520a53add7c9debd67fb34e96f9cef95b41faf13c80adf967472af5354` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m01/preview_540.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/0a1b4331c4a8/preview_540.webp` | `85dcc79d13ac22b78ffc6e83dfc8d8ba7e5242f80c3bb85ee2b2265064fe8379` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m01/preview_1200.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/0a1b4331c4a8/preview_1200.webp` | `6bc8fe516bdf1ff0d0acd67006a79278bc36b0e7f4aa9b2d203495cccd580407` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m01/outline.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/0a1b4331c4a8/ink_outline@2x.png (chuyển sang WebP không mất dữ liệu)` | `73c8300d5f722b2e954fb0126333a5be78d6c765c644477e0b7a95cb935d54a5` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m02/preview_540.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/82113b719fd5/preview_540.webp` | `cae11bad70cc376dab3af7229ef929ba3a66f9a6ad6d5a6f691b743bd80c8388` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m02/preview_1200.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/82113b719fd5/preview_1200.webp` | `abf4e794e3a4d2a9ec7109fa2eb027148217d91d758e6eb25d1edd37bbe88aed` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m02/outline.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/82113b719fd5/ink_outline@2x.png (chuyển sang WebP không mất dữ liệu)` | `05cd304738cd46e16c071040942e6324a64485a470f8a6b631da0b66c44f47e9` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m03/preview_540.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/a9c22107f411/preview_540.webp` | `5e2b878e8b6b06ad29f7d5448dc9ebfed6ac04413c5b91749bdeb0ce7b14afde` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m03/preview_1200.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/a9c22107f411/preview_1200.webp` | `cd093b72b73e77e833bc0ab31feec35fa45cb8893db069edba0b7e16e6f2a188` | borrowed | | |
| R2 staging: `borrowed/poses/img/v1/m03/outline.webp` | ảnh | `com.cam.simplepose.pose.camera.photo.guide` 1.0.1, `assets/pose-data/Man/a9c22107f411/ink_outline@2x.png (chuyển sang WebP không mất dữ liệu)` | `12f4ccb84bc4d3d9d071c382d14eb32b864ce24410d832847d4040eb9d0962cb` | borrowed | | |

Loại: `icon`, `ảnh`, `âm thanh`, `nội dung`, `chuỗi chữ`. Trạng thái: `borrowed` → `replaced` hoặc `removed`.

Ghi chú: 18 ảnh trên dùng cho catalog mẫu của R2 staging (`poses/catalog/v1/catalog-587b5394.json`, plan T-009, mục 3.3). Catalog ghi `w03`, `m03` là khung hình cận mặt, nhưng SimplePose chỉ có toàn thân và nửa người, nên hai tư thế này dùng ảnh nửa người. Ảnh không nằm trong repo; bản để tải lên ở thư mục khảo sát `borrowed-upload/`.
