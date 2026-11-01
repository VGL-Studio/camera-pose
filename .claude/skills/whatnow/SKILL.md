---
name: whatnow
description: Xem tình hình hiện tại của repo (nhánh, phiên tự chạy, PR, issue chặn, việc chờ người, hàng chờ plan) và gợi ý vài việc nên làm tiếp, mỗi việc kèm command của quy trình. Dùng khi dev hỏi "việc tiếp theo là gì", "đang tới đâu rồi", hay mới mở phiên.
---

# /whatnow: tình hình và việc nên làm tiếp

Lệnh này chỉ đọc. Không sửa file, không commit, không đẩy, không merge, không bình luận trên GitHub. Chỉ ngoại lệ: `scripts/status.py` ghi lại `docs/STATUS.md` trên máy (file không có trong git).

## 1. Thu thập

Chạy các lệnh độc lập song song. Lệnh nào lỗi (không có mạng, chưa đăng nhập `gh`) thì ghi "chưa kiểm được" cho phần đó, không đoán.

1. **Repo:** có `docs/ops/new-app.md` là dự án mẫu, không có là repo app. `git fetch -q origin`, rồi `git status --short`, nhánh hiện tại, số commit hơn và kém `origin/main`, `git worktree list`.
2. **Điểm chặn:** `gh issue list --state open --label main-broken`, cùng nhãn `env-broken`, `setup-broken` và `session-stalled`; nhãn `flaky` chỉ ghi ở phần tình hình. Có issue `main-broken` thì mọi gợi ý khác xếp sau.
3. **Phiên tự chạy:** liệt kê nhánh `claude/RUN-YYYYMMDD-NN-report` trên remote; phiên đang chạy khi file `docs/runs/<RUN>.md` trên nhánh đó có `status: running` (cùng cách `scripts/merge-if-green.sh` kiểm). Trên nhánh chính, danh sách `draft` hay `approved` chưa chạy trong `docs/runs/`.
4. **PR đang mở:** `gh pr list --state open --json number,title,headRefName,isDraft,labels,reviewDecision,statusCheckRollup`. Tách PR có nhãn `needs-human`, PR nháp, PR có CI đỏ, PR xanh chờ merge.
5. **Việc chờ người:** `python3 scripts/status.py`, rồi đọc mục 1 "Đang chờ người" và mục 6 "Sẵn sàng phát hành" của `docs/STATUS.md`.
6. **Hàng chờ plan:** `python3 scripts/plan-queue.py --max 5`. Thêm ticket `stopped` và plan `draft` chưa duyệt.
7. **Chỉ ở dự án mẫu:** `gh issue list --state open --label from-app`.
8. **Chỉ ở repo app:** dòng `base=` của `docs/template-sync.txt`, rồi đếm commit của dự án mẫu kể từ mốc đó nếu đọc được repo mẫu (`gh api repos/VGL-Studio/app-project-template/compare/<base>...main --jq .ahead_by`).

## 2. Báo cho dev

Viết ngắn, tiếng Việt, theo thứ tự:

1. **Tình hình:** vài dòng, chỉ những gì đáng chú ý. Phần sạch thì gộp một dòng ("không có PR mở, không có issue chặn").
2. **Nên làm tiếp:** tối đa ba việc, xếp theo mức gấp. Mỗi việc một dòng: việc gì, vì sao bây giờ, command nên gọi. Thứ tự ưu tiên:
   1. Nhánh chính đỏ, môi trường hỏng, phiên đứng im: báo người, không tự sửa issue `main-broken`.
   2. Phiên tự chạy đang chạy: chỉ theo dõi, không merge PR ngoài phiên.
   3. PR xanh chờ merge: `/merge-pr <số>`. PR đỏ: xem log bằng `scripts/failure-kind.sh --playbook`.
   4. Việc chờ người ở `docs/STATUS.md` (duyệt spec, plan, danh sách chạy, kiểm thủ công).
   5. Ticket trong hàng chờ: `/write-plan <T-NNN>`; việc nhỏ dev muốn có ngay: `/quick-lane`.
   6. Plan `approved` chưa vào danh sách: `/run-list`.
   7. Dự án mẫu: issue `from-app` đang mở: `/from-app <số>`. Repo app: dự án mẫu đã có commit mới: nhắc người phụ trách chạy `/to-app` ở dự án mẫu.
   8. Phản hồi `new`: `/triage-feedback`; bài học `accepted` chưa làm: `/lesson`.
3. **Dọn dẹp** (nếu có): worktree hay nhánh cục bộ của PR đã merge, nhánh hiện tại cũ. Chỉ gợi ý, không tự xóa.

## 3. Các command của quy trình

Khi dev hỏi có những command gì, đưa bảng này, rồi thêm các skill khác có trong `.claude/skills/` (command riêng của app) kèm dòng `description` của chúng:

| Command | Việc |
| --- | --- |
| `/whatnow` | Tình hình và việc nên làm tiếp |
| `/spec <feature>` | Từ yêu cầu ra spec `draft` |
| `/tickets <feature>` | Tách spec thành ticket |
| `/write-plan <T-NNN>` | Viết plan `draft` cho một ticket |
| `/run-list` | Lập và kiểm danh sách chạy |
| `/quick-lane <việc>` | Đường làm nhanh có dev ngồi cùng |
| `/pr-review [PR]` | Rà soát độc lập trước khi báo PR xong |
| `/merge-pr <PR>` | Merge bằng `scripts/merge-if-green.sh` |
| `/test-session <TS>` | Phân tích một phiên thử trên máy |
| `/triage-feedback` | Phân loại phản hồi |
| `/adr <chủ đề>` | Viết ADR, sinh lại `docs/foundation.md` |
| `/lesson <chuyện>` | Ghi bài học, làm bài học `accepted` |
| `/from-app <issue>` | Đưa cải tiến từ app về dự án mẫu |
| `/to-app <thư mục app>` | Đồng bộ dự án mẫu về một app |
| `/clone-study` | Khảo sát app mẫu cho app clone |
