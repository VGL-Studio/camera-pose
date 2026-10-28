---
ticket: T-071
title: Catalog mặc định của app là catalog tư thế thật (v3), bỏ 6 tư thế mượn
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.8M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (21 task)
blocking_points: [core]      # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-071: CATALOG MẶC ĐỊNH CỦA APP LÀ CATALOG TƯ THẾ THẬT (V3), BỎ 6 TƯ THẾ MƯỢN

## 1. Tóm tắt

Đổi `DEFAULT_CATALOG_PATH` của `:core:poses` từ catalog mẫu `poses/catalog/v1/catalog-587b5394.json` (6 tư thế mượn) sang `poses/catalog/v3/catalog-512aa695.json`: 350 tư thế, 8 nhóm, 50 tư thế của bộ khởi đầu, không có ảnh mượn. Đây cũng là file mà khóa Remote Config `poses_catalog_path` của staging đang trỏ tới. Bản sao nguyên byte của catalog v3 vào file mẫu của test, để CI kiểm parser đọc được toàn bộ nội dung thật và đường dẫn mặc định khớp mã băm. File R2 bắt buộc và sổ tài sản mượn được cập nhật theo.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/poses/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi vào thư viện thì dòng dưới tiêu đề ghi tổng số tư thế và số nhóm | Không đổi code. Với catalog v3, thư viện có 350 tư thế trong 7 nhóm: nhóm `kids` không có tư thế nào nên không có chip (tiêu chí "chỉ các nhóm có ít nhất một tư thế mới có chip"). Kiểm bằng `buildPoseLibrary` trên file mẫu v3. |
| KB-1 | Khi mỗi thẻ hiện thì thẻ có ảnh, tên tư thế, loại và số bước; tên không có chuỗi kỹ thuật | Không đổi code. Parser đọc 350 tư thế, không bỏ tư thế nào (`droppedPoses` là 0), nên mọi thẻ có đủ trường. |
| KB-5 | Khi vào thư viện thì các thẻ của bộ khởi đầu hiện ảnh ngay, không chờ tải | Bộ khởi đầu là 50 tư thế `starter` của v3; màn loading tải 100 file (ảnh nhỏ và nét viền), như đã làm cho máy có Remote Config. Không đổi code tải. |
| KB-5 | Khi một lần tải không có phản hồi sau 15 giây thì app coi là lỗi | Không đổi. Giới hạn 15 giây áp cho từng file (`LOAD_TIMEOUT_MILLIS`), không cho cả bộ khởi đầu. |

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md). Ticket không đổi giao diện.

ADR liên quan: ADR-0006 (file R2 không xóa, không ghi đè), ADR-0015 (đường dẫn có phiên bản và mã băm), ADR-0021 (tài sản mượn, mục 8), ADR-1001 (giới hạn 15 giây), ADR-0031 (viết lại câu khẳng định cũ).

## 3. Giải pháp

### 3.1. Cách làm

**Code.** `PosesSettings.kt`: `DEFAULT_CATALOG_PATH = "poses/catalog/v3/catalog-512aa695.json"`; chú thích KDoc đổi thành "Catalog of real poses on the staging bucket (the file Remote Config `poses_catalog_path` points to), used when Remote Config has no valid path." Không đổi `DefaultPoseCatalogRepository`, parser hay luồng tải.

**File mẫu của test.** Thêm `core/poses/src/test/resources/catalog/catalog-v3-512aa695.json`: bản sao nguyên byte của `https://camera-pose.vgl.io.vn/poses/catalog/v3/catalog-512aa695.json` (1 663 106 byte, SHA-256 bắt đầu `512aa6951f7d92c2`). Phiên tải bằng `curl -sS -o <file> <địa chỉ>` rồi kiểm `shasum -a 256 <file> | cut -c1-8` ra `512aa695` trước khi commit; mã băm khác thì dừng (mục 7). Không định dạng lại, không sửa file.

**Test mới** (`core/poses/src/test/.../RealCatalogV3Test.kt`), viết trước ở bước 3:

- `realCatalog_parsesAll350Poses_noneDropped_fiftyStarter`: `CatalogParser(RecordingAppLogger()).parse(Fixtures.text("catalog-v3-512aa695.json"))` là `Valid`, `droppedPoses == 0`, `poses.size == 350`, `poses.count { it.starter } == 50`, `catalog.categories.map { it.id } == listOf("women", "men", "couples", "friends", "family", "kids", "travel", "cafe")`.
- `realCatalog_libraryShowsOnlyCategoriesWithPoses`: `buildPoseLibrary(catalog, emptyList(), emptyList()).categories.map { it.id } == listOf("women", "men", "couples", "friends", "family", "travel", "cafe")` (không có `kids`).
- `realCatalog_hasNoBorrowedAsset`: không đường dẫn nào trong `thumbnailPath`, `imagePath`, `outlinePath` của mọi tư thế bắt đầu bằng `borrowed/` (ADR-0021: bản build không còn tài sản mượn).
- `defaultCatalog_isTheRealCatalogV3`: `DEFAULT_CATALOG_PATH == "poses/catalog/v3/catalog-${sha8}.json"`, với `sha8` là 8 ký tự đầu của SHA-256 nội dung file mẫu v3. Test này thất bại trên code hiện tại (đường dẫn còn v1).

Ba test đầu qua ngay trên code hiện tại vì chỉ đọc file mẫu (ngoại lệ ở mục 4); test thứ tư là test thất bại đúng lý do.

**Test cũ viết lại (ADR-0031).** `StagingSampleCatalogTest.defaultCatalogPath_isV1Folder_andEndsWithFirst8HexOfSha256OfStagingSample` khẳng định `DEFAULT_CATALOG_PATH` là đường dẫn của catalog mẫu `staging-sample.json`, trái câu "Đổi `DEFAULT_CATALOG_PATH` ... sang `poses/catalog/v3/catalog-512aa695.json`" của mục này. Ý định của test: đường dẫn mặc định của app gọi đúng file catalog mà repo giữ bản sao, theo mã băm của nội dung (ADR-0015). Câu mới, viết lại trong chính hàm đó (giữ tên hàm, vì xóa một hàm `@Test` của nhánh chính là mức chặn của rà soát, L-095), ở mục 3.5. Hai test còn lại của `StagingSampleCatalogTest` giữ nguyên: file mẫu `staging-sample.json` vẫn là bản ghi của 6 tư thế mượn còn trên R2 staging (`docs/borrowed-assets.md`).

**Tài liệu.**

- `docs/ops/required-assets.txt`: thay dòng `poses/catalog/v1/catalog-587b5394.json` bằng `poses/catalog/v3/catalog-512aa695.json`. `scripts/setup-check-app.sh` đọc `DEFAULT_CATALOG_PATH` từ code bằng `sed` và tự đếm file khởi đầu từ catalog, nên không cần sửa; `scripts/tests/app.sh` cũng đọc đường dẫn từ code (dòng 63 tới 66), còn dòng 263 chỉ dùng chuỗi v1 làm ví dụ đường dẫn hợp lệ.
- `docs/borrowed-assets.md`: 18 dòng `borrowed` chuyển sang `replaced`, cột "Thay bằng" ghi `catalog poses/catalog/v3/catalog-512aa695.json (nội dung riêng, T-071)`, cột "Ngày thay" ghi ngày merge. Thêm một câu vào Ghi chú: file mượn vẫn còn trên R2 staging (ADR-0006 không xóa file), nhưng không bản build nào còn trỏ tới catalog v1 khi không có Remote Config. Mẫu `docs/templates/borrowed-assets.md` cho phép đổi trạng thái khi thay xong, không xóa dòng.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PosesSettings.kt` | `DEFAULT_CATALOG_PATH` sang v3, KDoc mới |
| Tạo | `core/poses/src/test/resources/catalog/catalog-v3-512aa695.json` | Bản sao nguyên byte của catalog v3 trên R2 staging |
| Tạo | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/RealCatalogV3Test.kt` | Bốn test ở mục 3.1 |
| Sửa | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/StagingSampleCatalogTest.kt` | Viết lại một câu khẳng định (mục 3.5), KDoc của hàm ghi lý do giữ tên |
| Sửa | `docs/ops/required-assets.txt` | Dòng catalog v1 thành v3 |
| Sửa | `docs/borrowed-assets.md` | 18 dòng `replaced`, câu ghi chú |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` | (dự phòng) `START_SCREEN_TIMEOUT_MILLIS` từ 60 lên 120 giây và câu KDoc về thời gian tải, chỉ theo mục 3.4 |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` | (dự phòng) như trên |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: không thêm hay sửa file nào trên R2; catalog v3 và 100 file khởi đầu đã có (dev đưa lên; `env-canary` ngày 2026-10-28: "khóa poses_catalog_path trỏ tới catalog poses/catalog/v3/catalog-512aa695.json và 100 file khởi đầu trả 200"). Đổi file bắt buộc trong `docs/ops/required-assets.txt`.
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** test emulator của CI (`AppLaunchTest`, `SettingsNavigationTest`) và tệp kịch bản của `device-session` dùng cấu hình Firebase giả, nên đọc `DEFAULT_CATALOG_PATH`. Từ nay chúng tải catalog 1,6 MB và 100 file khởi đầu thay cho 9 file. Hai test chờ màn đầu tối đa 60 giây; KDoc ghi lần tải chậm nhất còn qua với catalog mẫu là khoảng 47 giây.
- **Điều kiện chuyển:** job `ui-test` của PR đỏ ở `waitUntil` của `START_SCREEN_TIMEOUT_MILLIS` (`ComposeTimeoutException`), log của test có dòng `VGL.Poses` tải bộ khởi đầu còn đang chạy (chưa có "Starter set is complete") và không có dòng lỗi tải nào; xảy ra ở 2 lần chạy liên tiếp.
- **Cách làm thay:** đổi `START_SCREEN_TIMEOUT_MILLIS` của hai test từ `60_000L` lên `120_000L` và câu KDoc về thời gian tải. Điều test kiểm (app tới màn chào hay camera) không đổi; câu khẳng định không đổi. Nếu vẫn đỏ ở 120 giây thì dừng task (mục 7).

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Không có Remote Config hợp lệ thì app đọc catalog mẫu v1 (6 tư thế mượn) | "Đổi `DEFAULT_CATALOG_PATH` ... sang `poses/catalog/v3/catalog-512aa695.json`" | `StagingSampleCatalogTest.defaultCatalogPath_isV1Folder_andEndsWithFirst8HexOfSha256OfStagingSample`: `assertEquals("poses/catalog/v1/catalog-${sha.take(8)}.json", DEFAULT_CATALOG_PATH)` với `sha` của `staging-sample.json` | Câu mới: `sha` tính trên `Fixtures.text("catalog-v3-512aa695.json")`, `assertEquals("poses/catalog/v3/catalog-${sha.take(8)}.json", DEFAULT_CATALOG_PATH)`. Ý định: đường dẫn mặc định gọi đúng file catalog mà repo giữ bản sao, theo mã băm nội dung. Đối chứng: tạm đổi một byte của file mẫu v3, câu mới đỏ. |
| Các test của repository dùng `DEFAULT_CATALOG_PATH` làm khóa của `FakeRemoteAssets` | Không đổi | `scripts/existing-tests.py`, nhóm "Nên xem", 9 test: `DefaultPoseCatalogRepositoryTest` (`invalidRemotePath_usesDefaultPath_andLogsWarning`, `loadCatalog_afterFailure_loadsAgain`, `loadCatalog_concurrentCalls_loadOnce`, `loadCatalog_timeout_failsWithTimeout`, `loadCatalog_unavailableNetwork_failsWithCatalogNetwork_andEmitsFailure`, `loadCatalog_unexpectedError_endsFailed_notLoading`, `starterSet_catalogFails_returnsIncompleteWithCatalogFailure`, `starterSet_loadsThumbnailAndOutlineOfStarterPosesOnly`) và `DefaultPoseLibraryRepositoryTest.library_catalogFailed_emitsFailed_thenRetryEmitsReady` | Giữ: các test dùng tên hằng số làm khóa của bản giả và nội dung catalog của chính test, không dùng giá trị đường dẫn. |
| `scripts/tests/app.sh` kiểm `setup-check-app.sh` với catalog mặc định | Không đổi | Khối `setup-check app` | Giữ: đường dẫn đọc từ `PosesSettings.kt` bằng `sed` (dòng 66). |

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Đường dẫn mặc định là catalog v3, khớp mã băm | Unit test | `RealCatalogV3Test.defaultCatalog_isTheRealCatalogV3` (thất bại trên code hiện tại: đường dẫn còn v1) |
| KB-1, mọi thẻ đủ trường: parser đọc đủ 350 tư thế, 50 khởi đầu | Unit test | `RealCatalogV3Test.realCatalog_parsesAll350Poses_noneDropped_fiftyStarter` |
| KB-1, chỉ nhóm có tư thế mới có chip | Unit test | `RealCatalogV3Test.realCatalog_libraryShowsOnlyCategoriesWithPoses` |
| ADR-0021, bản build không còn tài sản mượn | Unit test | `RealCatalogV3Test.realCatalog_hasNoBorrowedAsset` |
| Câu khẳng định cũ viết lại (ADR-0031) | Unit test | `StagingSampleCatalogTest.defaultCatalogPath_isV1Folder_andEndsWithFirst8HexOfSha256OfStagingSample` với câu mới ở mục 3.5; đối chứng: tạm đổi một byte của file mẫu, test đỏ, ghi vào nhật ký |
| KB-5, bộ khởi đầu tải ở màn loading, app tới camera | Test emulator có sẵn | `AppLaunchTest`, `SettingsNavigationTest` trong job `ui-test` của PR (đọc catalog mặc định) |
| KB-1, KB-5 trên luồng thật: thư viện hiện tư thế của v3, chi tiết, chụp | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`) | Tệp có sẵn; `device-session` cuối phiên chạy cả 6 tệp với catalog v3. Đạt khi 6/6 tệp đạt và ảnh `detail` của T-022 không phải tư thế mượn. |
| File R2 bắt buộc | Kiểm tra thủ công (người: kiểm bằng workflow) | Sau merge, `env-canary` hay `start-run` của phiên sau: dòng "catalog mặc định của app poses/catalog/v3/catalog-512aa695.json và 100 file khởi đầu trả 200" |

**Ngoại lệ viết test trước:** ba test `realCatalog_*` chỉ đọc file mẫu mới, nên qua ngay khi file mẫu có mặt ở commit test; chúng giữ nội dung thật không bị parser sau này làm rơi. Đối chứng cho chúng ở bước 3: tạm sửa `"starter": true` đầu tiên thành `false` trong bản tạm của file mẫu, `realCatalog_parsesAll350Poses...` đỏ (49 thay cho 50); tạm thêm `borrowed/` vào một `outline`, `realCatalog_hasNoBorrowedAsset` đỏ; hoàn tác trước commit. `docs/ops/required-assets.txt` và `docs/borrowed-assets.md` là tài liệu, không có test riêng (`scripts/setup-check.sh` kiểm file bắt buộc).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-28, trên `origin/main` 2b8d67e. Một test tạm không commit (`TmpV3ProbeTest`, đã xóa) đọc bản tải về của catalog v3 bằng `CatalogParser` và `buildPoseLibrary` của app.
- **Lệnh:** `./gradlew :core:poses:testDebugUnitTest --tests '*TmpV3ProbeTest*' -i`. Không chạy `scripts/check.sh`: kỹ thuật không mới (E là 0), chạy thử để kiểm nội dung thật.
- **Kết quả:**
  - Parser trả `Valid`: `poses=350 dropped=0`, 8 nhóm `[women, men, couples, friends, family, kids, travel, cafe]`, `starter=50`, 241 tư thế có `keypoints`, `borrowed=0`. Thư viện có 7 nhóm (không có `kids`; catalog không có tư thế nào nhóm này). Số tư thế theo nhóm: women 198, men 58, couples 36, friends 30, travel 16, cafe 9, family 3.
  - `shasum -a 256` của bản tải về bắt đầu `512aa695`, khớp tên file. Dung lượng 1 663 106 byte.
  - Tải thử 100 file khởi đầu từ máy dev, 4 luồng như `STARTER_PARALLELISM`: lần đầu 69 giây, 2 file không trả 200 (hết giờ của `curl`); lần hai cả 100 file trả 200, file chậm nhất 12,0 giây, catalog 6,6 giây. Mạng của máy dev chậm, không đại diện cho runner. `env-canary` trên runner ngày 2026-10-28: 100 file trả 200, cả bước `setup-check` khoảng 2 phút.
  - `scripts/secret-scan.py` trên bản sao: "Không thấy bí mật hay mã quảng cáo thật."

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7. Module core dùng chung (`:core:poses`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một hằng số trong `:core:poses`, một file mẫu, một test mới, một câu test viết lại, hai tài liệu |
| B. Độ rõ của yêu cầu | 1 | Dev xác nhận nội dung đã thay; còn câu hỏi về file mẫu 1,6 MB và ước tính dung lượng của spec (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Đường dẫn, parser và nội dung có unit test; thời gian tải bộ khởi đầu chỉ thấy ở test emulator và `device-session` |
| D. Ảnh hưởng chéo | 2 | Đổi dữ liệu mà loading, thư viện, camera và test emulator của mọi PR dựa vào |
| E. Độ mới của kỹ thuật | 0 | Đổi hằng số, thêm file mẫu như T-009 |
| **Tổng** | 5 | **Mức: Cao (chạm điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:poses` (`PosesSettings.kt`); `docs/ops/required-assets.txt` mà `setup-check.sh`, `env-canary.yml`, `start-run.yml` đọc. Không plan nào khác đang sửa các file này.
- Test emulator của mọi PR sau ticket này tải catalog v3 và 100 file khởi đầu từ R2 staging (mục 3.4).
- Task khác trong sprint có thể xung đột: không có plan `approved` nào khác.
- Đề xuất thứ tự chạy: chạy độc lập; nên đứng đầu danh sách để `device-session` cuối phiên kiểm luôn catalog v3 với các tệp kịch bản.

## 7. Điều kiện dừng riêng

- Mã băm của bản tải về không bắt đầu `512aa695`, hay địa chỉ catalog v3 không trả 200: dừng, vì nội dung trên R2 khác với plan đã duyệt.
- Test emulator vẫn đỏ vì hết giờ chờ màn đầu sau khi đã chuyển sang 120 giây theo mục 3.4: dừng, ghi thời gian tải bộ khởi đầu đo được từ log.

## 8. Câu hỏi cho người duyệt

1. **File mẫu 1,6 MB trong repo.** Đề xuất của AI: commit bản sao nguyên byte của catalog v3 vào `core/poses/src/test/resources/catalog/`, để CI kiểm parser đọc đủ 350 tư thế và đường dẫn khớp mã băm. Phương án khác: không giữ bản sao, test chỉ kiểm dạng đường dẫn `poses/catalog/v<n>/catalog-<8 hex>.json`, còn nội dung để `setup-check` kiểm mỗi sáng. Cách này yếu hơn: parser đổi về sau mà làm rơi tư thế thật thì CI không thấy.
2. **Giữ tên hàm test cũ.** Viết lại câu khẳng định trong `defaultCatalogPath_isV1Folder_andEndsWithFirst8HexOfSha256OfStagingSample` mà giữ tên, vì xóa hay đổi tên một hàm `@Test` là mức chặn (L-095); KDoc của hàm ghi tên cũ là của T-009. Người duyệt muốn đổi tên thì ghi rõ ở mục 9 là cho phép đổi tên hàm này.
3. **Thời gian tải trên test emulator.** Đề xuất: phương án dự phòng ở mục 3.4 (nâng thời gian chờ lên 120 giây), chỉ khi `ui-test` hết giờ hai lần liên tiếp. Người dùng thật có Remote Config đã tải đúng bộ này từ bản staging hiện tại.
4. **Spec `poses` mục 7** ước tính catalog 100 đến 300 KB với 200 tư thế; catalog v3 là 1,6 MB với 350 tư thế. Không đổi hành vi nào nên plan không sửa spec. Có muốn cập nhật ước tính này trong spec (thành `changed`, người xác nhận lại) không?
5. **Bản production.** `DEFAULT_CATALOG_PATH` chỉ tới bucket staging (`camera-pose.vgl.io.vn` trong `app/build.gradle.kts`). Câu 8 của plan T-009 đề xuất đưa catalog thật lên cả bucket production trước khi phát hành; ticket này không chạm cấu hình production (ADR-0007, ADR-0009).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
