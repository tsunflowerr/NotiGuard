package vn.notiguard;

import android.app.Application;

public final class GuardApplication extends Application {
    GuardController controller;
    @Override public void onCreate() {
        super.onCreate(); controller = new GuardController(this);
        if (controller.store.offline()) RecoveryJob.cancel(this);
        else if (controller.store.baseline() != null) RecoveryJob.schedule(this);
    }
}
