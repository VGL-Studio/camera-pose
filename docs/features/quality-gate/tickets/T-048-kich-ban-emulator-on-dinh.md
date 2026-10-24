---
id: T-048
title: Trình chạy kịch bản emulator chạy ổn định trên CI (sửa sau lần device-session đầu tiên)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là kết quả device-session của RUN-20261024-02 và rà soát độc lập của T-046
scenarios: []
owner: Tuấn
status: planned
risk: high
size: M
depends_on: [T-046]
sprint:
plan: features/quality-gate/plans/T-048-kich-ban-emulator-on-dinh.md
created: 2026-10-24
---

# T-048: TRÌNH CHẠY KỊCH BẢN EMULATOR CHẠY ỔN ĐỊNH TRÊN CI

## Mô tả

Lần `device-session` đầu tiên có trình chạy kịch bản của T-046 (RUN-20261024-02, lần chạy 40116129967 và 40117751068) cho cả 3 tệp kịch bản lỗi hay bỏ qua. Xem ảnh, dump và log của hai lần chạy thì thấy phần lớn lỗi nằm ở trình chạy và ở kịch bản, không phải ở app:

- `fresh` mở app khi Android còn đang dừng tiến trình cũ sau `pm clear`. Tiến trình mới bị hệ thống giết (`start not valid, killing pid`), nên màn khởi động đứng yên cho tới hết giờ (T-024, lần 2).
- Hộp thoại đồng ý quảng cáo hiện muộn, sau khi bước `consent decline` đã thấy màn đầu của app và qua. Từ đó `uiautomator` chỉ còn thấy cửa sổ của hộp thoại (T-020, lần 2).
- `selected=true` chỉ đọc nút có chữ. Chip của Compose đặt `selected` ở nút cha có thể chạm, nên không chip nào khớp, kể cả chip đang chọn (T-024, lần 1).
- Kịch bản T-022 chạm thẻ sau `network off` mà không kiểm chi tiết đã mở. Hộp thoại mạng toàn app hiện sau 3 giây và phủ lên thư viện, nên bước chờ ảnh chi tiết lỗi ở cả hai lần.

Ngoài ra còn các điểm "nên sửa" chưa sửa của rà soát độc lập T-046 (báo cáo RUN-20261024-02, Cần người kiểm tra).

Sau ticket này, mỗi lần `device-session` cho kết quả kịch bản có nghĩa: lỗi là lỗi của app hay của kịch bản, có đủ dấu vết để biết bước nào và vì sao.

## Phạm vi

- `scripts/device-scenario.py`:
  - `fresh` chờ `pm clear` xong và mở bằng `am start -W`, kiểm tiến trình app có chạy, mở lại một lần nếu cần;
  - từ chối hộp thoại đồng ý hiện muộn ở mọi bước đọc màn hình;
  - `selected=true` đọc cả nút cha;
  - chờ màn hình ổn định sau `device unfold|fold|rotate`;
  - ghi dòng `note` (tọa độ chạm, thời điểm) cho mỗi bước;
  - giá trị có dấu cách đặt trong ngoặc kép;
  - `check` chỉ đọc mã chính, không đọc `src/test`;
  - thời gian trả máy về gốc tính vào ngân sách;
  - bắt SIGTERM và giết tiến trình con;
  - từ chối tên tệp có ký tự lạ.
- `scripts/device-session.sh`: lệnh `timeout` gửi SIGKILL sau SIGTERM.
- Kịch bản T-022: mở chi tiết trước khi tắt mạng.
- `scripts/tests/fake-adb`, fixture và test trong `scripts/tests/run.sh`; `docs/ops/test-sessions.md` mục 5.

**Không bao gồm:**

- Sửa app. App khóa hướng dọc (`screenOrientation="portrait"`), nên trên emulator API 34 màn mở gập bị đóng khung và chỉ có 2 cột: đây là một phát hiện về sản phẩm (plan, mục 8), xử lý ở ticket hay phản hồi riêng.
- Đổi quy tắc 11 của `plan-check.py` thành lỗi: một dòng sửa riêng, sau khi một `device-session` chạy cả 3 kịch bản không lỗi (plan T-046, mục 6).
- Sửa `.github/` (emulator gập thật, có camera sau).

## Rủi ro sơ bộ

Cao: sửa `scripts/device-session.sh` mà `device-session.yml` chạy (điểm chặn 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận ở mục 2 của plan đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
