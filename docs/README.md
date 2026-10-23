# BỘ TÀI LIỆU DỰ ÁN

Thư mục `docs/` chứa mọi thứ mà người và AI cần để làm việc với app: yêu cầu, spec, thiết kế, ticket, plan, phản hồi, quyết định kỹ thuật và báo cáo. Tài liệu nằm cùng repo với code, cùng được quản lý phiên bản, và AI luôn đọc tài liệu trước khi code. Nhóm không dùng Confluence, SharePoint hay công cụ tài liệu nào khác.

## Cấu trúc thư mục

Tài liệu được chia **theo feature**. Mỗi feature có một thư mục riêng, khớp với module `:feature:<ten>` trong code, chứa toàn bộ tài liệu của feature đó. Những gì liên quan tới cả app thì nằm ở các thư mục dùng chung.

```
docs/
├── features/
│   └── <feature>/
│       ├── spec.md          spec hiện hành, lịch sử thay đổi ghi trong file
│       ├── requests/        yêu cầu gốc, biên bản họp
│       ├── design/          gói bàn giao thiết kế (Claude Design hoặc Figma), ảnh so sánh
│       ├── tickets/         ticket của feature
│       ├── plans/           plan của từng ticket, kèm nhật ký thực hiện
│       └── feedback/        phản hồi đã phân loại về feature này
├── adr/                     quyết định kỹ thuật, dùng chung cả app
├── inbox/                   yêu cầu và phản hồi chưa rõ thuộc feature nào
├── research/                app clone: báo cáo khảo sát app mẫu và bản đề xuất app (ops/clone-study.md)
├── runs/                    các phiên AI tự chạy: danh sách chạy và báo cáo
├── test-sessions/           báo cáo phân tích các phiên thử trên máy (log thô ở test-sessions/ ngoài docs, không vào git)
├── ops/                     báo cáo vận hành định kỳ, hướng dẫn vận hành
├── metrics/                 số liệu đo được qua các phiên, ví dụ token theo task (ops/token-usage.md)
├── templates/               các mẫu tài liệu
├── borrowed-assets.md       app clone: tài sản mượn tạm từ app mẫu, phải thay hết trước khi phát hành
├── foundation.md            quy tắc nền tảng, sinh từ các ADR bởi scripts/foundation.py, không sửa tay
├── STATUS.md                trang tiến độ, sinh bởi scripts/status.py; không có trong git, bản đọc được trên nhánh status (status-page.yml)
├── auto-run.md              quy trình phiên AI tự chạy
├── lessons.md               sổ bài học, đầu vào của retro
├── risk-scoring.md          thang điểm rủi ro và điều kiện dừng
├── CLAUDE.md                hướng dẫn AI làm việc với bộ tài liệu
└── README.md
```

| Tài liệu | Ai tạo | Ai xác nhận |
| --- | --- | --- |
| Báo cáo khảo sát app mẫu, đề xuất app (app clone) | AI theo yêu cầu của dev | Dev |
| Yêu cầu | Người phụ trách sản phẩm, chủ feature | Không cần |
| Spec | AI sinh từ yêu cầu | Người phụ trách sản phẩm |
| Thiết kế | Chủ feature cùng Claude Design hoặc Figma | Người phụ trách sản phẩm |
| Ticket | AI sinh từ spec | Chủ feature |
| Plan | AI sinh | Chủ feature, duyệt chéo nếu rủi ro cao |
| Phản hồi | Bất kỳ ai trong nhóm | Chủ feature phân loại |
| ADR | Dev, hoặc AI theo yêu cầu của dev | Cả hai dev |
| Phiên tự chạy | Danh sách chạy do AI lập sau họp, báo cáo do AI viết | Dev kích hoạt phiên duyệt danh sách chạy |
| Báo cáo vận hành | AI tổng hợp | Người phụ trách sản phẩm |

## Vòng đời của một yêu cầu

0. **Khảo sát app mẫu (chỉ app clone).** Dev đưa thông tin chung và danh sách app mẫu đã cài trên máy. AI dùng thử từng app, đọc APK, tìm trên web và Play, rồi viết bản đề xuất app tốt hơn app mẫu: tên, tính năng, luồng, màn hình, bài học và cải tiến. Dev duyệt đề xuất, chốt tên và tên gói, tạo repo app. AI dựng 2 đến 3 hướng thiết kế trên Figma, dev chọn một; AI dựng toàn bộ màn hình, dev làm mịn, AI quét lại. Xong thì AI viết yêu cầu và spec, đi tiếp từ bước 1. Chi tiết ở [ops/clone-study.md](ops/clone-study.md) và [ops/design-workflow.md](ops/design-workflow.md), mục 8 (ADR-0021).
1. **Yêu cầu.** Người phụ trách sản phẩm hoặc chủ feature ghi yêu cầu vào `features/<feature>/requests/`, viết tự nhiên, không bắt buộc theo mẫu. Có thể dán biên bản họp, hoặc dựa vào [mẫu gợi ý](templates/request.md). Chưa rõ thuộc feature nào, hoặc là feature mới chưa có tên, thì ghi vào `inbox/`.
2. **Spec.** AI đọc yêu cầu, sinh hoặc cập nhật `features/<feature>/spec.md` theo [mẫu spec](templates/spec.md). Với feature mới, AI tạo thư mục feature. Chỗ nào chưa rõ, AI ghi vào mục câu hỏi còn mở, không tự đoán.
3. **Xác nhận spec.** Người phụ trách sản phẩm đọc lại, trả lời câu hỏi, chuyển trạng thái spec sang `confirmed`.
4. **Thiết kế.** Chủ feature dựng bản mẫu trên Claude Design hoặc Figma (tùy dự án), người phụ trách sản phẩm duyệt. Gói bàn giao (ảnh khung chuẩn, bảng chữ, ánh xạ thành phần) lưu vào `features/<feature>/design/` theo [mẫu thiết kế](templates/design.md) và [quy trình thiết kế](ops/design-workflow.md). Giao diện làm ra được đối chiếu với thiết kế bằng ảnh so sánh (ADR-0020).
5. **Ticket.** AI tách spec thành các ticket trong `features/<feature>/tickets/` theo [mẫu ticket](templates/ticket.md). Ticket chỉ chuyển sang `ready` khi spec đã xác nhận và thiết kế (nếu cần) đã duyệt.
6. **Plan (ngày 1).** AI sinh plan cho từng ticket được chọn vào sprint, theo [mẫu plan](templates/plan.md), kèm điểm rủi ro.
7. **Họp đánh giá rủi ro.** Nhóm xem plan, đánh giá ảnh hưởng chéo, duyệt hoặc dừng. AI sắp thứ tự chạy các plan đã duyệt và lập danh sách chạy trong `runs/`, rồi kiểm tra cả danh sách bằng script và một lần rà soát độc lập: mâu thuẫn giữa spec và plan, giữa các plan, test chưa đủ tốt ([ops/pre-run-review.md](ops/pre-run-review.md)). Điểm chặn được xử lý trước khi dev duyệt danh sách.
8. **Phiên AI tự chạy.** Khi dev đã duyệt xong và kích hoạt, AI thực hiện lần lượt từng plan theo [quy trình phiên tự chạy](auto-run.md): viết test trước, viết code, rà soát độc lập, qua cổng chất lượng thì merge vào nhánh chính. Cuối phiên, AI build bản staging và viết báo cáo phiên.
9. **Kiểm tra và phản hồi (ngày 2).** Dev đọc báo cáo phiên, kiểm tra bản staging, ghi phản hồi theo [mẫu phản hồi](templates/feedback.md). AI cập nhật plan theo phản hồi, plan sửa lại đi qua họp đánh giá rủi ro trước phiên tự chạy tiếp theo.
10. **Nghiệm thu (ngày 3).** Dev nghiệm thu chéo, người phụ trách sản phẩm nghiệm thu cuối. Ticket đạt chuyển sang `accepted`.
11. **Phát hành và vận hành.** Người phụ trách sản phẩm quyết định phát hành. AI tổng hợp định kỳ số liệu từ store thành [báo cáo vận hành](templates/ops-report.md) trong `ops/`. Vấn đề cần xử lý được tách thành phản hồi, ghi vào feature tương ứng hoặc vào `inbox/`.

Khi một quyết định kỹ thuật có ảnh hưởng lâu dài được đưa ra ở bất kỳ bước nào, ghi lại thành ADR theo [mẫu ADR](templates/adr.md). Các quyết định nền tảng của dự án mẫu (ADR-0001 trở đi, `origin: template`) được mọi app tạo từ dự án mẫu kế thừa: với app, đó là quy tắc phải theo. Quy tắc của chúng gom ở [foundation.md](foundation.md); ADR giữ lý do, để ai muốn đổi quy tắc biết vì sao nó có. ADR riêng của app đánh số từ ADR-1001.

## Quy ước đặt tên

| Loại | Mã | Đường dẫn | Ví dụ |
| --- | --- | --- | --- |
| Thư mục feature | tên feature | `features/<feature>/` | `features/daily-reminder/` |
| Yêu cầu | không có | `features/<feature>/requests/YYYY-MM-DD-<tu-khoa>.md` | `.../requests/2026-10-20-nhac-nho-hang-ngay.md` |
| Spec | tên feature | `features/<feature>/spec.md` | `features/daily-reminder/spec.md` |
| Thiết kế | tên feature | `features/<feature>/design/README.md` | `features/daily-reminder/design/README.md` |
| Ticket | `T-NNN` | `features/<feature>/tickets/T-NNN-<tu-khoa>.md` | `.../tickets/T-012-reminder-settings.md` |
| Plan | trùng mã ticket | `features/<feature>/plans/T-NNN-<tu-khoa>.md` | `.../plans/T-012-reminder-settings.md` |
| Phản hồi | `FB-YYYYMMDD-NN` | `features/<feature>/feedback/FB-YYYYMMDD-NN-<tu-khoa>.md` | `.../feedback/FB-20261021-01-reminder-time.md` |
| ADR | `ADR-NNNN` (dự án mẫu dưới 1000, app từ 1001) | `adr/NNNN-<tu-khoa>.md` | `adr/1001-local-notifications.md` |
| Phiên tự chạy | `RUN-YYYYMMDD-NN` | `runs/RUN-YYYYMMDD-NN.md` | `runs/RUN-20261020-01.md` |
| Báo cáo vận hành | `OPS-YYYY-MM-DD` | `ops/OPS-YYYY-MM-DD.md` | `ops/OPS-2026-11-02.md` |
| Khảo sát app mẫu | tên gói app mẫu | `research/<package>.md` | `research/com.example.posecamera.md` |
| Đề xuất app | không có | `research/proposal.md` | `research/proposal.md` |
| Phiên thử trên máy | `TS-YYYYMMDD-NN` | `test-sessions/TS-YYYYMMDD-NN.md` | `test-sessions/TS-20261018-01.md` |

- Tên feature viết thường, nối bằng dấu gạch ngang, trùng với tên module `:feature:<ten>`.
- Từ khóa trong tên file viết không dấu, ngắn gọn.
- Nhánh của task đặt tên theo dạng `claude/T-NNN-<tu-khoa>`.
- **Mã ticket đánh số chung cho cả app**, không đánh lại cho từng feature, để "T-012" luôn chỉ đúng một task.
- Mã số tăng dần, không dùng lại mã đã cấp, kể cả khi ticket bị hủy.
- Phản hồi trong `inbox/` được chuyển vào thư mục feature khi phân loại xong. Phản hồi liên quan cả app (ví dụ tốc độ khởi động) ở lại `inbox/` và được liên kết tới ticket xử lý.

## Trạng thái

Trạng thái được ghi ở trường `status` trong phần đầu (frontmatter) của mỗi file, để người và AI cùng đọc được.

| Loại | Các trạng thái |
| --- | --- |
| Spec | `draft` → `confirmed` → `changed` (khi sửa sau xác nhận, cần xác nhận lại) |
| Thiết kế | `draft` → `approved` |
| Ticket | `backlog` → `ready` → `planned` → `approved` → `on-staging` → `accepted` → `released`, hoặc `stopped`, `cancelled` |
| Plan | `draft` → `approved` hoặc `stopped` → `done` |
| Phản hồi | `new` → `triaged` → `planned` → `fixed`, hoặc `wontfix` |
| ADR | `proposed` → `accepted`, sau này có thể `superseded` |
| Khảo sát app mẫu | `draft` → `reviewed` |
| Đề xuất app | `draft` → `approved` |
| Phiên tự chạy | `draft` → `approved` → `running` → `finished` |

## Nguyên tắc chung

- **Tài liệu là nguồn sự thật.** Code làm khác tài liệu thì một trong hai đang sai, phải sửa cho khớp ngay, không để lệch.
- **Không sửa lịch sử.** Spec đã xác nhận, plan đã duyệt, ADR đã chấp nhận không bị viết đè. Thay đổi được ghi thành mục mới, hoặc tạo bản thay thế có liên kết tới bản cũ.
- **Viết cho người đọc nhanh.** Spec viết bằng ngôn ngữ nghiệp vụ, tránh thuật ngữ kỹ thuật, để người phụ trách sản phẩm đọc và xác nhận nhanh. Plan được phép dùng thuật ngữ kỹ thuật.
- **Tiếng Việt tự nhiên, dễ hiểu.** Thuật ngữ kỹ thuật thông dụng (module, test, build, staging) giữ nguyên tiếng Anh.
