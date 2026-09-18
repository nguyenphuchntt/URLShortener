Bạn thực hiện code toàn bộ logic của feature được nhắc tới.

Các bước thực hiện:

Đầu tiên tìm xem có các phần nào đã được thực hiện trong công việc được nhắc tới.

1. Lên kế hoạch chi tiết làm những APIs nào vào trong docs/backend/<feat_name>/apis.md cùng yêu cầu cần có của các API
2. Thực hiện viết các lớp DTO để định dạng request và response
3. Viết các interface/ method vào repository nếu cần để thực hiện các tác vụ với database
4. Viết các signature của method cần thiết vào interface service, sau đó viết class concrete vào impl/ cần thiết để hiện thực API, thực hiện đảm bảo các vấn đề về bảo mật, throw exception nếu cần để thực hiện xử lý tập trung
5. Viết các API vào trong controller

Sau khi thực hiện xong, thực hiện security và bug logic check bằng cách fan out ra 2 agents.