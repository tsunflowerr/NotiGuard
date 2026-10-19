package vn.notiguard.keeper;
import org.junit.Test;
import static org.junit.Assert.*;

public class MilletListTest {
    @Test public void appendsGmsAndKeepsVendorTextVerbatim() {
        String vendor = "com.zing.zalo, com.VCB,com.google.android.gm";
        assertEquals(vendor + ", " + MilletList.GMS, MilletList.withGms(vendor));
    }
    @Test public void leavesListAloneWhenGmsPresentInAnySpacing() {
        assertNull(MilletList.withGms("com.zing.zalo,  com.google.android.gms ,com.VCB"));
    }
    @Test public void lookalikePackagesAreNotGms() {
        // A microG build and a GMS split package must not be mistaken for Google Play services.
        String vendor = "app.revanced.android.gms, com.google.android.gms.location.history";
        assertFalse(MilletList.hasGms(vendor));
        assertEquals(vendor + ", " + MilletList.GMS, MilletList.withGms(vendor));
    }
    @Test public void handlesMissingEmptyAndTrailingSeparators() {
        assertEquals(MilletList.GMS, MilletList.withGms(null));
        assertEquals(MilletList.GMS, MilletList.withGms("  "));
        assertEquals("com.zing.zalo, " + MilletList.GMS, MilletList.withGms("com.zing.zalo, ,"));
    }
    @Test public void reportsAppsThatDroppedOutOfTheList() {
        java.util.List<String> current = MilletList.packages("com.zing.zalo, com.google.android.gms");
        assertEquals(java.util.Arrays.asList("org.telegram.messenger"),
                MilletList.removed(java.util.Arrays.asList("com.zing.zalo", "org.telegram.messenger", MilletList.GMS), current));
    }
    @Test public void deletedListIsNotReportedAsEveryAppDemoted() {
        assertTrue(MilletList.removed(java.util.Arrays.asList("com.zing.zalo"), MilletList.packages(null)).isEmpty());
    }
    @Test public void repairIsIdempotent() {
        assertNull(MilletList.withGms(MilletList.withGms("com.zing.zalo")));
    }
}
