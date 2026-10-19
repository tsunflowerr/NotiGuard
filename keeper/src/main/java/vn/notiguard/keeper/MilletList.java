// SPDX-License-Identifier: GPL-3.0-or-later
package vn.notiguard.keeper;

import java.util.*;

/** Pure logic for Settings.System MILLET_NO_RESTRICT_APP. Only ever appends GMS. */
public final class MilletList {
    public static final String KEY = "MILLET_NO_RESTRICT_APP";
    public static final String GMS = "com.google.android.gms";
    private MilletList() { }

    static List<String> packages(String raw) {
        Set<String> result = new LinkedHashSet<>();
        if (raw != null) for (String part : raw.split(",")) if (!part.trim().isEmpty()) result.add(part.trim());
        return new ArrayList<>(result);
    }

    public static boolean hasGms(String raw) { return packages(raw).contains(GMS); }

    /** Apps previously seen with "No restrictions" that are no longer in the list, ignoring GMS itself. */
    static List<String> removed(Collection<String> known, List<String> current) {
        List<String> out = new ArrayList<>();
        // An empty list means the key was deleted or rebuilt badly, not that every app was demoted.
        if (current.isEmpty()) return out;
        for (String pkg : new TreeSet<>(known)) if (!pkg.equals(GMS) && !current.contains(pkg)) out.add(pkg);
        return out;
    }

    /** Returns the value to write, or null when GMS is already present. Vendor text is kept verbatim. */
    public static String withGms(String raw) {
        if (hasGms(raw)) return null;
        String base = raw == null ? "" : raw.trim();
        while (base.endsWith(",")) base = base.substring(0, base.length() - 1).trim();
        // PowerKeeper writes ", "; Greezer trims each entry, so this matches its own format.
        return base.isEmpty() ? GMS : base + ", " + GMS;
    }
}
