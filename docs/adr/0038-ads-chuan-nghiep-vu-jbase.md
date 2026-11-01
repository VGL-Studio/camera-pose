---
id: ADR-0038
title: Nghiệp vụ quảng cáo lấy JBase làm chuẩn; chỗ JBase không có thì giữ PARTNER, chỗ JBase sai thì không theo
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes:                  # bổ sung ADR-0026 (thay chuẩn đối chiếu ở mục "Cách đọc" của docs/ops/ads-business-rules.md); thay luật K-02, K-03, CL-03, S-03, S-12 (phần CACHED_EARLY), B-01 và bảng kích thước 5.7 cho ba biến thể native
superseded_by:
---

# ADR-0038: NGHIỆP VỤ QUẢNG CÁO LẤY JBASE LÀM CHUẨN

## Bối cảnh

ADR-0026 lấy bộ PARTNER của AdsKit làm chuẩn, và khi bản đặc tả với mã nguồn AdsKit khác nhau thì theo bản đặc tả. Ngày 2026-10-29 người phụ trách sản phẩm chọn lấy nghiệp vụ của gói JBase (Unity Android, bản 1.0.0, cùng gốc với AdsKit) làm chuẩn để ra quyết định, áp cho cả luật đã chốt ở `docs/ops/ads-business-rules.md` (yêu cầu `features/ads/requests/2026-10-29-hoc-tu-jbase.md`).

Hai agent đối chiếu từng luật với mã của JBase (C#, Java, bytecode của các `.aar`). Kết quả: JBase không phải bộ nghiệp vụ đầy đủ. Phần lớn luật rơi vào "JBase không có" (resume chưa nối, không có combo inter rồi native, không có app open lần đầu và lần thường, không có cổng theo vị trí, không có gỡ kẹt, không có chặn quảng cáo khi mở màn của hệ thống, không có nút rút đồng ý). Nhiều chỗ JBase làm khác là lỗi: MAX báo "đã đồng ý" viết cứng (`setHasUserConsent(true)`), bỏ qua đồng ý khi mở app lúc không có mạng, native không kiểm đồng ý (đoạn kiểm bị chú thích), APS chạy chế độ thử ở bản phát hành, hiện lại native đã ghi lượt hiển thị, tự ghi `ad_impression` (đếm đôi), tiền tệ luôn USD, mã thử của Google cho native ở bản phát hành, khóa thật trong `strings.xml`, adapter MAX phiên bản `+`, thời gian chờ của MAX không chạy lại sau khi app ra nền.

## Quyết định

1. **Thứ tự phân xử nghiệp vụ quảng cáo:** quyết định riêng của người phụ trách sản phẩm (ghi trong spec `ads`) trước; rồi JBase; chỗ JBase không có hành vi thì theo luật PARTNER đang có.
2. **Không theo JBase ở chỗ trái luật, chính sách hay là lỗi rõ ràng** (danh sách ở mục Bối cảnh). Chỗ đó giữ luật hiện tại và ghi lý do.
3. **Đổi theo JBase, người phụ trách sản phẩm chọn ngày 2026-10-29:**
   - Native toàn màn hình sau inter: đếm ngược mặc định 5 giây, khóa `time_close_native` (tên của JBase, nhận 1 tới 30, giá trị khác dùng 5), vòng đếm ngược và nút đóng 48 dp ở góc trên trái (thay K-02 phần 3 giây, K-03 phần 36 dp). Giữ phần chờ inter rời màn tối đa 2 giây của K-02 (JBase không có; đó là chống kẹt, L-A10).
   - Native thu gọn tự bung lại: `time_expand_collap_ad` mặc định 10 giây (thay 30, CL-03). Giữ nghĩa "0 là không bung lại": JBase coi 0 là bung ngay, đó là lỗi.
   - Kích thước native nhỏ ở đáy, native thu gọn, native medium theo layout của JBase (thay bảng 5.7 cho ba biến thể đó, D-02; chi tiết ở spec `ads`, câu hỏi 48). Màu vẫn theo token.
   - Khởi động sớm bằng đồng ý thật của lần mở trước (`CACHED_EARLY`) luôn bật, không cần cấu hình hay công tắc (thay S-12 cho phần này). Quyền tạm khi lỗi mạng (`ERROR_ALLOWED`) giữ S-04 tới S-12: mặc định tắt.
   - Banner cố định dùng cỡ thích ứng neo đáy thường thay cỡ lớn (B-01).
4. **Giữ luật hiện tại dù JBase khác, người phụ trách sản phẩm chọn ngày 2026-10-29:** banner thu gọn nạp lại mỗi 30 giây (B-03; JBase không tự nạp lại); doanh thu AppsFlyer ghi bằng sự kiện trong app `af_ad_revenue` (mục 8.3, L-A20; JBase dùng connector).
5. **Cho MAX (ADR-0037):** app open của MAX nạp lại 2 giây sau khi đóng như JBase; thử lại khi nạp lỗi theo F-05, không theo nhịp cố định 2 giây của JBase (gửi yêu cầu mỗi 2 giây khi không có quảng cáo là lỗi).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| JBase làm chuẩn, giữ PARTNER chỗ JBase không có, bỏ chỗ JBase sai (đã chọn) | Theo đúng chuẩn người phụ trách sản phẩm chọn; không mất các luật đã sửa lỗi của AdsKit | Hai nguồn chuẩn; mỗi luật phải ghi theo nguồn nào |
| JBase thay hẳn PARTNER, bỏ luật JBase không có | Một nguồn chuẩn | Mất đồng ý, gỡ kẹt, resume, combo, đo lường; lặp lại các lỗi L-A01 tới L-A24; trái GDPR ở chỗ JBase sai |
| Giữ PARTNER (ADR-0026) | Không đổi gì | Người phụ trách sản phẩm chọn JBase làm chuẩn |

## Hệ quả

- **Tích cực:** quảng cáo của dự án mẫu gần với gói JBase mà nhóm dùng; các lỗi tuân thủ của JBase được ghi rõ để báo lại nhóm JGames.
- **Phải chấp nhận:** vẽ lại ba biến thể native trên Figma và ghi lại ảnh chuẩn; khe đáy không còn cố định 80 dp nên câu hỏi 22 và luật D-02, D-04 đổi; đếm ngược native sau inter dài hơn 2 giây (rủi ro chính sách của ADR-0026 về quảng cáo toàn màn hình khó đóng tăng thêm); `CACHED_EARLY` luôn bật bỏ một công tắc tắt khẩn từ xa cho phần này.
- **Việc AI phải tuân thủ:** phân xử nghiệp vụ quảng cáo theo thứ tự quyết định của người phụ trách sản phẩm trong spec `ads`, rồi JBase, rồi luật PARTNER đang có; không theo JBase ở chỗ trái luật, chính sách hay là lỗi, ghi lý do; dẫn chứng JBase bằng đường dẫn và dòng, hoặc lớp và hằng số của bytecode, không theo trí nhớ; không chép code hay khóa của JBase; ghi mỗi luật đổi theo JBase vào `ads-business-rules.md` kèm mã ADR-0038 và nguồn JBase.
