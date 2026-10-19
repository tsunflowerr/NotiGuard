// SPDX-License-Identifier: GPL-3.0-or-later
package vn.notiguard.keeper;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.*;

/** Cheap checks for other things that silently drop pushes. Rides on existing jobs; adds no wakeups. */
final class Health {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL = "health";

    static final class Problem {
        final String text, action, forget;
        final Intent fix;
        Problem(String text, String action, Intent fix) { this(text, action, fix, null); }
        Problem(String text, String action, Intent fix, String forget) { this.text = text; this.action = action; this.fix = fix; this.forget = forget; }
    }

    /** Stops all warnings about one app, e.g. one that never needs notifications. */
    static void forget(Context context, String pkg) {
        SharedPreferences prefs = Keeper.prefs(context);
        Set<String> ignored = new TreeSet<>(prefs.getStringSet("ignored", Collections.<String>emptySet()));
        ignored.add(pkg);
        prefs.edit().putStringSet("ignored", ignored).apply();
    }

    static int ignoredCount(Context context) { return Keeper.prefs(context).getStringSet("ignored", Collections.<String>emptySet()).size(); }

    static void watchAll(Context context) { Keeper.prefs(context).edit().remove("ignored").apply(); }

    static List<Problem> problems(Context context) {
        List<Problem> out = new ArrayList<>();
        String millet = Settings.System.getString(context.getContentResolver(), MilletList.KEY);
        if (!MilletList.hasGms(millet)) out.add(new Problem("Dịch vụ Google Play chưa có trong danh sách miễn đóng băng"
                + ("DENIED".equals(Keeper.prefs(context).getString("lastResult", "")) ? ": Keeper bị từ chối ghi, kiểm tra quyền Sửa cài đặt hệ thống" : ""), null, null));
        if (!ContentResolver.getMasterSyncAutomatically())
            out.add(new Problem("Tự động đồng bộ dữ liệu đang tắt: Gmail sẽ không báo thư mới", "Mở cài đặt đồng bộ", new Intent(Settings.ACTION_SYNC_SETTINGS)));
        // The apps the user set to "No restrictions" are the ones whose notifications matter to them.
        PackageManager pm = context.getPackageManager();
        List<String> current = MilletList.packages(millet);
        // HyperOS reverts "No restrictions" and Autostart of rarely used apps by itself (seen for Telegram):
        // remember every app ever seen in the list and warn when one silently drops out.
        SharedPreferences prefs = Keeper.prefs(context);
        Set<String> known = new TreeSet<>(prefs.getStringSet("known", Collections.<String>emptySet()));
        Set<String> ignored = prefs.getStringSet("ignored", Collections.<String>emptySet());
        for (String pkg : MilletList.removed(known, current)) {
            if (ignored.contains(pkg)) continue;
            try {
                CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                out.add(new Problem(label + " không còn ở chế độ Không hạn chế (HyperOS có thể đã tự hoàn tác): đặt lại Không hạn chế và Tự khởi động",
                        "Mở cài đặt " + label, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + pkg)), pkg));
            } catch (PackageManager.NameNotFoundException gone) { known.remove(pkg); }
        }
        for (String pkg : current) if (!pkg.equals(MilletList.GMS)) known.add(pkg);
        prefs.edit().putStringSet("known", known).apply();
        for (String pkg : current) {
            if (pkg.equals(MilletList.GMS) || pkg.equals(context.getPackageName()) || ignored.contains(pkg)) continue;
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                // Android does not deliver FCM to a force-stopped app until the user opens it again.
                if ((info.flags & ApplicationInfo.FLAG_STOPPED) != 0) {
                    CharSequence label = pm.getApplicationLabel(info);
                    out.add(new Problem(label + " đang bị buộc dừng: mở app một lần để nhận lại thông báo. Khóa app trong đa nhiệm để \"Xóa tất cả\" không dừng nó nữa.",
                            "Mở " + label, pm.getLaunchIntentForPackage(pkg), pkg));
                }
            } catch (PackageManager.NameNotFoundException gone) { /* The vendor list can name uninstalled packages. */ }
        }
        return out;
    }

    static void channel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(new NotificationChannel(CHANNEL, "Cảnh báo trễ thông báo", NotificationManager.IMPORTANCE_DEFAULT));
    }

    /** Posts one notification only when the set of problems changes, and clears it when they are gone. */
    static List<Problem> report(Context context) {
        List<Problem> problems = problems(context);
        List<String> lines = new ArrayList<>();
        for (Problem problem : problems) lines.add(problem.text);
        String signature = TextUtils.join("\n", lines);
        SharedPreferences prefs = Keeper.prefs(context);
        if (signature.equals(prefs.getString("healthShown", null))) return problems;
        prefs.edit().putString("healthShown", signature).apply();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (problems.isEmpty()) { manager.cancel(NOTIFICATION_ID); return problems; }
        channel(context);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);
        manager.notify(NOTIFICATION_ID, builder.setSmallIcon(R.drawable.ic_keeper)
                .setContentTitle(problems.size() == 1 ? "Có thể bị trễ thông báo" : problems.size() + " vấn đề có thể làm trễ thông báo")
                .setContentText(lines.get(0)).setStyle(new Notification.BigTextStyle().bigText(signature))
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), flags))
                .setAutoCancel(true).build());
        return problems;
    }
}
