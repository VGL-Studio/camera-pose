---
id: TS-YYYYMMDD-NN
source: may-that            # may-that | emulator
tester: <người thử, hoặc "kịch bản chuẩn" với emulator>
build: <versionName của bản đã thử, trong session.md>
device: <hãng, dòng máy, Android, API>
features: [<feature đã thử>]
analysed_by: AI
analysed_at: YYYY-MM-DD
---

# PHIÊN THỬ TS-YYYYMMDD-NN: <MỤC ĐÍCH NGẮN>

<!--
Quy trình: docs/ops/test-sessions.md. AI viết file này trong phiên có người theo dõi, từ thư mục
test-sessions/TS-YYYYMMDD-NN/ (logcat.txt, app-log.txt, summary.md, notes.md, gfxinfo.txt, meminfo.txt, ảnh).
Thư mục đó không vào git; file này thì có, ở docs/test-sessions/.
Trích log ngắn, chỉ dòng làm bằng chứng. Bỏ mã định danh máy, tài khoản, đường dẫn có tên người, dữ liệu người dùng.
-->

## 1. Tóm tắt

<Hai đến bốn câu: thử gì, bao lâu, phát hiện quan trọng nhất, bao nhiêu phản hồi được tạo.>

| Tín hiệu (summary.md) | Số lần |
| --- | --- |
| Crash | |
| ANR | |
| Log lỗi của app | |
| Log cảnh báo của app | |

## 2. Diễn biến

<Hành trình của người thử theo thời gian, dựng từ log (dấu VGL.Session, màn hình, trạng thái) đối chiếu với notes.md. Mỗi dòng: giờ, việc, kết quả. Đánh dấu chỗ khác kịch bản trong spec.>

| Giờ | Việc | Kết quả theo log | Đúng kịch bản? |
| --- | --- | --- | --- |
| | | | |

## 3. Phát hiện

<!-- Mỗi phát hiện một mục. Mức: chặn (crash, ANR, mất dữ liệu, sai spec làm không dùng được), nặng, nhẹ, góp ý. -->

### P-1. <Tên ngắn>

- **Mức:** chặn / nặng / nhẹ / góp ý
- **Loại:** crash / ANR / lỗi đã bắt / chậm / giật / bộ nhớ / lệch kịch bản / khó dùng / cải tiến
- **Bằng chứng:**

  ```text
  <vài dòng log, có giờ>
  ```

- **Nguyên nhân có thể:** <đoạn code, file và dòng nếu tìm được; ghi rõ khi chỉ là giả thuyết>
- **Kịch bản liên quan:** <KB-n của spec, hoặc "Không có">
- **Phản hồi:** <FB-YYYYMMDD-NN đã tạo ở features/<feature>/feedback/, trạng thái new>

## 4. Đề xuất cải tiến

<Những điều không sai spec nhưng làm app tốt hơn, có bằng chứng từ log: thao tác lặp lại, chờ lâu, màn hình hay bị thoát ra. Mỗi đề xuất thành một phản hồi loại góp ý.>

## 5. Log còn thiếu

<Chỗ log không đủ để kết luận: sự kiện nào nên ghi thêm, trường nào nên có. Đầu vào cho quy tắc ghi log (CLAUDE.md, ADR-0019).>

## 6. Giới hạn của phiên này

<Ví dụ: emulator không có GPU nên số liệu khung hình chỉ để so; chưa thử chế độ tối; mạng chập chờn không giả lập được.>
