---
id: T-072
title: Ghi log khi không hỏi được danh sách camera, làm chặt test chữ của camera và loading
feature: camera
spec: features/camera/spec.md
scenarios: []                # chỉ thêm log và làm chặt test; KB-4 của camera không đổi hành vi (plan mục 2)
owner: Nhật
status: on-staging
risk: low
size: S
depends_on: []
sprint:
plan: features/camera/plans/T-072-log-camera-test-chu.md
created: 2026-10-28
---

# T-072: GHI LOG KHI KHÔNG HỎI ĐƯỢC DANH SÁCH CAMERA, LÀM CHẶT TEST CHỮ CỦA CAMERA VÀ LOADING

## Mô tả

Gom các ghi chú rà soát còn treo ở phần app (BR-20261028-01, dòng 6). Khi máy không trả lời được nó có những camera nào, app vẫn mở camera như trước nhưng nay ghi một dòng log `warn`, để lỗi giải thích được (ADR-0019, ghi chú rà soát T-064). Hai test chữ được làm chặt: test nhãn "Độ đậm" bắt được cả tính từ tiếng Hindi và chữ viết ở dạng tổ hợp (T-055), test lời báo máy chủ chặn mọi từ chỉ mạng, không chỉ một từ mỗi ngôn ngữ (T-057). Người dùng không thấy gì khác.

## Phạm vi

- `:feature:camera`: `CameraXController.availableLenses` ghi `logger.warn` khi hỏi danh sách camera lỗi, cả khi lấy `ProcessCameraProvider` lỗi; `lensesOf` báo lỗi cho bên gọi qua tham số mới có giá trị mặc định.
- `CameraStringsTest`: gốc tiếng Hindi `पारदर्श` thay cho `पारदर्शिता`; chuẩn hóa NFC nhãn và gốc từ trước khi so.
- `:feature:loading`, `LoadingStringsTest`: danh sách từ chỉ mạng mỗi ngôn ngữ (mạng, kết nối, internet, wi-fi) thay cho một từ.

**Không bao gồm:**

- Test nhánh "dấu của lần chạy chưa vào `logcat.txt`" của trình chạy kịch bản: T-073 (quality-gate, chạm CI nên rủi ro cao, tách riêng để ticket này giữ rủi ro thấp).
- Sửa bản dịch: chữ hiện tại đều qua test mới (đã chạy thử, plan mục 4.1).

## Rủi ro sơ bộ

Không chạm điểm chặn. Hai module feature, phần ở `:feature:loading` chỉ là test. Không đổi giao diện, không đổi ảnh chuẩn.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
