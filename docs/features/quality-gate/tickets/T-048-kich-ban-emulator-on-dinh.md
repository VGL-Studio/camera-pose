---
id: T-048
title: Trình chạy kịch bản emulator chạy ổn định trên CI (sửa sau lần device-session đầu tiên)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là kết quả device-session của RUN-20261024-02 và rà soát độc lập của T-046
scenarios: []
owner: Tuấn
status: approved
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

- Sau `fresh`, ActivityManager hủy hai lần khởi động mới (`start not valid, killing pid ... seq=102,expected=101`), nên màn khởi động đứng yên cho tới hết giờ (T-024, lần 2). Đây là một cuộc đua của hệ thống ngay sau khi xóa dữ liệu; mở lại là cách sửa.
- Hộp thoại đồng ý quảng cáo hiện muộn, sau khi bước `consent decline` đã thấy màn đầu của app và qua. Từ đó `uiautomator` chỉ còn thấy cửa sổ của hộp thoại (T-020, lần 2).
- `selected=true` chỉ đọc nút có chữ. Chip của Compose đặt `selected` ở nút cha có thể chạm, nên không chip nào khớp, kể cả chip đang chọn (T-024, lần 1).
- Kịch bản T-022 chạm thẻ sau `network off` mà không kiểm chi tiết đã mở. Hộp thoại mạng toàn app hiện sau 3 giây và phủ lên thư viện, nên bước chờ ảnh chi tiết lỗi ở cả hai lần.

Ngoài ra còn các điểm "nên sửa" chưa sửa của rà soát độc lập T-046 (báo cáo RUN-20261024-02, Cần người kiểm tra).

Sau ticket này, mỗi lần `device-session` cho kết quả kịch bản có nghĩa: lỗi là lỗi của app hay của kịch bản, có đủ dấu vết để biết bước nào và vì sao.

## Phạm vi

- `scripts/device-scenario.py`:
  - `fresh` mở bằng `am start -W` có hạn riêng, kiểm tiến trình app có chạy, mở lại một lần (trừ khi bộ đệm crash có app);
  - từ chối hộp thoại đồng ý hiện muộn ở mọi bước đọc màn hình, tối đa một lần mỗi `fresh`; hộp thoại hiện lại sau khi đã từ chối là lỗi;
  - `selected=true` đọc cả nút cha;
  - chờ màn hình ổn định sau `device unfold|fold|rotate|night|locale`;
  - ghi dòng `note` (tọa độ chạm, thời điểm) cho mỗi bước;
  - giá trị có dấu cách đặt trong ngoặc kép;
  - `check` chỉ đọc mã chính, không đọc `src/test`;
  - thời gian trả máy về gốc tính vào ngân sách, ngân sách mặc định 900 giây;
  - bắt SIGTERM và giết tiến trình con;
  - từ chối tên tệp có ký tự lạ.
- `scripts/device-session.sh`: `timeout -k 30`, ngân sách mặc định, mục "Ghi chú của trình chạy" trong `summary.md`.
- Kịch bản T-022: mở chi tiết trước khi tắt mạng. Kịch bản T-024: chú thích không kiểm số cột.
- `scripts/tests/fake-adb`, fixture và test trong `scripts/tests/run.sh`; `docs/ops/test-sessions.md` mục 5.

**Không bao gồm:**

- Sửa app. App khóa hướng dọc (`screenOrientation="portrait"`), nên trên emulator API 34 màn mở gập bị đóng khung và chỉ có 2 cột: đây là một phát hiện về sản phẩm, ghi ở phản hồi FB-20261024-01.
- Đổi quy tắc 11 của `plan-check.py` thành lỗi: một dòng sửa riêng, sau khi một `device-session` chạy cả 3 kịch bản không lỗi (plan T-046, mục 6).
- Sửa `.github/` (emulator gập thật, có camera sau).

## Rủi ro sơ bộ

Cao: sửa `scripts/device-session.sh` mà `device-session.yml` chạy (điểm chặn 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận ở mục 2 của plan đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
