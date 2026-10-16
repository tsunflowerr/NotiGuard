package vn.notiguard;

import android.app.job.*;
import android.content.*;
import android.os.*;

public final class RecoveryJob extends JobService {
    private static final int ID = 24014;
    public static void schedule(Context context) {
        if (new GuardStore(context).offline()) { cancel(context); return; }
        context.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(ID, new ComponentName(context, RecoveryJob.class))
                .setPersisted(true).setPeriodic(15 * 60 * 1000L).build());
    }
    public static void cancel(Context context) { context.getSystemService(JobScheduler.class).cancel(ID); }
    @Override public boolean onStartJob(JobParameters params) {
        GuardController controller = ((GuardApplication) getApplication()).controller;
        if (controller.store.offline() || controller.store.baseline() == null) { cancel(this); return false; }
        if (controller.connected()) { controller.recover(() -> jobFinished(params, false)); return true; }
        controller.connect();
        // A binder-received callback resumes saved protection when Shizuku becomes available.
        return false;
    }
    @Override public boolean onStopJob(JobParameters params) { return false; }
}
