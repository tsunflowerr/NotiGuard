package vn.notiguard;

import java.util.ArrayDeque;

/** Bounded reaction to repeated vendor rewrites; uses monotonic elapsed time. */
final class RepairPolicy {
    static final int WATCHDOG_SECONDS = 900;
    private final ArrayDeque<Long> costlyPasses = new ArrayDeque<>();

    void record(long elapsedMillis, int changed, int failed) {
        expire(elapsedMillis);
        if (changed > 0 || failed > 0) {
            costlyPasses.addLast(elapsedMillis);
            while (costlyPasses.size() > 6) costlyPasses.removeFirst();
        }
    }
    long eventDelayMillis(long elapsedMillis) {
        expire(elapsedMillis);
        return costlyPasses.size() >= 6 ? 30_000 : costlyPasses.size() >= 3 ? 5_000 : 400;
    }
    static int watchdogSeconds(boolean observing, GuardConfig config) {
        // Enhanced per-app AppOps are not Settings URIs; keep their previous polling interval.
        return observing && !config.enhanced ? WATCHDOG_SECONDS : config.intervalSeconds;
    }
    private void expire(long now) { while (!costlyPasses.isEmpty() && now - costlyPasses.peekFirst() >= 60_000) costlyPasses.removeFirst(); }
}
