---
ticket: T-035
title: java-check.sh đọc đúng phiên bản Java khi môi trường đặt JAVA_TOOL_OPTIONS
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-035: JAVA-CHECK.SH ĐỌC ĐÚNG PHIÊN BẢN JAVA KHI MÔI TRƯỜNG ĐẶT JAVA_TOOL_OPTIONS

## 1. Tóm tắt

`scripts/java-check.sh` đọc dòng có `version "` trong đầu ra của `java -version` thay vì dòng đầu, nên các dòng `Picked up JAVA_TOOL_OPTIONS: ...` hay `Picked up _JAVA_OPTIONS: ...` mà JVM in trước không làm script từ chối. Test với `java` giả in các dòng đó, viết trước, thất bại trên code hiện tại; mọi test cũ của `java-check.sh` giữ nguyên.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-049 (`docs/lessons.md`), từ RUN-20261023-03.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-049 | Khi `java -version` in `Picked up JAVA_TOOL_OPTIONS: ...` trước dòng phiên bản thì `java-check.sh` đọc đúng phiên bản và cho qua nếu nằm trong 17 đến 24 | JVM in dòng `Picked up` ra stderr trước mọi thứ khác, cho cả `JAVA_TOOL_OPTIONS`, `_JAVA_OPTIONS` và `JDK_JAVA_OPTIONS` (chỉ với lệnh `java`); có thể nhiều dòng. Lấy dòng đầu tiên khớp ` version "` |
| L-049 | Phiên bản ngoài khoảng, Java 8, không có `java`, không đọc được phiên bản: vẫn từ chối như hiện nay | Không đổi phần so sánh và thông báo; thông báo "không đọc được phiên bản" in dòng đầu của đầu ra như cũ khi không có dòng nào khớp |

Thiết kế tham chiếu: Không có thiết kế riêng (script, docs/ops/design-workflow.md).

ADR liên quan: ADR-0010 (cổng chất lượng `scripts/check.sh`).

## 3. Giải pháp

### 3.1. Cách làm

Trong `scripts/java-check.sh`, thay

`version_line="$("$java_cmd" -version 2>&1 | head -n 1)"`

bằng: lưu cả đầu ra (`output="$("$java_cmd" -version 2>&1)"`); không chạy được hay đầu ra rỗng thì từ chối như cũ; `version_line="$(grep -m1 ' version "' <<< "$output" || head -n 1 <<< "$output")"`. Phần đọc số (`sed`), Java 8 (`1.8.0_392`) và khoảng 17 đến 24 giữ nguyên. Khi không có dòng khớp, `version_line` là dòng đầu nên thông báo lỗi giống hiện nay.

Kiểm tra lúc lập plan (2026-10-23, máy dev, JDK 21): `JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=x" scripts/java-check.sh` in `LỖI: không đọc được phiên bản Java từ 'Picked up JAVA_TOOL_OPTIONS: -Dhttps.proxyHost=x'` và thoát 1; `java -version` khi đó in `Picked up JAVA_TOOL_OPTIONS: -Dx=1` rồi `openjdk version "21.0.13" 2026-10-20 LTS`. Tái hiện đúng lỗi của phiên.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/java-check.sh` | Lấy dòng có ` version "` thay vì dòng đầu (mục 3.1); thêm một dòng chú thích nhắc L-049 |
| Sửa | `scripts/tests/run.sh` | `fake_java` nhận thêm dòng in trước (tham số thứ hai, tùy chọn); ba test mới ở mục 4. Hàm phụ trợ và test vào cùng commit test của bước 3: `fake_java` cũ vẫn chạy như trước khi không có tham số thứ hai (L-025) |
| Sửa | `docs/lessons.md` | L-049: dòng Áp dụng ghi T-035 khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-035-java-check-java-tool-options.md` | Trạng thái theo phiên |

Nơi khác đọc `java-check.sh`: chỉ `scripts/check.sh` (dòng 28) gọi nó (`grep -rn java-check scripts/ .github/`); `check.sh` không đổi. CI (`pr-check.yml`, `main-guard.yml`) chạy `check.sh` với `setup-java` không đặt `JAVA_TOOL_OPTIONS`, nên kết quả trên CI không đổi.

Nơi khác nhắc tới file plan sửa, không bị ảnh hưởng: `scripts/status.py` đọc `docs/lessons.md` để đếm bài học theo trạng thái; plan chỉ thêm chữ vào dòng Áp dụng của L-049, không đổi tiêu đề hay dòng Trạng thái. `scripts/wait-run.sh` chỉ nhắc `scripts/tests/run.sh` trong chú thích (test của nó nằm ở đó); plan không đổi phần test của `wait-run.sh`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| L-049, dòng `Picked up JAVA_TOOL_OPTIONS` trước phiên bản 21 | Test script | `java-check: Java 21 after a Picked up JAVA_TOOL_OPTIONS line passes`: `fake_java 'openjdk version "21.0.12.1" 2026-07-21' 'Picked up JAVA_TOOL_OPTIONS: -Dhttps.proxyHost=proxy'` thoát 0, in `Java 21: dùng được`. Thất bại trên code hiện tại (thoát 1, "không đọc được phiên bản"), đã thấy ở mục 3.1 |
| L-049, nhiều dòng `Picked up` | Test script | `java-check: Java 17 after two Picked up lines passes`: hai dòng `Picked up JAVA_TOOL_OPTIONS: ...` và `Picked up _JAVA_OPTIONS: ...` trước phiên bản 17, thoát 0 |
| L-049, phiên bản ngoài khoảng sau dòng `Picked up` vẫn bị từ chối | Test script | `java-check: Java 26 after a Picked up line is refused`: thoát 1, có `LỖI: Java 26`. Đối chứng cho hai test trên: script không cho qua chỉ vì bỏ dòng đầu (L-035) |
| Hành vi cũ giữ nguyên | Test script có sẵn | Java 17, 21 qua; 26, 1.8, không có `java` bị từ chối; `check.sh` gọi `java-check.sh` trước Gradle |
| `check.sh` chạy được trên môi trường cloud | Kiểm tra ở phiên sau | Kiểm tra đầu phiên của danh sách chạy kế tiếp: `scripts/check.sh` qua bước `java-check.sh` (in `Java 21: dùng được`) |

**Ngoại lệ viết test trước:** Không có.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, cổng chất lượng): `scripts/check.sh` gọi script này trước Gradle.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 0 | Một dòng của một script, ba test |
| B. Độ rõ của yêu cầu | 0 | Lỗi tái hiện được, cách sửa đã nêu ở L-049 |
| C. Khả năng kiểm chứng tự động | 1 | Test với `java` giả; môi trường cloud thật chỉ kiểm ở phiên sau |
| D. Ảnh hưởng chéo | 2 | Mọi lần chạy `check.sh`, trên máy dev, CI và phiên |
| E. Độ mới của kỹ thuật | 0 | `grep` trên đầu ra đã đọc |
| **Tổng** | 3 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/java-check.sh` (cổng chất lượng), `scripts/tests/run.sh`.
- Task khác trong sprint có thể xung đột: không plan nào khác sửa hai file này.
- Đề xuất thứ tự chạy: làm ngay trong phiên có người theo dõi, trước khi lập lại danh sách chạy cho 8 task của RUN-20261023-03; nếu đưa vào danh sách chạy thì phải đứng đầu, nhưng kiểm tra đầu phiên chạy `check.sh` trước task đầu nên phiên sẽ dừng lại trước khi tới task này.

## 7. Điều kiện dừng riêng

Không có.

## 8. Câu hỏi cho người duyệt

1. **Làm ở đâu.** Phiên tự chạy không vượt được kiểm tra đầu phiên khi `check.sh` còn lỗi này, nên task không thể chạy trong phiên. Đề xuất của AI: làm trong phiên có người theo dõi ngay sau khi duyệt, merge qua `scripts/merge-if-green.sh`, rồi lập danh sách chạy RUN-20261023-04 với 8 task cũ (plan và ticket vẫn `approved`).
2. **Phát hiện sớm hơn.** Lỗi chỉ lộ ra ở đầu phiên vì không kiểm tra nào trước phiên chạy `check.sh` trên môi trường cloud: `preflight.yml` và `start-run.yml` chạy trên máy của GitHub, không có `JAVA_TOOL_OPTIONS`. Đề xuất của AI: ghi thành bài học riêng sau khi sửa (cùng hai bài học đang chờ về GitHub không phát sự kiện push và `start-run.yml` không kiểm `main-guard`), để quyết có cần một bước chạy `java-check.sh` trong `cloud-env-check.sh` hay không; không làm trong ticket này.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. L-049 chuyển sang accepted.

## 10. Nhật ký thực hiện

### 2026-10-23, phiên có người theo dõi (Tuấn)

- Bước 3: ba test mới trong `scripts/tests/run.sh` thất bại đúng lý do trên code cũ (thoát 1, "không đọc được phiên bản Java từ 'Picked up ...'"); commit test riêng.
- Bước 4: `scripts/java-check.sh` lấy dòng đầu tiên có ` version "`, không có thì dòng đầu như cũ. Toàn bộ test của script qua ("All script tests passed"). Trên máy dev, `JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=x" scripts/java-check.sh` in "Java 21: dùng được với Gradle 8.14.3."
- `scripts/check.sh` đầy đủ chạy trên CI của PR (`pr-check.yml`).

### 2026-10-23, sau khi merge

- PR #69 merge qua `scripts/merge-if-green.sh` (commit 6127a97, nhánh chính a0d561d); `pr-check.yml` xanh (lần chạy 39991729362). Ticket chuyển sang `on-staging`, L-049 sang `applied`.
