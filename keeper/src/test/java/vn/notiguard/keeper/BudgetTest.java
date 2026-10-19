package vn.notiguard.keeper;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class BudgetTest {
    @Test public void blocksOnlyAfterMaxRepairsInsideWindow() {
        long now = 5_000_000;
        List<Long> stamps = new ArrayList<>();
        for (int i = 0; i < Budget.MAX - 1; i++) stamps.add(now - i);
        assertTrue(Budget.allows(Budget.prune(stamps, now)));
        stamps.add(now - Budget.MAX);
        assertFalse(Budget.allows(Budget.prune(stamps, now)));
    }
    @Test public void forgetsStampsOutsideWindowAndFromAnEarlierBoot() {
        long now = 5_000_000;
        // elapsedRealtime restarts at boot, so a stamp larger than now belongs to a previous boot.
        assertEquals(Collections.singletonList(now - 1), Budget.prune(Arrays.asList(now - Budget.WINDOW_MS, now - 1, now + 5), now));
    }
    @Test public void roundTripsAndDropsCorruptStamps() {
        assertEquals(Arrays.asList(1L, 3L), Budget.decode("1,x,3,"));
        assertEquals("1,3", Budget.encode(Arrays.asList(1L, 3L)));
        assertTrue(Budget.decode(null).isEmpty());
    }
}
