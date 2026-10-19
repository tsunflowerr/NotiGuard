# NotiGuard

Sửa tình trạng thông báo đến chậm trên điện thoại Xiaomi chạy **HyperOS bản nội địa Trung Quốc**: ngân hàng, Zalo, Messenger, Gmail… chỉ báo khi mở màn hình. Thành phần chính là **NotiGuard Keeper**, một app nhỏ chạy được khi Tùy chọn nhà phát triển đã tắt, nên app ngân hàng vẫn dùng bình thường.

> Đã kiểm chứng trên Xiaomi 14, HyperOS OS3.0.307.0.WNCCNXM, Android 16 (16–19/10/2026). Máy hoặc bản HyperOS khác có thể chạy khác. Không cam kết mọi thông báo đều tức thì.

## Vì sao thông báo bị chậm

- **HyperOS nội địa đóng băng Google Play services** khoảng 5 giây sau khi tắt màn hình. Đo trên máy thử: 269 lần trong khoảng 17 giờ. Kết nối push (FCM) đứt theo, nên app nào dùng push cũng nhận trễ.
- Hệ thống chỉ tha cho app có trong danh sách miễn đóng băng `MILLET_NO_RESTRICT_APP`. PowerKeeper tự dựng lại danh sách này từ các app đặt "Không hạn chế". Google Play services không có lựa chọn đó, nên luôn bị bỏ ra mỗi khi bạn đổi chế độ pin của app nào đó, cài hoặc gỡ app.
- Các nguyên nhân phụ trên máy thử: tắt "Tự động đồng bộ dữ liệu" (Gmail không tải thư mới), app bị buộc dừng (Android không giao push cho app này), dọn bộ nhớ sau khi khóa máy.

Chi tiết và số đo: [docs/NOTIFICATION_CAUSES.md](docs/NOTIFICATION_CAUSES.md).

## NotiGuard Keeper làm gì

- Mỗi khi PowerKeeper xóa Google Play services khỏi danh sách miễn đóng băng, Keeper thêm lại trong khoảng 1 giây. Các mục khác giữ nguyên. Hệ thống tự báo khi danh sách đổi, nên Keeper không phải chạy ngầm liên tục.
- **Kiểm tra sức khỏe**, chỉ một thông báo khi có thay đổi:
  - App bạn đặt "Không hạn chế" bị buộc dừng, hoặc bị HyperOS tự đổi về mặc định.
  - "Tự động đồng bộ dữ liệu" bị tắt.
- Không hiện trong màn hình đa nhiệm, nên "Xóa tất cả" không dừng được nó.
- **Không cần Shizuku**, không cần root, không mở khóa bootloader, không che giấu trạng thái gỡ lỗi.

Thiết kế và giới hạn: [docs/KEEPER.md](docs/KEEPER.md).

## Yêu cầu

- Xiaomi / Redmi / POCO chạy **HyperOS 3 bản nội địa (Android 16)**. ROM quốc tế thường không cần Keeper.
- **Một máy tính có ADB, dùng một lần khi cài** (và khi cập nhật). Từ Android 14, Keeper không cài được bằng cách mở file APK trên điện thoại, vì app được build cho Android cũ (lý do ở [docs/KEEPER.md](docs/KEEPER.md)).
- File `NotiGuard-Keeper-<phiên bản>.apk` do người phát hành gửi, kèm mã SHA-256 để kiểm tra.

## Cài đặt (một lần)

1. **Kiểm tra file.** Trên máy tính chạy `certutil -hashfile NotiGuard-Keeper-1.0.3.apk SHA256` (Windows) hoặc `sha256sum` (macOS/Linux). Kết quả phải trùng mã SHA-256 người phát hành gửi; không trùng thì đừng cài.
2. **Trên điện thoại:**
   - Cài đặt → Giới thiệu điện thoại → chạm 7 lần vào "Phiên bản HyperOS" để bật Tùy chọn nhà phát triển.
   - Trong Tùy chọn nhà phát triển, bật **USB debugging** và **Install via USB**. ROM nội địa có thể yêu cầu đăng nhập tài khoản Mi và lắp SIM cho mục này.
3. **Trên máy tính:** tải [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) và giải nén. Cắm điện thoại, bấm **Cho phép** ở hộp "Cho phép USB debugging?" trên điện thoại, rồi chạy trong thư mục platform-tools:
   ```
   adb install --bypass-low-target-sdk-block NotiGuard-Keeper-1.0.3.apk
   ```
   Hộp "Install via USB" hiện trên điện thoại: **tự bấm Install trong khoảng 8 giây**. Hết giờ, hệ thống tự từ chối; chạy lại lệnh là được.
4. **Mở NotiGuard Keeper.** Màn hình xem lại quyền hiện ra, mọi công tắc đều tắt sẵn: **chỉ bật "Sửa cài đặt hệ thống"**, để nguyên các mục khác, bấm OK. Nếu được hỏi quyền thông báo, chọn **Cho phép**.
5. **Làm ngay sau bước 4:** Cài đặt → Ứng dụng → NotiGuard Keeper:
   - Bật **Tự khởi động**.
   - Vào **Power**, chọn **Không hạn chế**.

   Để Keeper ở nền lâu trước bước này thì HyperOS sẽ đóng băng nó.
6. **Mở lại Keeper.** Phải thấy "GMS trong MILLET: có", "Quyền sửa cài đặt hệ thống: có" và "Theo dõi thay đổi: đang chờ".
7. **Tắt Tùy chọn nhà phát triển** (công tắc trên cùng). USB debugging tắt theo; rút cáp.

## Thiết lập để nhận thông báo đầy đủ

Làm một lần qua giao diện Cài đặt; không cần máy tính.

- **Mỗi app cần thông báo tức thì** (ngân hàng, ví, Zalo, Messenger, Gmail…), vào Cài đặt → Ứng dụng → [app]:
  - Bật **Tự khởi động**.
  - **Power → Không hạn chế**.
- **Cài đặt → Tài khoản & đồng bộ → bật "Tự động đồng bộ dữ liệu".** Gmail cần mục này để báo thư mới.
- **Cài đặt → Pin:** tìm mục "Xóa bộ nhớ đệm khi khóa máy" (Clear cache when device is locked), chọn **Không bao giờ**. Trên máy thử mục này nằm trong trang tối ưu pin, mặc định 10 phút.
- **Khóa các app quan trọng trong đa nhiệm:** mở đa nhiệm, nhấn giữ thẻ app, chọn biểu tượng ổ khóa. Trên máy thử, "Xóa tất cả" buộc dừng mọi app chưa khóa, kể cả app "Không hạn chế".
- **Mở mỗi app ngân hàng một lần** sau khi tắt gỡ lỗi, để chắc chắn app không còn ở trạng thái bị buộc dừng.

"Không hạn chế" cho app nặng (Facebook, TikTok…) làm hao pin nền. Chỉ bật cho app thật sự cần thông báo tức thì.

## Sử dụng hằng ngày

Không cần làm gì. Mở Keeper khi muốn kiểm tra:

| Dòng | Ý nghĩa |
|---|---|
| GMS trong MILLET: có | Google Play services đang được miễn đóng băng |
| Theo dõi thay đổi: đang chờ | Keeper sẵn sàng sửa khi danh sách bị dựng lại |
| Đã thêm lại: N lần | Số lần Keeper đã tự sửa |
| Cảnh báo | Việc cần làm, kèm nút mở đúng app hoặc đúng màn hình |
| Nhật ký gần đây | 20 sự kiện gần nhất |

Trên mỗi cảnh báo về một app:
- **"Bỏ qua app này"** dùng cho app không cần thông báo.
- **"Theo dõi lại tất cả"** hoàn tác các lần bỏ qua.

Kiểm tra kết nối push mà không cần máy tính: bấm `*#*#426#*#*` trong ứng dụng Điện thoại (FCM Diagnostics).

## Không nên làm

- **Không gỡ cài đặt và không "Xóa dữ liệu" Keeper.** Android sẽ xóa cả danh sách miễn đóng băng, mọi app mất miễn trừ. Nếu buộc phải gỡ: gỡ xong, vào một app bất kỳ → Power, chọn chế độ khác rồi chọn lại như cũ, để PowerKeeper dựng lại danh sách.
- **Không buộc dừng Keeper, không tắt quyền "Sửa cài đặt hệ thống"** của nó. Lỡ buộc dừng thì mở Keeper một lần.
- **Sau mỗi lần cập nhật HyperOS**, mở Keeper xem có báo "Bị từ chối ghi" không.

## Cập nhật và gỡ bỏ

- **Cập nhật:** bật lại USB debugging và Install via USB, rồi chạy `adb install -r --bypass-low-target-sdk-block NotiGuard-Keeper-<bản mới>.apk`. Quyền và cài đặt pin được giữ nguyên. Bản mới phải được ký cùng khóa với bản đang cài.
- **Gỡ bỏ:** xem mục "Không nên làm" ở trên, và [docs/KEEPER.md](docs/KEEPER.md#gỡ-bỏ).

## Pin, quyền riêng tư và bảo mật

- **Pin:** Keeper dùng khoảng 0,2 mAh trong 9 giờ trên máy thử. Phần tốn thật là Google Play services không còn bị đóng băng: khoảng 28 mAh trong 9 giờ (dưới 1% pin), tương đương ROM quốc tế.
- **Quyền:** chỉ có ba quyền: "Sửa cài đặt hệ thống", chạy khi khởi động máy, đọc công tắc đồng bộ.
- **Không có quyền Internet**, nên không gửi được dữ liệu ra ngoài.
- **Không đọc** thông báo, tin nhắn, tài khoản hay dữ liệu ngân hàng. Không dùng Trợ năng.
- **Chỉ ghi một setting** (`MILLET_NO_RESTRICT_APP`). Nhật ký nằm trong bộ nhớ riêng của app, đã tắt sao lưu.
- **Keeper nhắm Android 5.1 (targetSdk 22)** để được phép ghi setting của hãng. Vì thế Android cảnh báo "ứng dụng dành cho phiên bản cũ", và Play Protect có thể cảnh báo.
- **Chỉ cài APK từ người bạn tin.** Quyền "Sửa cài đặt hệ thống" sẽ nguy hiểm nếu APK bị thay bằng bản độc hại; luôn kiểm tra SHA-256.
- Chưa có kiểm toán bảo mật độc lập. Xem [SECURITY.md](SECURITY.md).

## Build từ mã nguồn

Cần JDK 17 trong `%LOCALAPPDATA%\NotiGuardBuild\jdk-*` và Android SDK (platform 36, build-tools 36.0.0) trong `%LOCALAPPDATA%\Android\Sdk`. Chạy:

```
powershell -File scripts\build-keeper.ps1
```

Script build trong thư mục tạm mới, chạy unit test, lint, assembleRelease, ký bằng khóa riêng của Keeper (`%LOCALAPPDATA%\NotiGuardBuild\keys\keeper.jks`, mật khẩu mã hóa theo tài khoản Windows) và in SHA-256 của `dist\NotiGuard-Keeper-<phiên bản>.apk`.

- **Giữ khóa ký** để phát hành bản cập nhật cài đè được.
- Khóa không bao giờ được đưa lên git.
- Phiên bản Gradle và mọi thư viện đều được khóa checksum trong repo.

## Ứng dụng NotiGuard (Shizuku, cũ)

Module `app` là ứng dụng NotiGuard 0.4 dùng Shizuku. Nó chỉ hoạt động khi USB/Wireless debugging còn bật, nên không phù hợp nếu bạn dùng app ngân hàng. Tài liệu cũ: [docs/NOTIGUARD_SHIZUKU.md](docs/NOTIGUARD_SHIZUKU.md).

## Tài liệu

- [docs/KEEPER.md](docs/KEEPER.md): thiết kế, đánh đổi, số đo của Keeper.
- [docs/NOTIFICATION_CAUSES.md](docs/NOTIFICATION_CAUSES.md): các nguyên nhân thông báo chậm và kết quả đo trên Xiaomi 14.
- [docs/PROVENANCE_AND_CHANGES.md](docs/PROVENANCE_AND_CHANGES.md): nguồn gốc mã và lịch sử thay đổi.
- [SECURITY.md](SECURITY.md): phạm vi bảo mật.

## Giấy phép

GPL-3.0-or-later. Ghi nhận nguồn tham khảo (dingwen07/hyperos-fcm-fix) trong [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Khi phát hành APK cho người khác, cung cấp kèm mã nguồn theo điều khoản GPL.
