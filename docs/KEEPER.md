# NotiGuard Keeper

App phụ, chỉ làm một việc: thêm lại `com.google.android.gms` vào `Settings.System.MILLET_NO_RESTRICT_APP` mỗi khi PowerKeeper xóa nó. Chạy được khi Developer Options, USB debugging và wireless debugging đã tắt thật. Không che giấu hay giả mạo trạng thái gỡ lỗi; ngân hàng thấy đúng trạng thái máy.

## Vì sao cần

- Trên ROM nội địa HyperOS 3, Greezer đóng băng Google Play services khoảng 5 giây sau khi tắt màn hình và mỗi lần GMS xuống nền. Đo trên Xiaomi 14 đang dùng: 269 lần trong khoảng 17 giờ. Kết nối FCM (`mtalk.google.com:5228`) mất theo, nên mọi app dùng FCM nhận thông báo trễ.
- Greezer chỉ bỏ qua package có trong `MILLET_NO_RESTRICT_APP`. Danh sách miễn Doze không ngăn được Greezer: GMS đã ở mức `system` và `system-excidle` mà vẫn bị đóng băng.
- `MILLET_NO_RESTRICT_APP` là bản chiếu PowerKeeper sinh lại từ bảng riêng (các app chọn "Không hạn chế"). Mỗi lần dựng lại, mục GMS ghi tay bị xóa: khi đổi chế độ pin của bất kỳ app nào, khi cài hoặc gỡ app, có khi chỉ do mở trang pin của một app. Cập nhật app và khởi động lại không xóa.
- GMS không có lựa chọn "Không hạn chế". PowerKeeper 4.2.00 xét biểu tượng launcher của trưởng nhóm UID; GMS chung UID với `com.google.android.gsf` (không có launcher), nên bộ chọn bị ẩn. Không có màn hình chính thức hay setting nào để PowerKeeper lưu GMS là "Không hạn chế".
- NotiGuard targetSdk 36 không ghi được khóa này khi không có Shizuku: SettingsProvider Android 16 ném `You cannot keep your settings in the secure settings` cho app không đặc quyền có targetSdk > 22 (`warnOrThrowForUndesiredSecureSettingsMutationForTargetSdk`). App targetSdk ≤ 22 có `WRITE_SETTINGS` chỉ bị ghi cảnh báo. Đây là lý do Keeper dùng targetSdk 22.

## Keeper làm gì và không làm gì

- Chỉ thêm GMS khi thiếu. Giữ nguyên văn các mục khác, dùng dấu phân cách `", "` như PowerKeeper, đọc lại để xác nhận. Không nhầm `app.revanced.android.gms` hay `com.google.android.gms.location.history` với GMS.
- Kích hoạt: JobScheduler content trigger trên `content://settings/system/MILLET_NO_RESTRICT_APP`, chạy 0,5–2 giây sau khi danh sách đổi. Trigger do system_server theo dõi, không cần tiến trình Keeper chạy nền. Dự phòng: job định kỳ 30 phút, khi khởi động máy, khi mở Keeper.
- Tối đa 12 lần sửa trong 10 phút, để tranh chấp với công cụ khác không thành vòng ghi.
- Quyền: `WRITE_SETTINGS` (Android cấp sẵn cho app targetSdk ≤ 22; người dùng tắt được trong "Sửa cài đặt hệ thống"), `RECEIVE_BOOT_COMPLETED` và từ 1.0.2 `READ_SYNC_SETTINGS`. Không có Internet, Trợ năng, quyền đọc thông báo hay `QUERY_ALL_PACKAGES`.
- Không ghi bảng riêng của PowerKeeper, không tắt Greezer/PowerKeeper, không đổi cờ gỡ lỗi, không chạm app ngân hàng.

## Đánh đổi

- Android 14 trở lên chặn cài app targetSdk < 23 bằng trình cài đặt thường. Cài và cập nhật phải qua `adb install --bypass-low-target-sdk-block`, nghĩa là bật gỡ lỗi tạm thời mỗi lần cài.
- Android hiện cảnh báo app dành cho phiên bản cũ; Play Protect có thể cảnh báo. Không phát hành trên Play.
- Android hoặc HyperOS bản sau có thể bỏ ngoại lệ targetSdk ≤ 22. Khi đó Keeper báo "Bị từ chối ghi" và không tự vượt qua.
- Từ lúc PowerKeeper xóa đến lúc Keeper thêm lại (thường vài giây), Greezer vẫn có thể đóng băng GMS. Đóng băng đã xảy ra không tự rã; GMS chờ sự kiện rã đông hợp lệ (bật màn hình, broadcast hệ thống).
- Khóa ký riêng `keeper.jks` trong `%LOCALAPPDATA%\NotiGuardBuild\keys`, mật khẩu DPAPI `keeper-password.dpapi`. Mất khóa thì phải gỡ Keeper trước khi cài bản ký khóa khác.

## Build

`scripts\build-keeper.ps1` build trong thư mục ASCII mới, chạy unit test, lint, assembleRelease, ký bằng khóa `keeper` và in SHA256. Khóa NotiGuard (`notiguard.jks`) không bị dùng hay tạo ra.

## Cài đặt một lần

1. Bật Developer Options, USB debugging, "Install via USB". Cắm máy vào PC đã cho phép.
2. `adb install --bypass-low-target-sdk-block dist\NotiGuard-Keeper-1.0.3.apk` (cập nhật thêm `-r`). Tự bấm Install trên hộp "Install via USB": HyperOS bỏ qua thao tác chạm gửi từ ADB vào hộp này, và tự từ chối khi hết đếm ngược.
3. Mở Keeper. Màn hình xem lại quyền của app đời cũ hiện ra, mọi công tắc mặc định tắt: chỉ bật "Sửa cài đặt hệ thống", để nguyên các mục khác, bấm OK. HyperOS đặt app-op `WRITE_SETTINGS` của Keeper là `ignore` cho đến bước này.
4. Ngay sau đó: Cài đặt → Ứng dụng → NotiGuard Keeper: bật Tự khởi động; Power → Không hạn chế. Làm trước khi để Keeper ở nền lâu: Greezer đóng băng app chưa "Không hạn chế" 5 giây sau khi xuống nền, và JobScheduler của HyperOS không chạy job có trigger kích hoạt trong lúc app bị đóng băng. Bước này làm PowerKeeper dựng lại MILLET; Keeper tự thêm lại GMS.
5. Mở lại Keeper. Phải thấy "GMS trong MILLET: có", "Quyền sửa cài đặt hệ thống: có" và "Theo dõi thay đổi: đang chờ".
6. Tắt Developer Options.

## Kiểm chứng

- Đổi chế độ pin của một app không quan trọng rồi mở Keeper: nhật ký có dòng "Đã thêm lại GMS · danh sách vừa đổi".
- Khi còn ADB: tắt màn hình một phút rồi đọc `dumpsys greezer`. Không còn dòng `FZ uid = <uid GMS> … reason : screen off`.
- Không cần ADB: bấm `*#*#426#*#*` để xem trạng thái kết nối FCM; nhờ người khác nhắn tin khi máy khóa.

## Gỡ bỏ

Khi gỡ app hoặc xóa dữ liệu app, SettingsProvider xóa mọi khóa `Settings.System` không thuộc AOSP mà app đó ghi cuối cùng (`removeSettingsForPackageLocked`, gọi từ `onPackageRemoved` và `onPackageDataCleared`, Android 16). Keeper thường là bên ghi `MILLET_NO_RESTRICT_APP` cuối cùng, nên **gỡ Keeper hoặc bấm Xóa dữ liệu sẽ xóa cả danh sách**. Mọi app "Không hạn chế" mất miễn đóng băng cho tới khi PowerKeeper dựng lại.

Cách gỡ: gỡ Keeper, rồi ngay sau đó vào Cài đặt → Ứng dụng → một app bất kỳ → Power, chọn chế độ pin khác rồi chọn lại như cũ. PowerKeeper sẽ dựng lại danh sách (không có GMS). Nếu chỉ xóa dữ liệu mà Keeper vẫn còn, Keeper ghi lại GMS và nhật ký báo danh sách đã bị xóa; vẫn làm bước đổi chế độ pin như trên để các app khác có lại miễn trừ.

Bị buộc dừng (Force stop) làm Android hủy các job của Keeper và bỏ qua `BOOT_COMPLETED` cho tới khi mở lại app. Từ 1.0.2, Keeper không hiện trong màn hình đa nhiệm, nên "Xóa tất cả" không chạm tới nó.

## Kiểm tra sức khỏe (từ 1.0.2)

Chạy cùng job định kỳ 30 phút, khi khởi động máy và khi mở Keeper; không thêm lần đánh thức nào. Theo dõi chính các app trong danh sách "Không hạn chế":

- App bị buộc dừng (`FLAG_STOPPED`): Android không giao FCM cho app này cho tới khi được mở lại.
- "Tự động đồng bộ dữ liệu" tắt: Gmail không tải thư mới, nên không báo thư.
- GMS vắng mặt trong danh sách, kèm lý do nếu Keeper bị từ chối ghi.
- App từng ở danh sách "Không hạn chế" bị rơi ra mà vẫn còn cài. Ngày 19/10 đo được HyperOS tự hoàn tác cả "Không hạn chế" lẫn Tự khởi động (MIUIOP 10008/10053) của một app ít dùng, không có cập nhật app nào liên quan. Có nút mở cài đặt app đó. Mọi cảnh báo theo app có nút "Bỏ qua app này" (từ 1.0.3), hoàn tác bằng "Theo dõi lại tất cả".

"Xóa tất cả" trong đa nhiệm buộc dừng cả app đã đặt "Không hạn chế" + Tự khởi động (đo ngày 19/10); app khóa trong đa nhiệm không bị ảnh hưởng. Khóa các app cần nhận thông báo (ngân hàng, Zalo, Messenger, Gmail).

Chỉ hiện một thông báo khi danh sách vấn đề thay đổi và tự gỡ khi hết vấn đề. Quyền thêm: `READ_SYNC_SETTINGS` (quyền thường, chỉ để đọc công tắc đồng bộ).

## Số đo trên Xiaomi 14 (OS3.0.307.0.WNCCNXM.C11, 16/10/2026)

- Trước: GMS bị đóng băng 269 lần trong khoảng 17 giờ (82 lần `screen off`, 176 lần `from system`).
- Sau khi thêm GMS vào MILLET: trong 60 giây tắt màn hình, GMS không bị đóng băng; kết nối FCM giữ liên tục (`connects` không tăng); push Messenger đến khi màn hình tắt.
- App không ở chế độ "Không hạn chế" và đang bị đóng băng: Greezer từ chối push FCM (`Greezer Denial … c2dm.intent.RECEIVE`, GMS nhận `result=CANCELLED`). App nhắn tin cần "Không hạn chế" cùng với Tự khởi động.
- Cài Keeper làm PowerKeeper dựng lại MILLET và bỏ GMS, đúng như báo cáo của cộng đồng về việc cài app.
- Keeper 1.0.1 ở nền, không bị đóng băng: qua 5 lần PowerKeeper dựng lại (đổi chế độ pin trong Cài đặt), GMS có lại trong khoảng 0,6–1 giây; một lần lấy mẫu mỗi 0,5 giây chỉ thấy GMS vắng một mẫu.
- Lần thử đầu, Keeper bị Greezer đóng băng (`reason : tobg`) trước khi được đặt "Không hạn chế". Trigger vẫn kích hoạt nhưng JobScheduler báo `Restricted due to: frozen.`; sau khi tiến trình chết, job ở trạng thái `Ready: true` mà không được chạy. 1.0.1 đăng ký lại trigger ở mọi lần chạy, kể cả job định kỳ 30 phút.
