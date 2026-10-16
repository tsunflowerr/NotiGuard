package vn.notiguard;

import org.json.JSONObject;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class GuardEngineTest {
    static class Device implements Shell {
        Map<String, String> settings = new HashMap<>();
        Map<String, String> ops = new HashMap<>();
        Set<String> doze = new HashSet<>();
        List<String> commands = new ArrayList<>();
        boolean rejectWrites, race;
        int milletReads;
        Device() { settings.put(XiaomiRules.MILLET, "com.zing.zalo"); settings.put(XiaomiRules.AUROGON, "broadcastctrl:true"); }
        public String run(String... args) {
            List<String> parts = Arrays.asList(args); commands.add(String.join(" ", parts));
            if (parts.contains("settings")) {
                String key = args[args.length - 1];
                if (parts.contains("get")) {
                    if (race && key.equals(XiaomiRules.MILLET) && ++milletReads == 2) settings.put(key, "com.vendor.new");
                    return settings.getOrDefault(key, "null");
                }
                if (rejectWrites) throw new SecurityException("Permission Denial");
                if (parts.contains("put")) settings.put(args[args.length - 2], key);
                else settings.remove(key);
                return "";
            }
            if (parts.contains("appops")) {
                int opIndex = parts.indexOf("get") >= 0 ? parts.indexOf("get") : parts.indexOf("set");
                String pkg = args[opIndex + 3], op = args[opIndex + 4], key = pkg + "/" + op;
                if (parts.contains("set")) { ops.put(key, args[opIndex + 5]); return ""; }
                return ops.containsKey(key) ? op + ": " + ops.get(key) : "No operations.\nDefault mode: allow";
            }
            if (parts.contains("whitelist")) {
                if (args.length == 4) {
                    String action = args[3];
                    if (action.startsWith("+")) doze.add(action.substring(1)); else doze.remove(action.substring(1));
                    return "";
                }
                StringBuilder result = new StringBuilder();
                for (String pkg : doze) result.append("user,").append(pkg).append(",10001\n");
                return result.toString();
            }
            throw new IllegalArgumentException("Unexpected command");
        }
    }
    private GuardConfig balanced() { return new GuardConfig(Set.of("com.zing.zalo"), false, 120); }
    @Test public void balancedDoesNotAlterDozeOrAppOpsAndRestoresOwnChanges() throws Exception {
        Device device = new Device(); GuardEngine engine = new GuardEngine(device);
        JSONObject snapshot = engine.inspect(balanced());
        assertEquals(2, engine.apply(balanced(), snapshot).changed);
        assertEquals(0, engine.apply(balanced(), snapshot).changed);
        device.settings.put(XiaomiRules.MILLET, device.settings.get(XiaomiRules.MILLET) + ", com.vendor.later");
        assertEquals(0, engine.restore(snapshot).failed);
        assertEquals("com.zing.zalo, com.vendor.later", device.settings.get(XiaomiRules.MILLET));
        assertEquals("broadcastctrl:true", device.settings.get(XiaomiRules.AUROGON));
        assertTrue(device.commands.stream().noneMatch(s -> s.contains("deviceidle") || s.contains("appops")));
    }
    @Test public void neverRemovesPreexistingGmsExemption() throws Exception {
        Device device = new Device(); device.settings.put(XiaomiRules.MILLET, GuardConfig.GMS);
        GuardEngine engine = new GuardEngine(device); JSONObject snapshot = engine.inspect(balanced());
        engine.apply(balanced(), snapshot); engine.restore(snapshot);
        assertEquals(GuardConfig.GMS, device.settings.get(XiaomiRules.MILLET));
    }
    @Test public void permissionFailuresAreReportedAsFailures() throws Exception {
        Device device = new Device(); GuardEngine engine = new GuardEngine(device);
        JSONObject snapshot = engine.inspect(balanced()); device.rejectWrites = true;
        GuardEngine.Outcome result = engine.apply(balanced(), snapshot);
        assertEquals(2, result.failed); assertFalse(new JSONObject(result.json()).getBoolean("ok"));
        assertEquals("com.zing.zalo", device.settings.get(XiaomiRules.MILLET));
    }
    @Test public void unknownVendorSettingsAreSkippedWithoutInventingKeys() throws Exception {
        Device device = new Device(); device.settings.clear(); GuardEngine engine = new GuardEngine(device);
        JSONObject snapshot = engine.inspect(balanced()); GuardEngine.Outcome result = engine.apply(balanced(), snapshot);
        assertEquals(2, result.unsupported); assertEquals(0, result.changed); assertTrue(device.settings.isEmpty());
    }
    @Test public void concurrentVendorRewriteIsNotOverwritten() throws Exception {
        Device device = new Device(); GuardEngine engine = new GuardEngine(device);
        JSONObject snapshot = engine.inspect(balanced()); device.race = true;
        GuardEngine.Outcome result = engine.apply(balanced(), snapshot);
        assertTrue(result.failed > 0); assertEquals("com.vendor.new", device.settings.get(XiaomiRules.MILLET));
    }
    @Test public void enhancedRestoresOriginalModesAndPreservesLaterUserChanges() throws Exception {
        Device device = new Device(); String key = "com.zing.zalo/RUN_ANY_IN_BACKGROUND"; device.ops.put(key, "ignore");
        device.doze.add(GuardConfig.GMS);
        GuardConfig config = new GuardConfig(Set.of("com.zing.zalo"), true, 120); GuardEngine engine = new GuardEngine(device);
        JSONObject snapshot = engine.inspect(config); assertEquals(0, engine.apply(config, snapshot).failed);
        assertEquals("allow", device.ops.get(key));
        device.ops.put("com.zing.zalo/RUN_IN_BACKGROUND", "deny");
        assertEquals(0, engine.restore(snapshot).failed);
        assertEquals("ignore", device.ops.get(key)); assertEquals("deny", device.ops.get("com.zing.zalo/RUN_IN_BACKGROUND"));
        assertTrue(device.doze.contains(GuardConfig.GMS)); assertFalse(device.doze.contains("com.zing.zalo"));
    }
    @Test public void unsupportedAppOpOutputCannotBecomeDefaultSilently() {
        try { GuardEngine.parseMode("Permission denied", "RUN_IN_BACKGROUND"); fail("must reject"); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("Không xác định")); }
    }
}
