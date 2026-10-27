# ĐƯA THAY ĐỔI CỦA DỰ ÁN MẪU VỀ APP

Chiều ngược của [from-app.md](from-app.md). App nhận sửa lỗi quy trình, script, CI và module core mới của dự án mẫu qua một PR đồng bộ, dựng bằng `scripts/template-sync.sh` của dự án mẫu (ADR-0030).

Người làm: AI trong phiên có người theo dõi, trên máy có cả repo dự án mẫu và repo app, theo yêu cầu của người phụ trách. Phiên tự chạy của app không làm việc này: nó không đọc được repo mẫu (L-101).

## 1. Khi nào đồng bộ

- App cần một phần mới của dự án mẫu, ví dụ sửa lỗi của `merge-if-green.sh` hay bộ quảng cáo mới.
- Sau mỗi đợt lớn của dự án mẫu, ít nhất hai tuần một lần, để mỗi đợt nhỏ và ít xung đột.
- Chỉ giữa hai phiên tự chạy của app: không có phiên nào đang chạy, và đợt đồng bộ merge xong trước phiên sau.

## 2. Phân loại file

| Loại | File | Khi xung đột |
| --- | --- | --- |
| Không bao giờ đồng bộ | `scripts/new-app.sh`, `scripts/template-sync.sh`, `docs/ops/new-app.md`, `.github/ISSUE_TEMPLATE/`, `docs/lessons.md`, `docs/runs/`, `docs/features/`, `docs/metrics/`, `docs/inbox/`, `docs/test-sessions/`, `docs/design-system/tokens.json`, `docs/ops/required-assets.txt`, trang sinh ra (`docs/foundation.md`, `docs/STATUS.md`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`) | Không xảy ra: script bỏ các file này ở cả hai phía. Trang sinh ra được sinh lại trong app |
| Của dự án mẫu, app không sửa | `scripts/`, `.github/`, `build-logic/`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/`, `docs/ops/`, ADR `origin: template`, `:core:logging`, `:core:network`, `:core:assets`, `:core:ads` trừ `AdPlacement.kt` | Lấy bản mẫu (`git checkout --theirs -- <file>`), sau khi chắc phần app sửa đã về dự án mẫu (mục 4, bước 1); rồi đặt lại giá trị riêng của app (mục 6) |
| Cả hai cùng sửa | `CLAUDE.md`, `docs/CLAUDE.md`, `README.md`, `.gitignore`, `:app`, `:core:designsystem`, `:core:config`, `:core:analytics`, `AdPlacement.kt`, `feature/sample`, `feature/loading`, cấu hình build ở gốc | Gộp tay: lấy phần mới của mẫu, giữ phần riêng của app (module, vị trí quảng cáo, cờ, màu) |
| Chỉ của app | Feature của app, module core riêng của app, ADR từ 1001 | Không xảy ra: dự án mẫu không có các file này |

Script in danh sách xung đột theo hai nhóm giữa: "Phần của dự án mẫu" và "Phần app cũng sửa".

## 3. File mốc `docs/template-sync.txt` của app

```
base=<commit đầy đủ của dự án mẫu mà app đang theo>
package=<tên gói của app>
hold=<đường dẫn như trong dự án mẫu> <commit của dự án mẫu>
```

- `new-app.sh` ghi `base=` và `package=` cho app mới.
- App tạo trước ADR-0030: tạo tay trong PR đồng bộ đầu tiên. `base=` là commit ghi trong commit đầu tiên của app ("Khởi tạo ... từ dự án mẫu (<commit>)"). Commit đó nằm trên một nhánh đã merge kiểu squash thì script tự tìm commit của nhánh chính có cùng nội dung.
- `hold=`: phần app tạm giữ ở mốc cũ, mỗi dòng một đường dẫn (ví dụ `hold=core/ads <commit>`). Đợt thường bỏ qua phần đó. `--take <đường dẫn>` đưa nó lên đúng mốc `base=` rồi xóa dòng.

Chỉ PR đồng bộ được sửa `base=` và `hold=`. Script tự sửa, trong cùng commit với thay đổi.

## 4. Các bước

1. **Đưa phần chung của app về trước.** Xem bài học phạm vi `chung` của app chưa có issue `from-app`, và commit của app ở file thuộc loại "của dự án mẫu" kể từ đợt trước (`git log <ngày đợt trước>.. -- scripts .github docs/ops docs/auto-run.md`). Phần nào chưa về dự án mẫu thì mở issue `from-app` (`from-app.md`, mục 2). Gấp thì ghi lại để giữ khi gộp (bước 4).
2. **Cập nhật hai repo.** Repo app sạch, ở nhánh chính mới nhất. Dự án mẫu `git fetch`.
3. **Chạy script** từ thư mục dự án mẫu:

   ```bash
   scripts/template-sync.sh --app ../<app> [--to <commit>]   # mặc định origin/main
   ```

   Script tạo nhánh `template-sync/<commit>` trong app. Không xung đột thì nó commit luôn. Có xung đột thì nó in danh sách theo mục 2 và dừng giữa chừng.
4. **Gộp xung đột** theo cột cuối của bảng ở mục 2. Phần của app ghi lại ở bước 1 thì áp lại sau khi lấy bản mẫu (`git show <commit của app> -- <file> | git apply -3`). Xong thì:
   - chạy `python3 scripts/foundation.py`;
   - `git add` từng file;
   - chạy `git commit` (lời commit đã soạn sẵn).
5. **Kiểm** trong app:
   - `scripts/tests/run.sh`;
   - `scripts/check.sh` khi đợt có code;
   - file test mới của mẫu nhắc tới file chỉ có ở mẫu (ví dụ thẻ của `MainActivity`): sửa app cho khớp, hoặc ghi lại cho người quyết.
6. **Đẩy nhánh, mở PR** trong repo app.
   - Mô tả PR ghi khoảng commit của dự án mẫu và cách gộp từng file xung đột (lấy bản mẫu, gộp tay giữ gì, áp lại gì).
   - Ghi thêm phần riêng của app bị thay theo quyết định của dự án mẫu (ví dụ ADR của app bị ADR mới của mẫu thay).
   - Người phụ trách app duyệt. Merge bằng `scripts/merge-if-green.sh` của app.

## 5. Phần lớn: giữ lại rồi đưa vào bằng plan

Phần thay gần hết một module, cần SDK hay quyền mới, hay đổi ảnh chuẩn screenshot (ví dụ bộ quảng cáo theo AdsKit) thì không gộp trong một đợt thường:

1. Đợt thường: thêm `hold=<đường dẫn> <base hiện tại>` vào `docs/template-sync.txt` trước khi chạy.
2. Soạn ticket và plan rủi ro cao trong app. Spec của app phải được duyệt cho SDK, quyền và vị trí mới.
3. Người (hay AI có người theo dõi) chạy `scripts/template-sync.sh --app ../<app> --take <đường dẫn>`, gộp xung đột, đẩy nhánh `template-sync/take-...`.
4. Plan ghi nhánh đó là điểm bắt đầu. Phần còn lại (khai báo lại vị trí của app, nối vào `:app`, test, ảnh chuẩn) phiên tự chạy làm như task thường.

## 6. Giá trị riêng của app nằm trong file của dự án mẫu

Lấy bản mẫu thì các giá trị này về giá trị của mẫu. Đặt lại sau mỗi đợt, cho tới khi dự án mẫu chuyển chúng ra chỗ cấu hình riêng:

| Giá trị | File | Ở dự án mẫu |
| --- | --- | --- |
| Hạn chót cả lượt kịch bản emulator, `DEVICE_SCENARIOS_BUDGET` | `scripts/device-scenario.py`, `scripts/device-session.sh`, `docs/ops/test-sessions.md` | 600 giây; camera-pose dùng 1300 |

Thấy thêm giá trị như vậy thì thêm vào bảng này qua issue `from-app`.

## 7. Lưu ý

- Tài liệu lấy từ dự án mẫu dẫn số bài học của dự án mẫu (`L-0NN`), khác số trong sổ bài học của app. Tra ở `docs/lessons.md` của repo dự án mẫu.
- Bảng ở mục 2 khớp với `NEVER_SYNCED` và `TEMPLATE_OWNED` trong `scripts/template-sync.sh`. Đổi một chỗ thì đổi cả hai.
