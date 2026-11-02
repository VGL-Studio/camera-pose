---
id: FB-20261031-01
title: Ba kịch bản emulator chờ camera ngay sau "Chụp với tư thế này" nhưng interstitial inter_pose_detail hiện trước
reporter: AI (RUN-20261031-01, device-session)
source: staging
build: nhánh chính sau PR #257 (device-session lần chạy 41210750330, phiên TS-20261031-01)
feature: poses
ticket: T-089                # nguồn: T-079 (PR #257)
severity: nang
status: planned
created: 2026-10-31
---

# FB-20261031-01: BA KỊCH BẢN EMULATOR CHỜ CAMERA NGAY SAU "CHỤP VỚI TƯ THẾ NÀY" NHƯNG INTERSTITIAL HIỆN TRƯỚC

## Điều đã xảy ra

`device-session` cuối RUN-20261031-01 báo ba kịch bản theo plan lỗi ở cùng một bước: sau khi bấm "Chụp với tư thế này", lệnh `wait id=camera_screen 15` không thấy màn camera.

| Tệp | Bước lỗi |
| --- | --- |
| `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` | 13: `wait id=camera_screen 15` |
| `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` | 23: `wait id=camera_screen 15` |
| `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` | 32: `wait id=camera_screen 15` |

Ảnh lúc lỗi (`FB-20261031-01-1.png`) là interstitial thử của AdMob ("This is an interstitial test ad"). Log của app ngay sau lần bấm:

```text
14:51:40.102 I VGL.Poses: Shoot with pose
14:51:40.105 I VGL.Ads : Interstitial shown: inter_pose_detail
14:51:40.371 I VGL.Ads : Interstitial displays since install: 1
```

Đây là hành vi mới của T-079 lượt 2 (PR #257): bản staging bật `is_show_inter_pose_detail` (spec ads câu 11), nên interstitial hiện trước camera như KB-4. App không lỗi: không có crash, ANR hay log lỗi `E VGL.*`. Ba kịch bản viết trước T-079, chờ camera ngay sau lần bấm, nên chờ hết 15 giây trong lúc quảng cáo còn trên màn. Plan T-079 ghi "không thêm tệp kịch bản; `device-session` cuối phiên chạy các tệp có sẵn" và không thấy ba tệp này đi qua vị trí mới.

Hệ quả: từ nay mỗi `device-session` sẽ báo ba dòng lỗi này, và phần sau bước lỗi (chụp ở T-058; lưu, bỏ lưu và camera ở T-022; camera sau gập mở ở T-024) không còn được kiểm trên emulator.

Mức `nang` theo `docs/auto-run.md` mục 4, bước 1 (dòng `lỗi` của kịch bản theo plan); nguyên nhân là kịch bản cũ, không phải app sai spec.

## Các bước làm lại

1. Bản staging từ nhánh chính sau PR #257, máy mới cài, đồng ý hay từ chối quảng cáo.
2. Mở thư viện tư thế, chọn một tư thế, bấm "Chụp với tư thế này".
3. Interstitial thử hiện; camera chỉ mở sau khi đóng quảng cáo.

## Mong đợi

Kịch bản kiểm đúng luồng KB-4: sau lần bấm, chờ interstitial (nếu có) rồi đóng nó, sau đó chờ `camera_screen`. Đề xuất cho ticket sửa: thêm vào ba tệp, ngay sau lần bấm, một bước đóng quảng cáo có điều kiện (ví dụ chạm nút đóng của `AdActivity` khi có, hay `back`), rồi giữ `wait id=camera_screen`. Hay tắt `is_show_inter_pose_detail` riêng cho phiên emulator, nếu trình chạy kịch bản có cách đặt giá trị Remote Config cục bộ. Interstitial thật vẫn kiểm thủ công theo `docs/ops/manual-test.md` mục 11.1 bước 7, 8.

## Ảnh chụp, video

`FB-20261031-01-1.png`: màn lúc bước 13 của T-058 lỗi.

## Phân loại

- **Loại:** lỗi của kịch bản emulator, không phải lỗi của app. App đúng spec poses KB-4: camera mở sau khi đóng interstitial `inter_pose_detail` (T-079). Ba tệp viết trước T-079 nên không chờ quảng cáo.
- **Xử lý:** ticket T-089, sửa ở phiên tự chạy kế tiếp. Nhật chọn ngày 2026-11-02 (`/triage-feedback`). Vẫn lỗi ở `device-session` lần chạy 41334189241 (TS-20261101-02, sau RUN-20261101-01), cả ba tệp, cùng bước.
- **Lý do:** log `VGL.Poses: Shoot with pose` rồi `VGL.Ads: Interstitial shown: inter_pose_detail` cách nhau 5 ms; `fail.xml` là quảng cáo thử, nút đóng là `android.widget.Button` chữ `Close`; không crash, ANR hay log lỗi `E VGL.*`.
- **Liên kết:** ticket và plan T-089 (`docs/features/poses/plans/T-089-kich-ban-emulator-qua-interstitial.md`).
