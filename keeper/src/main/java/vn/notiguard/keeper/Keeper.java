// SPDX-License-Identifier: GPL-3.0-or-later
package vn.notiguard.keeper;

import android.app.job.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import java.text.DateFormat;
import java.util.*;

/** Re-adds GMS to MILLET_NO_RESTRICT_APP after PowerKeeper rebuilds it. Writes no other key. */
final class Keeper {
    static final int CONTENT_JOB = 1, PERIODIC_JOB = 2, RETRY_JOB = 3;
    static final Uri MILLET_URI = Settings.System.getUriFor(MilletList.KEY);
    private static final int LOG_LINES = 20;

    enum Result { OK, REPAIRED, THROTTLED, DENIED, FAILED }

    static SharedPreferences prefs(Context context) { return context.getSharedPreferences("keeper", Context.MODE_PRIVATE); }

    static synchronized Result repair(Context context, String trigger) {
        ContentResolver resolver = context.getContentResolver();
        SharedPreferences prefs = prefs(context);
        String before;
        try { before = Settings.System.getString(resolver, MilletList.KEY); }
        catch (RuntimeException unreadable) { return log(prefs, Result.FAILED, trigger, unreadable.toString()); }
        String after = MilletList.withGms(before);
        if (after == null) { prefs.edit().putLong("lastOk", System.currentTimeMillis()).apply(); return Result.OK; }
        long now = SystemClock.elapsedRealtime();
        List<Long> recent = Budget.prune(stamps(context, prefs), now);
        if (!Budget.allows(recent)) {
            scheduleRetry(context, Budget.WINDOW_MS - (now - recent.get(0)) + 1_000);
            return log(prefs, Result.THROTTLED, trigger, Budget.MAX + " lần/10 phút; tự thử lại sau");
        }
        // Narrow the window in which a PowerKeeper rebuild could be overwritten by a stale copy.
        String fresh = Settings.System.getString(resolver, MilletList.KEY);
        if (!Objects.equals(fresh, before)) {
            before = fresh;
            after = MilletList.withGms(fresh);
            if (after == null) return Result.OK;
        }
        try {
            if (!Settings.System.putString(resolver, MilletList.KEY, after)) return log(prefs, Result.DENIED, trigger, "hệ thống từ chối ghi");
        } catch (SecurityException | IllegalArgumentException refused) { return log(prefs, Result.DENIED, trigger, refused.getMessage()); }
        recent.add(now);
        prefs.edit().putString("stamps", Budget.encode(recent)).putInt("stampsBoot", bootCount(context)).apply();
        String verified = Settings.System.getString(resolver, MilletList.KEY);
        if (!MilletList.hasGms(verified)) return log(prefs, Result.FAILED, trigger, "đọc lại không thấy GMS");
        prefs.edit().putInt("repairs", prefs.getInt("repairs", 0) + 1).putLong("lastRepair", System.currentTimeMillis()).apply();
        // A missing key usually means Android deleted it with a package (see docs/KEEPER.md): other apps lost
        // their exemption too until PowerKeeper rebuilds the list.
        return log(prefs, Result.REPAIRED, trigger, before == null ? "danh sách đã bị xóa; đổi chế độ pin của một app để PowerKeeper dựng lại" : null);
    }

    /** elapsedRealtime restarts at boot, so stamps from an earlier boot are discarded. */
    private static List<Long> stamps(Context context, SharedPreferences prefs) {
        if (prefs.getInt("stampsBoot", -1) != bootCount(context)) return new ArrayList<>();
        return Budget.decode(prefs.getString("stamps", null));
    }

    private static int bootCount(Context context) { return Settings.Global.getInt(context.getContentResolver(), "boot_count", -1); }

    static void schedule(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName job = new ComponentName(context, RepairJob.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Content triggers are observed by system_server: no process of ours has to stay alive.
            scheduler.schedule(new JobInfo.Builder(CONTENT_JOB, job)
                    .addTriggerContentUri(new JobInfo.TriggerContentUri(MILLET_URI, 0))
                    .setTriggerContentUpdateDelay(500).setTriggerContentMaxDelay(2_000).build());
        }
        boolean periodicPending = false;
        for (JobInfo info : scheduler.getAllPendingJobs()) if (info.getId() == PERIODIC_JOB) periodicPending = true;
        // Backstop for a missed trigger and the health check; one run every 30 minutes is negligible.
        if (!periodicPending) scheduler.schedule(new JobInfo.Builder(PERIODIC_JOB, job).setPersisted(true).setPeriodic(30 * 60_000L).build());
    }

    private static void scheduleRetry(Context context, long delayMs) {
        ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(
                new JobInfo.Builder(RETRY_JOB, new ComponentName(context, RepairJob.class))
                        .setMinimumLatency(delayMs).setOverrideDeadline(delayMs + 60_000).build());
    }

    static boolean watching(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        for (JobInfo info : ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getAllPendingJobs())
            if (info.getId() == CONTENT_JOB) return true;
        return false;
    }

    private static Result log(SharedPreferences prefs, Result result, String trigger, String detail) {
        String line = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date())
                + " · " + label(result) + " · " + trigger + (detail == null ? "" : " · " + detail);
        List<String> lines = new ArrayList<>();
        lines.add(line);
        for (String old : prefs.getString("log", "").split("\n")) if (!old.isEmpty()) lines.add(old);
        while (lines.size() > LOG_LINES) lines.remove(lines.size() - 1);
        StringBuilder out = new StringBuilder();
        for (String kept : lines) out.append(kept).append('\n');
        prefs.edit().putString("log", out.toString()).putString("lastResult", result.name()).apply();
        return result;
    }

    static String label(Result result) {
        switch (result) {
            case REPAIRED: return "Đã thêm lại GMS";
            case THROTTLED: return "Tạm dừng sửa (quá nhiều lần)";
            case DENIED: return "Bị từ chối ghi";
            case FAILED: return "Lỗi";
            default: return "GMS đã có";
        }
    }
}
