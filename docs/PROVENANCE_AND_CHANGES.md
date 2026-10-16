# Nguồn và mức độ cải tiến

## Bản 0.1

Nền tảng kỹ thuật tham khảo chính là [dingwen07/hyperos-fcm-fix](https://github.com/dingwen07/hyperos-fcm-fix), commit `fe594239b01ba4b67d60da764683cadd13e5d287`.

`XiaomiRules.java` là bản chuyển sang Java của `MilletNoRestrictList.kt` và `AurogonConfig.kt`, gồm giữ danh sách hiện có, thêm GMS, ghép quy tắc broadcast FCM và đánh dấu quy tắc do app quản lý. Việc chuyển ngôn ngữ và đổi marker không tạo ra cơ chế sửa thông báo mới. Giấy phép và ghi nhận tác giả ở THIRD_PARTY_NOTICES.md.

Các phần viết cho NotiGuard gồm UI Java, cấu hình, Binder UserService/client, command runner với giới hạn, snapshot/hoàn tác, job khôi phục và kiểm thử. Đây là triển khai riêng các chức năng quản trị; không có bằng chứng những chức năng này vượt trội mọi mặt so với upstream. Shizuku API là thư viện của RikkaApps, không phải mã tự viết.

Trên Xiaomi 14 đã thử, khóa Aurogon không tồn tại nên 0.1 bỏ qua phần đó. Chế độ mặc định thực tế chỉ duy trì mục GMS trong MILLET. Chưa có phép đo chứng minh nhận push tốt hơn upstream hoặc giảm hao pin.

## Bản 0.2

Phần bổ sung riêng là `SettingsObserver.java`, `RepairPolicy.java` và phối hợp trong `GuardUserService`:

- Đăng ký đúng URI của các khóa đã nhận diện qua ContentService ở tiến trình Shizuku. Không theo dõi nội dung thông báo hay dữ liệu ứng dụng.
- Khi có thay đổi, gom sự kiện trong 400 ms rồi đọc kiểm tra; chỉ ghi nếu cần sửa. Ghi của chính app có thể tạo một lần đọc theo sau nhưng không tạo vòng ghi vô hạn khi cấu hình đã ổn định.
- Chế độ cân bằng chỉ đổi kiểm tra dự phòng từ 120 thành 900 giây sau khi observer đã nhận được ít nhất một sự kiện thực tế. Đăng ký được nhưng chưa nhận sự kiện vẫn giữ 120/300 giây. Đây là giảm tần suất gọi lệnh định kỳ, không phải số đo pin hoặc cam kết độ trễ thông báo.
- Đăng ký thất bại hoặc ContentService chết: quay về chu kỳ 120/300 giây đã chọn. Khi bật chế độ miễn Doze, vẫn giữ chu kỳ này cho các AppOp không thuộc hai URI theo dõi.
- Nếu bị ghi đè hoặc thao tác thất bại liên tục, thời gian gom sự kiện tăng từ 400 ms lên 5 rồi 30 giây để tránh sửa liên tục; trở lại bình thường sau cửa sổ 60 giây. Có đánh đổi tốc độ sửa trong trường hợp tranh chấp.
- Bộ đếm lệnh và nguồn kiểm tra giúp đối chiếu phản ứng theo sự kiện và số lệnh lúc ổn định.

Binder interface được đối chiếu với AOSP, không nhập thêm thư viện runtime:

- [IContentService.aidl, android16-release](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/content/IContentService.aidl).
- [IContentObserver.aidl, android16-release](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/database/IContentObserver.aidl).

Các tệp này được dùng để xác định giao thức có sẵn trong framework; không sao chép triển khai ContentService. Đây là API nội bộ và có thể khác trên ROM/Android khác. Đăng ký thành công cần được kiểm chứng thêm bằng sự kiện thực tế. Khi máy ngủ sâu, callback và công việc vẫn có thể bị trì hoãn; không giữ wake lock để ép thực thi.

0.2 tiếp tục cùng cơ chế miễn hạn chế GMS; không phải giải pháp mới cho mọi loại push. Chưa đo được tỷ lệ thông báo mất, độ trễ từ máy chủ hoặc pin qua đêm.

**Kết quả thực tế trên Xiaomi 14:** ContentService từ chối đăng ký của tiến trình shell với `Failed to find PID`. Observer không hoạt động trên ROM này; đã kiểm tra bản chạy thực tế quay về polling 120 giây. Không tính phần thử nghiệm này là tối ưu đã đạt được.

## Bản 0.3 / 0.3.1

Viết thêm DeviceDiagnostics và màn hình thao tác theo từng app để kiểm tra các nhóm nguyên nhân ngoài MILLET. Đây là chẩn đoán và dẫn đến cài đặt, không phải thuật toán phục hồi push của mọi ứng dụng.

Thử WRITE_SECURE_SETTINGS cấp một lần: app được cấp quyền nhưng SettingsProvider từ chối ghi lại cùng giá trị MILLET bằng UID app. Đã thu hồi quyền và loại bỏ cả permission lẫn mã probe khỏi 0.3.1. Không phát hành tuyên bố chạy độc lập Developer Options dựa trên phép thử thất bại.

Thêm kiểm tra trạng thái Developer/ADB và RPC Shizuku thật; không giả mạo cờ hoặc sửa app ngân hàng. Người dùng đã thực hiện phép thử tắt Developer Options: RPC mất kết nối và GMS không còn trong MILLET. Không xem cách khởi động Shizuku trước rồi tắt Developer Options là giải pháp dùng được trên máy này. Yêu cầu hoạt động khi Developer Options tắt và tương thích mọi ngân hàng chưa được đáp ứng. Tham khảo docs/NOTIFICATION_CAUSES.md để xem nguyên nhân và giới hạn.

## Bản 0.4

Viết thêm AppSettingsWatch để dùng API ContentObserver công khai trong tiến trình app đã đăng ký với Android, thay vì chỉ dựa vào observer shell đã thất bại. Callback gửi requestCheck không tham số tới phiên hiện hành, qua kiểm tra UID và gom sự kiện/backoff. Không tăng watchdog lên 15 phút từ sự kiện app vì tiến trình app có thể bị giết. Chưa có thiết bị ở lượt build này để xác minh callback trên ROM Xiaomi.

Viết luồng Chỉ kiểm tra: hoàn tác trước, không tự nối Shizuku hoặc lập job sửa, mở màn hình cấu hình GMS của hệ thống theo lựa chọn người dùng. Không nhập thư viện mới, không sao chép mã UI của ứng dụng khác, không ghi cơ sở dữ liệu PowerKeeper. Các intent Xiaomi không phải API ổn định; resolve và kiểm tra system/exported/permission trước, có trang thông tin app chuẩn làm fallback.

Điều tra upstream đã ghim commit mô tả việc PowerKeeper sinh MILLET từ bảng noRestrict riêng và việc ROM ẩn bộ chọn cho package không có launcher. Đó là cơ sở để thử cấu hình qua UI, không phải bằng chứng đã mở được bộ chọn GMS trên máy này. Không tự gỡ bộ lọc ẩn hoặc hạ target SDK để né giới hạn SettingsProvider.

OfflineCheck cùng kiểm thử mới phân biệt mẫu khi còn sửa nền, dữ liệu không đọc được, thiếu GMS, còn gỡ lỗi, hai mẫu cách nhau 2 phút và mẫu sau reboot. Các mẫu chỉ là quan sát cấu hình tại thời điểm kiểm tra. Chưa xác nhận đường cấu hình này hoạt động, push đến đúng hạn, pin giảm hoặc ngân hàng chấp nhận. Bản phát hành là preview để thử ở máy công ty.
