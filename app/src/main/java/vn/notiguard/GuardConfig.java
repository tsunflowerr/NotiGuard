package vn.notiguard;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class GuardConfig {
    public static final String GMS = "com.google.android.gms";
    public static final String FCM = "com.google.android.c2dm.intent.RECEIVE";
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+");
    public final Set<String> packages;
    public final boolean enhanced;
    public final int intervalSeconds;

    public GuardConfig(Set<String> packages, boolean enhanced, int intervalSeconds) {
        if (packages.size() > 30) throw new IllegalArgumentException("Chỉ chọn tối đa 30 ứng dụng");
        this.packages = new TreeSet<>();
        for (String pkg : packages) { validatePackage(pkg); this.packages.add(pkg); }
        this.enhanced = enhanced;
        if (intervalSeconds != 120 && intervalSeconds != 300) throw new IllegalArgumentException("Invalid interval");
        this.intervalSeconds = intervalSeconds;
    }

    public String encode() {
        try {
            return new JSONObject().put("packages", new JSONArray(packages)).put("enhanced", enhanced)
                    .put("intervalSeconds", intervalSeconds).toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static GuardConfig decode(String raw) throws Exception {
        JSONObject json = new JSONObject(raw);
        JSONArray array = json.getJSONArray("packages");
        Set<String> packages = new TreeSet<>();
        for (int i = 0; i < array.length(); i++) packages.add(array.getString(i));
        return new GuardConfig(packages, json.getBoolean("enhanced"), json.getInt("intervalSeconds"));
    }

    public static void validatePackage(String pkg) {
        if (pkg == null || !PACKAGE.matcher(pkg).matches()) throw new IllegalArgumentException("Invalid package: " + pkg);
    }
}
