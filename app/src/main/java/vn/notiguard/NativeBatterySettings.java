package vn.notiguard;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.provider.Settings;

/** Opens system-owned UI only. It does not modify PowerKeeper's private database. */
final class NativeBatterySettings {
    static boolean openGms(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        int uid;
        try { uid = pm.getApplicationInfo(GuardConfig.GMS, 0).uid; }
        catch (PackageManager.NameNotFoundException missing) { return false; }
        for (String owner : new String[] { "com.miui.securitycenter", "com.miui.powerkeeper" }) {
            Intent candidate = new Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY").setPackage(owner);
            if (launch(activity, candidate, uid)) return true;
        }
        Intent detail = new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.powercenter.legacypowerrank.PowerDetailActivity"));
        if (launch(activity, detail, uid)) return true;
        // Standard app info is only a fallback; ROMs may hide the battery selector for GMS.
        try {
            activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + GuardConfig.GMS)));
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) { return false; }
    }

    private static boolean launch(Activity activity, Intent intent, int uid) {
        PackageManager pm = activity.getPackageManager();
        try {
            ResolveInfo resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            ActivityInfo info = resolved == null ? null : resolved.activityInfo;
            if (info == null || !info.exported || !info.enabled || !info.applicationInfo.enabled
                    || (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) return false;
            if (!info.packageName.equals("com.miui.securitycenter") && !info.packageName.equals("com.miui.powerkeeper")) return false;
            if (info.permission != null && pm.checkPermission(info.permission, activity.getPackageName()) != PackageManager.PERMISSION_GRANTED) return false;
            // Pin the resolved system component; never let an unrelated intent handler receive extras.
            intent.setComponent(new ComponentName(info.packageName, info.name));
            intent.putExtra("package_name", GuardConfig.GMS).putExtra("package_label", "Google Play services").putExtra("uid", uid);
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) { return false; }
    }
}
