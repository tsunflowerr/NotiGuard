# Phạm vi bảo mật

Đây là bản thử nghiệm, chưa được kiểm toán độc lập. Không thể chứng minh phần mềm không có mã độc chỉ bằng việc nó là mã nguồn mở hoặc build thành công.

## Giảm bề mặt quyền

- Không khai báo Internet, quyền đọc SMS/danh bạ/tệp, NotificationListener hoặc AccessibilityService. Không có analytics, quảng cáo, server hay tự tải bản cập nhật.
- Từ 0.3 có ACCESS_NETWORK_STATE để đọc trạng thái mạng/VPN/Data Saver; đây không phải quyền Internet và không thực hiện kết nối thử. Bản 0.3.1 không yêu cầu WRITE_SECURE_SETTINGS; quyền này chỉ được cấp tạm cho thử nghiệm ghi cùng giá trị rồi thu hồi khi ROM từ chối.
- Chỉ liệt kê app có launcher/receiver FCM và các package hệ thống đã chỉ định. Không yêu cầu QUERY_ALL_PACKAGES.
- Không đọc nội dung thông báo, tài khoản, OTP hay dữ liệu app ngân hàng. Báo cáo cục bộ chứa tên package và kết quả cấu hình.
- Lưu cấu hình/snapshot trong SharedPreferences riêng của app. Backup bị tắt. Báo cáo chỉ ra clipboard nếu người dùng bấm sao chép.
- Shizuku provider được bảo vệ bằng INTERACT_ACROSS_USERS_FULL. Không có service privileged Android export tùy ý; UserService cấp bởi Shizuku và kiểm tra Binder caller UID phải là UID của NotiGuard.
- AIDL chỉ có inspect/start/stop/status, requestCheck và destroy. requestCheck không nhận lệnh hoặc cấu hình; chỉ yêu cầu kiểm tra phiên hiện hành với cùng kiểm tra UID, gom sự kiện và backoff. Không có API chạy chuỗi lệnh tùy ý. Package đầu vào kiểm tra cú pháp, số app tối đa 30, chu kỳ chỉ 120 hoặc 300 giây.
- Từ 0.2 có Binder callback riêng chỉ nhận hai thông điệp ContentObserver từ UID system_server, kiểm tra URI thuộc danh sách đăng ký và user 0/USER_ALL. Không có endpoint công khai để gửi lệnh vào callback; callback chỉ yêu cầu kiểm tra cấu hình đã chọn. ContentService chết thì quay lại kiểm tra định kỳ.
- ProcessBuilder sử dụng argv cố định, không dùng `sh -c`, `su`, eval hay nội suy lệnh. Kết quả giới hạn 64 KB, mỗi lệnh timeout 8 giây.
- Từ 0.4 có ContentObserver trong tiến trình app, chỉ đăng ký MILLET/Aurogon khi đang bảo vệ. Giới hạn một yêu cầu chờ trong hàng đợi client và một lần kiểm tra sự kiện chờ ở shell. Tiến trình app chết thì vẫn còn watchdog shell khi dịch vụ có quyền chạy.
- Chế độ Chỉ kiểm tra không tự kết nối UserService hoặc lập job sửa. Chỉ chuyển sau hoàn tác thành công; snapshot vẫn giữ nếu mất kết nối. Mẫu kiểm tra chỉ chứa cờ gỡ lỗi, hiện diện GMS, boot counter và thời gian monotonic, không dùng chúng làm chứng nhận tương thích ngân hàng.
- Nút cấu hình pin chỉ mở thành phần exported/enabled thuộc app hệ thống com.miui.securitycenter/com.miui.powerkeeper đã kiểm tra quyền gọi; ghim component sau resolve. Chỉ gửi package/nhãn/UID của GMS. Nếu không mở được, dùng trang thông tin app chuẩn. Không viết DB PowerKeeper, không sửa danh sách ẩn hoặc cờ gỡ lỗi.

## Mã và dependency

Xem các dependency trực tiếp trong app/build.gradle. Chỉ API/provider Shizuku và dependency annotation được đóng vào app. Build tools đến từ Gradle/Google/Maven Central; wrapper/distribution/JDK được đối chiếu checksum. Hai thuật toán chuyển từ nguồn được ghim commit, có kiểm thử giữ quy tắc bên thứ ba và hoàn tác. Không chạy APK/script từ repo tham khảo.

Khóa SHA256 dependency là ghi nhận lần tải đầu từ các kho trên; không thay thế việc kiểm tra nhà xuất bản, kiểm toán mã hoặc xác thực chữ ký độc lập. Không có kết quả quét phần mềm độc hại hay tuyên bố chứng nhận bảo mật.

## Rủi ro còn lại

- Shizuku có quyền shell mạnh. Lỗi logic có thể làm thay đổi cấu hình chạy nền hoặc tăng hao pin dù app không có Internet.
- ROM có thể không thực thi một setting dù đọc/ghi được. App báo riêng lỗi và setting chưa hỗ trợ, không suy luận tỷ lệ nhận thông báo từ trạng thái cấu hình.
- Theo dõi sự kiện dùng giao thức nội bộ Android đối chiếu AOSP Android 16; các ROM có thể thay đổi. Chu kỳ 15 phút chỉ kích hoạt sau khi đã nhận sự kiện. Một sự kiện thành công không bảo đảm mọi sự kiện sau đó được giao đúng lúc khi ngủ sâu; vẫn có kiểm tra dự phòng.
- Hoàn tác chỉ theo dõi baseline và các mục app quản lý; không có compare-and-swap nguyên tử với SettingsProvider hay sở hữu riêng AppOp của hệ thống.
- Nếu gỡ app, xóa dữ liệu hoặc đổi chữ ký khi đang bật bảo vệ, snapshot có thể mất. Tắt và hoàn tác trước. Thu hồi quyền Shizuku không tự gỡ những cấu hình đã áp dụng.
- Nếu Shizuku bị giết/khởi động lại, daemon và sửa định kỳ có thể gián đoạn; JobScheduler bị hệ thống trì hoãn. Không hứa bảo vệ liên tục sau reboot/force stop.
- Bản APK preview cần kiểm tra cờ debuggable và chứng chỉ trước khi dùng; chỉ bản release ký cục bộ là bản cung cấp chính thức cho đợt thử này. Không phát hành APK debug như một bản hardened.
