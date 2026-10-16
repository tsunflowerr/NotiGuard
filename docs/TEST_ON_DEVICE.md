# Thử bản 0.4 trên điện thoại ở công ty

Tải `NotiGuard-0.4.0-preview.apk` trong Assets của [release](https://github.com/tsunflowerr/NotiGuard/releases/tag/v0.4.0-preview). Repo riêng tư cần đăng nhập GitHub. Cài cập nhật lên app hiện tại; chữ ký giữ nguyên. Không gỡ/xóa dữ liệu khi còn snapshot chưa hoàn tác.

## Thử hướng không duy trì Shizuku

1. Nếu app còn phiên bảo vệ hoặc chờ hoàn tác từ bản cũ: tạm bật gỡ lỗi, khởi động Shizuku theo [hướng dẫn chính thức](https://shizuku.rikka.app/guide/setup/) rồi kết nối NotiGuard. Dữ liệu phiên cũ được giữ để hoàn tác. Không cần thêm quyền Trợ năng, đọc thông báo hoặc WRITE_SECURE_SETTINGS.
2. Bấm **Chuyển sang chỉ kiểm tra · hoàn tác trước**, xác nhận. Chỉ tiếp tục khi trạng thái là **Chỉ kiểm tra · không tự sửa**. Nếu báo chờ hoàn tác/lỗi, chưa chuyển thành công; nối lại Shizuku để thử hoàn tác.
3. Bấm **Mở cấu hình pin Google Play services**. Nếu có bộ chọn, tự chọn **Không hạn chế / No restrictions** cho đúng Google Play services. Không nhầm với Play Store. Nếu ROM ẩn lựa chọn hoặc mở màn hình khác, ghi nhận bước này chưa hỗ trợ; không coi mở màn hình là thành công.
4. Tắt USB debugging, wireless debugging và Developer Options thật trong Cài đặt. Quay lại bấm **Kiểm tra cấu hình khi tắt gỡ lỗi**, rồi bấm lại sau ít nhất 2 phút. Nếu GMS thiếu, cấu hình đang thử chưa đạt; chế độ này không tự sửa.
5. Nếu GMS vẫn có: thay đổi cấu hình pin của một app không quan trọng theo ý bạn, rồi kiểm tra lại GMS. PowerKeeper có thể tạo lại MILLET khi một chính sách khác thay đổi. Ghi lại lựa chọn ban đầu để tự khôi phục.
6. Khởi động lại điện thoại, mở NotiGuard và kiểm tra tiếp, không khởi động Shizuku. App nhận biết boot counter thay đổi; không dùng đồng hồ giờ thực để suy ra reboot.
7. Tự mở một app ngân hàng thường dùng để kiểm tra cảnh báo. Không cần thực hiện giao dịch. Cờ gỡ lỗi false và GMS có mặt chưa chứng minh ngân hàng chấp nhận hoặc push hoạt động.

Không chạy đồng thời app khác tự sửa MILLET khi thử: nó có thể thêm GMS và làm sai việc quy nguồn kết quả. Hai lần đọc chỉ cho biết hai thời điểm, không bảo đảm giữa hai lần GMS luôn hiện diện. Chế độ Chỉ kiểm tra không phát hiện/sửa liên tục khi app đóng.

## Thử đường Shizuku và sự kiện mới

Chọn **Chuyển lại dùng Shizuku**, khởi động Shizuku, kết nối rồi bật bảo vệ. Giữ app mở trong lượt đầu. Sau một thay đổi pin của app khác, dùng **Cập nhật trạng thái dịch vụ**: xem bộ đếm Sự kiện app và nguồn kiểm tra gần nhất. Không tự động coi callback đã chạy chỉ vì đăng ký observer thành công. Tắt bảo vệ và hoàn tác khi kết thúc nếu không tiếp tục dùng chế độ này.

Shizuku mất quyền chạy thì NotiGuard không tự sửa được; bản này không che gỡ lỗi khỏi ngân hàng. Máy ngủ sâu có thể trì hoãn cả callback và watchdog.

## Kiểm tra thông báo và pin

Gửi tin từ thiết bị/tài khoản khác cho Zalo, Messenger, Gmail và các app thực sự cần thử. Ghi thời điểm gửi/nhận lúc màn hình bật, khóa 30 phút, sau 2 giờ và qua đêm. Không dùng thông báo test nội bộ để suy ra push của app khác. Thông báo ngân hàng chỉ quan sát từ hoạt động bình thường, không tạo giao dịch để thử.

So pin trong các khoảng chờ có độ dài, sóng và hoạt động tương đương. Chưa có số đo pin hay tỷ lệ mất thông báo của bản này.

## Gửi kết quả

Sao chép báo cáo kỹ thuật trong app. Kèm bước nào không mở được, có thấy bộ chọn Không hạn chế không, GMS còn sau 2 phút/đổi chính sách/reboot không, và app ngân hàng có còn cảnh báo không. Không gửi OTP, số tài khoản, tin nhắn hoặc dữ liệu giao dịch. Báo cáo chỉ xuất clipboard khi bạn bấm nút.
