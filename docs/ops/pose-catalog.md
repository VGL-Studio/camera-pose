# KHUÔN CATALOG TƯ THẾ

Tài liệu này dành cho người làm nội dung, dev và AI. Nó nói catalog tư thế trên R2 gồm những gì, app đọc nó thế nào, và làm gì khi đổi catalog. Code đọc catalog ở `:core:poses` (`internal/CatalogJson.kt`, `internal/CatalogParser.kt`); kiểm catalog trên R2 ở `scripts/setup-check-app.sh`. Tạo ở plan T-085 (tư thế premium); khuôn có từ plan T-009.

## 1. Đường dẫn và phiên bản

- Đường dẫn dạng `poses/catalog/v<n>/catalog-<sha8>.json`, với `<sha8>` là 8 ký tự đầu của mã SHA-256 nội dung file. Nội dung đổi thì đường dẫn đổi; không bao giờ ghi đè một file đã có (ADR-0015).
- App đọc đường dẫn từ khóa Remote Config `poses_catalog_path`; không đọc được thì dùng catalog mặc định của app (`DEFAULT_CATALOG_PATH` trong `internal/PosesSettings.kt`).
- Thứ tự khi đưa catalog mới lên, như `docs/ops/staging-and-runs.md` mục 2.4: đẩy catalog và mọi file ảnh của nó lên R2 trước, rồi mới đổi khóa `poses_catalog_path`, rồi chạy lại `env-canary` với `setup_only` để `scripts/setup-check.sh` kiểm catalog mới.

## 2. Gốc của catalog

| Trường | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `schema` | số | Phiên bản khuôn của file |
| `minAppSchema` | số | Phiên bản khuôn nhỏ nhất app phải hiểu. App hiện đọc `1`; lớn hơn thì app báo cần cập nhật |
| `datasetVersion` | chuỗi | Tên bản dữ liệu, ví dụ `2026.10.1` |
| `categories` | danh sách | Mỗi nhóm có `id` và `name` theo ngôn ngữ (`{"en": "...", "vi": "..."}`), bắt buộc có `en` |
| `poses` | danh sách | Các tư thế, theo thứ tự hiển thị (mục 3) |

App bỏ qua trường lạ, nên thêm trường tùy chọn không làm bản app cũ hỏng.

## 3. Mỗi tư thế

Tên trường đúng như `PoseJson`:

| Trường | Bắt buộc | Giá trị |
| --- | --- | --- |
| `id` | có | Mã duy nhất trong catalog |
| `category` | có | `id` của một nhóm ở `categories` |
| `subjectType` | có | `single`, `couple`, `group` |
| `gender` | có | `female`, `male`, `mixed`, `any` |
| `framing` | có | `full`, `half`, `close` |
| `tags` | không | Danh sách chuỗi |
| `difficulty` | không | Số, mặc định `1` |
| `starter` | không | `true` hay `false`, mặc định `false`: tư thế của bộ khởi đầu, tải sẵn khi mở app |
| `premium` | không | `true` hay `false`, mặc định `false` (mục 4) |
| `name` | có | Tên theo ngôn ngữ, bắt buộc có `en` |
| `steps` | có | Các bước theo ngôn ngữ, bắt buộc có `en` |
| `mistakes` | không | Lỗi hay gặp theo ngôn ngữ |
| `preview` | có | `small` (ảnh nhỏ) và `large` (ảnh lớn), đường dẫn tương đối có phiên bản hay mã băm |
| `outline` | có | Đường dẫn ảnh nét viền |
| `keypoints` | không | 33 điểm `[x, y, visibility]`, `x` và `y` từ 0 tới 1; sai số điểm thì app coi như không có |

Tư thế thiếu hay sai trường bắt buộc, trùng `id`, nhóm không có, giá trị lạ hay đường dẫn sai thì app bỏ riêng tư thế đó, giữ các tư thế còn lại. Một trường sai kiểu (ví dụ chuỗi ở chỗ cần `true` hay `false`) cũng làm app bỏ tư thế đó.

## 4. Tư thế premium (T-085)

- `premium` là `true` hay `false`; không đặt thì `false`. Giá trị khác kiểu (chuỗi như `"yes"`, số) làm app bỏ cả tư thế đó; `scripts/setup-check.sh` không báo riêng trường hợp này.
- Khoảng 20% tư thế là premium, rải đều các nhóm (spec poses, câu 6).
- Tư thế đầu tiên của catalog (tư thế mặc định của camera ở lần mở đầu) và mọi tư thế `starter` không được là premium. `scripts/setup-check.sh` báo lỗi khi gặp, và phiên tự chạy không bắt đầu tới khi sửa. App vẫn chạy được với catalog sai: nó bỏ `premium` của tư thế khởi đầu và chọn tư thế miễn phí đầu tiên làm tư thế mặc định. Sửa catalog, đẩy bản mới lên R2 với đường dẫn mới.
- Catalog cũ không có trường `premium` thì mọi tư thế miễn phí, kể cả trên bản app mới.

## 5. Tư thế đã mở khóa

- App giữ danh sách tư thế đã mở khóa theo `id`, trên máy, tới khi xóa dữ liệu app hay cài lại; danh sách không phụ thuộc catalog.
- Đổi `id` của một tư thế premium thì người đã mở phải mở lại.
- Bỏ `premium` của một tư thế thì nó miễn phí cho mọi người. Đặt `premium` cho một tư thế đang miễn phí thì người chưa mở thấy nó bị khóa.

## 6. Bật trên production

Bật cùng lúc cờ `feature_premium_poses_enabled` và khóa `is_show_rw_premium_pose` (T-086), sau khi catalog có `premium` đã lên R2 production và đã được kiểm như mục 4 (spec poses, câu 10). `scripts/setup-check.sh` chỉ kiểm catalog của staging: catalog production do người kiểm trước khi đổi khóa `poses_catalog_path` trên production.
