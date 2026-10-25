---
ticket: T-051
title: Chữ của thư viện tư thế và hệ thống thiết kế theo ngôn ngữ đã chọn
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 8M           # cỡ M (5.0M, scripts/token-usage.py estimate) cộng khoảng 60%: 12 tệp chữ mới, 2 test chữ, 9 file test đổi, 3 tệp mẫu của test script
blocking_points: [core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-051: CHỮ CỦA THƯ VIỆN TƯ THẾ VÀ HỆ THỐNG THIẾT KẾ THEO NGÔN NGỮ ĐÃ CHỌN

## 1. Tóm tắt

`values/` của `:feature:poses` (30 chuỗi) và `:core:designsystem` (5 chuỗi) viết bằng tiếng Việt và không có thư mục ngôn ngữ nào khác. Vì vậy đổi ngôn ngữ trong cài đặt thì thư viện, chi tiết tư thế và hộp thoại mất mạng vẫn hiện tiếng Việt. Plan đổi `values/` sang tiếng Anh, chuyển nguyên chữ tiếng Việt hiện có sang `values-vi/`, và thêm 5 ngôn ngữ phát hành còn lại theo bảng dịch ở mục 3.1, đúng cách của settings và onboarding.

Mỗi module có một test chữ riêng, kiểu `SettingsStringsTest`. Screenshot test đang chụp chữ tiếng Việt thêm `vi` vào `qualifiers`, nên ảnh chuẩn giữ nguyên. Thêm một ảnh chuẩn mới ở tiếng Bồ Đào Nha, khung thấp.

Đã chạy thử toàn bộ trên code hiện tại và trên bản cài thử không commit (mục 4.1):
- 48 ảnh chuẩn có `vi` qua `verify`, không đổi byte nào;
- các test viết trước thất bại đúng lý do;
- `scripts/check.sh` qua trên bản cài thử.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/settings/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khi bấm "Ngôn ngữ" trên Android 13 trở lên thì trang ngôn ngữ của app trong cài đặt máy mở ra; chọn xong quay lại thì app hiện ngôn ngữ mới. | Cơ chế đổi ngôn ngữ đã chạy đúng (Nhật kiểm trên máy thật, ticket T-051). Phần thiếu là tài nguyên chữ: thư viện, chi tiết tư thế và hộp thoại mất mạng phải hiện đúng ngôn ngữ đã chọn trong 7 ngôn ngữ phát hành, ngôn ngữ chưa có thì hiện tiếng Anh. |
| KB-4 | Khi bấm "Ngôn ngữ" trên máy dưới Android 13 thì một hộp chọn trong app hiện ... chọn xong app đổi ngôn ngữ ngay | Như trên: `MainActivity` đã áp ngôn ngữ đã lưu cho cả activity, nên tài nguyên của `:feature:poses` và `:core:designsystem` đổi theo khi có bản dịch. |

Spec onboarding, câu hỏi 1 (dùng chung cho mọi spec của app): ngôn ngữ gốc trong `strings.xml` là tiếng Anh; AI dịch trong cùng ticket; một người biết tiếng duyệt trước khi phát hành.

Thiết kế tham chiếu: `features/poses/design/README.md`, mục 2 (bảng chữ tiếng Việt), và các ảnh `design/screens/` của poses và network. Ảnh thiết kế vẽ bằng tiếng Việt, nên screenshot test cùng tên chụp ở `vi`, như `SettingsScreenScreenshotTest`.

ADR liên quan: ADR-0008 (hệ thống thiết kế; câu khẩu hiệu `brand_tagline` là của từng app), ADR-0011 (ảnh chuẩn), ADR-0020 (screenshot test cùng tên ảnh thiết kế).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo đúng mẫu của `:feature:settings` (T-025) và `:feature:onboarding`.

1. `values/strings.xml` của hai module: thay giá trị bằng cột `en` của bảng dưới, giữ nguyên khóa, thứ tự và chú thích. Dấu nháy đơn viết `\'` (ví dụ `Couldn\'t load the library`).
2. `values-vi/strings.xml` của hai module: chép nguyên chữ tiếng Việt hiện có của `values/` (cột `vi`), không đổi chữ nào, bỏ chú thích.
3. `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th`: theo các cột cùng tên. Tên thư mục giống settings và onboarding.
4. Không đổi code Kotlin: chỉ đổi tài nguyên chữ và test.
5. Số lượng (`%d bước`, `%1$d tư thế trong %2$d nhóm`) giữ là chuỗi thường, tiếng Anh viết số nhiều ("steps", "poses", "groups"). Spec poses ghi mỗi tư thế có 3 đến 5 bước, và catalog luôn có nhiều tư thế và nhiều nhóm (mục 8, câu 2).

**Bảng dịch.** Chữ tiếng Anh theo giọng của settings và onboarding: "Try again" cho "Thử lại" (như `loading_retry`), "Back", "Settings" như `settings_back`, `settings_title`. Chữ của các ngôn ngữ khác dùng lại từ đã có ở settings, loading và onboarding khi trùng nghĩa: `पोज़`, `Configurações`, `Reintentar`, `Setelan`... Không ô nào trùng chữ tiếng Anh, nên test chữ không có ngoại lệ `SAME_AS_ENGLISH`.

**`:feature:poses`** (30 chuỗi):

| Khóa | en (`values/`) | vi (`values-vi/`, chữ hiện có) | hi | pt-BR | es-419 | in | th |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `poses_title` | Pose library | Thư viện tư thế | पोज़ लाइब्रेरी | Biblioteca de poses | Biblioteca de poses | Pustaka pose | คลังท่าโพส |
| `poses_summary` | %1$d poses in %2$d groups | %1$d tư thế trong %2$d nhóm | %2$d ग्रुप में %1$d पोज़ | %1$d poses em %2$d grupos | %1$d poses en %2$d grupos | %1$d pose dalam %2$d grup | %1$d ท่าใน %2$d กลุ่ม |
| `poses_chip_all` | All | Tất cả | सभी | Todas | Todas | Semua | ทั้งหมด |
| `poses_chip_saved` | Saved | Đã lưu | सेव किए गए | Salvas | Guardadas | Tersimpan | ที่บันทึกไว้ |
| `poses_card_meta` | %1$s · %2$d steps | %1$s · %2$d bước | %1$s · %2$d स्टेप | %1$s · %2$d passos | %1$s · %2$d pasos | %1$s · %2$d langkah | %1$s · %2$d ขั้นตอน |
| `poses_subject_single` | Solo | Một người | अकेले | Individual | Individual | Sendiri | คนเดียว |
| `poses_subject_couple` | Couple | Cặp đôi | कपल | Casal | Pareja | Pasangan | คู่รัก |
| `poses_subject_group` | Group | Nhóm | ग्रुप | Grupo | Grupo | Grup | กลุ่ม |
| `poses_error_title` | Couldn't load the library | Không tải được thư viện | लाइब्रेरी लोड नहीं हो सकी | Não foi possível carregar a biblioteca | No se pudo cargar la biblioteca | Pustaka tidak dapat dimuat | โหลดคลังท่าโพสไม่ได้ |
| `poses_error_message` | Something went wrong while loading the library. Try again in a few minutes. | Đã có lỗi khi tải thư viện. Thử lại sau ít phút. | लाइब्रेरी लोड करते समय गड़बड़ी हुई। कुछ मिनट बाद फिर से कोशिश करें। | Algo deu errado ao carregar a biblioteca. Tente novamente em alguns minutos. | Algo salió mal al cargar la biblioteca. Vuelve a intentarlo en unos minutos. | Terjadi kesalahan saat memuat pustaka. Coba lagi dalam beberapa menit. | เกิดข้อผิดพลาดขณะโหลดคลังท่าโพส ลองอีกครั้งในอีกสักครู่ |
| `poses_error_action` | Try again | Thử lại | फिर से कोशिश करें | Tentar novamente | Reintentar | Coba lagi | ลองอีกครั้ง |
| `poses_saved_empty_title` | No saved poses yet | Chưa lưu tư thế nào | अभी कोई पोज़ सेव नहीं है | Nenhuma pose salva ainda | Aún no guardaste poses | Belum ada pose tersimpan | ยังไม่มีท่าโพสที่บันทึกไว้ |
| `poses_saved_empty_message` | Tap the save icon on a pose card to keep it here. | Chạm biểu tượng lưu trên thẻ tư thế để giữ lại ở đây. | किसी पोज़ कार्ड पर सेव आइकन टैप करें, वह यहाँ दिखेगा। | Toque no ícone de salvar em um card de pose para guardá-la aqui. | Toca el ícono de guardar en una tarjeta de pose para tenerla aquí. | Ketuk ikon simpan di kartu pose untuk menyimpannya di sini. | แตะไอคอนบันทึกบนการ์ดท่าโพสเพื่อเก็บไว้ที่นี่ |
| `poses_saved_empty_action` | See all poses | Xem tất cả tư thế | सभी पोज़ देखें | Ver todas as poses | Ver todas las poses | Lihat semua pose | ดูท่าโพสทั้งหมด |
| `poses_save_failed` | Couldn't save the pose. Try again later. | Chưa lưu được tư thế. Thử lại sau. | पोज़ सेव नहीं हो सका। बाद में फिर से कोशिश करें। | Não foi possível salvar a pose. Tente novamente mais tarde. | No se pudo guardar la pose. Vuelve a intentarlo más tarde. | Pose tidak dapat disimpan. Coba lagi nanti. | บันทึกท่าโพสไม่สำเร็จ ลองอีกครั้งภายหลัง |
| `poses_cd_back` | Back | Quay lại | वापस | Voltar | Volver | Kembali | กลับ |
| `poses_cd_settings` | Settings | Cài đặt | सेटिंग | Configurações | Configuración | Setelan | การตั้งค่า |
| `poses_cd_save` | Save pose | Lưu tư thế | पोज़ सेव करें | Salvar pose | Guardar pose | Simpan pose | บันทึกท่าโพส |
| `poses_cd_unsave` | Remove from saved | Bỏ lưu tư thế | सेव से हटाएँ | Remover das salvas | Quitar de guardadas | Hapus dari tersimpan | เลิกบันทึกท่าโพส |
| `poses_cd_reload_image` | Reload image | Tải lại ảnh | इमेज फिर से लोड करें | Recarregar imagem | Volver a cargar la imagen | Muat ulang gambar | โหลดรูปอีกครั้ง |
| `poses_detail_meta` | %1$s · %2$s · %3$s · %4$d steps | %1$s · %2$s · %3$s · %4$d bước | %1$s · %2$s · %3$s · %4$d स्टेप | %1$s · %2$s · %3$s · %4$d passos | %1$s · %2$s · %3$s · %4$d pasos | %1$s · %2$s · %3$s · %4$d langkah | %1$s · %2$s · %3$s · %4$d ขั้นตอน |
| `poses_detail_meta_no_group` | %1$s · %2$s · %3$d steps | %1$s · %2$s · %3$d bước | %1$s · %2$s · %3$d स्टेप | %1$s · %2$s · %3$d passos | %1$s · %2$s · %3$d pasos | %1$s · %2$s · %3$d langkah | %1$s · %2$s · %3$d ขั้นตอน |
| `poses_framing_full` | Full body | Toàn thân | पूरा शरीर | Corpo inteiro | Cuerpo completo | Seluruh badan | เต็มตัว |
| `poses_framing_half` | Half body | Nửa người | आधा शरीर | Meio corpo | Medio cuerpo | Setengah badan | ครึ่งตัว |
| `poses_framing_close` | Close-up | Cận mặt | क्लोज़-अप | Rosto em close | Primer plano | Jarak dekat | ระยะใกล้ |
| `poses_detail_steps_title` | Steps | Các bước | स्टेप | Passos | Pasos | Langkah | ขั้นตอน |
| `poses_detail_mistakes_title` | Common mistakes | Lỗi hay gặp | आम गलतियाँ | Erros comuns | Errores comunes | Kesalahan umum | ข้อผิดพลาดที่พบบ่อย |
| `poses_detail_shoot` | Shoot with this pose | Chụp với tư thế này | इस पोज़ के साथ फ़ोटो लें | Fotografar com esta pose | Tomar foto con esta pose | Foto dengan pose ini | ถ่ายด้วยท่านี้ |
| `poses_image_error_title` | Couldn't load the image | Không tải được ảnh | इमेज लोड नहीं हो सकी | Não foi possível carregar a imagem | No se pudo cargar la imagen | Gambar tidak dapat dimuat | โหลดรูปไม่ได้ |
| `poses_image_error_message` | Something went wrong while loading the image. Try again in a few minutes. | Đã có lỗi khi tải ảnh. Thử lại sau ít phút. | इमेज लोड करते समय गड़बड़ी हुई। कुछ मिनट बाद फिर से कोशिश करें। | Algo deu errado ao carregar a imagem. Tente novamente em alguns minutos. | Algo salió mal al cargar la imagen. Vuelve a intentarlo en unos minutos. | Terjadi kesalahan saat memuat gambar. Coba lagi dalam beberapa menit. | เกิดข้อผิดพลาดขณะโหลดรูป ลองอีกครั้งในอีกสักครู่ |

**`:core:designsystem`** (5 chuỗi):

| Khóa | en (`values/`) | vi (`values-vi/`, chữ hiện có) | hi | pt-BR | es-419 | in | th |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `brand_tagline` | Everything falls into rhythm. | Mọi việc vào đúng nhịp. | सब कुछ सही लय में। | Tudo entra no ritmo. | Todo encuentra su ritmo. | Semuanya pas iramanya. | ทุกอย่างลงตัวตามจังหวะ |
| `network_dialog_title` | No network connection | Mất kết nối mạng | नेटवर्क कनेक्शन नहीं है | Sem conexão de rede | Sin conexión a la red | Tidak ada koneksi jaringan | ไม่มีการเชื่อมต่อเครือข่าย |
| `network_dialog_message` | The app needs a network connection to work. Check your internet connection, then try again. | App cần mạng để hoạt động. Kiểm tra kết nối Internet rồi thử lại. | ऐप को चलने के लिए नेटवर्क चाहिए। इंटरनेट कनेक्शन जाँचें, फिर से कोशिश करें। | O app precisa de rede para funcionar. Verifique sua conexão com a internet e tente novamente. | La app necesita conexión para funcionar. Revisa tu conexión a internet y vuelve a intentarlo. | Aplikasi memerlukan jaringan agar dapat berfungsi. Periksa koneksi internet Anda, lalu coba lagi. | แอปต้องใช้เครือข่ายในการทำงาน ตรวจสอบการเชื่อมต่ออินเทอร์เน็ตแล้วลองอีกครั้ง |
| `network_dialog_retry` | Try again | Thử lại | फिर से कोशिश करें | Tentar novamente | Reintentar | Coba lagi | ลองอีกครั้ง |
| `network_dialog_still_offline` | Still offline | Vẫn chưa có mạng | अभी भी नेटवर्क नहीं है | Ainda sem conexão | Todavía sin conexión | Masih belum ada jaringan | ยังไม่มีเครือข่าย |

`brand_tagline` chỉ hiện ở trang `Brand` của catalog hệ thống thiết kế (`BrandCatalog`, ảnh `DesignSystem_brand_*`), không hiện trong app (màn tải dùng `loading_tagline` của `:feature:loading`). Câu tiếng Anh dịch sát câu tiếng Việt; người duyệt chốt ở mục 8, câu 1.

**Test bị ảnh hưởng.** Đã tìm mọi test, tệp kịch bản và tệp mẫu chứa chữ của hai module: quét mọi giá trị trong `values/` của hai module trên `*/src/test`, `*/src/androidTest`, `*/src/deviceScenarios`, `scripts/`. Mọi chỗ dùng `stringResource` của `:core:designsystem` nằm trong `NetworkDialog.kt` và `DesignSystemCatalog.kt`. Robolectric chạy ở `en-US` khi `qualifiers` không ghi ngôn ngữ, nên sau thay đổi các test sau sẽ thấy chữ tiếng Anh:

| Module | File | Vì sao bị ảnh hưởng | Sửa |
| --- | --- | --- | --- |
| `:feature:poses` | `PosesScreenScreenshotTest.kt` | 15 ảnh chuẩn chụp chữ tiếng Việt, `qualifiers` không có ngôn ngữ (lớp: `w411dp-h891dp-xxhdpi`; hằng `UNFOLDED`, `SHORT_SCREEN`) | Thêm `vi-` vào ba chuỗi đó. Ảnh chuẩn giữ nguyên (mục 4.1) |
| `:feature:poses` | `detail/PoseDetailScreenScreenshotTest.kt` | 13 ảnh chuẩn, như trên | Như trên; thêm test `default_ptBr_small` (mục 4) |
| `:feature:poses` | `PosesScreenTest.kt` | `card_stepCountUsesScreenLanguage_english` chạy ở `en` mà khẳng định chữ tiếng Việt "Một người · 5 bước": test đang ghi lại đúng lỗi của ticket | Đổi thành "Solo · 5 steps". Các test khác của lớp đã ở `vi` (lớp và `UNFOLDED`) |
| `:feature:poses` | `detail/PoseDetailScreenTest.kt` | `meta_countsStepsInScreenLanguage` chạy ở `en` mà khẳng định "Một người · Women · Toàn thân · 5 bước" | Đổi thành "Solo · Women · Full body · 5 steps"; thêm test `smallScreen_portuguese_buttonFullyVisible` (mục 4) |
| `:core:designsystem` | `NetworkDialogScreenshotTest.kt` | 4 ảnh chuẩn chụp hộp thoại mất mạng tiếng Việt | Thêm `vi-` vào hai `qualifiers` |
| `:core:designsystem` | `NetworkDialogTest.kt` | So chữ tiếng Việt của bảng chữ thiết kế (`TITLE`, `MESSAGE`, `RETRY`, `STILL_OFFLINE`) | Thêm `vi-` vào hai `qualifiers`; giữ hằng chữ |
| `:core:designsystem` | `DesignSystemCatalogScreenshotTest.kt` | `brand_light`, `brand_dark` chụp `brand_tagline` | Thêm `vi-` vào `qualifiers` của lớp. Các trang khác không có chữ tài nguyên, ảnh giữ nguyên (mục 4.1) |
| `scripts/tests` | `fixtures/device-scenario/grid.xml`, `chips.xml`, `chips-saved.xml` | Ba test của `device-scenario` trong `scripts/tests/run.sh` dùng `str=poses_chip_saved`, `str=poses_cd_save`, `str=poses_chip_all`. Ở chế độ `run`, `scripts/device-scenario.py` so `str=` với giá trị trong `res/values/strings.xml` của chính repo, nên sau thay đổi sẽ tìm "Saved", trong khi tệp mẫu vẫn ghi "Đã lưu" | Đổi chữ trong ba tệp mẫu: "Tất cả" thành "All", "Đã lưu" thành "Saved", `content-desc` "Lưu tư thế" thành "Save pose" |
| `scripts/tests` | `run.sh` | Hai test `text=` dùng chính các nút đó của tệp mẫu: `expect text=Đã lưu selected=true`, `expect text=Tất cả selected=true` và mẫu log `step=1 expect text=Tất cả selected=true FAIL` | Đổi sang `text=Saved`, `text=All`. Hai test này qua cả trước lẫn sau, là đối chứng (mục 4.1) |

Không bị ảnh hưởng, đã kiểm:
- `PoseCardTest`, `PoseComponentsTest` của `:core:designsystem` truyền chữ tiếng Việt làm tham số, không đọc tài nguyên.
- Test của `:feature:camera`, `:feature:onboarding`, `:feature:loading`, `:feature:settings` khớp các chữ như "Thử lại", "Cài đặt" của chính module đó (camera thuộc T-050). Feature không phụ thuộc `:feature:poses`.
- `app/src/test`:
  - `NetworkDialogHostTest` chỉ dùng `testTag`;
  - `PoseDetailEntryTest` chỉ nhắc "Chụp với tư thế này" trong chú thích.
- `app/src/androidTest` (`AppLaunchTest`, `SettingsNavigationTest`, `PhotoSaveDeviceTest`) chỉ dùng `testTag` và `resource-id`.
- Tệp kịch bản `T-022-chi-tiet-sang-camera.txt`, `T-024-thu-vien-mo-gap.txt` chỉ dùng `id=`, `idp=`, `str=`, không dùng `text=` hay `desc=` tiếng Việt.
  - `str=` đọc `values/`, nên sau thay đổi khớp chữ tiếng Anh. Emulator của `device-session.yml` chạy ở `en-US` và app hiện tiếng Anh, nên vẫn khớp.
  - Bước "(7) tiếng Anh" của T-022 (`device locale en-US`) nay cho cùng ngôn ngữ với mặc định; bước vẫn qua. Xem mục 6 về máy thật đặt tiếng Việt.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/poses/src/main/res/values/strings.xml` | Giá trị tiếng Anh theo cột `en`, giữ khóa, thứ tự, chú thích |
| Tạo | `feature/poses/src/main/res/values-vi/strings.xml` | Chữ tiếng Việt hiện có (cột `vi`), không đổi chữ |
| Tạo | `feature/poses/src/main/res/values-hi/strings.xml` | Cột `hi` |
| Tạo | `feature/poses/src/main/res/values-pt-rBR/strings.xml` | Cột `pt-BR` |
| Tạo | `feature/poses/src/main/res/values-b+es+419/strings.xml` | Cột `es-419` |
| Tạo | `feature/poses/src/main/res/values-in/strings.xml` | Cột `in` |
| Tạo | `feature/poses/src/main/res/values-th/strings.xml` | Cột `th` |
| Sửa | `core/designsystem/src/main/res/values/strings.xml` | Giá trị tiếng Anh theo cột `en`, giữ chú thích (ADR-0008) |
| Tạo | `core/designsystem/src/main/res/values-vi/strings.xml` | Chữ tiếng Việt hiện có |
| Tạo | `core/designsystem/src/main/res/values-hi/strings.xml` | Cột `hi` |
| Tạo | `core/designsystem/src/main/res/values-pt-rBR/strings.xml` | Cột `pt-BR` |
| Tạo | `core/designsystem/src/main/res/values-b+es+419/strings.xml` | Cột `es-419` |
| Tạo | `core/designsystem/src/main/res/values-in/strings.xml` | Cột `in` |
| Tạo | `core/designsystem/src/main/res/values-th/strings.xml` | Cột `th` |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesStringsTest.kt` | Test chữ của `:feature:poses` (mục 4) |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemStringsTest.kt` | Test chữ của `:core:designsystem` (mục 4) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | `vi-` trong `qualifiers` của lớp, `UNFOLDED`, `SHORT_SCREEN` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | Như trên; thêm `default_ptBr_small` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | `card_stepCountUsesScreenLanguage_english`: "Solo · 5 steps" |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | `meta_countsStepsInScreenLanguage`: "Solo · Women · Full body · 5 steps"; thêm `smallScreen_portuguese_buttonFullyVisible` |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_default-ptbr_small.png` | Ảnh chuẩn mới, ghi ở bước 4 sau khi có bản dịch (không ghi ở bước 3) |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/NetworkDialogScreenshotTest.kt` | `vi-` trong hai `qualifiers` |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/NetworkDialogTest.kt` | `vi-` trong hai `qualifiers` |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt` | `vi-` trong `qualifiers` của lớp |
| Sửa | `scripts/tests/fixtures/device-scenario/grid.xml` | "Tất cả", "Đã lưu", "Lưu tư thế" thành "All", "Saved", "Save pose" |
| Sửa | `scripts/tests/fixtures/device-scenario/chips.xml` | "Tất cả", "Đã lưu" thành "All", "Saved" |
| Sửa | `scripts/tests/fixtures/device-scenario/chips-saved.xml` | Như trên |
| Sửa | `scripts/tests/run.sh` | Hai kịch bản `text=` và một mẫu log của khối `device-scenario` (`selected.txt`, `unselected.txt`) theo chữ mới của tệp mẫu |

**Nơi khác đọc file plan đổi (L-020):**
- `scripts/tests/run.sh` được `scripts/check.sh` chạy cả tệp, và được `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh` nhắc trong chú thích. Plan chỉ đổi chữ của hai lệnh `ds_script` và một mẫu log trong khối `device-scenario`; cách gọi, hàm `ds_run` và dòng tổng kết không đổi, nên các script đó không bị ảnh hưởng.
- Ba tệp mẫu chỉ được `scripts/tests/run.sh` đọc, qua `ds_run`.
- Ảnh chuẩn có sẵn được `scripts/check.sh` (`verifyRoborazziDebug`) và `scripts/design-compare.sh` đọc; plan không đổi ảnh nào.

Không file nào khác. Không sửa code Kotlin của `src/main`, không sửa `docs/features/poses/design/README.md` (bảng chữ của thiết kế là tiếng Việt và vẫn đúng ở `vi`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm (tên, bước, lỗi hay gặp của tư thế đến từ catalog, đã theo ngôn ngữ máy)
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Không có kỹ thuật mới: cách làm lặp lại đúng settings và onboarding, và đã chạy thử trọn vẹn (mục 4.1).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-4: thư viện và chi tiết tư thế hiện đúng 7 ngôn ngữ | Unit test (Robolectric) | `PosesStringsTest.everyReleaseLanguage_isTranslated`: với `hi`, `pt-BR`, `es-419`, `id`, `th`, `vi`, mọi khóa trong danh sách `TRANSLATED` (đủ 30 khóa của `values/`) không rỗng và khác chữ `en`. Dựng ngữ cảnh như `SettingsStringsTest.context(tag)` |
| Ngôn ngữ chưa có thì hiện tiếng Anh | Unit test | `PosesStringsTest.defaultLanguage_hasNoVietnamese`: ở `en`, không khóa nào có chữ cái riêng của tiếng Việt (`ăâđêôơư` và các nguyên âm có dấu, cả chữ hoa) |
| KB-4: hộp thoại mất mạng hiện đúng 7 ngôn ngữ | Unit test | `DesignSystemStringsTest.everyReleaseLanguage_isTranslated`, `DesignSystemStringsTest.defaultLanguage_hasNoVietnamese`: như trên với 5 khóa |
| Chữ thật trên màn ở ngôn ngữ khác tiếng Việt | Compose UI test | `PosesScreenTest.card_stepCountUsesScreenLanguage_english` ("Solo · 5 steps"); `PoseDetailScreenTest.meta_countsStepsInScreenLanguage` ("Solo · Women · Full body · 5 steps") |
| Chữ dài không bị cắt, nút chính không bị che (L-028) | Compose UI test, khung `pt-rBR-w360dp-h640dp-xxhdpi` | `PoseDetailScreenTest.smallScreen_portuguese_buttonFullyVisible`: chi tiết có 5 bước và hai lỗi hay gặp; nút "Fotografar com esta pose" nằm trọn trong màn cả chiều dọc lẫn chiều ngang |
| Màn hình thấp ở ngôn ngữ có chữ dài | Screenshot test khung `pt-rBR-w360dp-h640dp-xxhdpi` | `PoseDetailScreenScreenshotTest.default_ptBr_small`, `src/test/screenshots/PoseDetailScreen_default-ptbr_small.png`: trạng thái nhiều nội dung nhất (`longDetail()`). Tiếng Bồ Đào Nha vì nút chính và dòng phụ dài nhất trong các ngôn ngữ chữ Latinh ("Fotografar com esta pose", "Individual · ... · Corpo inteiro · 5 passos") |
| Đúng thiết kế (ảnh chuẩn không đổi) | Screenshot test đã có, nay ở `vi` | 28 ảnh của `:feature:poses`, 20 ảnh của `:core:designsystem` (4 của hộp thoại mất mạng, 16 của catalog) qua `verifyRoborazziDebug` không đổi. `scripts/check.sh` qua |
| Ảnh thiết kế của feature settings | Không đổi | Ảnh thiết kế của ticket khác (T-025): `SettingsScreen_default_light`, `SettingsScreen_default_dark`, `SettingsScreen_theme-picker_light`, `SettingsScreen_theme-picker_dark`. Ticket thuộc feature settings vì lỗi nằm ở KB-4, nhưng plan không chạm màn cài đặt |
| `str=` của kịch bản emulator vẫn khớp | Test script | `scripts/tests/run.sh`, khối `device-scenario`: "str= matches text and content-desc", "selected=true matches a text whose parent chip is selected", "selected=true stops at the clickable parent of an unselected chip" với tệp mẫu chữ tiếng Anh |
| Thư viện, chi tiết tư thế, hộp thoại mất mạng đổi ngôn ngữ trên máy | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`) | Hai kịch bản có sẵn dùng `str=` của poses, nên chạy qua ở `device-session` cuối phiên chứng tỏ app hiện chữ tiếng Anh của `values/` trên emulator `en-US`. Không thêm tệp kịch bản |
| Đổi ngôn ngữ trên máy thật, Android 13 trở lên và dưới 13 | Kiểm tra thủ công (máy thật: khác, trang ngôn ngữ của hệ thống và hộp chọn trong app; Nhật đã kiểm cơ chế trên Xiaomi Android 15) | Trên bản staging: chọn English rồi Tiếng Việt, mở thư viện, chi tiết, tắt mạng để hiện hộp thoại mất mạng; chữ đổi theo |
| Bản dịch đúng nghĩa, tự nhiên | Kiểm tra thủ công (người: người biết tiếng duyệt bản dịch trước khi phát hành, spec onboarding câu hỏi 1) | Đọc bảng dịch ở mục 3.1 |

**Thứ tự commit (L-025).** Commit test của bước 3 gồm:
- hai test chữ mới;
- mọi thay đổi `qualifiers` sang `vi-`;
- hai test đổi chữ khẳng định;
- `smallScreen_portuguese_buttonFullyVisible` và `default_ptBr_small`;
- ba tệp mẫu và `run.sh`.

Không ghi ảnh `PoseDetailScreen_default-ptbr_small.png` ở bước 3: ghi ở bước 4, sau khi có tệp chữ, bằng `scripts/check.sh` (tự ghi ảnh thiếu khi chạy trên máy). Kiểm ảnh có chữ tiếng Bồ Đào Nha ("Passos", "Fotografar com esta pose") trước khi commit.

**Thất bại đúng lý do ở bước 3** (đã chạy thật, mục 4.1):
- `PosesStringsTest` (2 test), `DesignSystemStringsTest` (2 test): "poses_title is Vietnamese", "hi poses_title is not translated", "brand_tagline is Vietnamese", "hi brand_tagline is not translated";
- `card_stepCountUsesScreenLanguage_english`, `meta_countsStepsInScreenLanguage`: không tìm thấy chữ tiếng Anh;
- `smallScreen_portuguese_buttonFullyVisible`: không tìm thấy "Fotografar com esta pose";
- ba test script: `str=` không khớp (`'0 0 0'`, `'0 0'`, `'0 1 0 0'`).

**Đối chứng, qua cả trước lẫn sau:**
- mọi screenshot test thêm `vi-`;
- `NetworkDialogTest`;
- hai test `text=` của `run.sh` ("selected=true matches selected or checked", "selected=true does not match an unselected chip").

Đối chứng chứng tỏ `vi` giữ ảnh chuẩn và tệp mẫu mới vẫn đọc đúng.

**Ngoại lệ viết test trước:** bảng dịch là tài nguyên chữ: test chữ chỉ kiểm có bản dịch và khác tiếng Anh, không kiểm nghĩa. Nghĩa do người biết tiếng duyệt (dòng cuối bảng trên). Tệp kịch bản emulator không "thất bại trước" được (`device-session` cuối phiên).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-25, trên `origin/main` `0dfd2a4`, trong worktree riêng, không commit.
  1. Viết hai test chữ, thêm `vi-` vào các `qualifiers` ở mục 3.1, đổi hai test `en` sang chữ tiếng Anh. Chạy trên code hiện tại.
  2. Đổi ba tệp mẫu và `run.sh`. Chạy `scripts/tests/run.sh` trên code hiện tại.
  3. Bản cài thử: sinh 12 tệp chữ và đổi hai `values/` theo đúng bảng ở mục 3.1. Thêm hai test tiếng Bồ Đào Nha, ghi ảnh mới, chạy `scripts/check.sh`.
- **Lệnh:**
  - `./gradlew :feature:poses:verifyRoborazziDebug :core:designsystem:verifyRoborazziDebug --continue`, trên code hiện tại;
  - `scripts/tests/run.sh`, trên code hiện tại;
  - `./gradlew :feature:poses:recordRoborazziDebug --tests '*PoseDetailScreenScreenshotTest.default_ptBr_small'`, trên bản cài thử;
  - `scripts/check.sh`, trên bản cài thử.
- **Kết quả trên code hiện tại:**
  - Gradle: 275 test, 6 thất bại, đúng 6 test dự kiến:
    - `PosesStringsTest.defaultLanguage_hasNoVietnamese` ("poses_title is Vietnamese: Thư viện tư thế");
    - `PosesStringsTest.everyReleaseLanguage_isTranslated` ("hi poses_title is not translated");
    - `DesignSystemStringsTest.defaultLanguage_hasNoVietnamese` ("brand_tagline is Vietnamese");
    - `DesignSystemStringsTest.everyReleaseLanguage_isTranslated` ("hi brand_tagline is not translated");
    - `PosesScreenTest.card_stepCountUsesScreenLanguage_english` (không có "Solo · 5 steps");
    - `PoseDetailScreenTest.meta_countsStepsInScreenLanguage` (không có "Solo · Women · Full body · 5 steps").
  - Mọi screenshot test thêm `vi-` qua `verify`, không ảnh nào đổi:
    - `PosesScreenScreenshotTest` 15/15, `PoseDetailScreenScreenshotTest` 13/13;
    - `NetworkDialogScreenshotTest` 4/4, `DesignSystemCatalogScreenshotTest` 16/16.
  - `NetworkDialogTest` (ở `vi`) qua.
  - `run.sh`: đúng 3 test thất bại:
    - "str= matches text and content-desc", `'0 0 0'` thay vì `'0 1 1'`;
    - "selected=true matches a text whose parent chip is selected", `'0 0'`;
    - "selected=true stops at the clickable parent of an unselected chip", `'0 1 0 0'`.
  - Hai test `text=` (đối chứng) qua. Lần chạy đầu quên đổi mẫu log `step=1 expect text=Tất cả selected=true FAIL` nên đối chứng "does not match an unselected chip" thất bại; đã thêm vào mục 3.2.
- **Kết quả trên bản cài thử:** `scripts/check.sh` qua (`== Cổng chất lượng: QUA`, Test JVM 1056 test, 0 lỗi; `:feature:poses` 163 test, `:core:designsystem` 114 test; `All script tests passed`).
  - Lần đầu `check.sh` dừng ở ktlint vì một dòng của test chữ dài quá 120 ký tự; viết tên khóa ra biến `name` trước `assertFalse` như `SettingsStringsTest` thì qua. Phiên viết test theo cách đó.
  - Ảnh mới `PoseDetailScreen_default-ptbr_small.png` (1080x1920) đã mở xem: thanh trên, dòng phụ "Casal · Nữ · Meio corpo · 5 passos", tiêu đề "Passos", nút "Fotografar com esta pose" nằm trọn, không chữ nào bị cắt. Tên và bước của tư thế trong ảnh là tiếng Việt vì dữ liệu giả của test chỉ có tiếng Việt, đúng như các ảnh `_small` khác.
  - `PoseDetailScreenTest.smallScreen_portuguese_buttonFullyVisible` chạy riêng trên code hiện tại: thất bại đúng lý do (không tìm thấy node "Fotografar com esta pose"); trên bản cài thử: qua.
  - Không ảnh chuẩn có sẵn nào đổi (94 ảnh, `git status` chỉ có ảnh mới). Bản cài thử đã bỏ, worktree trả về `origin/main`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7, module core dùng chung. Plan đổi tài nguyên chữ của `:core:designsystem`, nên hộp thoại mất mạng trên mọi màn đổi chữ theo ngôn ngữ. Plan cũng sửa ba tệp mẫu và `scripts/tests/run.sh` của cổng chất lượng. Không chạm điểm 2 (không thêm thư viện), điểm 3 (không thêm quyền), điểm 4 (không có sự kiện). Người duyệt thấy khác thì sửa `blocking_points`.

Điểm tham khảo (mức theo điểm chặn):

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:feature:poses`, `:core:designsystem`) và test của script |
| B. Độ rõ của yêu cầu | 1 | Spec và cách dịch đã chốt (spec onboarding câu hỏi 1); còn câu khẩu hiệu tiếng Anh (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 1 | Có bản dịch và không cắt chữ đều có test; nghĩa của bản dịch do người duyệt |
| D. Ảnh hưởng chéo | 2 | Chữ của `:core:designsystem` hiện trên mọi màn; `scripts/tests/run.sh` cũng do T-052 sửa trong cùng danh sách |
| E. Độ mới của kỹ thuật | 0 | Lặp lại đúng mẫu của settings và onboarding |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:**
  - `:core:designsystem`: `NetworkDialogScreen` hiện chữ theo ngôn ngữ trên mọi màn (vỏ app vẽ nó qua `NetworkDialogHost`);
  - ba tệp mẫu `scripts/tests/fixtures/device-scenario/{grid,chips,chips-saved}.xml`;
  - khối `device-scenario` của `scripts/tests/run.sh`.
- **Task khác trong danh sách có thể xung đột:**
  - **T-050** (chữ của camera, plan viết song song): sửa `:feature:camera`, không chạm file nào của plan này. Mỗi module có test chữ riêng, không có hàm dùng chung giữa hai plan. Hai plan không cùng sửa file nào.
    - Nếu plan T-050 cũng phải sửa tệp mẫu hay `run.sh` (camera không có `str=` trong test script lúc viết plan này), hai plan sửa khác dòng.
    - Task chạy sau tạo nhánh từ nhánh chính mới nhất.
  - **T-052** (trình chạy kịch bản, quy tắc 11): có thể thêm test vào cùng khối `device-scenario` của `scripts/tests/run.sh`, và có thể sửa `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`, `docs/ops/test-sessions.md`.
    - Plan này chỉ sửa hai lệnh `ds_script` (`selected.txt`, `unselected.txt`) và một mẫu log của `unselected.txt`, không thêm khối, không sửa tệp kịch bản hay tài liệu.
    - T-052 chạy trước nên T-051 gộp đoạn của T-052.
  - **T-053** (`token-usage.py`): không chung file.
- **Đề xuất thứ tự chạy:** cuối danh sách, sau T-053, T-052, T-050 (thứ tự Tuấn đề ra). Plan độc lập: không phụ thuộc task nào, không task nào phụ thuộc nó.
- **Việc sau task này, không làm ở đây:**
  - `str=` của kịch bản emulator đọc `values/`, nay là tiếng Anh với cả settings, onboarding, loading và (sau T-051) poses. Trên máy thật đặt tiếng Việt (`scripts/device-session.sh scenarios --yes` trên máy dev), app hiện tiếng Việt nên `str=` không khớp.
    - Giới hạn này đã có với settings, onboarding và loading trước plan này; CI không bị, vì emulator chạy `en-US`.
    - Đề xuất về sau (ticket của quality-gate): trình chạy đặt ngôn ngữ app `en-US` khi bắt đầu mỗi tệp, hoặc `docs/ops/test-sessions.md` mục 5 ghi rõ máy chạy kịch bản phải để tiếng Anh. Báo cáo phiên ghi vào Đề xuất cho buổi họp tiếp theo.
  - Người biết tiếng duyệt bản dịch của 5 ngôn ngữ trước khi phát hành (spec onboarding, câu hỏi 1). Báo cáo phiên ghi vào "Cần người kiểm tra".

## 7. Điều kiện dừng riêng

1. `verifyRoborazziDebug` báo một ảnh chuẩn có sẵn đổi (ngoài ảnh mới `PoseDetailScreen_default-ptbr_small.png`): dừng, không ghi lại ảnh. Mục 4.1 cho thấy `vi-` giữ nguyên cả 48 ảnh, nên ảnh đổi nghĩa là có chỗ khác chưa thấy.
2. Một test ngoài mục 3.2 thất bại vì chữ đổi sang tiếng Anh (ví dụ test của module khác vẽ `NetworkDialogScreen`): dừng, ghi tên test vào nhật ký, không sửa test đó.

## 8. Câu hỏi cho người duyệt

1. **Câu khẩu hiệu tiếng Anh `brand_tagline`.** Câu tiếng Việt hiện có là "Mọi việc vào đúng nhịp." (câu mẫu của dự án mẫu, ADR-0008: mỗi app thay bằng câu của mình). Câu này chỉ hiện ở trang `Brand` của catalog hệ thống thiết kế, không hiện trong app.
   - **Đề xuất của AI:** dịch sát, "Everything falls into rhythm.", các ngôn ngữ khác theo bảng ở mục 3.1. Ảnh chuẩn không đổi vì catalog chụp ở `vi`.
   - Phương án khác: người duyệt ghi câu tiếng Anh khác vào mục 9; phiên dùng đúng câu đó cho `values/` và giữ bản dịch các ngôn ngữ khác theo nghĩa câu mới (phiên dịch lại 5 ô của dòng này). Test chữ không đổi.
2. **Số ít, số nhiều.** Tiếng Anh, Bồ Đào Nha, Tây Ban Nha và Hindi phân biệt số ít và số nhiều, còn ba chuỗi đếm (`poses_summary`, `poses_card_meta`, `poses_detail_meta*`) đang là chuỗi thường.
   - **Đề xuất của AI:** giữ chuỗi thường, viết số nhiều ("5 steps"). Spec poses ghi mỗi tư thế có 3 đến 5 bước, và catalog có nhiều tư thế, nhiều nhóm, nên số 1 không xảy ra. Không đổi code, không thêm kỹ thuật mới.
   - Phương án khác: `<plurals>` với `pluralStringResource`. Cách này phải sửa ba chỗ gọi trong `PosesScreen.kt`, `PosesComponents.kt`, `PoseDetailComponents.kt`, viết plan phiên bản 2 và chạy thử lại.
3. **Bản dịch do AI làm.** Bảng ở mục 3.1 là bản phiên sẽ dùng nguyên văn.
   - **Đề xuất của AI:** duyệt plan với bảng này. Người biết tiếng sửa từ nào thì sửa thẳng vào bảng trước khi duyệt, hoặc sửa tệp chữ sau phiên bằng PR riêng; test chữ không phải đổi.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
