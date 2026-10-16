package vn.notiguard;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.os.Process;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import rikka.shizuku.Shizuku;

final class GuardController {
    private final Context context;
    final GuardStore store;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Runnable> listeners = new ArrayList<>();
    private volatile IGuardService service;
    private boolean binding;
    volatile boolean busy;
    private final Shizuku.UserServiceArgs args;
    private final AppSettingsWatch appWatch;
    private final AtomicBoolean checkQueued = new AtomicBoolean();
    GuardController(Context context) {
        this.context = context; store = new GuardStore(context);
        appWatch = new AppSettingsWatch(context, main, this::requestCheck);
        args = new Shizuku.UserServiceArgs(new ComponentName(context, GuardUserService.class))
                .daemon(true).processNameSuffix("guard").tag("notiguard-v1").version(BuildConfig.VERSION_CODE).debuggable(BuildConfig.DEBUG);
        Shizuku.addBinderReceivedListenerSticky(this::connect);
        Shizuku.addBinderDeadListener(() -> main.post(() -> { service = null; binding = false; changed(); }));
        Shizuku.addRequestPermissionResultListener((request, result) -> {
            if (result == PackageManager.PERMISSION_GRANTED) connect(); else changed();
        });
    }
    boolean ready() { try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; } catch (Exception e) { return false; } }
    boolean connected() { IGuardService current = service; return current != null && current.asBinder().isBinderAlive(); }
    boolean supported() {
        String make = Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        return (make.equals("xiaomi") || make.equals("redmi")) && Process.myUid() < 100_000;
    }
    void authorize() {
        try {
            if (store.offline()) store.offline(false);
            if (!Shizuku.pingBinder()) {
                Intent launch = context.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                if (launch != null) context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                else throw new IllegalStateException("Cài Shizuku từ shizuku.rikka.app rồi khởi động dịch vụ");
            } else if (!ready()) Shizuku.requestPermission(10);
            else connect();
        } catch (Exception e) { report("Shizuku: " + e.getMessage()); }
    }
    void connect() {
        main.post(() -> {
            if (store.offline() || !ready() || connected() || binding || !supported()) { changed(); return; }
            binding = true;
            try { Shizuku.bindUserService(args, connection); }
            catch (Exception e) { binding = false; report("Không nối được dịch vụ: " + e.getMessage()); }
        });
    }
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            main.post(() -> {
                binding = false; service = IGuardService.Stub.asInterface(binder);
                if (store.enabled() || store.pending()) recover(null); else changed();
            });
        }
        @Override public void onServiceDisconnected(ComponentName name) { main.post(() -> { service = null; binding = false; changed(); }); }
    };
    void inspect() { task(() -> {
        JSONObject snapshot = new JSONObject(requireService().inspect(store.config().encode()));
        report(GuardEngine.inspectionReport(snapshot));
    }, null); }
    void start() { task(() -> {
        if (store.offline()) throw new IllegalStateException("Đang ở chế độ chỉ kiểm tra. Chọn kết nối Shizuku trước khi bật bảo vệ.");
        if (store.baseline() != null) throw new IllegalStateException("Cần hoàn tác phiên trước trước khi bật lại");
        GuardConfig config = store.config();
        String snapshot = requireService().inspect(config.encode());
        store.begin(snapshot); // Synchronous durable write BEFORE any privileged mutation.
        RecoveryJob.schedule(context);
        result(requireService().startProtection(config.encode(), snapshot));
    }, null); }
    void stop() {
        store.requestStop(); RecoveryJob.schedule(context);
        recover(null);
    }
    void recover(Runnable finished) { task(() -> {
        if (store.offline()) { RecoveryJob.cancel(context); return; }
        String snapshot = store.baseline();
        if (snapshot == null) { RecoveryJob.cancel(context); return; }
        if (store.enabled()) result(requireService().startProtection(store.config().encode(), snapshot));
        else {
            String raw = requireService().stopProtection(snapshot);
            result(raw);
            if (new JSONObject(raw).getBoolean("ok")) { store.restored(); RecoveryJob.cancel(context); }
        }
    }, finished); }
    void refresh() { task(() -> report(new JSONObject(requireService().status()).getString("report")), null); }
    void diagnoseRuntime() { task(() -> {
        DeviceDiagnostics.RuntimeState runtime = DeviceDiagnostics.readRuntime(context);
        String diagnostic = DeviceDiagnostics.runtime(runtime);
        if (store.offline()) {
            OfflineCheck.Stage stage = OfflineCheck.evaluate(store.baseline() != null || store.enabled(), runtime.gms(), runtime.developer(), runtime.usb(), runtime.wireless(),
                    store.offlineBoot(), runtime.boot(), store.offlineElapsed(), runtime.elapsed());
            if (stage == OfflineCheck.Stage.FIRST_SAMPLE) store.offlineSample(runtime.boot(), runtime.elapsed());
            else if (stage == OfflineCheck.Stage.UNKNOWN || stage == OfflineCheck.Stage.GMS_MISSING || stage == OfflineCheck.Stage.DEBUG_ENABLED || stage == OfflineCheck.Stage.MONITORING) store.clearOfflineSample();
            diagnostic += "\nCHẾ ĐỘ CHỈ KIỂM TRA · không ghi cấu hình\n" + OfflineCheck.explanation(stage);
        }
        else if (ready() && connected()) diagnostic += "\nRPC Shizuku thành công:\n" + new JSONObject(requireService().status()).getString("report");
        else diagnostic += "\nKhông có kết nối để duy trì/hoàn tác bằng Shizuku. Cấu hình đã áp dụng có thể còn tồn tại nhưng không bảo đảm được tự sửa.";
        report(diagnostic);
    }, null); }
    void enterOffline() { task(() -> {
        // Persist stop before attempting IPC; failed rollback remains recoverable after reconnect.
        store.requestStop();
        String saved = store.baseline();
        if (saved != null) {
            RecoveryJob.schedule(context);
            String raw = requireService().stopProtection(saved);
            result(raw);
            if (!new JSONObject(raw).getBoolean("ok")) throw new IllegalStateException("Hoàn tác chưa xong; chưa chuyển sang chế độ chỉ kiểm tra.");
            store.restored();
        }
        store.offline(true); RecoveryJob.cancel(context);
        report("Đã chuyển sang CHỈ KIỂM TRA. Không có công việc tự sửa chạy nền của NotiGuard.\nMở cấu hình pin GMS, chọn Không hạn chế nếu hệ thống có lựa chọn này. Sau đó tắt các cờ gỡ lỗi và kiểm tra lại.\nNếu ROM ẩn lựa chọn, app chưa có cách đặt cấu hình bền vững đã được xác minh. Không cần cấp thêm quyền.");
    }, null); }
    private void requestCheck() {
        if (!store.enabled() || store.offline() || !connected() || !checkQueued.compareAndSet(false, true)) return;
        io.execute(() -> {
            try { if (store.enabled() && !store.offline()) requireService().requestCheck(); }
            catch (Exception ignored) { /* The shell watchdog is independent of this optional app callback. */ }
            finally { checkQueued.set(false); }
        });
    }
    void diagnoseSystem() { task(() -> report(DeviceDiagnostics.system(context)), null); }
    void diagnoseApp(String pkg) { task(() -> report(DeviceDiagnostics.app(context, pkg)), null); }
    private IGuardService requireService() {
        if (!connected()) throw new IllegalStateException("Chưa kết nối Shizuku. Nếu chờ hoàn tác, cấu hình vẫn được lưu để gỡ khi kết nối lại.");
        return service;
    }
    private void result(String raw) throws Exception { report(new JSONObject(raw).getString("report")); }
    private interface Work { void run() throws Exception; }
    private void task(Work work, Runnable finished) {
        if (busy) { if (finished != null) finished.run(); return; }
        busy = true; changed();
        io.execute(() -> {
            try { work.run(); } catch (Exception e) { report("LỖI: " + e.getMessage()); }
            finally { busy = false; main.post(() -> { changed(); if (finished != null) finished.run(); }); }
        });
    }
    private void report(String report) { store.report(report); main.post(this::changed); }
    void addListener(Runnable listener) { listeners.add(listener); }
    void removeListener(Runnable listener) { listeners.remove(listener); }
    void changed() {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(this::changed); return; }
        if (store.enabled() && !store.offline()) appWatch.start(); else appWatch.stop();
        for (Runnable listener : new ArrayList<>(listeners)) listener.run();
    }
}
