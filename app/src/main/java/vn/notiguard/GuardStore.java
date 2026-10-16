package vn.notiguard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

final class GuardStore {
    private final SharedPreferences prefs;
    GuardStore(Context context) { prefs = context.getSharedPreferences("guard", Context.MODE_PRIVATE); }
    GuardConfig config() { return new GuardConfig(new HashSet<>(prefs.getStringSet("packages", Collections.emptySet())), prefs.getBoolean("enhanced", false), prefs.getInt("interval", 120)); }
    void saveConfig(GuardConfig config) {
        commit(prefs.edit().putStringSet("packages", new HashSet<>(config.packages)).putBoolean("enhanced", config.enhanced).putInt("interval", config.intervalSeconds));
    }
    boolean enabled() { return prefs.getBoolean("enabled", false); }
    boolean offline() { return prefs.getBoolean("offline", false); }
    void offline(boolean value) { commit(prefs.edit().putBoolean("offline", value).remove("offlineBoot").remove("offlineElapsed")); }
    int offlineBoot() { return prefs.getInt("offlineBoot", -1); }
    long offlineElapsed() { return prefs.getLong("offlineElapsed", -1); }
    void offlineSample(int boot, long elapsed) { commit(prefs.edit().putInt("offlineBoot", boot).putLong("offlineElapsed", elapsed)); }
    void clearOfflineSample() { commit(prefs.edit().remove("offlineBoot").remove("offlineElapsed")); }
    boolean hasSelection() { return prefs.contains("packages"); }
    String baseline() { return prefs.getString("baseline", null); }
    boolean pending() { return !enabled() && baseline() != null; }
    void begin(String baseline) { commit(prefs.edit().putString("baseline", baseline).putBoolean("enabled", true)); }
    void requestStop() { commit(prefs.edit().putBoolean("enabled", false)); }
    void restored() { commit(prefs.edit().remove("baseline").putBoolean("enabled", false)); }
    String report() { return prefs.getString("report", "Chưa kiểm tra cấu hình."); }
    void report(String report) { prefs.edit().putString("report", report.substring(0, Math.min(20_000, report.length()))).apply(); }
    private void commit(SharedPreferences.Editor editor) { if (!editor.commit()) throw new IllegalStateException("Không lưu được bản sao cấu hình; chưa thay đổi hệ thống"); }
}
