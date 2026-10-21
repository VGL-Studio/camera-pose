---
id: T-NNN
title: <tên ngắn của task>
feature: <ten-feature>
spec: features/<ten-feature>/spec.md
scenarios: [KB-1, KB-2]      # kịch bản trong spec mà ticket này xử lý
owner: <chủ feature>
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: low                    # đánh giá sơ bộ: low | medium | high
size: M                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-010]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
created: YYYY-MM-DD
---

# T-NNN: <TÊN TASK>

## Mô tả

<Hai đến bốn câu: task này làm ra phần nào của feature, người dùng thấy gì sau khi xong.>

## Phạm vi

- <phần việc cụ thể>
- <phần việc cụ thể>

**Không bao gồm:**

- <phần thuộc ticket khác, ghi rõ mã ticket nếu có>

## Rủi ro sơ bộ

<Một đến hai câu. Ghi rõ nếu chạm điểm chặn trong risk-scoring.md (quảng cáo, SDK, quyền truy cập, dữ liệu, module core...).>

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.

<!--
Cỡ task. Plan dựa vào cỡ để ghi token_estimate; số token thật theo cỡ ở docs/metrics/token-usage.csv (docs/ops/token-usage.md):
S: thay đổi nhỏ trong một module, lặp lại mẫu có sẵn.
M: một màn hình hoặc một luồng xử lý mới trong một module.
L: nhiều màn hình hoặc nhiều phần. Ticket cỡ L nên cân nhắc tách nhỏ, vì phải chạy xong trong một lượt của phiên tự chạy.
-->
