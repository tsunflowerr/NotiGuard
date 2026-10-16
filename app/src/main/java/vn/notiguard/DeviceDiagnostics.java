package vn.notiguard;

import android.Manifest;
import android.app.NotificationManager;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import java.util.*;

/** Read-only checks; does not inspect messages, accounts, transactions, network addresses or tokens. */
final class DeviceDiagnostics {
    record RuntimeState(Boolean developer, Boolean usb, Boolean wireless, Boolean gms, int boot, long elapsed) { }

    private static Boolean flag(Context context, String key) {
        try {
            String value = Settings.Global.getString(context.getContentResolver(), key);
            return "1".equals(value) ? Boolean.TRUE : "0".equals(value) ? Boolean.FALSE : null;
        } catch (RuntimeException unavailable) { return null; }
    }

    static RuntimeState readRuntime(Context context) {
        Boolean gms = null;
        int boot = -1;
        try { gms = XiaomiRules.milletPackages(Settings.System.getString(context.getContentResolver(), XiaomiRules.MILLET)).contains(GuardConfig.GMS); }
        catch (RuntimeException unavailable) { /* Unknown is not treated as an exemption. */ }
        try { boot = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT); }
        catch (Exception unavailable) { /* Do not claim a reboot from elapsed time alone. */ }
        return new RuntimeState(flag(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED), flag(context, Settings.Global.ADB_ENABLED),
                flag(context, "adb_wifi_enabled"), gms, boot, SystemClock.elapsedRealtime());
    }

    private static String value(Boolean value) { return value == null ? "chưa đọc được" : value.toString(); }

    static String runtime(RuntimeState state) {
        StringBuilder out = new StringBuilder("TRẠNG THÁI THIẾT LẬP\n");
        out.append("NotiGuard ").append(BuildConfig.VERSION_NAME).append(" · ").append(Build.MODEL).append(" · Android ").append(Build.VERSION.RELEASE).append('\n');
        out.append("Developer Options: ").append(value(state.developer)).append('\n');
        out.append("USB debugging: ").append(value(state.usb)).append('\n');
        out.append("Wireless debugging: ").append(value(state.wireless)).append('\n');
        out.append("GMS đang có trong MILLET: ").append(value(state.gms)).append('\n');
        out.append("Các cờ này không chứng minh ngân hàng sẽ chấp nhận thiết bị. Không chỉnh sửa hoặc giả mạo trạng thái mà ngân hàng đọc.\n");
        return out.toString();
    }
    static String system(Context context) {
        StringBuilder out = new StringBuilder("KIỂM TRA HỆ THỐNG · chỉ đọc\n");
        PowerManager power = context.getSystemService(PowerManager.class);
        out.append("Tiết kiệm pin: ").append(power.isPowerSaveMode()).append("\nDoze sâu hiện tại: ").append(power.isDeviceIdleMode()).append('\n');
        try {
            ConnectivityManager network = context.getSystemService(ConnectivityManager.class);
            NetworkCapabilities caps = network.getNetworkCapabilities(network.getActiveNetwork());
            out.append("Có mạng mặc định: ").append(caps != null).append('\n');
            if (caps != null) {
                out.append("Mạng được Android xác thực: ").append(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).append('\n');
                out.append("Có captive portal: ").append(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)).append('\n');
                out.append("Đường mạng qua VPN: ").append(caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).append('\n');
                out.append("Mạng tính dung lượng: ").append(network.isActiveNetworkMetered()).append('\n');
            }
            out.append("Data Saver đối với NotiGuard: ").append(network.getRestrictBackgroundStatus()).append(" (1=tắt, 2=app được miễn, 3=bị hạn chế)\n");
        } catch (Exception e) { out.append("Mạng: chưa đọc được (không kết luận FCM mất kết nối)\n"); }
        try {
            int dnd = context.getSystemService(NotificationManager.class).getCurrentInterruptionFilter();
            out.append("Bộ lọc không làm phiền: ").append(dnd).append(" (1=cho tất cả; giá trị khác cần kiểm tra)\n");
        } catch (Exception e) { out.append("Chưa đọc được bộ lọc không làm phiền\n"); }
        out.append(app(context, GuardConfig.GMS));
        out.append("\nMạng được xác thực không chứng minh kết nối FCM thông. VPN/DNS/firewall có thể ảnh hưởng; app không tự tắt chúng hoặc dò mạng.\n");
        out.append("Không thể đo TTL, priority, token hoặc lỗi máy chủ của app khác. Đó là các nguyên nhân phía dịch vụ mà app này không tự sửa được.\n");
        return out.toString();
    }

    static String app(Context context, String pkg) {
        GuardConfig.validatePackage(pkg);
        StringBuilder out = new StringBuilder("\n" + pkg + "\n");
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkg, 0);
            out.append("App đang bật: ").append(info.enabled).append('\n');
            out.append("Có cờ buộc dừng: ").append((info.flags & ApplicationInfo.FLAG_STOPPED) != 0).append('\n');
            out.append("Được miễn tối ưu pin AOSP: ").append(context.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(pkg)).append('\n');
            if (Build.VERSION.SDK_INT >= 33 && !pkg.equals(GuardConfig.GMS)) out.append("Quyền POST_NOTIFICATIONS: ").append(context.getPackageManager().checkPermission(Manifest.permission.POST_NOTIFICATIONS, pkg) == PackageManager.PERMISSION_GRANTED).append('\n');
            out.append("Quyền thông báo được cấp chưa chứng minh từng kênh, âm thanh hoặc màn hình khóa đang bật.\n");
            boolean fcm = !context.getPackageManager().queryBroadcastReceivers(new Intent(GuardConfig.FCM).setPackage(pkg), 0).isEmpty();
            out.append("Thấy bộ nhận FCM: ").append(fcm).append("; không đủ để xác định mọi loại thông báo dùng FCM.\n");
        } catch (PackageManager.NameNotFoundException e) { out.append("Không tìm thấy app trong hồ sơ chính hoặc app không hiển thị cho NotiGuard.\n"); }
        return out.toString();
    }
}
