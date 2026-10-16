# NotiGuard · 0.4.0 preview

Ứng dụng Android kiểm tra và duy trì một số cấu hình thông báo trên Xiaomi qua Shizuku. Phát triển cho Xiaomi 14 / HyperOS 3 / Android 16.

**Chưa sửa xong yêu cầu chạy độc lập khi tắt Developer Options.** Trên điện thoại đã thử, tắt gỡ lỗi làm mất kết nối Shizuku và GMS khỏi MILLET. Bản 0.4 bổ sung một luồng thử cấu hình qua màn hình hệ thống và kiểm tra độc lập; chưa có điện thoại để xác minh luồng mới. Không cam kết hết mất thông báo, giảm pin hoặc tương thích mọi ngân hàng.

[Tải APK đã ký và xem SHA256](https://github.com/tsunflowerr/NotiGuard/releases/tag/v0.4.0-preview). Repo riêng tư: cần đăng nhập tài khoản được cấp quyền. Cài APK trong mục Assets; các tệp Source code là mã nguồn, không phải bộ cài.

## Thay đổi trong 0.4

- **Chỉ kiểm tra:** dừng tự sửa, hoàn tác phiên đang quản lý trước khi chuyển. Khi hoàn tác lỗi, giữ snapshot và trạng thái chờ, không báo đã chuyển thành công. Chế độ này không tự nối Shizuku hoặc lập job sửa nền.
- **Mở cấu hình pin Google Play services:** thử mở màn hình thuộc ứng dụng hệ thống Xiaomi; nếu không có, mở thông tin GMS. Người dùng tự chọn Không hạn chế nếu ROM hiển thị. Không sửa cơ sở dữ liệu riêng của PowerKeeper hoặc gỡ bộ lọc ẩn của hệ thống. Mở được màn hình chưa chứng minh thay đổi thành công.
- **Kiểm tra khi gỡ lỗi tắt:** phân biệt GMS thiếu, giá trị không đọc được, còn gỡ lỗi, hai mẫu cách nhau ít nhất 2 phút và mẫu sau khởi động lại. Không dùng cờ false để suy ra ngân hàng chấp nhận; không dùng hai mẫu để suy ra theo dõi liên tục.
- **Observer trong tiến trình app:** khi app còn sống và đang bảo vệ, thay đổi MILLET/Aurogon yêu cầu shell kiểm tra qua API cố định, có gom sự kiện/backoff. Không giảm chu kỳ dự phòng chỉ vì nhận sự kiện ở app. Chưa kiểm chứng callback mới trên ROM này.
- Giữ ba quyền như 0.3.1: boot completed, network state và Shizuku. Không thêm Internet, Trợ năng hoặc quyền đọc thông báo.

## Dùng chế độ Shizuku

1. Cài [Shizuku chính thức](https://shizuku.rikka.app/download/), khởi động theo [hướng dẫn](https://shizuku.rikka.app/guide/setup/). Một số Xiaomi cần USB debugging (Security options) nếu thao tác bị từ chối.
2. Cài APK, bấm **Kết nối / cấp quyền Shizuku**, tự cho phép trong Shizuku.
3. Bấm **Đọc cấu hình**, kiểm tra nhóm app và giữ miễn Doze tắt lúc đầu.
4. Bấm **Bật bảo vệ**, xem kết quả từng thao tác. Đọc lại setting chỉ chứng minh giá trị được lưu.
5. Sau reboot cần khởi động lại Shizuku. Tắt Developer Options có thể làm mất dịch vụ; việc này đã xảy ra trên máy thử.
6. Trước khi gỡ app hoặc xóa dữ liệu, **Tắt bảo vệ & hoàn tác**, đợi hết lỗi. Nếu mất Shizuku, nối lại để hoàn tác snapshot đang giữ.

## Thử hướng không cần duy trì Shizuku

Xem [quy trình kiểm thử ở máy công ty](docs/TEST_ON_DEVICE.md). Bước đặt cấu hình có thể không được ROM hỗ trợ; bản này không vượt quyền hệ thống để ép nó.

Sau khi chuyển sang **Chỉ kiểm tra**, tự đặt GMS thành Không hạn chế nếu có. Tắt gỡ lỗi và kiểm tra ngay, sau 2 phút, sau thay đổi một cấu hình pin khác và sau reboot. Nếu GMS thiếu hoặc mục bị ẩn, ghi nhận thất bại. Trong chế độ này NotiGuard không tự thêm GMS để làm kết quả trông thành công.

## Phạm vi sửa hiện tại

- Thêm riêng GMS vào `Settings.System.MILLET_NO_RESTRICT_APP` ở user 0, giữ các mục khác. PowerKeeper có thể ghi đè danh sách từ bảng riêng; Shizuku dùng watchdog 120/300 giây để sửa khi còn quyền chạy. Không giữ CPU thức, không báo thức đánh thức máy. Ngủ sâu có thể trì hoãn kiểm tra.
- Chỉ thêm overlay FCM có marker riêng vào `Settings.Global.aurogon_enable` khi setting đã tồn tại; ROM đang thử không có setting này nên bỏ qua.
- Observer trong shell đã bị từ chối với lỗi `Failed to find PID` trên máy thử. Chỉ dùng watchdog 900 giây nếu observer shell thực sự nhận sự kiện. Callback mới từ app không thay thế watchdog.
- Miễn Doze và hai AppOp chạy nền cho GMS cùng nhóm đã chọn là tùy chọn, mặc định tắt. Có thể tăng hao pin. Không tắt toàn bộ Doze, PowerKeeper hoặc Greezer.
- Snapshot được lưu đồng bộ trước khi sửa; hoàn tác riêng phần được quản lý. Giữ snapshot nếu hoàn tác lỗi. Không có giao dịch nguyên tử với hệ thống; xem [SECURITY.md](SECURITY.md).
- Chẩn đoán chỉ đọc mạng, DND, cờ buộc dừng, quyền thông báo và miễn pin; không đọc tin nhắn, OTP, tài khoản hoặc dữ liệu ngân hàng. Không thể sửa priority/TTL/token/máy chủ của app khác.

Chỉ hỗ trợ hồ sơ chủ máy. Danh sách theo package có thể tác động hồ sơ khác do cách Xiaomi/Android thực thi; chưa kiểm chứng app nhân bản hoặc hồ sơ công việc.

## Build và xác minh

JDK 17, Android SDK 36, build-tools 36.0.0. Tạo `local.properties` với `sdk.dir=...`, chạy:

```text
gradlew testDebugUnitTest lintDebug assembleRelease
```

Lệnh trên tạo APK release **chưa ký**. `scripts/build.ps1` build trong thư mục ASCII mới, kiểm thử và ký bằng khóa cục bộ tại `%LOCALAPPDATA%/NotiGuardBuild/keys`. Đặt JDK 17 tại `%LOCALAPPDATA%/NotiGuardBuild/jdk-*`. Khóa và mật khẩu DPAPI không đưa lên GitHub; giữ nguyên khóa để cập nhật app đã cài. APK debug dùng chữ ký khác.

Checksum Gradle và dependency được khóa trong repo. Không dùng `-RecordDependencyChecksums` để tự chấp nhận dependency thay đổi chưa xem xét. Checksum không thay thế kiểm toán mã. [Báo cáo bản 0.4](docs/releases/0.4.0.md) phân biệt kiểm tra tự động với thử trên thiết bị.

## Nguồn và giấy phép

[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) ghi nguồn, commit và giấy phép. Hai bộ xử lý cấu hình được chuyển từ dingwen07/hyperos-fcm-fix; các phần UI, quản trị, Binder, snapshot và kiểm thử được viết cho NotiGuard. Không có bằng chứng hiệu quả hoặc pin vượt upstream. GPL-3.0-or-later cho phần NotiGuard, giữ điều kiện GPL-3.0 của phần tham khảo.

[Chi tiết thay đổi và các phép thử thất bại](docs/PROVENANCE_AND_CHANGES.md) · [Các nhóm nguyên nhân mất thông báo](docs/NOTIFICATION_CAUSES.md) · [Bảo mật](SECURITY.md)