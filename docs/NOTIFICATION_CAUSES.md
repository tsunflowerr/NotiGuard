# Phân loại nguyên nhân trễ / mất thông báo

Mục tiêu là phân biệt cấu hình nhận push, truyền qua mạng, xử lý trong app và hiển thị thông báo. Một phép thử thông báo nội bộ không kiểm chứng toàn bộ chuỗi này.

| Nhóm | Điều cần kiểm tra | NotiGuard làm được | Giới hạn |
|---|---|---|---|
| Xiaomi đóng băng GMS | MILLET, Greezer, cấu hình PowerKeeper | Duy trì mục GMS qua Shizuku, đọc lại để đối chiếu | Không chứng minh mọi đường đóng băng đều được loại bỏ; không sửa được khi mất quyền chạy |
| App bị buộc dừng / vô hiệu hóa | FLAG_STOPPED, enabled | Chẩn đoán theo app; mở trang cài đặt theo yêu cầu | Không tự bật hoặc khởi chạy app ngân hàng |
| Quyền thông báo | POST_NOTIFICATIONS, quyền tổng và từng kênh | Đọc quyền runtime, dẫn đến cài đặt thông báo | Không đọc kênh riêng của app khác; quyền được cấp chưa bảo đảm hiển thị |
| Doze / App Standby | Ngủ sâu, hạn chế nền | Báo trạng thái ngủ/tiết kiệm pin và miễn pin theo app | Không thay được priority do máy chủ gửi; miễn pin có đánh đổi năng lượng |
| Data Saver / mạng tính dung lượng | Hạn chế dữ liệu nền | Báo trạng thái hệ thống đối với NotiGuard | Không suy ra mọi app khác được dùng dữ liệu nền |
| Mạng / VPN / firewall | Mất mạng, captive portal, kết nối dài bị cắt | Báo mạng validated, VPN, captive portal | Không có quyền Internet nên không dò FCM, không tự tắt VPN hoặc đổi DNS |
| Không làm phiền / màn hình khóa | Bộ lọc gián đoạn, kênh bị im lặng | Báo bộ lọc tổng, mở cài đặt của app | Push có thể đã đến dù người dùng không nghe hoặc không thấy trên màn hình khóa |
| FCM priority / TTL / collapse | Thông điệp thường, hết hạn, bị thay thế | Giải thích, cần thử từ nguồn gửi thật | Không sửa payload, token hay máy chủ Zalo/ngân hàng |
| Xử lý trong app | Startup chậm, xử lý payload quá lâu | Hướng dẫn tách theo app và mốc thời gian | Không can thiệp mã hoặc dữ liệu app khác |
| Token / backup / đăng ký | Token lỗi hoặc bị thay sau khôi phục | Đánh dấu là nguyên nhân cần dịch vụ/app xử lý | Không tự xóa dữ liệu hoặc buộc đăng nhập lại |
| GMS / đường push riêng | Bộ nhận FCM hiện diện hay không | Phát hiện bộ nhận khai báo | Hiện diện receiver không chứng minh mọi thông báo dùng FCM; không thấy receiver chưa chứng minh không dùng push |
| Thông báo giao dịch ngân hàng | Đăng ký dịch vụ và điều kiện gửi | Chỉ chẩn đoán điện thoại; người dùng kiểm tra trong app ngân hàng | Không đọc giao dịch, tạo giao dịch, OTP hoặc thay đổi dịch vụ tài khoản |

## Nguồn kỹ thuật

- [Android Doze/App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby): cơ chế trì hoãn nền và vai trò của push ưu tiên cao.
- [Android notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission): quyền hiển thị thông báo, khác với vận chuyển FCM.
- [Firebase Android receive messages](https://firebase.google.com/docs/cloud-messaging/android/receive-messages): khác biệt foreground/background và giới hạn thời gian xử lý payload.
- [Firebase message priority](https://firebase.google.com/docs/cloud-messaging/android-message-priority): ưu tiên do phía gửi đặt, không thể nâng mọi thông điệp của app khác từ app này.
- [Firebase network configuration](https://firebase.google.com/docs/cloud-messaging/network-configuration): kết nối FCM, cổng 5228–5230/443, NAT và VPN có thể ảnh hưởng độ tin cậy.
- [Firebase troubleshooting](https://firebase.google.com/docs/cloud-messaging/troubleshooting): một số lỗi đăng ký/token sau backup thuộc phía app/dịch vụ.
- [Điều tra HyperOS của dingwen07](https://github.com/dingwen07/hyperos-fcm-fix/blob/fe594239b01ba4b67d60da764683cadd13e5d287/docs/xiaomi-hyperos-gms-fcm-greezer-investigation.md): bằng chứng trên các build của tác giả; không thay thế kiểm chứng trên máy người dùng.

## Kết quả riêng của Xiaomi 14 đang thử

- OS3.0.307.0.WNCCNXM / Android 16. GMS trước đó thiếu trong MILLET trong khi một số app đích đã hiện diện. Đã kiểm tra thêm, giữ các mục cũ, hoàn tác và tự thêm lại khi giả lập việc ghi đè.
- `dumpsys greezer` báo `mGmsLimitEnabled: true` và có lịch sử đóng băng GMS. Đây là cờ/counter tích lũy, chưa chứng minh GMS đang bị đóng băng đúng lúc đọc hay là nguyên nhân duy nhất.
- Aurogon setting vắng mặt: bản hiện tại bỏ qua, chưa chứng minh cơ chế đó không tồn tại trong framework.
- Thử ContentService observer trong shell UserService bị từ chối `Failed to find PID`; 0.2/0.3 quay về polling. Chưa có cải tiến theo sự kiện được xác minh trên máy này.
- Thử cấp WRITE_SECURE_SETTINGS rồi ghi lại cùng giá trị MILLET bằng UID app bị từ chối `You cannot keep your settings in the secure settings`. Đã thu hồi quyền; bản cuối không khai báo quyền đó. Nguyên nhân là targetSdk 36 (xem dưới).

### Đo bằng ADB chỉ đọc, 16/10/2026 (OS3.0.307.0.WNCCNXM.C11, PowerKeeper 4.2.00, GMS 26.39.33)

| Nguyên nhân | Bằng chứng | Đã sửa |
|---|---|---|
| Greezer đóng băng GMS | 269 lần trong khoảng 17 giờ: 82 `screen off` (khoảng 5 giây sau khi tắt màn hình), 176 `from system` ngay sau mỗi lần rã ngắn; `IM mGmsLimitEnabled : true`; GMS đã ở danh sách miễn Doze `system` và `system-excidle` | GMS trong MILLET, giữ bằng [NotiGuard Keeper](KEEPER.md); thử 60 giây tắt màn hình không còn đóng băng GMS, kết nối FCM giữ liên tục |
| Tắt "Tự động đồng bộ dữ liệu" | `dumpsys content`: `Auto sync: u0=false`; GMS vẫn giao FCM cho Gmail (`Successful broadcast to com.google.android.gm`) | Bật lại. Thư mới cần đồng bộ; Google Chat không cần, nên Chat vẫn báo |
| App bị buộc dừng | BIDV, VCB, Telegram `stopped=true`; log GMS `Failed to broadcast to stopped app`. "Xóa bộ nhớ đệm khi khóa máy" đặt 10 phút | Đặt "Không bao giờ"; mở app một lần để gỡ trạng thái dừng |
| Tự khởi động bị chặn | MIUIOP 10008/10053 `ignore` cho BIDV, VCB, Facebook, Instagram, Telegram; log GMS `No response to broadcast from com.facebook.katana`. Công tắc Tự khởi động của SecurityCenter ghi MIUIOP 10053 | Bật bằng công tắc chính thức |
| Greezer từ chối push tới app đang đóng băng | `Greezer Denial: sending Intent { act=com.google.android.c2dm.intent.RECEIVE … pkg=org.telegram.messenger`, GMS nhận `result=CANCELLED` | "Không hạn chế" cho app nhắn tin, mạng xã hội và ngân hàng; PowerKeeper tự đưa vào MILLET và danh sách miễn Doze `user` |

Màn hình chi tiết pin của GMS chỉ có "Battery details" và "Included packages", không có bộ chọn. PowerKeeper 4.2.00 xét biểu tượng launcher của trưởng nhóm UID (`PreSetGroup`), với GMS là `com.google.android.gsf`; danh sách ẩn lưu trong `SimpleSettings.Misc` riêng của PowerKeeper, không phải `Settings.Secure` (đọc ra `null`). `miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY` không tồn tại trên bản này. Không có cách chính thức để PowerKeeper lưu GMS là "Không hạn chế".

## Developer Options và ngân hàng

Không có bằng chứng về một cấu hình Developer Options bật mà mọi phiên bản app ngân hàng đều chấp nhận. Không giả mạo cờ hệ thống hoặc chỉnh sửa ứng dụng ngân hàng.

**Đã thử và thất bại trên máy này:** người dùng tắt Developer Options, USB debugging và wireless debugging. Báo cáo NotiGuard sau thao tác cho biết cả ba cờ đều false, GMS không có trong MILLET và RPC Shizuku không kết nối. Bản hiện tại không tiếp tục duy trì cơ chế bảo vệ trong điều kiện đó. Kết quả này do người dùng cung cấp từ giao diện app vì ADB đã bị ngắt.

Tắt Developer Options không xóa khóa: AOSP chỉ đặt lại các setting thuộc Developer Options, tắt ADB chỉ dừng adbd. Mục GMS mất vì Shizuku chết nên không ai thêm lại khi PowerKeeper dựng lại MILLET (đổi chế độ pin của app bất kỳ, cài hoặc gỡ app; đo được trên máy này khi cài Keeper và khi đổi chế độ pin).

[Hướng dẫn Shizuku](https://shizuku.rikka.app/guide/setup/) khuyên giữ USB debugging và Developer Options bật, nên Shizuku không phải đường dùng lâu dài khi ngân hàng chặn gỡ lỗi. Đường đã kiểm chứng trên máy này là [NotiGuard Keeper](KEEPER.md): app targetSdk 22 có `WRITE_SETTINGS`, cài một lần qua ADB, sau đó tự thêm lại GMS trong khoảng một giây sau mỗi lần PowerKeeper dựng lại, khi gỡ lỗi đã tắt. Không che giấu cờ gỡ lỗi; ngân hàng thấy đúng trạng thái máy. Chưa có số đo qua đêm hay qua khởi động lại cho Keeper.
