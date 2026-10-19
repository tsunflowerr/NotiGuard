// SPDX-License-Identifier: GPL-3.0-or-later
package vn.notiguard.keeper;

import android.app.job.*;

public final class RepairJob extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        int id = params.getJobId();
        Keeper.repair(this, id == Keeper.CONTENT_JOB ? "danh sách vừa đổi" : id == Keeper.RETRY_JOB ? "thử lại sau giới hạn" : "kiểm tra định kỳ");
        // Cheap (a few package lookups); on a list change it catches an app HyperOS just demoted.
        Health.report(this);
        // A content-trigger job fires once, so re-arm it. The periodic run re-arms it too: on HyperOS a
        // trigger that fires while Greezer has the app frozen stays "ready" but is never started.
        Keeper.schedule(this);
        return false;
    }
    @Override public boolean onStopJob(JobParameters params) { return false; }
}
