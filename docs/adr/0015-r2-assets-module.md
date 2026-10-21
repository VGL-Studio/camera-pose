---
id: ADR-0015
title: Module `:core:assets` tải file tĩnh từ R2 một lần, giữ trên máy, đường dẫn bắt buộc có phiên bản
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển]
---

# ADR-0015: MODULE `:CORE:ASSETS` TẢI FILE TĨNH TỪ R2 MỘT LẦN, GIỮ TRÊN MÁY, ĐƯỜNG DẪN BẮT BUỘC CÓ PHIÊN BẢN

## Bối cảnh

ADR-0006 quyết định file tĩnh nằm trên R2, chỉ thêm mới, truy cập qua `:core:assets` có lưu đệm và xử lý khi không tải được. ADR-0007 tách địa chỉ R2 của staging và production. ADR này ghi cách làm cụ thể.

## Quyết định

**Feature dùng một hàm:** `RemoteAssets.load(AssetPath)`, gọi từ ViewModel. Kết quả:

| Kết quả | Ý nghĩa | Feature làm gì |
| --- | --- | --- |
| `AssetResult.Ready(file)` | File đã có trên máy | Đọc file, không sửa, không xóa |
| `AssetResult.Unavailable` | Chưa có trên máy và không tải được (mất mạng, file không có trên R2) | Đưa vào `UiState`, cho người dùng thử lại |

Hàm không bao giờ ném lỗi mạng.

**Đường dẫn:**

- `AssetPath` là đường dẫn tương đối so với địa chỉ gốc của môi trường. Feature khai báo đường dẫn của mình, không bao giờ viết địa chỉ gốc hay URL đầy đủ. Đây là cách hiểu quy tắc "mọi đường dẫn R2 lấy từ `:core:assets`" của ADR-0006: địa chỉ R2 chỉ do `:core:assets` ghép.
- Đường dẫn phải chỉ đúng một phiên bản nội dung, nếu không `AssetPath` báo lỗi ngay khi tạo: thư mục phiên bản (`images/v2/hero.webp`), hoặc phiên bản hay mã băm trong tên file (`images/hero.v2.webp`, `images/hero.3f2a9c1e.webp`). Chỉ cho phép chữ, số, `.`, `_`, `-`, mỗi phần bắt đầu bằng chữ hoặc số.

**Lưu trên máy:**

- Mỗi file tải một lần, giữ trong `noBackupFilesDir/r2-assets` theo đúng đường dẫn. Không dùng thư mục cache vì hệ thống có thể xóa, làm mất file khi không có mạng. Không sao lưu vì luôn tải lại được.
- File trên R2 không bao giờ đổi (ADR-0006), nên file đã có trên máy không bao giờ kiểm tra lại.
- File tải vào tên tạm rồi mới đổi tên, nên tải dở không bao giờ để lại file trông như đã xong. Nhiều nơi cùng xin một file thì chỉ tải một lần.
- Tải bằng `HttpURLConnection` của Android, không thêm thư viện.

**Địa chỉ gốc theo biến thể build:**

| | Staging | Production |
| --- | --- | --- |
| Địa chỉ gốc | `https://app-project-template.vgl.io.vn/`, trong `app/build.gradle.kts` | Tham số `-PassetsBaseUrl` lúc build phát hành |

- Thiếu `-PassetsBaseUrl` thì build production hỏng ngay, cùng chỗ kiểm tra tham số quảng cáo (ADR-0013).
- Địa chỉ phải dùng https.
- Script khởi tạo app mới (phần 5 của kế hoạch hoàn thiện) phải thay địa chỉ staging bằng địa chỉ của app mới.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tự tải và giữ file, trả về `File` | Không thêm thư viện; dùng được cho mọi loại file (ảnh, JSON, âm thanh); kiểm soát rõ chuyện dùng khi mất mạng | Feature tự đọc file; muốn hiện ảnh thì tự giải mã |
| Thư viện tải ảnh (Coil) với bộ nhớ đệm đĩa | Hiện ảnh trong Compose rất gọn | Thêm SDK; bộ nhớ đệm có giới hạn dung lượng, có thể xóa file cũ nên không bảo đảm dùng được khi mất mạng; chỉ cho ảnh |
| Danh sách mọi đường dẫn khai báo tập trung trong `:core:assets` | Thấy hết file trên R2 ở một chỗ | Mỗi file mới của feature phải sửa module core |

## Hệ quả

- **Tích cực:** file đã tải dùng được khi mất mạng mãi mãi; không thể vô tình dùng đường dẫn có thể bị ghi đè; staging và production không lẫn file.
- **Phải chấp nhận:**
  - File cũ không tự xóa trên máy. Khi feature chuyển sang phiên bản mới, bản cũ vẫn chiếm chỗ. Nếu sau này dung lượng thành vấn đề, cần thêm bước dọn dẹp.
  - Chưa hỗ trợ hiện ảnh trực tiếp trong Compose; app nào cần nhiều ảnh thì đề xuất thêm thư viện qua ADR mới.
  - App mẫu chưa có feature nào dùng file từ R2, vì chưa có file mẫu trên R2 và không có spec yêu cầu.
- **Việc AI phải tuân thủ:** feature chỉ dùng `RemoteAssets`, không gọi HTTP hay viết địa chỉ R2; mỗi thay đổi nội dung file là một đường dẫn mới; không viết code hay script ghi đè, xóa file trên R2.
