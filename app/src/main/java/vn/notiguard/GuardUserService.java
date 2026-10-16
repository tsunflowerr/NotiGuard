package vn.notiguard;

import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.os.SystemClock;
import android.net.Uri;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/** The only privileged entry points are the fixed operations in IGuardService. */
public final class GuardUserService extends IGuardService.Stub {
    private final CommandRunner commands = new CommandRunner();
    private final GuardEngine engine = new GuardEngine(commands);
    private final int ownerUid;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> monitor;
    private ScheduledFuture<?> eventCheck;
    private SettingsObserver observer;
    private boolean observerVerified;
    private RepairPolicy policy = new RepairPolicy();
    private long generation;
    private long events, checks;
    private int watchdogSeconds;
    private String observerState = "Chưa đăng ký theo dõi";
    private String lastTrigger = "chưa có";
    private String activeConfig, activeBaseline;
    private GuardConfig currentConfig;
    private JSONObject currentBaseline;
    private long appEvents;
    private String lastReport = "Chưa áp dụng";
    private long lastCheck;
    private int repairs, failures;

    public GuardUserService(Context context) {
        ownerUid = context.getApplicationInfo().uid;
        if (ownerUid < 10_000 || ownerUid >= 100_000) throw new SecurityException("Chỉ hỗ trợ hồ sơ chủ máy");
    }

    private void requireCaller() {
        if (Binder.getCallingUid() != ownerUid) throw new SecurityException("Caller is not NotiGuard");
    }

    @Override public synchronized String inspect(String raw) {
        requireCaller();
        try { return engine.inspect(GuardConfig.decode(raw)).toString(); }
        catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    @Override public synchronized String startProtection(String raw, String saved) {
        requireCaller();
        if (monitor != null && !monitor.isCancelled() && raw.equals(activeConfig) && saved.equals(activeBaseline)) return lastReport;
        stopTimer();
        try {
            GuardConfig config = GuardConfig.decode(raw);
            JSONObject baseline = new JSONObject(saved);
            activeConfig = raw; activeBaseline = saved;
            currentConfig = config; currentBaseline = baseline;
            policy = new RepairPolicy();
            observerVerified = false;
            long session = generation;
            Set<Uri> uris = new HashSet<>();
            if (baseline.getBoolean("milletSupported")) uris.add(Uri.parse("content://settings/system/" + XiaomiRules.MILLET));
            if (baseline.getBoolean("aurogonSupported")) uris.add(Uri.parse("content://settings/global/" + XiaomiRules.AUROGON));
            try {
                observer = new SettingsObserver(uris, () -> onSettingChanged(session, config, baseline), () -> onObserverDisconnected(session, config, baseline));
                observerState = "Theo dõi sự kiện đang đăng ký (đăng ký được chưa chứng minh đã nhận sự kiện)";
            } catch (Exception error) {
                observer = null;
                Throwable cause = error.getCause() == null ? error : error.getCause();
                observerState = "Theo dõi sự kiện không dùng được: " + cause + "; dùng kiểm tra định kỳ";
            }
            apply(config, baseline, "bật / nối lại");
            // ScheduledExecutor is not a wakeup alarm. Deep sleep may delay this check.
            scheduleWatchdog(config, baseline);
            return lastReport;
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    private void scheduleWatchdog(GuardConfig config, JSONObject baseline) {
        if (monitor != null) monitor.cancel(false);
        watchdogSeconds = RepairPolicy.watchdogSeconds(observer != null && observerVerified, config);
        long session = generation;
        monitor = timer.scheduleWithFixedDelay(() -> {
            synchronized (GuardUserService.this) { if (generation == session) apply(config, baseline, "kiểm tra dự phòng"); }
        }, watchdogSeconds, watchdogSeconds, TimeUnit.SECONDS);
    }

    private synchronized void onSettingChanged(long session, GuardConfig config, JSONObject baseline) {
        if (generation != session || monitor == null) return;
        events++;
        if (!observerVerified) {
            observerVerified = true;
            scheduleWatchdog(config, baseline);
        }
        observerState = "Theo dõi sự kiện: đã nhận thay đổi từ hệ thống";
        queueEventCheck(session, config, baseline, "sự kiện cấu hình (shell)");
    }

    @Override public synchronized void requestCheck() {
        requireCaller();
        if (monitor == null || currentConfig == null || currentBaseline == null) return;
        appEvents++;
        // App-process callbacks may disappear when Xiaomi kills the app: keep the short watchdog.
        queueEventCheck(generation, currentConfig, currentBaseline, "sự kiện cấu hình (app)");
    }

    private void queueEventCheck(long session, GuardConfig config, JSONObject baseline, String trigger) {
        if (eventCheck != null && !eventCheck.isDone()) return;
        eventCheck = timer.schedule(() -> {
            synchronized (GuardUserService.this) {
                eventCheck = null;
                if (generation == session && monitor != null) apply(config, baseline, trigger);
            }
        }, policy.eventDelayMillis(SystemClock.elapsedRealtime()), TimeUnit.MILLISECONDS);
    }

    private synchronized void onObserverDisconnected(long session, GuardConfig config, JSONObject baseline) {
        if (generation != session || monitor == null) return;
        observer = null; observerVerified = false; observerState = "ContentService mất kết nối; dùng kiểm tra định kỳ";
        scheduleWatchdog(config, baseline);
    }

    private void apply(GuardConfig config, JSONObject baseline, String trigger) {
        lastCheck = System.currentTimeMillis();
        lastTrigger = trigger; checks++;
        try {
            GuardEngine.Outcome outcome = engine.apply(config, baseline);
            repairs += outcome.changed; failures += outcome.failed;
            policy.record(SystemClock.elapsedRealtime(), outcome.changed, outcome.failed);
            lastReport = outcome.json();
        } catch (Exception e) {
            failures++;
            policy.record(SystemClock.elapsedRealtime(), 0, 1);
            try { lastReport = new JSONObject().put("ok", false).put("report", "LỖI: " + e.getMessage()).toString(); }
            catch (Exception impossible) { throw new IllegalStateException(impossible); }
        }
    }

    @Override public synchronized String stopProtection(String saved) {
        requireCaller(); stopTimer();
        try { lastReport = engine.restore(new JSONObject(saved)).json(); return lastReport; }
        catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    @Override public synchronized String status() {
        requireCaller();
        try {
            String last = lastCheck == 0 ? "chưa có" : DateFormat.getDateTimeInstance().format(new Date(lastCheck));
            return new JSONObject().put("running", monitor != null && !monitor.isCancelled())
                    .put("report", "UID dịch vụ: " + Process.myUid() + "\nLần kiểm tra: " + last
                            + "\nThay đổi trong phiên: " + repairs + " · Lỗi: " + failures
                            + "\n" + observerState
                            + "\nSự kiện shell: " + events + " · Sự kiện app: " + appEvents + " · Lần kiểm tra: " + checks + " · Lệnh hệ thống: " + commands.executionCount()
                            + "\nNguồn kiểm tra gần nhất: " + lastTrigger + "\nChu kỳ dự phòng: " + watchdogSeconds + " giây"
                            + "\n\n" + new JSONObject(lastReport.startsWith("{") ? lastReport : "{}").optString("report", lastReport)).toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private void stopTimer() {
        generation++;
        currentConfig = null; currentBaseline = null;
        if (monitor != null) { monitor.cancel(false); monitor = null; }
        if (eventCheck != null) { eventCheck.cancel(false); eventCheck = null; }
        if (observer != null) {
            try { observer.close(); } catch (Exception e) { observerState = "Gỡ observer: " + e.getMessage(); }
            observer = null;
        }
    }

    @Override public synchronized void destroy() {
        int caller = Binder.getCallingUid();
        if (caller != ownerUid && caller != 0 && caller != 2000) throw new SecurityException("Unauthorized destroy");
        stopTimer(); timer.shutdownNow(); System.exit(0);
    }
}
