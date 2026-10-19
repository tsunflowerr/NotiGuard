// SPDX-License-Identifier: GPL-3.0-or-later
package vn.notiguard.keeper;

import java.util.*;

/** Caps repairs so a fight with another writer of the list cannot become a write loop. */
final class Budget {
    static final int MAX = 12;
    static final long WINDOW_MS = 10 * 60_000L;
    private Budget() { }

    /** Keeps elapsed-realtime stamps inside the window; stamps from a previous boot are in the future. */
    static List<Long> prune(List<Long> stamps, long now) {
        List<Long> kept = new ArrayList<>();
        for (Long stamp : stamps) if (stamp <= now && now - stamp < WINDOW_MS) kept.add(stamp);
        return kept;
    }

    static boolean allows(List<Long> pruned) { return pruned.size() < MAX; }

    static String encode(List<Long> stamps) {
        StringBuilder out = new StringBuilder();
        for (Long stamp : stamps) { if (out.length() > 0) out.append(','); out.append(stamp); }
        return out.toString();
    }

    static List<Long> decode(String raw) {
        List<Long> stamps = new ArrayList<>();
        if (raw == null) return stamps;
        for (String part : raw.split(",")) {
            try { if (!part.isEmpty()) stamps.add(Long.parseLong(part)); }
            catch (NumberFormatException corrupt) { /* Drop a corrupt stamp rather than block repairs forever. */ }
        }
        return stamps;
    }
}
