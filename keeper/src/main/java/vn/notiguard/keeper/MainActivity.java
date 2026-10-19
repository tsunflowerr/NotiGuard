// SPDX-License-Identifier: GPL-3.0-or-later
package vn.notiguard.keeper;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;

public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(22, 46, 39), MUTED = Color.rgb(83, 101, 92), GREEN = Color.rgb(17, 105, 87), RED = Color.rgb(165, 42, 32);
    private LinearLayout body, issues;
    private TextView status, log;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Color.rgb(244, 246, 242));
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(22), dp(36), dp(22), dp(32));
        scroll.addView(body); setContentView(scroll);
        TextView title = text("NotiGuard Keeper", 30, INK); title.setTypeface(null, Typeface.BOLD); body.addView(title);
        body.addView(text("Thêm lại Dịch vụ Google Play vào danh sách miễn đóng băng của HyperOS mỗi khi PowerKeeper xóa nó. "
                + "Chạy khi đã tắt gỡ lỗi. Không có quyền Internet, không đọc thông báo, tin nhắn hay dữ liệu app khác.", 15, MUTED));
        status = text("", 17, INK); status.setPadding(0, dp(18), 0, dp(8)); body.addView(status);
        body.addView(button("Kiểm tra và sửa ngay", () -> { Keeper.repair(this, "bấm sửa ngay"); render(); }));
        body.addView(button("Cài đặt Keeper: Không hạn chế + Tự khởi động", () -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            body.addView(button("Quyền Sửa cài đặt hệ thống", () -> open(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName())))));
        TextView warnings = text("Cảnh báo", 15, INK); warnings.setTypeface(null, Typeface.BOLD); warnings.setPadding(0, dp(18), 0, dp(4)); body.addView(warnings);
        issues = new LinearLayout(this); issues.setOrientation(LinearLayout.VERTICAL); body.addView(issues);
        TextView heading = text("Nhật ký gần đây", 15, INK); heading.setTypeface(null, Typeface.BOLD); heading.setPadding(0, dp(18), 0, dp(4)); body.addView(heading);
        log = text("", 13, MUTED); body.addView(log);
        // Legacy apps get the notification permission prompt when a channel is created in the foreground.
        Health.channel(this);
    }

    @Override protected void onResume() {
        super.onResume();
        // Re-arming here also releases a trigger that HyperOS stranded while the app was frozen.
        Keeper.schedule(this);
        Keeper.repair(this, "mở Keeper");
        render();
    }

    private void render() {
        boolean gms = MilletList.hasGms(Settings.System.getString(getContentResolver(), MilletList.KEY));
        Boolean write = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Settings.System.canWrite(this) : null;
        SharedPreferences prefs = Keeper.prefs(this);
        long last = prefs.getLong("lastRepair", 0);
        status.setText("GMS trong MILLET: " + (gms ? "có" : "KHÔNG")
                + "\nQuyền sửa cài đặt hệ thống: " + (write == null ? "mặc định" : write ? "có" : "KHÔNG")
                + "\nTheo dõi thay đổi: " + (Keeper.watching(this) ? "đang chờ" : "chưa đăng ký")
                + "\nĐã thêm lại: " + prefs.getInt("repairs", 0) + " lần"
                + (last == 0 ? "" : " · gần nhất " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(last))));
        status.setTextColor(gms && !Boolean.FALSE.equals(write) ? GREEN : RED);
        String lines = prefs.getString("log", "").trim();
        log.setText(lines.isEmpty() ? "Chưa có sự kiện." : lines);
        issues.removeAllViews();
        java.util.List<Health.Problem> problems = Health.report(this);
        if (problems.isEmpty()) issues.addView(text("Không thấy app bị buộc dừng; Tự động đồng bộ đang bật.", 14, GREEN));
        for (Health.Problem problem : problems) {
            issues.addView(text(problem.text, 14, RED));
            if (problem.fix != null) issues.addView(button(problem.action, () -> open(problem.fix)));
            if (problem.forget != null) issues.addView(button("Bỏ qua app này", () -> { Health.forget(this, problem.forget); render(); }));
        }
        int ignored = Health.ignoredCount(this);
        if (ignored > 0) issues.addView(button("Theo dõi lại tất cả (đang bỏ qua " + ignored + " app)", () -> { Health.watchAll(this); render(); }));
    }

    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException unavailable) { Toast.makeText(this, "Không mở được màn hình này", Toast.LENGTH_SHORT).show(); }
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setLineSpacing(0, 1.15f);
        return view;
    }

    private Button button(String label, Runnable action) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setOnClickListener(v -> action.run());
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
