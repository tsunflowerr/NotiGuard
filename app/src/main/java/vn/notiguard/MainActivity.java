package vn.notiguard;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(22, 46, 39), MUTED = Color.rgb(83, 101, 92), GREEN = Color.rgb(17, 105, 87);
    private GuardController guard;
    private LinearLayout body, appsContainer;
    private TextView state, report, connection;
    private Button enable, permission, inspect, refresh, offline, nativeGms;
    private Switch enhanced, slow;
    private final List<CheckBox> appChoices = new ArrayList<>();
    private final Runnable listener = this::render;
    private boolean rendering;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        guard = ((GuardApplication) getApplication()).controller;
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Color.rgb(244, 246, 242)); scroll.setFillViewport(true);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(22), dp(36), dp(22), dp(32));
        scroll.addView(body); setContentView(scroll);
        TextView badge = text("XIAOMI 14 · HYPEROS 3", 12, GREEN); badge.setLetterSpacing(.12f); body.addView(badge);
        TextView title = text("NotiGuard", 38, INK); title.setTypeface(null, Typeface.BOLD); body.addView(title);
        body.addView(text("Kiểm tra và giảm hạn chế thông báo trên Xiaomi.", 16, MUTED));
        LinearLayout statusCard = card(); state = text("Chưa bật bảo vệ", 21, INK); state.setTypeface(null, Typeface.BOLD); statusCard.addView(state);
        connection = text("", 14, MUTED); statusCard.addView(connection);
        statusCard.addView(text(Build.MODEL + " · Android " + Build.VERSION.RELEASE + " · " + BuildConfig.VERSION_NAME, 12, MUTED));
        permission = button("1. Kết nối / cấp quyền Shizuku", () -> guard.authorize()); statusCard.addView(permission);
        enable = button("2. Bật bảo vệ", () -> { if (guard.store.enabled() || guard.store.pending()) guard.stop(); else guard.start(); }); statusCard.addView(enable);
        body.addView(statusCard);

        LinearLayout standalone = card(); standalone.addView(heading("Dùng khi tắt Developer Options"));
        standalone.addView(text("Trên máy đã thử, tắt Developer Options làm mất Shizuku và GMS khỏi MILLET. Bản này chưa có cách tự duy trì đã được xác minh trong điều kiện đó. Bạn có thể thử cấu hình GMS qua màn hình hệ thống nếu ROM cho phép.", 14, MUTED));
        offline = button("Chuyển sang chỉ kiểm tra · hoàn tác trước", this::enterOffline); standalone.addView(offline);
        nativeGms = button("Mở cấu hình pin Google Play services", this::openNativeGms); standalone.addView(nativeGms);
        standalone.addView(button("Kiểm tra cấu hình khi tắt gỡ lỗi", () -> guard.diagnoseRuntime()));
        standalone.addView(text("Chế độ chỉ kiểm tra không tự ghi cấu hình, không có job tự sửa hoặc yêu cầu giữ Shizuku. Nếu GMS vẫn thiếu hoặc hệ thống ẩn Không hạn chế, chưa thể xác nhận giải pháp dùng độc lập.", 14, MUTED));
        body.addView(standalone);

        LinearLayout power = card(); power.addView(heading("Ưu tiên pin"));
        power.addView(text("Khi app còn sống, nhận thay đổi cấu hình để yêu cầu kiểm tra sớm. Dịch vụ Shizuku vẫn có dự phòng 2/5 phút; chỉ tăng lên 15 phút nếu observer ở shell đã nhận sự kiện thực tế. Không giữ CPU thức; máy ngủ sâu có thể trì hoãn xử lý.", 14, MUTED));
        enhanced = toggle("Miễn Doze cho nhóm app đã chọn", "Chỉ bật nếu thử chế độ mặc định vẫn trễ. Có thể hao pin hơn; áp dụng cả Google Play Services.");
        power.addView(enhanced);
        enhanced.setOnCheckedChangeListener((v, checked) -> updateConfig());
        slow = toggle("Chu kỳ dự phòng chậm: 5 phút", "Áp dụng khi không theo dõi được sự kiện hoặc đang miễn Doze. Chế độ sự kiện mặc định vẫn có dự phòng 15 phút."); power.addView(slow);
        slow.setOnCheckedChangeListener((v, checked) -> updateConfig());
        body.addView(power);

        LinearLayout appsCard = card(); appsCard.addView(heading("App cần ưu tiên"));
        appsCard.addView(text("Chọn tối đa 30 app. Với bản đầu, lựa chọn này áp dụng cho chế độ miễn Doze. Aurogon chỉ được thêm quy tắc khi đã có cấu hình trên ROM. Không tự mở lại app bạn đã buộc dừng.", 14, MUTED));
        EditText search = new EditText(this); search.setSingleLine(); search.setTextSize(14); search.setHint("Tìm Zalo, Gmail, ngân hàng…"); appsCard.addView(search);
        appsContainer = new LinearLayout(this); appsContainer.setOrientation(LinearLayout.VERTICAL); appsCard.addView(appsContainer); body.addView(appsCard);
        search.setVisibility(View.GONE); appsContainer.setVisibility(View.GONE);
        Button showApps = button("Chọn app cần ưu tiên", () -> {
            boolean open = appsContainer.getVisibility() == View.VISIBLE;
            appsContainer.setVisibility(open ? View.GONE : View.VISIBLE); search.setVisibility(open ? View.GONE : View.VISIBLE);
        }); appsCard.addView(showApps, 2);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                for (CheckBox choice : appChoices) choice.setVisibility(choice.getText().toString().toLowerCase(Locale.ROOT).contains(s.toString().toLowerCase(Locale.ROOT)) ? View.VISIBLE : View.GONE);
            }
            public void afterTextChanged(android.text.Editable s) { }
        });
        loadApps();

        LinearLayout diagnostics = card(); diagnostics.addView(heading("Kiểm tra & hoàn tác"));
        diagnostics.addView(button("Kiểm tra nguyên nhân khác · hệ thống", () -> guard.diagnoseSystem()));
        diagnostics.addView(button("Kiểm tra từng app", this::chooseDiagnosticApp));
        diagnostics.addView(button("Kiểm tra trạng thái thật · Developer Options", () -> guard.diagnoseRuntime()));
        diagnostics.addView(button("Hướng dẫn kiểm tra với app ngân hàng", this::bankCompatibility));
        inspect = button("Đọc cấu hình · không thay đổi hệ thống", () -> guard.inspect()); diagnostics.addView(inspect);
        refresh = button("Cập nhật trạng thái dịch vụ", () -> guard.refresh()); diagnostics.addView(refresh);
        diagnostics.addView(button("Sao chép báo cáo kỹ thuật", () -> {
            getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("NotiGuard", report.getText()));
            Toast.makeText(this, "Đã sao chép; báo cáo không chứa nội dung thông báo", Toast.LENGTH_SHORT).show();
        }));
        report = text("", 13, MUTED); report.setTextIsSelectable(true); diagnostics.addView(report); body.addView(diagnostics);

        LinearLayout privacy = card(); privacy.addView(heading("Quyền ít, thao tác rõ"));
        privacy.addView(text("Không có quyền Internet, đọc thông báo hay Trợ năng. Không thu nội dung tin nhắn hoặc dữ liệu ngân hàng. Shizuku chỉ được dùng để đọc / sửa cấu hình nguồn điện đã mô tả. Hãy tắt bảo vệ và hoàn tác trước khi gỡ app.", 14, MUTED));
        privacy.addView(text("Khởi động lại máy: cần bật lại Shizuku. Nếu Shizuku bị dừng, NotiGuard không thể duy trì hoặc hoàn tác cho đến khi kết nối lại. Tiết kiệm pin và độ trễ phải được đo thực tế; bản thử nghiệm chưa bảo đảm hết mất thông báo.", 14, MUTED));
        privacy.addView(button("Mở cài đặt pin của NotiGuard", () -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));
        privacy.addView(button("Nguồn tham khảo & giấy phép", () -> new AlertDialog.Builder(this).setTitle("Mã nguồn mở")
                .setMessage("NotiGuard: GPL-3.0-or-later. Hai bộ xử lý cấu hình được chuyển từ dingwen07/hyperos-fcm-fix, commit fe594239b01ba4b67d60da764683cadd13e5d287, GPL-3.0. Shizuku API 13.1.5: MIT.\n\nMã và tài liệu kiểm tra bảo mật nằm trong thư mục dự án. Đây là bản thử nghiệm; không có tuyên bố kiểm toán bảo mật độc lập.")
                .setPositiveButton("Đóng", null).show())); body.addView(privacy);
        guard.addListener(listener); guard.connect(); render();
    }

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Map<String, AppRow> found = new HashMap<>();
            Set<String> fcm = new HashSet<>();
            for (ResolveInfo receiver : pm.queryBroadcastReceivers(new Intent(GuardConfig.FCM), PackageManager.GET_META_DATA))
                if (receiver.activityInfo != null) fcm.add(receiver.activityInfo.packageName);
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo info : pm.queryIntentActivities(launcher, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && !pkg.equals("com.google.android.gm")) continue;
                found.put(pkg, new AppRow(pkg, info.loadLabel(pm).toString(), fcm.contains(pkg)));
            }
            List<AppRow> rows = new ArrayList<>(found.values()); rows.sort(Comparator.<AppRow, Boolean>comparing(row -> !suggested(row)).thenComparing(row -> row.label.toLowerCase(Locale.ROOT)));
            runOnUiThread(() -> {
                if (!guard.store.hasSelection() && guard.store.baseline() == null) {
                    Set<String> initial = new HashSet<>();
                    for (AppRow row : rows) if (suggested(row) && initial.size() < 30) initial.add(row.pkg);
                    guard.store.saveConfig(new GuardConfig(initial, false, 120));
                }
                for (AppRow row : rows) {
                    CheckBox check = new CheckBox(this); check.setText(String.format("%s\n%s · %s", row.label, row.pkg, row.fcm ? "thấy bộ nhận FCM" : "chưa xác định đường push"));
                    check.setTextSize(13); check.setTextColor(INK); check.setTag(row.pkg); check.setPadding(0, dp(7), 0, dp(7));
                    check.setOnCheckedChangeListener((v, checked) -> updateConfig()); appChoices.add(check); appsContainer.addView(check);
                }
                if (rows.isEmpty()) appsContainer.addView(text("Chưa tìm được app có biểu tượng mở trong hồ sơ chính.", 14, MUTED));
                render();
            });
        }, "app-list").start();
    }
    private record AppRow(String pkg, String label, boolean fcm) { }
    private void enterOffline() {
        new AlertDialog.Builder(this).setTitle("Chuyển sang chỉ kiểm tra")
                .setMessage("NotiGuard sẽ dừng tự sửa và hoàn tác phiên do app quản lý trước. Nếu còn snapshot, cần Shizuku hoạt động để hoàn tác.\n\nSau đó thử chọn Không hạn chế cho Google Play services bằng màn hình hệ thống, tắt gỡ lỗi rồi kiểm tra. Đây là hướng cần thử, chưa được xác nhận trên ROM này. Không cấp thêm quyền và không thay đổi app ngân hàng.")
                .setPositiveButton("Hoàn tác & chuyển", (d, w) -> guard.enterOffline()).setNegativeButton("Đóng", null).show();
    }
    private void openNativeGms() {
        if (!guard.store.offline() || guard.store.baseline() != null) {
            Toast.makeText(this, "Chuyển sang chỉ kiểm tra và hoàn tác trước để kết quả không bị NotiGuard tự sửa", Toast.LENGTH_LONG).show(); return;
        }
        new AlertDialog.Builder(this).setTitle("Cấu hình pin GMS của hệ thống")
                .setMessage("Nếu màn hình có mục Tiết kiệm pin / Không hạn chế, chọn Không hạn chế cho Google Play services. Nếu mục này bị ẩn hoặc không mở được, ghi nhận là chưa hỗ trợ.\n\nQuay lại kiểm tra sau khi tắt gỡ lỗi, kiểm tra lần nữa sau ít nhất 2 phút và sau khởi động lại. Mở được màn hình chưa chứng minh cấu hình đã được lưu.")
                .setPositiveButton("Mở màn hình hệ thống", (d, w) -> {
                    if (!NativeBatterySettings.openGms(this)) Toast.makeText(this, "ROM chưa cho mở màn hình này hoặc không có GMS", Toast.LENGTH_LONG).show();
                }).setNegativeButton("Đóng", null).show();
    }
    private void bankCompatibility() {
        new AlertDialog.Builder(this).setTitle("Ngân hàng và Developer Options").setMessage("Trên Xiaomi 14 đã thử, tắt Developer Options làm mất kết nối Shizuku và GMS khỏi MILLET. Không xem cách bật Shizuku rồi tắt gỡ lỗi là giải pháp ổn định.\n\nĐể thử cấu hình bằng màn hình hệ thống, dùng chế độ Chỉ kiểm tra và hoàn tác trước. Nếu ROM không cho đặt GMS thành Không hạn chế, app chưa đáp ứng việc tự sửa khi tắt gỡ lỗi.\n\nBạn tự mở app ngân hàng để kiểm tra; các cờ false không chứng minh ngân hàng chấp nhận. App không che cờ hoặc sửa app ngân hàng.")
                .setPositiveButton("Mở Tùy chọn nhà phát triển", (d, w) -> {
                    try { startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)); }
                    catch (ActivityNotFoundException e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                }).setNegativeButton("Đóng", null).show();
    }
    private void chooseDiagnosticApp() {
        List<String> packages = new ArrayList<>(guard.store.config().packages); Collections.sort(packages);
        if (packages.isEmpty()) { Toast.makeText(this, "Chọn app cần ưu tiên trước", Toast.LENGTH_SHORT).show(); return; }
        String[] labels = new String[packages.size()];
        for (int i = 0; i < labels.length; i++) {
            String pkg = packages.get(i);
            try { labels[i] = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)) + " · " + pkg; }
            catch (PackageManager.NameNotFoundException e) { labels[i] = pkg; }
        }
        new AlertDialog.Builder(this).setTitle("Chẩn đoán app").setItems(labels, (dialog, index) -> {
            String pkg = packages.get(index); guard.diagnoseApp(pkg);
            new AlertDialog.Builder(this).setTitle(labels[index]).setMessage("Báo cáo xuất hiện trong mục Kiểm tra. Bạn có thể mở cài đặt để xem các kênh thông báo; NotiGuard không tự đổi chúng.")
                    .setPositiveButton("Cài đặt thông báo", (d, w) -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg)))
                    .setNeutralButton("Thông tin app", (d, w) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))))
                    .setNegativeButton("Đóng", null).show();
        }).setNegativeButton("Đóng", null).show();
    }
    private static boolean suggested(AppRow row) {
        String label = row.label.toLowerCase(Locale.ROOT).replace(" ", "");
        return Set.of("com.zing.zalo", "com.facebook.orca", "com.google.android.gm", "com.mbmobile", "com.mservice.momotransfer", "com.vnpay.bidv", "com.vib.myvib2", "com.locket.Locket", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically").contains(row.pkg)
                || label.contains("mbbank") || label.contains("bidv") || label.contains("myvib") || label.equals("momo") || label.contains("locket") || label.contains("tiktok");
    }

    private void updateConfig() {
        if (rendering || guard.store.baseline() != null) return;
        Set<String> selected = new HashSet<>();
        for (CheckBox check : appChoices) if (check.isChecked()) selected.add((String) check.getTag());
        try { guard.store.saveConfig(new GuardConfig(selected, enhanced.isChecked(), slow.isChecked() ? 300 : 120)); }
        catch (Exception e) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); render(); }
    }
    private void render() {
        if (isDestroyed()) return;
        rendering = true;
        GuardConfig config = guard.store.config(); boolean locked = guard.store.baseline() != null || guard.busy;
        state.setText(guard.busy ? "Đang kiểm tra…" : guard.store.pending() ? "Đang chờ hoàn tác" : guard.store.offline() ? "Chỉ kiểm tra · không tự sửa" : guard.store.enabled() && (!guard.ready() || !guard.connected()) ? "Bảo vệ đang gián đoạn" : guard.store.enabled() ? "Đã yêu cầu bảo vệ · xem báo cáo" : "Chưa bật bảo vệ");
        connection.setText(!guard.supported() ? "Chỉ hỗ trợ Xiaomi / Redmi ở hồ sơ chủ máy." : guard.store.offline() ? "Không tự kết nối Shizuku. Kiểm tra cấu hình bằng nút bên dưới." : !guard.ready() ? "Shizuku chưa chạy hoặc chưa cấp quyền." : guard.connected() ? "Đã kết nối Shizuku. Xem kết quả từng thao tác bên dưới." : "Đang nối dịch vụ Shizuku…");
        enable.setText(guard.store.pending() ? "Thử hoàn tác lại" : guard.store.enabled() ? "Tắt bảo vệ & hoàn tác" : "2. Bật bảo vệ");
        enable.setEnabled(!guard.busy && !guard.store.offline() && (guard.store.enabled() || guard.store.pending() || guard.connected()) && guard.supported());
        permission.setText(guard.store.offline() ? "Chuyển lại dùng Shizuku" : "1. Kết nối / cấp quyền Shizuku");
        permission.setEnabled(!guard.busy);
        offline.setEnabled(!guard.busy && !guard.store.offline() && guard.supported());
        nativeGms.setEnabled(!guard.busy && guard.store.offline() && guard.store.baseline() == null && guard.supported());
        inspect.setEnabled(guard.connected() && !guard.busy && !guard.store.offline()); refresh.setEnabled(guard.connected() && !guard.busy && !guard.store.offline());
        enhanced.setChecked(config.enhanced); enhanced.setEnabled(!locked);
        slow.setChecked(config.intervalSeconds == 300); slow.setEnabled(!locked);
        for (CheckBox check : appChoices) { check.setChecked(config.packages.contains(check.getTag())); check.setEnabled(!locked); }
        report.setText(guard.store.report()); rendering = false;
    }
    @Override protected void onResume() { super.onResume(); if (guard != null) guard.connect(); }
    @Override protected void onDestroy() { guard.removeListener(listener); super.onDestroy(); }
    private Switch toggle(String label, String description) {
        Switch view = new Switch(this); view.setText(String.format("%s\n%s", label, description)); view.setTextSize(14); view.setTextColor(INK); view.setPadding(0, dp(12), 0, dp(12)); return view;
    }
    private TextView heading(String label) { TextView view = text(label, 19, INK); view.setTypeface(null, Typeface.BOLD); return view; }
    private TextView text(String value, int size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setPadding(0, dp(4), 0, dp(7)); return view; }
    private Button button(String label, Runnable action) { Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextSize(14); button.setOnClickListener(v -> action.run()); return button; }
    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); view.setPadding(dp(16), dp(16), dp(16), dp(14));
        GradientDrawable background = new GradientDrawable(); background.setColor(Color.WHITE); background.setCornerRadius(dp(18)); view.setBackground(background);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2); layout.setMargins(0, dp(18), 0, 0); view.setLayoutParams(layout); return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
