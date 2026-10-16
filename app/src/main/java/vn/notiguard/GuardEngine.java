package vn.notiguard;

import org.json.JSONObject;
import java.util.*;
import java.util.regex.*;

/** All mutations are scoped to owned entries or explicit per-package exemptions. */
public final class GuardEngine {
    private final Shell shell;
    public GuardEngine(Shell shell) { this.shell = shell; }

    private String setting(String table, String key) throws Exception {
        return shell.run("/system/bin/cmd", "settings", "--user", "0", "get", table, key);
    }
    private void writeSetting(String table, String key, String value) throws Exception {
        if (value.isEmpty()) shell.run("/system/bin/cmd", "settings", "--user", "0", "delete", table, key);
        else shell.run("/system/bin/cmd", "settings", "--user", "0", "put", table, key, value);
        String verified = XiaomiRules.normalize(setting(table, key));
        if (!verified.equals(value)) throw new IllegalStateException(key + ": ghi nhưng đọc lại không khớp");
    }
    private String appOp(String pkg, String op) throws Exception {
        return parseMode(shell.run("/system/bin/cmd", "appops", "get", "--user", "0", pkg, op), op);
    }
    static String parseMode(String output, String op) {
        if (output.contains("No operations")) return "default";
        Matcher match = Pattern.compile("(?m)^\\s*" + Pattern.quote(op) + ":\\s*(allow|ignore|deny|default|foreground)\\b").matcher(output);
        if (!match.find()) throw new IllegalStateException("Không xác định được AppOp " + op + ": " + output);
        return match.group(1);
    }
    private void setOp(String pkg, String op, String mode) throws Exception {
        if (!Set.of("allow", "ignore", "deny", "default", "foreground").contains(mode)) throw new IllegalArgumentException("Invalid AppOp mode");
        shell.run("/system/bin/cmd", "appops", "set", "--user", "0", pkg, op, mode);
        if (!appOp(pkg, op).equals(mode)) throw new IllegalStateException(pkg + ": AppOp không được giữ lại");
    }
    private Set<String> dozePackages() throws Exception {
        String output = shell.run("/system/bin/dumpsys", "deviceidle", "whitelist");
        Set<String> packages = new HashSet<>();
        for (String line : output.split("\\R")) {
            String[] fields = line.split(",");
            if (fields.length >= 3) packages.add(fields[1].trim());
        }
        return packages;
    }
    private Set<String> enhancedTargets(GuardConfig config) {
        Set<String> targets = new TreeSet<>(config.packages);
        targets.add(GuardConfig.GMS); return targets;
    }

    public JSONObject inspect(GuardConfig config) throws Exception {
        JSONObject baseline = new JSONObject().put("version", 1);
        String millet = setting("system", XiaomiRules.MILLET);
        String aurogon = setting("global", XiaomiRules.AUROGON);
        baseline.put("milletSupported", !"null".equals(millet));
        baseline.put("aurogonSupported", !"null".equals(aurogon));
        baseline.put("gmsPreexisting", XiaomiRules.milletPackages(millet).contains(GuardConfig.GMS));
        baseline.put("milletOriginal", millet).put("aurogonOriginal", aurogon);
        JSONObject targets = new JSONObject();
        if (config.enhanced) {
            Set<String> doze = dozePackages();
            for (String pkg : enhancedTargets(config)) {
                targets.put(pkg, new JSONObject().put("dozePreexisting", doze.contains(pkg))
                        .put("RUN_IN_BACKGROUND", appOp(pkg, "RUN_IN_BACKGROUND"))
                        .put("RUN_ANY_IN_BACKGROUND", appOp(pkg, "RUN_ANY_IN_BACKGROUND")));
            }
        }
        baseline.put("targets", targets);
        return baseline;
    }

    public static String inspectionReport(JSONObject baseline) throws Exception {
        return "MILLET: " + (baseline.getBoolean("milletSupported") ? "có thiết lập; chưa chứng minh ROM thực thi" : "chưa thấy thiết lập, sẽ bỏ qua")
                + "\nAurogon: " + (baseline.getBoolean("aurogonSupported") ? "có thiết lập; chưa chứng minh ROM thực thi" : "chưa thấy thiết lập, sẽ bỏ qua")
                + "\nGMS đã có trong MILLET: " + baseline.getBoolean("gmsPreexisting")
                + "\nMiễn Doze dự kiến: " + baseline.getJSONObject("targets").length()
                + "\nĐây là kiểm tra cấu hình, không đo được thông báo bị mất.";
    }

    public Outcome apply(GuardConfig config, JSONObject baseline) throws Exception {
        validateBaseline(config, baseline);
        Outcome out = new Outcome();
        if (baseline.getBoolean("milletSupported")) {
            out.attempt("MILLET / Google Play Services", () -> {
                String before = setting("system", XiaomiRules.MILLET);
                if ("null".equals(before)) throw new IllegalStateException("ROM đã xóa thiết lập; bỏ qua");
                if (!XiaomiRules.milletPackages(before).contains(GuardConfig.GMS)) {
                    stable("system", XiaomiRules.MILLET, before);
                    writeSetting("system", XiaomiRules.MILLET, XiaomiRules.addGms(before)); return true;
                }
                return false;
            });
        } else out.unsupported("MILLET: chưa thấy thiết lập trên ROM này");
        if (baseline.getBoolean("aurogonSupported")) {
            out.attempt("Aurogon / " + config.packages.size() + " app", () -> {
                String before = setting("global", XiaomiRules.AUROGON);
                if ("null".equals(before)) throw new IllegalStateException("ROM đã xóa thiết lập; bỏ qua");
                String after = XiaomiRules.mergeAurogon(before, config.packages);
                if (!after.equals(XiaomiRules.normalize(before))) {
                    stable("global", XiaomiRules.AUROGON, before);
                    writeSetting("global", XiaomiRules.AUROGON, after); return true;
                }
                return false;
            });
        } else out.unsupported("Aurogon: chưa thấy thiết lập trên ROM này");
        if (config.enhanced) {
            JSONObject targets = baseline.getJSONObject("targets");
            Set<String> doze = dozePackages();
            for (String pkg : enhancedTargets(config)) {
                JSONObject saved = targets.getJSONObject(pkg);
                if (!saved.getBoolean("dozePreexisting")) out.attempt(pkg + " / Doze", () -> {
                    if (doze.contains(pkg)) return false;
                    shell.run("/system/bin/dumpsys", "deviceidle", "whitelist", "+" + pkg);
                    if (!dozePackages().contains(pkg)) throw new IllegalStateException("Miễn Doze không có hiệu lực");
                    return true;
                });
                for (String op : List.of("RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND")) {
                    if (!saved.getString(op).equals("allow")) out.attempt(pkg + " / " + op, () -> {
                        if (appOp(pkg, op).equals("allow")) return false;
                        setOp(pkg, op, "allow"); return true;
                    });
                }
            }
        }
        return out;
    }

    private void stable(String table, String key, String expected) throws Exception {
        if (!setting(table, key).equals(expected)) throw new IllegalStateException(key + ": hệ thống đang đổi cấu hình, thử lại sau");
    }

    private void validateBaseline(GuardConfig config, JSONObject baseline) throws Exception {
        if (baseline.getInt("version") != 1) throw new IllegalArgumentException("Unknown snapshot version");
        if (config.enhanced) {
            JSONObject targets = baseline.getJSONObject("targets");
            for (String pkg : enhancedTargets(config)) if (!targets.has(pkg)) throw new IllegalStateException("Thiếu bản sao trước thay đổi: " + pkg);
        }
    }

    public Outcome restore(JSONObject baseline) throws Exception {
        if (baseline.getInt("version") != 1) throw new IllegalArgumentException("Unknown snapshot version");
        Outcome out = new Outcome();
        out.attempt("Gỡ quy tắc Aurogon của NotiGuard", () -> {
            String before = setting("global", XiaomiRules.AUROGON);
            String after = XiaomiRules.mergeAurogon(before, Collections.emptySet());
            if (after.equals(XiaomiRules.normalize(before))) return false;
            stable("global", XiaomiRules.AUROGON, before);
            writeSetting("global", XiaomiRules.AUROGON, after); return true;
        });
        if (baseline.getBoolean("milletSupported") && !baseline.getBoolean("gmsPreexisting")) {
            out.attempt("Gỡ mục GMS đã thêm vào MILLET", () -> {
                String before = setting("system", XiaomiRules.MILLET);
                if (!XiaomiRules.milletPackages(before).contains(GuardConfig.GMS)) return false;
                stable("system", XiaomiRules.MILLET, before);
                String after = XiaomiRules.removeGms(before);
                // Restore an originally present empty string as an empty string, not a missing key.
                if (after.isEmpty() && !"null".equals(baseline.getString("milletOriginal"))) {
                    shell.run("/system/bin/cmd", "settings", "--user", "0", "put", "system", XiaomiRules.MILLET, "");
                    if (!setting("system", XiaomiRules.MILLET).isEmpty()) throw new IllegalStateException("MILLET: khôi phục chuỗi rỗng thất bại");
                } else writeSetting("system", XiaomiRules.MILLET, after);
                return true;
            });
        }
        JSONObject targets = baseline.getJSONObject("targets");
        Iterator<String> keys = targets.keys();
        while (keys.hasNext()) {
            String pkg = keys.next(); GuardConfig.validatePackage(pkg);
            JSONObject saved = targets.getJSONObject(pkg);
            if (!saved.getBoolean("dozePreexisting")) out.attempt(pkg + " / khôi phục Doze", () -> {
                if (!dozePackages().contains(pkg)) return false;
                shell.run("/system/bin/dumpsys", "deviceidle", "whitelist", "-" + pkg);
                if (dozePackages().contains(pkg)) throw new IllegalStateException("Gỡ miễn Doze thất bại");
                return true;
            });
            for (String op : List.of("RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND")) {
                String original = saved.getString(op);
                if (!original.equals("allow")) out.attempt(pkg + " / khôi phục " + op, () -> {
                    String current = appOp(pkg, op);
                    // Keep a subsequent explicit change made by another tool or by the user.
                    if (!current.equals("allow")) return false;
                    setOp(pkg, op, original); return true;
                });
            }
        }
        return out;
    }

    interface Action { boolean run() throws Exception; }
    public static final class Outcome {
        public int changed, failed, unsupported;
        private final StringBuilder lines = new StringBuilder();
        void attempt(String label, Action action) {
            try { boolean change = action.run(); if (change) changed++; lines.append(change ? "Đã đổi: " : "Đã kiểm tra: ").append(label).append('\n'); }
            catch (Exception e) { failed++; lines.append("LỖI: ").append(label).append(" — ").append(e.getMessage()).append('\n'); }
        }
        void unsupported(String message) { unsupported++; lines.append("BỎ QUA: ").append(message).append('\n'); }
        public String report() { return lines + "\n" + changed + " thay đổi · " + failed + " lỗi · " + unsupported + " chưa hỗ trợ\nĐọc lại cấu hình thành công không bảo đảm ROM thực thi hoặc thông báo đến đúng hạn."; }
        public String json() throws Exception { return new JSONObject().put("ok", failed == 0).put("changed", changed).put("failed", failed).put("unsupported", unsupported).put("report", report()).toString(); }
    }
}
