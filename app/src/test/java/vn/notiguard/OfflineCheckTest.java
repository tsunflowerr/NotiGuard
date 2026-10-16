package vn.notiguard;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class OfflineCheckTest {
    private OfflineCheck.Stage check(boolean monitoring, Boolean gms, Boolean dev, Boolean usb, Boolean wifi, int first, int boot, long since, long now) {
        return OfflineCheck.evaluate(monitoring, gms, dev, usb, wifi, first, boot, since, now);
    }
    @Test public void activeRepairCannotCountAsIndependentEvidence() {
        assertEquals(OfflineCheck.Stage.MONITORING, check(true, true, false, false, false, 2, 3, 100, 300000));
    }
    @Test public void unreadableFlagNeverCountsAsOff() {
        assertEquals(OfflineCheck.Stage.UNKNOWN, check(false, true, null, false, false, 2, 3, 100, 300000));
    }
    @Test public void missingGmsInvalidatesAPreviouslySuccessfulRebootSample() {
        assertEquals(OfflineCheck.Stage.GMS_MISSING, check(false, false, false, false, false, 2, 3, 100, 300000));
    }
    @Test public void wirelessDebuggingAlsoPreventsOffClaim() {
        assertEquals(OfflineCheck.Stage.DEBUG_ENABLED, check(false, true, false, false, true, 2, 3, 100, 300000));
    }
    @Test public void firstSampleDoesNotClaimPersistence() {
        assertEquals(OfflineCheck.Stage.FIRST_SAMPLE, check(false, true, false, false, false, -1, 3, -1, 300000));
    }
    @Test public void thresholdUsesMonotonicTime() {
        assertEquals(OfflineCheck.Stage.WAITING, check(false, true, false, false, false, 3, 3, 1000, 120999));
        assertEquals(OfflineCheck.Stage.TWO_SAMPLES, check(false, true, false, false, false, 3, 3, 1000, 121000));
    }
    @Test public void rebootCanResetUptimeWithoutDiscardingBootEvidence() {
        assertEquals(OfflineCheck.Stage.AFTER_REBOOT, check(false, true, false, false, false, 2, 3, 500000, 30000));
    }
    @Test public void backwardsCounterOrClockStartsFresh() {
        assertEquals(OfflineCheck.Stage.FIRST_SAMPLE, check(false, true, false, false, false, 4, 3, 100, 300000));
        assertEquals(OfflineCheck.Stage.FIRST_SAMPLE, check(false, true, false, false, false, 3, 3, 500000, 30000));
    }
    @Test public void unreadableBootCounterDoesNotClaimReboot() {
        assertEquals(OfflineCheck.Stage.UNKNOWN, check(false, true, false, false, false, 2, -1, 100, 300000));
    }
}
