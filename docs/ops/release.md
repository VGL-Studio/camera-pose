# PHÁT HÀNH BẢN PRODUCTION

Tài liệu này dành cho người phụ trách phát hành. Bản production được build trên máy của người đó bằng `scripts/release.sh`, rồi người tải lên Google Play bằng tay (ADR-0009, ADR-0017). Không có bước nào chạy trên GitHub, AI không tham gia.

## 1. Chuẩn bị một lần cho mỗi app

### 1.1. Khóa upload

Google Play giữ khóa ký app (Play App Signing, ADR-0007). Nhóm chỉ giữ khóa upload, dùng để ký bản tải lên. Tạo một lần:

```sh
keytool -genkeypair -keystore upload.jks -storetype PKCS12 \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=<Tên app>"
```

Cất `upload.jks`, tên khóa (`upload`) và mật khẩu vào trình quản lý mật khẩu của nhóm. Mất khóa upload thì phải xin Google đặt lại khóa trong Play Console, mất vài ngày.

### 1.2. Các giá trị production

Cất vào trình quản lý mật khẩu, mỗi app một mục:

| Giá trị | Lấy ở đâu |
| --- | --- |
| `google-services.json` của production | Firebase, project production, app Android có package bằng `applicationId` |
| Mã ứng dụng AdMob, dạng `ca-app-pub-<số>~<số>` | AdMob, mục App settings |
| Mã quảng cáo của từng vị trí, dạng `ca-app-pub-<số>/<số>` | AdMob, mục Ad units. Mỗi vị trí trong `AdPlacement` một mã |
| Địa chỉ gốc của file trên R2 production, kết thúc bằng `/` | Cloudflare R2, tên miền công khai của bucket production |

### 1.3. Google Play

Tạo app trong Play Console, bật Play App Signing (mặc định với app mới), khai báo Data safety (ADR-0012) và đối tượng người dùng (ADR-0005).

### 1.4. `app-ads.txt`

Làm một lần cho mỗi nhà phát triển. File `app-ads.txt` đặt ở gốc tên miền website ghi trên trang Google Play của app (ví dụ `https://<tên miền>/app-ads.txt`). File nằm trên website, không vào repo.

- Dòng của AdMob: lấy từ AdMob, mục **Apps > app-ads.txt**.
- Dòng của AppLovin: lấy từ tài khoản AppLovin, theo hướng dẫn của AppLovin (mediation, [ads-guide.md](ads-guide.md) mục 7).
- Dòng của tám mạng còn lại trong nhóm mediation (Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads): lấy từ bảng điều khiển của từng mạng ([ads-guide.md](ads-guide.md) mục 7).

Thiếu dòng của một mạng thì doanh thu của mạng đó giảm đáng kể.

## 2. Mỗi lần phát hành

### 2.1. Trước khi chạy script

1. Tăng `versionCode` và `versionName` trong `defaultConfig` của `app/build.gradle.kts` bằng một PR thường, merge bằng `scripts/merge-if-green.sh`. Script từ chối `versionCode` đã phát hành.
2. Chờ `main-guard.yml` của commit đó xanh.
3. Bật khóa quảng cáo trên Remote Config của project production cho các định dạng và vị trí muốn hiện quảng cáo (tên khóa ở `docs/ops/ads-business-rules.md`, mục 10; mặc định tắt, ADR-0014, ADR-0026).

### 2.2. Thư mục phát hành

Chép các giá trị ở mục 1.2 từ trình quản lý mật khẩu vào thư mục `~/.vgl-release/<applicationId>/`, nằm ngoài repo:

```text
~/.vgl-release/<applicationId>/
├── google-services.json
├── upload.jks
└── release.properties
```

Nội dung `release.properties`:

```properties
admobAppId=ca-app-pub-<số>~<số>
adUnits=<khóa vị trí>=ca-app-pub-<số>/<số>;<khóa vị trí>=ca-app-pub-<số>/<số>
assetsBaseUrl=https://<địa chỉ production trên R2>/
uploadKeyAlias=upload
appsFlyerDevKey=<khóa dev AppsFlyer>
```

Khóa vị trí là giá trị `key` trong `AdPlacement`, ví dụ `sample_native_small`. Vị trí chưa có mã thật thì bỏ khỏi `adUnits`: script tự dùng mã quảng cáo thử của Google theo định dạng của vị trí, in tên các vị trí đó ("không có doanh thu") để người phát hành biết (anh Nhật chốt 2026-10-26: phát hành trước khi có đủ mã thật). Vị trí dùng mã thử hiện quảng cáo thử, không có doanh thu; thêm mã thật vào `adUnits` ở bản sau. Mã ứng dụng AdMob (`admobAppId`) vẫn phải là mã thật.

`appsFlyerDevKey` lấy ở **AppsFlyer > App settings > Dev key** (ADR-0027); script không in khóa ra, chỉ truyền cho build qua biến môi trường. Mật khẩu khóa upload không ghi vào thư mục này, script hỏi khi chạy.

### 2.3. Chạy script

```sh
scripts/release.sh                      # commit mới nhất của main
scripts/release.sh --commit <sha>       # một commit cũ hơn của main
scripts/release.sh --config <thư mục>   # thư mục phát hành ở chỗ khác
```

Script làm theo thứ tự, dừng ngay khi có điều kiện không đạt:

1. Kiểm tra thư mục phát hành: đủ file, nằm ngoài repo, mã ứng dụng AdMob không phải mã thử của Google, có khóa dev AppsFlyer, địa chỉ R2 là https.
2. Kiểm tra commit: nằm trên `main`, đã qua `main-guard.yml`, nhánh chính không có issue `main-broken` đang mở, `versionCode` chưa có tag `release/<versionCode>`.
3. Đối chiếu cấu hình với code của commit: `google-services.json` có đúng `applicationId`; vị trí trong `AdPlacement` chưa có mã thật thì dùng mã thử của Google theo định dạng, script in danh sách các vị trí đó (dòng "Mã quảng cáo thử").
4. Hỏi mật khẩu khóa upload.
5. Build `bundleProductionRelease` trong một bản sao sạch của commit (git worktree), nên thay đổi chưa commit trên máy không lọt vào bản phát hành. Crashlytics tải file mapping lên project production trong lúc build.
6. Kiểm tra bản build đã được ký, chép vào `release-output/<versionCode>/`, gắn tag `release/<versionCode>` vào commit và đẩy tag lên GitHub.

### 2.4. Tải lên Google Play

1. So dòng "Chứng chỉ ký (SHA-256)" script in ra với **Upload key certificate** trong **Play Console > Test and release > App integrity**. Lần tải đầu tiên của app mới chưa có chứng chỉ để so; khóa ký bản đầu tiên trở thành khóa upload.
2. Tải file `.aab` lên kênh **Internal testing**. File mapping của R8 đã nằm trong file `.aab`, không cần tải riêng.
3. Cài từ Google Play trên máy thật, kiểm tra quảng cáo hiện đúng vị trí (vị trí dùng mã thử hiện nhãn "Test Ad") và file trên R2 tải được.
4. Kiểm trên AdMob, mục **Apps > app-ads.txt**, rằng `app-ads.txt` của app đã được xác minh (mục 1.4).
5. Chuyển bản đó sang kênh **Production** trên Play Console.
6. Xóa thư mục phát hành khỏi máy.
