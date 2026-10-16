package vn.notiguard;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class XiaomiRulesTest {
    @Test public void milletPreservesOtherPackagesAndAvoidsDuplicates() {
        String original = "com.zing.zalo, com.google.android.gm";
        String added = XiaomiRules.addGms(original);
        assertEquals(original + ", " + GuardConfig.GMS, added);
        assertEquals(added, XiaomiRules.addGms(added));
        assertEquals(original, XiaomiRules.removeGms(added));
    }
    @Test public void overlayPreservesUnrelatedSectionsAndActions() {
        String original = "other:false#vendor/value;broadcastctrl:true#com.zing.zalo/SOME_ACTION;tail:7;";
        String merged = XiaomiRules.mergeAurogon(original, Set.of("com.zing.zalo"));
        assertTrue(merged.startsWith(original + ";"));
        assertTrue(merged.contains("com.zing.zalo/SOME_ACTION," + GuardConfig.FCM));
        assertEquals(merged, XiaomiRules.mergeAurogon(merged, Set.of("com.zing.zalo")));
        assertEquals(original, XiaomiRules.mergeAurogon(merged, Set.of()));
    }
    @Test public void doesNotOverrideVendorsDisabledBroadcastController() {
        String original = "broadcastctrl:false#com.vendor.app/ALL";
        assertEquals(original, XiaomiRules.mergeAurogon(original, Set.of("com.zing.zalo")));
    }
    @Test public void doesNotDuplicateExistingFcmPermission() {
        String original = "broadcastctrl:true#com.zing.zalo/ALL";
        assertEquals(original, XiaomiRules.mergeAurogon(original, Set.of("com.zing.zalo")));
    }
    @Test public void cannotRemoveAnotherManagersOverlay() {
        String original = "broadcastctrl:true#other.manager/__NOTIGUARD_AUROGON_V1__#com.vendor.app/ALL";
        String merged = XiaomiRules.mergeAurogon(original, Set.of("com.zing.zalo"));
        assertEquals(original, XiaomiRules.mergeAurogon(merged, Set.of()));
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsPackageCommandInjection() {
        new GuardConfig(Set.of("com.zing.zalo; reboot"), false, 120);
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsAggressivePollingInterval() {
        new GuardConfig(Set.of(), false, 1);
    }
}
