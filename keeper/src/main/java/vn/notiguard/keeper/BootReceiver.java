// SPDX-License-Identifier: GPL-3.0-or-later
package vn.notiguard.keeper;

import android.content.*;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        // Content-trigger jobs are not persisted across reboot; re-arm them here.
        Keeper.repair(context, Intent.ACTION_BOOT_COMPLETED.equals(action) ? "khởi động máy" : "cập nhật Keeper");
        Keeper.schedule(context);
        Health.report(context);
    }
}
