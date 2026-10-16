// SPDX-License-Identifier: GPL-3.0-or-later
// Java adaptation of AurogonConfig / MilletNoRestrictList, dingwen07/hyperos-fcm-fix.
// See THIRD_PARTY_NOTICES.md for the pinned source and license.
package vn.notiguard;

import java.util.*;

public final class XiaomiRules {
    public static final String MILLET = "MILLET_NO_RESTRICT_APP";
    public static final String AUROGON = "aurogon_enable";
    private static final String MARKER = BuildConfig.APPLICATION_ID + "/__NOTIGUARD_AUROGON_V1__";

    public static String normalize(String value) {
        if (value == null) return "";
        String result = value.replaceFirst("\\r?\\n$", "");
        return result.equals("null") ? "" : result;
    }

    public static List<String> milletPackages(String raw) {
        Set<String> result = new LinkedHashSet<>();
        for (String part : normalize(raw).split(",")) if (!part.trim().isEmpty()) result.add(part.trim());
        return new ArrayList<>(result);
    }

    public static String addGms(String raw) {
        List<String> packages = milletPackages(raw);
        if (!packages.contains(GuardConfig.GMS)) packages.add(GuardConfig.GMS);
        return String.join(", ", packages);
    }

    public static String removeGms(String raw) {
        List<String> packages = milletPackages(raw);
        packages.remove(GuardConfig.GMS);
        return String.join(", ", packages);
    }

    private static List<String> sections(String raw) {
        String value = normalize(raw);
        return value.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(value.split(";", -1)));
    }

    public static String mergeAurogon(String raw, Collection<String> desired) {
        for (String pkg : desired) GuardConfig.validatePackage(pkg);
        List<String> sections = sections(raw);
        sections.removeIf(section -> section.startsWith("broadcastctrl:")
                && Arrays.asList(section.split("#", -1)).contains(MARKER));
        boolean enabled = true;
        Map<String, String> actions = new HashMap<>();
        for (String section : sections) {
            String[] tokens = section.split("#", -1);
            if (!tokens[0].startsWith("broadcastctrl:")) continue;
            if (tokens[0].contains("false")) enabled = false;
            else if (tokens[0].contains("true")) enabled = true;
            for (int i = 1; i < tokens.length; i++) {
                int slash = tokens[i].indexOf('/');
                if (slash > 0 && slash < tokens[i].length() - 1)
                    actions.put(tokens[i].substring(0, slash), tokens[i].substring(slash + 1));
            }
        }
        if (enabled) {
            List<String> tokens = new ArrayList<>();
            tokens.add("broadcastctrl:true"); tokens.add(MARKER);
            for (String pkg : new TreeSet<>(desired)) {
                String base = actions.get(pkg);
                if ("ALL".equals(base) || (base != null && Arrays.asList(base.split(",")).contains(GuardConfig.FCM))) continue;
                tokens.add(pkg + "/" + (base == null || base.isEmpty() ? "" : base + ",") + GuardConfig.FCM);
            }
            if (tokens.size() > 2) sections.add(String.join("#", tokens));
        }
        return String.join(";", sections);
    }
}
