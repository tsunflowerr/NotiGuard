package vn.notiguard;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

public class RepairPolicyTest {
    @Test public void healthyChecksNeverTriggerBackoff() {
        RepairPolicy policy = new RepairPolicy();
        for (int i = 0; i < 100; i++) policy.record(i, 0, 0);
        assertEquals(400, policy.eventDelayMillis(100));
    }
    @Test public void repeatedRepairsBackOffAndRecoverAfterQuietWindow() {
        RepairPolicy policy = new RepairPolicy();
        for (int i = 0; i < 3; i++) policy.record(i, 1, 0);
        assertEquals(5000, policy.eventDelayMillis(4));
        for (int i = 3; i < 6; i++) policy.record(i, 1, 0);
        assertEquals(30000, policy.eventDelayMillis(7));
        assertEquals(400, policy.eventDelayMillis(60006));
    }
    @Test public void persistentPermissionErrorsAreAlsoThrottled() {
        RepairPolicy policy = new RepairPolicy();
        for (int i = 0; i < 3; i++) policy.record(i, 0, 1);
        assertEquals(5000, policy.eventDelayMillis(4));
    }
    @Test public void observerUsesSparseWatchdogAndFallbackKeepsConfiguredInterval() {
        GuardConfig balanced = new GuardConfig(Set.of("com.zing.zalo"), false, 120);
        GuardConfig enhanced = new GuardConfig(Set.of("com.zing.zalo"), true, 120);
        assertEquals(900, RepairPolicy.watchdogSeconds(true, balanced));
        assertEquals(120, RepairPolicy.watchdogSeconds(false, balanced));
        assertEquals(120, RepairPolicy.watchdogSeconds(true, enhanced));
    }
}
