package vn.notiguard;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

/** Normal app-process observer; its lifetime never replaces the shell watchdog. */
final class AppSettingsWatch {
    private final Context context;
    private final ContentObserver observer;
    private boolean registered;

    AppSettingsWatch(Context context, Handler handler, Runnable changed) {
        this.context = context;
        observer = new ContentObserver(handler) {
            @Override public void onChange(boolean selfChange, Uri uri) {
                if (registered) changed.run();
            }
        };
    }

    void start() {
        if (registered) return;
        try {
            context.getContentResolver().registerContentObserver(Settings.System.getUriFor(XiaomiRules.MILLET), false, observer);
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(XiaomiRules.AUROGON), false, observer);
            registered = true;
        } catch (RuntimeException unavailable) { stop(); }
    }

    void stop() {
        registered = false;
        try { context.getContentResolver().unregisterContentObserver(observer); }
        catch (RuntimeException unavailable) { /* Local registered flag still prevents callbacks from requesting repair. */ }
    }
}
